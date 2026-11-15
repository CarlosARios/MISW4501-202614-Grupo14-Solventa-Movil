package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.core.ubicacion.Ubicacion

/**
 * La línea de atención de Solventa (HU-M2.3-01, D4). Vive en la aplicación:
 * el servicio de siniestros no la conoce ni la devuelve. Es el dato del
 * prototipo (M14).
 */
object LineaDeAtencion {
    /** Como se le muestra al asegurado. */
    const val VISIBLE = "+57 601 745 0000"

    /** Como se le entrega al marcador (`tel:`), sin espacios. */
    const val MARCABLE = "+576017450000"
}

/**
 * Por qué el teléfono no pudo compartir la ubicación, con el código que el
 * contrato espera en `location.unavailable_reason`.
 */
enum class MotivoSinUbicacion(val codigo: String) {
    /** El asegurado no dio el permiso de ubicación (o cerró el diálogo sin darlo). */
    PERMISO_NEGADO("permission_denied"),

    /** El servicio de ubicación del teléfono está apagado. */
    UBICACION_APAGADA("location_disabled"),

    /** Hay permiso y servicio, pero no llegó una lectura a tiempo o la que hay es vieja. */
    SIN_LECTURA("no_fix"),
    ;

    companion object {
        fun desdeCodigo(codigo: String?): MotivoSinUbicacion? = entries.firstOrNull { it.codigo == codigo }
    }
}

/** Lo que el teléfono pudo decir de dónde está el asegurado (criterios 2 y 4). */
sealed interface ResultadoDeUbicacion {
    data class Compartida(val ubicacion: Ubicacion) : ResultadoDeUbicacion

    data class NoDisponible(val motivo: MotivoSinUbicacion) : ResultadoDeUbicacion
}

/** Por qué una solicitud de la cola no llegó al servicio y se dejó de intentar (S5, S7). */
enum class MotivoDeDescarte(val codigo: String) {
    /** `422 policy_not_eligible`: la póliza ya no es propia o no está vigente. */
    POLIZA_NO_ACTIVA("poliza_no_activa"),

    /** No salió en 60 minutos desde el toque (S7). */
    VENCIDA("vencida"),

    /** El servicio la rechazó por un error de la aplicación (`400`, `403`, `422`). */
    RECHAZADA("rechazada"),
    ;

    companion object {
        fun desdeCodigo(codigo: String?): MotivoDeDescarte = entries.firstOrNull { it.codigo == codigo } ?: RECHAZADA
    }
}

/**
 * Dónde va el envío de la solicitud al servicio. Mientras está en la cola, la
 * pantalla dice «Enviando tu ubicación…» o, sin datos, que se enviará en
 * cuanto haya señal (D10): la diferencia la da la conectividad, no la fila.
 */
sealed interface EstadoDeEnvio {
    data object EnCola : EstadoDeEnvio

    data object Confirmada : EstadoDeEnvio

    data class Descartada(val motivo: MotivoDeDescarte) : EstadoDeEnvio
}

/**
 * El estado de la solicitud en el servicio. `new` lo escribe esta historia; los
 * demás los escribe la bandeja de operaciones (HU-W2.3-03) y la aplicación los
 * acepta aunque no los produzca. Uno que no conozca no la hace fallar (D11).
 */
enum class EstadoDeLaSolicitud(val codigo: String?) {
    NUEVA("new"),
    EN_LLAMADA("in_call"),
    DESPACHADA("dispatched"),
    CERRADA("closed"),
    DESCONOCIDO(null),
    ;

    companion object {
        fun desdeCodigo(codigo: String?): EstadoDeLaSolicitud =
            entries.firstOrNull { it.codigo != null && it.codigo == codigo } ?: DESCONOCIDO
    }
}

/**
 * Una solicitud de asistencia tal como la guarda el teléfono (HU-M2.3-01).
 *
 * [id] es local; [requestId], [numero] (`AST-…`) y [registradaEn] llegan con la
 * confirmación del servicio. [registradaEn] es la hora en que el **servidor**
 * la registró (`requested_at`), en milisegundos desde la época.
 */
data class SolicitudDeAsistencia(
    val id: Long,
    val poliza: String,
    /** Lo que se lee en «Datos que enviamos»: «Autos · AU-2025-118204 · KJT-482». */
    val etiquetaDePoliza: String,
    val resultado: ResultadoDeUbicacion,
    val envio: EstadoDeEnvio,
    /** Instante del toque, en milisegundos desde la época. */
    val creadaEn: Long,
    /** La dirección legible que se resolvió en el teléfono (D7), si hubo. */
    val lugar: String? = null,
    val requestId: String? = null,
    val numero: String? = null,
    val registradaEn: Long? = null,
    val estado: EstadoDeLaSolicitud? = null,
)
