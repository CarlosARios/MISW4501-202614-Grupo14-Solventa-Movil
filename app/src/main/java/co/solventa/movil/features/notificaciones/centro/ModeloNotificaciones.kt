package co.solventa.movil.features.notificaciones.centro

/**
 * Una notificación del centro (HU-M3.1-01). Las fechas van en milisegundos
 * desde época; los montos, en unidades de la moneda.
 */
data class Notificacion(
    val id: String,
    /** Uno de [TiposDeNotificacion]. */
    val tipo: String,
    val ocurrioEn: Long,
    val leidaEn: Long?,
    /** El pago, para una notificación de pago paramétrico. */
    val pago: PagoParametrico?,
    /** El cambio de etapa, para una notificación de cambio de etapa (HU-M3.3-01). */
    val etapa: CambioDeEtapa? = null,
) {
    val leida: Boolean get() = leidaEn != null

    /**
     * A dónde lleva tocarla: el detalle del pago (M10b) o, la de un cambio de
     * etapa, la línea de tiempo del siniestro (M10, HU-M3.3-01 criterio 5).
     */
    val destino: DestinoDeNotificacion
        get() = etapa?.let { DestinoDeNotificacion.Siniestro(it.siniestroId, id) } ?: DestinoDeNotificacion.Pago(id)
}

/**
 * El cambio de etapa de un siniestro (HU-M3.3-01): lo que dicen la tarjeta del
 * centro y la notificación del sistema (criterio 5: la etapa y la fecha y hora)
 * y, en la aprobación y el pago, el monto (criterios 2 y 3).
 */
data class CambioDeEtapa(
    val cambio: Long,
    /** El id con el que se abre la línea de tiempo. */
    val siniestroId: String,
    /** `SIN-<año>-<cinco dígitos>`. */
    val radicado: String,
    val poliza: String,
    /** Uno de [EtapasDeSiniestro]. */
    val etapa: String,
    val cambioEn: Long,
    val monto: Double?,
    val moneda: String?,
    val pagadoEn: Long?,
)

/** Las etapas que avisan (HU-M3.3-01). */
object EtapasDeSiniestro {
    const val EN_EVALUACION = "in_evaluation"
    const val APROBADO = "approved"
    const val PAGADO = "paid"
}

/** A dónde lleva una notificación al tocarla. */
sealed interface DestinoDeNotificacion {
    /** El detalle del pago paramétrico (M10b). */
    data class Pago(val notificacionId: String) : DestinoDeNotificacion

    /** La línea de tiempo del siniestro (M10); [notificacionId] se marca leída al abrirla. */
    data class Siniestro(val siniestroId: String, val notificacionId: String) : DestinoDeNotificacion
}

/**
 * El pago paramétrico automático: lo que muestra M10b y lo que dice la
 * notificación (criterio 2: monto, moneda y el evento que lo originó).
 */
data class PagoParametrico(
    val id: String,
    val siniestro: String,
    val poliza: String,
    val monto: Double,
    val moneda: String,
    val disparador: Disparador,
    val medioDePago: MedioDePago,
    val claveDeIdempotencia: String,
    val pagadoEn: Long,
)

/** El evento externo que superó el umbral de la póliza. */
data class Disparador(
    /** `flight_delay`… */
    val parametro: String,
    /** El evento en pocas palabras: el vuelo `AV-8421`. */
    val evento: String,
    val umbral: Double,
    val observado: Double,
    /** `minutes`… */
    val unidad: String,
    val observadoEn: Long?,
)

data class MedioDePago(val marca: String, val ultimos4: String)

/** Los tipos de notificación que nombra el contrato. */
object TiposDeNotificacion {
    const val PAGO_PARAMETRICO = "parametric_payout"
    const val CAMBIO_DE_ETAPA = "claim_stage_changed"
}

/** Los parámetros y unidades que la aplicación sabe nombrar. */
object ParametrosDePago {
    const val RETRASO_DE_VUELO = "flight_delay"
    const val MINUTOS = "minutes"
}
