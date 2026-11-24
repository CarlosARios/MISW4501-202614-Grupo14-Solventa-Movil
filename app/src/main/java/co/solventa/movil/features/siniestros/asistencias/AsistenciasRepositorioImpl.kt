package co.solventa.movil.features.siniestros.asistencias

import android.util.Log
import co.solventa.movil.core.api.siniestros.ErrorDeAsistenciaDto
import co.solventa.movil.core.api.siniestros.RespuestaDeAsistenciaDto
import co.solventa.movil.core.api.siniestros.SiniestrosApi
import co.solventa.movil.core.errores.Correlacion
import co.solventa.movil.core.persistencia.cifrado.CifradorDeDatos
import co.solventa.movil.core.persistencia.dao.SolicitudDeAsistenciaDao
import co.solventa.movil.core.persistencia.entidades.SolicitudDeAsistenciaEntidad
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.features.siniestros.UsuarioActual
import co.solventa.movil.features.siniestros.radicacion.PolizaElegible
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AsistenciasRepositorio] sobre el servicio de siniestros, la cola cifrada
 * en Room y WorkManager (HU-M2.3-01).
 *
 * - **Primero el teléfono (D2):** la fila se escribe y el envío se programa
 *   antes de que la pantalla abra el marcador. La llamada nunca espera al
 *   servicio (criterio 3).
 * - **La misma clave siempre:** cada reintento reusa la clave y el mismo
 *   cuerpo; por eso la dirección se resuelve una sola vez, antes del primer
 *   envío (D7).
 * - **Respuestas (S5):** `201`/`200` confirman; `503`, otros `5xx`, `401`,
 *   `404` (la ruta aún no desplegada) y la falta de red se reintentan;
 *   `422 policy_not_eligible` se descarta con su motivo; el resto de `422`,
 *   `400` y `403` son errores de la aplicación y se descartan dejando la
 *   correlación en el registro.
 * - **Vencimiento (S7):** la que no sale en 60 minutos se descarta.
 * - **Sin datos de ubicación en los registros:** solo el id local, el código
 *   y la correlación.
 */
