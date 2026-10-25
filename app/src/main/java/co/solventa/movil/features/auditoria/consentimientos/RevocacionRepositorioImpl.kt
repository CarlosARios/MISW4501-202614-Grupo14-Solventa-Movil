package co.solventa.movil.features.auditoria.consentimientos

import android.os.Build
import co.solventa.movil.core.api.auditoria.AuditoriaApi
import co.solventa.movil.core.api.auditoria.ErrorDeAuditoriaDto
import co.solventa.movil.core.api.auditoria.RevocacionDto
import co.solventa.movil.core.api.auditoria.SolicitudDeRevocacionDto
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import co.solventa.movil.core.persistencia.dao.RevocacionPendienteDao
import co.solventa.movil.core.persistencia.entidades.RevocacionPendienteEntidad
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [RevocacionRepositorio] sobre el servicio de auditoria, la cola en Room y
 * WorkManager (HU-M1.3-02).
 *
 * - **Primero el telefono:** la fila de la cola se escribe antes de llamar al
 *   servicio (AGENTS.md del movil, §2.5). Si el envio funciona, se borra.
 * - **Con red se intenta en linea con un corte de 4 s**, para que la pantalla
 *   siempre se resuelva antes de los 5 s del criterio 3: revocado o pendiente.
 * - **Sin red, sin respuesta o con el servicio caido** queda pendiente y se
 *   programa el envio urgente, que corre al volver la red aunque la aplicacion
 *   este cerrada (criterio 5).
 * - **La misma clave siempre:** cada reintento reusa la clave y la hora de la
 *   confirmacion, asi el servicio nunca revoca dos veces y la fecha de la
 *   solicitud es la del momento en que el titular la confirmo.
 * - Todo va por el `sub` de la sesion: la cola de una cuenta nunca la envia
 *   otra, y al cerrar sesion **no** se borra (decision D-7 a).
 */
