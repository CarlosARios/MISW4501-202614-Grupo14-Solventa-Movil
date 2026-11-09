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
 * `GET /v1/claims/{id}` y `GET /v1/claims/{id}/history` del API Gateway
 * (HU-M2.4-01, HA-L02).
 *
 * - Sin red, [ResultadoDeConsulta.SinConexion].
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
    private val reloj: () -> Long,
) : ConsultaDeSiniestrosRepositorio {

    @Inject
    constructor(api: SiniestrosApi) : this(api, System::currentTimeMillis)

    override suspend fun listar(): ResultadoDeConsulta<List<Siniestro>> = consultar {
        val cuerpo = cuerpoDe(api.siniestros())
        ResultadoDeConsulta.Exito(
            cuerpo.elementos.map(MapeadorDeSiniestros::desdeResumen),
            MapeadorDeSiniestros.frescura(cuerpo.desdeCache, cuerpo.leidoEn, reloj()),
        )
    }

    override suspend fun detalle(id: String): ResultadoDeConsulta<DetalleDeSiniestro> = consultar {
        val (respuestaDetalle, respuestaHistorial) = coroutineScope {
            val detalle = async { api.siniestro(id) }
            val historial = async { api.historial(id) }
            detalle.await() to historial.await()
        }
        val detalle = cuerpoDe(respuestaDetalle)
        val historial = cuerpoDe(respuestaHistorial)
        val ahora = reloj()
        ResultadoDeConsulta.Exito(
            DetalleDeSiniestro(MapeadorDeSiniestros.desdeDetalle(detalle), MapeadorDeSiniestros.cambios(historial.cambios)),
            Frescura.combinar(
                MapeadorDeSiniestros.frescura(detalle.desdeCache, detalle.leidoEn, ahora),
                MapeadorDeSiniestros.frescura(historial.desdeCache, historial.leidoEn, ahora),
            ),
        )
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