@Singleton
class AsistenciasRepositorioImpl internal constructor(
    private val api: SiniestrosApi,
    private val dao: SolicitudDeAsistenciaDao,
    private val cifrador: CifradorDeDatos,
    private val usuario: UsuarioActual,
    conectividad: ObservadorDeConectividad,
    private val programador: ProgramadorDeEnvioDeAsistencias,
    private val traductor: TraductorDeCoordenadas,
    private val avisador: AvisadorDeAsistencia,
    private val reloj: () -> Long,
    private val nuevaClave: () -> String,
    private val registrar: (String) -> Unit,
) : AsistenciasRepositorio {

    @Inject
    constructor(
        api: SiniestrosApi,
        dao: SolicitudDeAsistenciaDao,
        cifrador: CifradorDeDatos,
        usuario: UsuarioActual,
        conectividad: ObservadorDeConectividad,
        programador: ProgramadorDeEnvioDeAsistencias,
        traductor: TraductorDeCoordenadas,
        avisador: AvisadorDeAsistencia,
    ) : this(
        api, dao, cifrador, usuario, conectividad, programador, traductor, avisador,
        reloj = System::currentTimeMillis,
        nuevaClave = { UUID.randomUUID().toString() },
        registrar = { Log.w(ETIQUETA, it) },
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var visible = false

    override val conectado: StateFlow<Boolean> = conectividad.conectado

    override fun pantallaVisible(visible: Boolean) {
        this.visible = visible
    }

    override suspend fun solicitar(poliza: PolizaElegible, resultado: ResultadoDeUbicacion): Long? {
        val sub = usuario.sub()
        if (sub.isEmpty()) return null
        val carga = CargaDeAsistencia.de(poliza, resultado)
        val id = dao.insertar(
            SolicitudDeAsistenciaEntidad(
                propietario = sub,
                clave = nuevaClave(),
                cargaCifrada = cifrador.cifrar(json.encodeToString(carga)),
                estadoDeEnvio = SolicitudDeAsistenciaEntidad.EN_COLA,
                creadaEn = reloj(),
            ),
        )
        programador.programar()
        return id
    }

    override fun observarUltima(): Flow<SolicitudDeAsistencia?> = flow {
        val sub = usuario.sub()
        if (sub.isEmpty()) {
            emit(null)
            return@flow
        }
        emitAll(dao.ultima(sub).map { fila -> fila?.let(::aModelo) })
    }

    override suspend fun reanudarPendientes() {
        val sub = usuario.sub()
        if (sub.isNotEmpty() && dao.pendientes(sub).isNotEmpty()) programador.programar()
    }

    override suspend fun enviarPendientes(): ResultadoDelEnvio {
        val sub = usuario.sub()
        if (sub.isEmpty()) return ResultadoDelEnvio.COMPLETO
        dao.borrarResueltasAntesDe(reloj() - ReglasDeAsistencia.CONSERVACION_MS)
        var reintentar = false
        for (pendiente in dao.pendientes(sub)) {
            val carga = cargaDe(pendiente)
            if (carga == null) {
                // Sin la clave del Keystore la carga no se puede leer: no hay
                // nada que enviar.
                descartar(pendiente, MotivoDeDescarte.RECHAZADA)
                continue
            }
            if (ReglasDeAsistencia.vencida(pendiente.creadaEn, reloj())) {
                descartar(pendiente, MotivoDeDescarte.VENCIDA)
                continue
            }
            when (enviar(conLugar(pendiente, carga), carga)) {
                Envio.SIN_RED -> return ResultadoDelEnvio.REINTENTAR
                Envio.REINTENTAR -> reintentar = true
                Envio.RESUELTO -> Unit
            }
        }
        return if (reintentar) ResultadoDelEnvio.REINTENTAR else ResultadoDelEnvio.COMPLETO
    }

    override suspend fun consultarEstado(requestId: String): EstadoDeLaSolicitud? {
        val sub = usuario.sub()
        if (sub.isEmpty()) return null
        val respuesta = try {
            api.solicitudDeAsistencia(requestId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            return null
        } catch (_: RuntimeException) {
            return null
        }
        val cuerpo = respuesta.body()
        if (!respuesta.isSuccessful || cuerpo == null) return null
        dao.porRequestId(sub, requestId)?.let { dao.actualizar(it.copy(estadoRemoto = cuerpo.estado)) }
        return EstadoDeLaSolicitud.desdeCodigo(cuerpo.estado)
    }

    private enum class Envio { RESUELTO, REINTENTAR, SIN_RED }

    private suspend fun enviar(fila: SolicitudDeAsistenciaEntidad, carga: CargaDeAsistencia): Envio {
        val correlacion = Correlacion.nuevo()
        val cuerpo = ReglasDeAsistencia.aCuerpo(carga.poliza, carga.resultado(), fila.lugarCifrado?.let(cifrador::descifrar))
        val respuesta = try {
            api.solicitarAsistencia(fila.clave, correlacion, cuerpo)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            dao.actualizar(fila.copy(intentos = fila.intentos + 1))
            return Envio.SIN_RED
        } catch (_: RuntimeException) {
            // Una respuesta que no se puede leer: con la misma clave, el
            // reintento devuelve la solicitud que ya exista.
            dao.actualizar(fila.copy(intentos = fila.intentos + 1))
            return Envio.REINTENTAR
        }
        val confirmada = respuesta.body()
        val codigo = respuesta.code()
        return when {
            respuesta.isSuccessful && confirmada != null -> {
                confirmar(fila, confirmada)
                Envio.RESUELTO
            }
            codigo == NO_PROCESABLE -> {
                val error = errorDe(respuesta)
                if (error == POLIZA_NO_ELEGIBLE) {
                    descartar(fila, MotivoDeDescarte.POLIZA_NO_ACTIVA)
                } else {
                    registrar("asistencia=${fila.id} codigo=$codigo error=$error correlacion=$correlacion")
                    descartar(fila, MotivoDeDescarte.RECHAZADA)
                }
                Envio.RESUELTO
            }
            codigo in CODIGOS_DEFINITIVOS -> {
                registrar("asistencia=${fila.id} codigo=$codigo correlacion=$correlacion")
                descartar(fila, MotivoDeDescarte.RECHAZADA)
                Envio.RESUELTO
            }
            else -> {
                dao.actualizar(fila.copy(intentos = fila.intentos + 1))
                Envio.REINTENTAR
            }
        }
    }

    private suspend fun confirmar(fila: SolicitudDeAsistenciaEntidad, respuesta: RespuestaDeAsistenciaDto) {
        dao.actualizar(
            fila.copy(
                estadoDeEnvio = SolicitudDeAsistenciaEntidad.CONFIRMADA,
                intentos = fila.intentos + 1,
                requestId = respuesta.id,
                numero = respuesta.numero,
                registradaEn = runCatching { Instant.parse(respuesta.registradaEn).toEpochMilli() }.getOrElse { reloj() },
                estadoRemoto = respuesta.estado,
            ),
        )
    }

    private suspend fun descartar(fila: SolicitudDeAsistenciaEntidad, motivo: MotivoDeDescarte) {
        dao.actualizar(fila.copy(estadoDeEnvio = SolicitudDeAsistenciaEntidad.DESCARTADA, motivoDeDescarte = motivo.codigo))
        if (!visible) avisador.noRegistrada(motivo)
    }

    /**
     * D7: la dirección se resuelve una sola vez, antes del primer envío, y se
     * guarda cifrada. Sin ubicación compartida no hay nada que traducir.
     */
    private suspend fun conLugar(fila: SolicitudDeAsistenciaEntidad, carga: CargaDeAsistencia): SolicitudDeAsistenciaEntidad {
        if (fila.geocodificada) return fila
        val resultado = carga.resultado()
        val lugar = if (resultado is ResultadoDeUbicacion.Compartida) {
            try {
                ReglasDeAsistencia.lugarValido(traductor.traducir(resultado.ubicacion.latitud, resultado.ubicacion.longitud))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
        val actualizada = fila.copy(lugarCifrado = lugar?.let(cifrador::cifrar), geocodificada = true)
        dao.actualizar(actualizada)
        return actualizada
    }

    private fun aModelo(fila: SolicitudDeAsistenciaEntidad): SolicitudDeAsistencia? {
        val carga = cargaDe(fila) ?: return null
        return SolicitudDeAsistencia(
            id = fila.id,
            poliza = carga.poliza,
            etiquetaDePoliza = carga.etiqueta,
            resultado = carga.resultado(),
            envio = when (fila.estadoDeEnvio) {
                SolicitudDeAsistenciaEntidad.CONFIRMADA -> EstadoDeEnvio.Confirmada
                SolicitudDeAsistenciaEntidad.DESCARTADA -> EstadoDeEnvio.Descartada(MotivoDeDescarte.desdeCodigo(fila.motivoDeDescarte))
                else -> EstadoDeEnvio.EnCola
            },
            creadaEn = fila.creadaEn,
            lugar = fila.lugarCifrado?.let(cifrador::descifrar),
            requestId = fila.requestId,
            numero = fila.numero,
            registradaEn = fila.registradaEn,
            estado = fila.estadoRemoto?.let(EstadoDeLaSolicitud::desdeCodigo),
        )
    }

    private fun cargaDe(fila: SolicitudDeAsistenciaEntidad): CargaDeAsistencia? =
        cifrador.descifrar(fila.cargaCifrada)?.let { claro ->
            runCatching { json.decodeFromString<CargaDeAsistencia>(claro) }.getOrNull()
        }

    private fun errorDe(respuesta: Response<*>): String? = respuesta.errorBody()?.string()?.let { cuerpo ->
        runCatching { json.decodeFromString<ErrorDeAsistenciaDto>(cuerpo).error }.getOrNull()
    }

    companion object {
        /** Nombre único del trabajo urgente de envío. */
        const val TRABAJO = "envio-asistencias"

        private const val ETIQUETA = "SolventaAsistencia"
        private const val NO_PROCESABLE = 422
        private const val POLIZA_NO_ELEGIBLE = "policy_not_eligible"

        /** Reintentar no los arregla: `400 invalid_request` y `403 forbidden`. */
        private val CODIGOS_DEFINITIVOS = setOf(400, 403)
    }
}

/**
 * Lo que se guarda cifrado de cada solicitud (D8): la póliza y el resultado de
 * la ubicación tal como se leyó al tocar.
 */
@Serializable
internal data class CargaDeAsistencia(
    @SerialName("poliza") val poliza: String,
    @SerialName("etiqueta") val etiqueta: String,
    @SerialName("latitud") val latitud: Double? = null,
    @SerialName("longitud") val longitud: Double? = null,
    @SerialName("precision_m") val precisionM: Float? = null,
    @SerialName("leida_en") val leidaEn: Long? = null,
    @SerialName("motivo") val motivo: String? = null,
) {
    fun resultado(): ResultadoDeUbicacion =
        if (latitud != null && longitud != null && precisionM != null && leidaEn != null) {
            ResultadoDeUbicacion.Compartida(Ubicacion(latitud, longitud, altitud = null, precisionM = precisionM, leidaEn = leidaEn))
        } else {
            ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.desdeCodigo(motivo) ?: MotivoSinUbicacion.SIN_LECTURA)
        }

    companion object {
        fun de(poliza: PolizaElegible, resultado: ResultadoDeUbicacion): CargaDeAsistencia = when (resultado) {
            is ResultadoDeUbicacion.Compartida -> CargaDeAsistencia(
                poliza = poliza.id,
                etiqueta = poliza.etiqueta,
                latitud = resultado.ubicacion.latitud,
                longitud = resultado.ubicacion.longitud,
                precisionM = resultado.ubicacion.precisionM,
                leidaEn = resultado.ubicacion.leidaEn,
            )
            is ResultadoDeUbicacion.NoDisponible -> CargaDeAsistencia(
                poliza = poliza.id,
                etiqueta = poliza.etiqueta,
                motivo = resultado.motivo.codigo,
            )
        }
    }
}