@Singleton
class RevocacionRepositorioImpl internal constructor(
    private val api: AuditoriaApi,
    private val cola: RevocacionPendienteDao,
    private val copia: ConsentimientoDao,
    private val sesion: SesionRepositorio,
    private val conectividad: ObservadorDeConectividad,
    private val programador: ProgramadorDeEnvioDeRevocaciones,
    private val reloj: () -> Long,
    private val zona: () -> ZoneId,
    private val dispositivo: () -> String,
    private val nuevaClave: () -> String,
) : RevocacionRepositorio {

    @Inject
    constructor(
        api: AuditoriaApi,
        cola: RevocacionPendienteDao,
        copia: ConsentimientoDao,
        sesion: SesionRepositorio,
        conectividad: ObservadorDeConectividad,
        programador: ProgramadorDeEnvioDeRevocaciones,
    ) : this(
        api, cola, copia, sesion, conectividad, programador,
        reloj = System::currentTimeMillis,
        zona = ZoneId::systemDefault,
        dispositivo = { "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(LARGO_MAXIMO_DEL_DISPOSITIVO) },
        nuevaClave = { UUID.randomUUID().toString() },
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val _avisos = MutableStateFlow<List<AvisoDeRevocacion>>(emptyList())
    override val avisos: StateFlow<List<AvisoDeRevocacion>> = _avisos.asStateFlow()

    override val conectado: StateFlow<Boolean> = conectividad.conectado

    override fun pendientes(): Flow<List<RevocacionPendiente>> = flow {
        val sub = sub()
        if (sub.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        emitAll(cola.observarDePropietario(sub).map { filas -> filas.map(MapeadorDeConsentimientos::pendienteDesdeEntidad) })
    }

    override fun descartarAviso(consentimientoId: String) {
        _avisos.update { lista -> lista.filterNot { it.consentimientoId == consentimientoId } }
    }

    override suspend fun cuantasPendientes(): Int = sub().takeIf { it.isNotEmpty() }?.let { cola.contar(it) } ?: 0

    override suspend fun revocar(consentimiento: Consentimiento, metodo: MetodoDeConfirmacion): ResultadoDeRevocacion {
        val sub = sub()
        if (sub.isEmpty()) return ResultadoDeRevocacion.SinSesion
        cola.encolar(
            RevocacionPendienteEntidad(
                propietario = sub,
                consentimientoId = consentimiento.id,
                clave = nuevaClave(),
                entidad = consentimiento.nombreDeEntidad,
                solicitadaEn = reloj(),
                zona = zona().id,
                dispositivo = dispositivo().ifBlank { DISPOSITIVO_DESCONOCIDO },
                metodo = metodo.codigo,
            ),
        )
        // Si ya habia una pendiente para este consentimiento, se envia esa:
        // su clave y su hora son las de la primera confirmacion.
        val fila = cola.una(sub, consentimiento.id) ?: return ResultadoDeRevocacion.Error
        if (!conectado.value) return encolada()
        val respuesta = try {
            withTimeout(CORTE_EN_LINEA_MS) { api.revocar(fila.consentimientoId, fila.clave, solicitud(fila)) }
        } catch (_: TimeoutCancellationException) {
            return encolada()
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            return encolada()
        } catch (_: RuntimeException) {
            return encolada()
        }
        return when (val resultado = procesar(sub, fila, respuesta, avisar = false)) {
            is Procesada.Registrada -> ResultadoDeRevocacion.Revocado(resultado.revocacion)
            Procesada.Vencida -> ResultadoDeRevocacion.YaNoActivo
            Procesada.YaRevocada -> ResultadoDeRevocacion.YaRevocado
            Procesada.Descartada -> ResultadoDeRevocacion.Error
            Procesada.Reintentar -> encolada()
        }
    }

    override suspend fun enviarPendientes(): ResultadoDeEnvio {
        val sub = sub()
        if (sub.isEmpty()) return ResultadoDeEnvio.SinSesion
        var enviadas = 0
        var fallas = 0
        for (fila in cola.dePropietario(sub)) {
            val respuesta = try {
                api.revocar(fila.consentimientoId, fila.clave, solicitud(fila))
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                cola.registrarIntento(sub, fila.consentimientoId, ERROR_SIN_RED)
                return ResultadoDeEnvio.SinConexion
            } catch (e: RuntimeException) {
                cola.registrarIntento(sub, fila.consentimientoId, e.javaClass.simpleName)
                fallas++
                continue
            }
            when (procesar(sub, fila, respuesta, avisar = true)) {
                Procesada.Reintentar -> fallas++
                else -> enviadas++
            }
        }
        return if (fallas > 0) ResultadoDeEnvio.ConFallas else ResultadoDeEnvio.Enviadas(enviadas)
    }

    override suspend fun comprobante(consentimientoId: String, idioma: String): ResultadoDeComprobante = try {
        val respuesta = api.comprobanteDeRevocacion(consentimientoId, idioma)
        val cuerpo = respuesta.body()
        if (respuesta.isSuccessful && cuerpo != null) {
            val nombre = respuesta.headers()["Content-Disposition"]?.let(::nombreDelArchivo) ?: "$consentimientoId.pdf"
            ResultadoDeComprobante.Descargado(cuerpo.bytes(), nombre)
        } else {
            ResultadoDeComprobante.Error
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        ResultadoDeComprobante.SinConexion
    } catch (_: RuntimeException) {
        ResultadoDeComprobante.Error
    }

    /** Lo que se hizo con la respuesta del servicio a una fila de la cola. */
    private sealed interface Procesada {
        data class Registrada(val revocacion: Revocacion) : Procesada
        data object Vencida : Procesada
        data object YaRevocada : Procesada
        data object Descartada : Procesada
        data object Reintentar : Procesada
    }

    private suspend fun procesar(
        sub: String,
        fila: RevocacionPendienteEntidad,
        respuesta: Response<RevocacionDto>,
        avisar: Boolean,
    ): Procesada {
        val cuerpo = respuesta.body()
        val codigo = respuesta.code()
        return when {
            respuesta.isSuccessful && cuerpo != null -> {
                val revocacion = MapeadorDeConsentimientos.revocacionDesdeDto(cuerpo)
                aplicarEnLaCopia(sub, fila.consentimientoId, revocacion)
                cola.borrar(sub, fila.consentimientoId)
                Procesada.Registrada(revocacion)
            }
            codigo == CODIGO_CONFLICTO -> {
                // El servicio ya no la acepta: se quita de la cola, se refresca
                // ese consentimiento y, si venia de la cola, se avisa.
                cola.borrar(sub, fila.consentimientoId)
                refrescar(sub, fila.consentimientoId)
                val vencio = codigoDeError(respuesta) == AuditoriaApi.ERROR_NO_ACTIVO
                if (avisar) {
                    val motivo = if (vencio) MotivoDeDescarte.VENCIO else MotivoDeDescarte.YA_REVOCADO
                    _avisos.update { it.filterNot { a -> a.consentimientoId == fila.consentimientoId } + AvisoDeRevocacion(fila.consentimientoId, fila.entidad, motivo) }
                }
                if (vencio) Procesada.Vencida else Procesada.YaRevocada
            }
            codigo in CODIGOS_DEFINITIVOS -> {
                cola.borrar(sub, fila.consentimientoId)
                Procesada.Descartada
            }
            else -> {
                cola.registrarIntento(sub, fila.consentimientoId, "http $codigo")
                Procesada.Reintentar
            }
        }
    }

    private fun encolada(): ResultadoDeRevocacion {
        programador.programar()
        return ResultadoDeRevocacion.PendienteDeEnvio
    }

    /** La copia local queda revocada en seguida con la respuesta del servicio. */
    private suspend fun aplicarEnLaCopia(sub: String, id: String, revocacion: Revocacion) {
        val fila = copia.uno(sub, id) ?: return
        val actualizado = MapeadorDeConsentimientos.conRevocacion(MapeadorDeConsentimientos.desdeEntidad(fila), revocacion)
        copia.guardar(MapeadorDeConsentimientos.aEntidad(actualizado, sub))
    }

    /** Trae del servicio un consentimiento y lo deja asi en la copia; sin red no hace nada. */
    private suspend fun refrescar(sub: String, id: String) {
        try {
            val respuesta = api.consentimiento(id)
            val cuerpo = respuesta.body()
            if (respuesta.isSuccessful && cuerpo != null) {
                copia.guardar(MapeadorDeConsentimientos.aEntidad(MapeadorDeConsentimientos.desdeDto(cuerpo), sub))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            // La siguiente sincronizacion lo corrige.
        } catch (_: RuntimeException) {
            // Igual.
        }
    }

    private fun solicitud(fila: RevocacionPendienteEntidad) = SolicitudDeRevocacionDto(
        solicitadaEn = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(fila.solicitadaEn).truncatedTo(ChronoUnit.MILLIS).atZone(zonaDe(fila.zona)),
        ),
        dispositivo = fila.dispositivo,
        metodoDeConfirmacion = fila.metodo,
    )

    private fun zonaDe(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.of("UTC"))

    private fun codigoDeError(respuesta: Response<*>): String? = respuesta.errorBody()?.string()?.let { cuerpo ->
        runCatching { json.decodeFromString<ErrorDeAuditoriaDto>(cuerpo).error }.getOrNull()
    }

    private suspend fun sub(): String = sesion.sesionGuardada()?.sub.orEmpty()

    companion object {
        /** Nombre unico del trabajo urgente de envio. */
        const val TRABAJO = "envio-revocaciones"

        /** Corte del intento en linea: la pantalla se resuelve antes de 5 s. */
        const val CORTE_EN_LINEA_MS = 4_000L

        /** El contrato acepta hasta 120 caracteres para el dispositivo. */
        private const val LARGO_MAXIMO_DEL_DISPOSITIVO = 120
        private const val DISPOSITIVO_DESCONOCIDO = "Android"
        private const val ERROR_SIN_RED = "sin red"
        private const val CODIGO_CONFLICTO = 409

        /** Respuestas que reintentar no arregla: la solicitud se quita de la cola. */
        private val CODIGOS_DEFINITIVOS = setOf(400, 404)

        private val NOMBRE = Regex("filename=\"?([^\";]+)\"?")

        internal fun nombreDelArchivo(disposicion: String): String? = NOMBRE.find(disposicion)?.groupValues?.get(1)
    }
}
