package co.solventa.movil.features.auditoria.consentimientos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Como confirmo el titular la revocacion en el telefono (decision D-6 a). */
enum class MetodoDeConfirmacion(val codigo: String) {
    BIOMETRIA("biometric"),
    CONTRASENA("password"),
}

/** Resultado de pedir una revocacion (HU-M1.3-02). */
sealed interface ResultadoDeRevocacion {
    /** El servicio la registro: el consentimiento ya figura revocado (criterio 3). */
    data class Revocado(val revocacion: Revocacion) : ResultadoDeRevocacion

    /**
     * Sin red, sin respuesta en 4 s o con el servicio caido: quedo en la cola
     * local y se enviara sola con prioridad alta (criterio 5).
     */
    data object PendienteDeEnvio : ResultadoDeRevocacion

    /** Otra solicitud ya lo habia revocado; la copia se actualiza. */
    data object YaRevocado : ResultadoDeRevocacion

    /** El consentimiento ya no esta activo (vencio); la copia se actualiza. */
    data object YaNoActivo : ResultadoDeRevocacion

    /** No hay sesion: no se escribe nada. */
    data object SinSesion : ResultadoDeRevocacion

    /** El servicio rechazo la solicitud por otra causa; no queda en la cola. */
    data object Error : ResultadoDeRevocacion
}

/** Resultado de enviar la cola de revocaciones pendientes. */
sealed interface ResultadoDeEnvio {
    /** Se enviaron todas las que habia (puede ser ninguna). */
    data class Enviadas(val cantidad: Int) : ResultadoDeEnvio

    /** No hay red: la cola sigue igual y el trabajo se reintenta. */
    data object SinConexion : ResultadoDeEnvio

    /** Alguna fallo por el servicio (sesion vencida, 5xx): sigue en la cola. */
    data object ConFallas : ResultadoDeEnvio

    data object SinSesion : ResultadoDeEnvio
}

/**
 * Por que una revocacion de la cola no se pudo enviar y se quito (se muestra
 * con el aviso de `38:1503`, sin diseño nuevo).
 */
enum class MotivoDeDescarte { VENCIO, YA_REVOCADO }

/** Una revocacion de la cola que el servicio ya no acepto. */
data class AvisoDeRevocacion(val consentimientoId: String, val entidad: String, val motivo: MotivoDeDescarte)

/** Resultado de descargar el comprobante en PDF. */
sealed interface ResultadoDeComprobante {
    class Descargado(val bytes: ByteArray, val nombre: String) : ResultadoDeComprobante
    data object SinConexion : ResultadoDeComprobante
    data object Error : ResultadoDeComprobante
}

/**
 * Envia la cola de revocaciones pendientes. La sincronizacion de los
 * consentimientos de HU-M1.3-01 la llama primero, para que una revocacion en
 * cola salga antes que cualquier otra cosa (prioridad alta, criterio 5).
 */
fun interface EnviadorDeRevocaciones {
    suspend fun enviarPendientes(): ResultadoDeEnvio
}

/**
 * La revocacion de los consentimientos del titular de la sesion
 * (HU-M1.3-02). Room es la fuente de la verdad de la cola: cada revocacion se
 * escribe en el telefono **antes** de intentar enviarla, asi nada se pierde.
 */
interface RevocacionRepositorio : EnviadorDeRevocaciones {

    /** Si hay red: sin ella no se intenta enviar y se encola de una vez. */
    val conectado: StateFlow<Boolean>

    /** Las revocaciones del titular que todavia no llegan al servicio. */
    fun pendientes(): Flow<List<RevocacionPendiente>>

    /** Revocaciones de la cola que el servicio ya no acepto, para avisarlas. */
    val avisos: StateFlow<List<AvisoDeRevocacion>>

    fun descartarAviso(consentimientoId: String)

    /**
     * Revoca el consentimiento confirmado con el metodo dado. La clave de
     * idempotencia y la hora de la solicitud se fijan aqui, en el momento de
     * la confirmacion, y se conservan aunque el envio salga horas despues.
     */
    suspend fun revocar(consentimiento: Consentimiento, metodo: MetodoDeConfirmacion): ResultadoDeRevocacion

    /** El comprobante de la revocacion en PDF, en el idioma dado (`es` o `en`). */
    suspend fun comprobante(consentimientoId: String, idioma: String): ResultadoDeComprobante

    /** Cuantas revocaciones del titular siguen en la cola (aviso al cerrar sesion, D-7 a). */
    suspend fun cuantasPendientes(): Int
}
