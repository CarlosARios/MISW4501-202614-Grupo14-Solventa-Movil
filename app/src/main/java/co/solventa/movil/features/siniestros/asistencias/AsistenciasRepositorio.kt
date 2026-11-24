package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.features.siniestros.radicacion.PolizaElegible
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Lo que el trabajador hace después de recorrer la cola. */
enum class ResultadoDelEnvio {
    /** No queda nada por reintentar (puede haber confirmadas y descartadas). */
    COMPLETO,

    /** Sin red o con el servicio caído: se reintenta con la misma clave. */
    REINTENTAR,
}

/**
 * Las solicitudes de asistencia en sitio del asegurado con sesión
 * (HU-M2.3-01). Room es la fuente de la verdad: cada solicitud se escribe en
 * el teléfono antes de abrir el marcador y la envía un trabajo urgente (D2).
 */
interface AsistenciasRepositorio {

    /** Si hay red: decide entre «Enviando tu ubicación…» y «Sin conexión de datos…» (D10). */
    val conectado: StateFlow<Boolean>

    /**
     * Escribe la solicitud cifrada en la cola con una clave nueva y programa
     * su envío. **No espera a la red** (criterio 3, D4). Devuelve el id local,
     * o `null` sin sesión (S9).
     */
    suspend fun solicitar(poliza: PolizaElegible, resultado: ResultadoDeUbicacion): Long?

    /** La solicitud más reciente del asegurado, para M14 (D9). */
    fun observarUltima(): Flow<SolicitudDeAsistencia?>

    /** Vuelve a programar el envío si quedó algo en la cola del asegurado (S9). */
    suspend fun reanudarPendientes()

    /** Lo llama el trabajador: geocodifica una vez, envía, interpreta (S5) y vence (S7). */
    suspend fun enviarPendientes(): ResultadoDelEnvio

    /** El estado actual de una solicitud confirmada (D11), o `null` si no se pudo leer. */
    suspend fun consultarEstado(requestId: String): EstadoDeLaSolicitud?

    /**
     * Si M14 está a la vista: una solicitud descartada se avisa en la pantalla
     * y no con una notificación del sistema (S6).
     */
    fun pantallaVisible(visible: Boolean)
}
