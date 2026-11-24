package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.core.api.siniestros.SiniestrosApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ConsultaDeSiniestrosRepositorio] contra `GET /v1/claims`,
 * `GET /v1/claims/{id}`, `GET /v1/claims/{id}/history` y, para un caso en
 * evaluación, `GET /v1/claims/{id}/triage` del API Gateway (HU-M2.4-01,
 * HA-L02).
 *
 * - Cada respuesta exitosa se guarda en [copia] con la hora de sus datos.
 * - Sin red, la última versión guardada ([ResultadoDeConsulta.CopiaLocal]); si
 *   no la hay, [ResultadoDeConsulta.SinConexion].
 * - El triage es un complemento: si falla o todavía no se calcula, el detalle
 *   llega sin estimado en lugar de fallar (el triage no pasa por la caché de
 *   HA-L02, así que con el servicio degradado puede no responder).
 * - `404`, [ResultadoDeConsulta.NoEncontrado]: un siniestro ajeno responde
 *   igual que uno que no existe.
 * - Cualquier otra respuesta que no sea `2xx`, o un cuerpo que no se puede
 *   leer, [ResultadoDeConsulta.Error].
 * - Un `200` con `stale: true` es un éxito: la pantalla lo muestra con el
 *   aviso de M9c y la hora de la versión guardada.
 */
@Singleton
class ConsultaDeSiniestrosRepositorioHttp internal constructor(
    private val api: SiniestrosApi,
    private val copia: CopiaLocalDeSiniestros = CopiaLocalDeSiniestros.Ninguna,
    private val reloj: () -> Long,
) : ConsultaDeSiniestrosRepositorio {

    @Inject
    constructor(api: SiniestrosApi, copia: CopiaLocalDeSiniestros) : this(api, copia, System::currentTimeMillis)

    override suspend fun listar(): ResultadoDeConsulta<List<Siniestro>> {
        val resultado = consultar {
            val cuerpo = cuerpoDe(api.siniestros())
            ResultadoDeConsulta.Exito(
                cuerpo.elementos.map(MapeadorDeSiniestros::desdeResumen),
                MapeadorDeSiniestros.frescura(cuerpo.desdeCache, cuerpo.leidoEn, reloj()),
            )
        }
        return when (resultado) {
            is ResultadoDeConsulta.Exito -> resultado.also { copia.guardarLista(it.valor, it.frescura.leidoEn) }
            ResultadoDeConsulta.SinConexion -> copia.lista()?.let { ResultadoDeConsulta.CopiaLocal(it.valor, it.guardadoEn) } ?: resultado
            else -> resultado
        }
    }

    override suspend fun detalle(id: String): ResultadoDeConsulta<DetalleDeSiniestro> {
        val resultado = consultarDetalle(id)
        return when (resultado) {
            is ResultadoDeConsulta.Exito -> resultado.also { copia.guardarDetalle(it.valor, it.frescura.leidoEn) }
            ResultadoDeConsulta.SinConexion -> copia.detalle(id)?.let { ResultadoDeConsulta.CopiaLocal(it.valor, it.guardadoEn) } ?: resultado
            else -> resultado
        }
    }

    private suspend fun consultarDetalle(id: String): ResultadoDeConsulta<DetalleDeSiniestro> = consultar {
        val (respuestaDetalle, respuestaHistorial) = coroutineScope {
            val detalle = async { api.siniestro(id) }
            val historial = async { api.historial(id) }
            detalle.await() to historial.await()
        }
        val detalle = cuerpoDe(respuestaDetalle)
        val historial = cuerpoDe(respuestaHistorial)
        val siniestro = MapeadorDeSiniestros.desdeDetalle(detalle)
        val estimacion = if (siniestro.estado == EstadoDeSiniestro.EN_EVALUACION) estimacionDe(id) else null
        val ahora = reloj()
        ResultadoDeConsulta.Exito(
            DetalleDeSiniestro(siniestro, MapeadorDeSiniestros.cambios(historial.cambios), estimacion),
            Frescura.combinar(
                MapeadorDeSiniestros.frescura(detalle.desdeCache, detalle.leidoEn, ahora),
                MapeadorDeSiniestros.frescura(historial.desdeCache, historial.leidoEn, ahora),
            ),
        )
    }

    /** El estimado del triage (criterio 2), o `null` si no lo hay o no se pudo leer. */
    private suspend fun estimacionDe(id: String): EstimacionDeResolucion? = try {
        api.triaje(id).body()?.let(MapeadorDeSiniestros::estimacion)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    /** Corre la consulta y traduce lo que falle. */
    private suspend fun <T> consultar(bloque: suspend () -> ResultadoDeConsulta<T>): ResultadoDeConsulta<T> = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: RespuestaInvalida) {
        e.resultado
    } catch (_: IOException) {
        ResultadoDeConsulta.SinConexion
    } catch (_: RuntimeException) {
        // Un cuerpo o una fecha que no se pueden leer.
        ResultadoDeConsulta.Error
    }

    /** El cuerpo de una respuesta exitosa; si no lo hay, corta la consulta con su resultado. */
    private fun <B> cuerpoDe(respuesta: Response<B>): B {
        val cuerpo = respuesta.body()
        if (respuesta.isSuccessful && cuerpo != null) return cuerpo
        throw RespuestaInvalida(
            if (respuesta.code() == CODIGO_NO_ENCONTRADO) ResultadoDeConsulta.NoEncontrado else ResultadoDeConsulta.Error,
        )
    }

    private class RespuestaInvalida(val resultado: ResultadoDeConsulta<Nothing>) : RuntimeException()

    private companion object {
        const val CODIGO_NO_ENCONTRADO = 404
    }
}
