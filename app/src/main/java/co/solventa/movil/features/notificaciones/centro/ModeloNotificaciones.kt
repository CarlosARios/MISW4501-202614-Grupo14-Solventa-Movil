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
) {
    val leida: Boolean get() = leidaEn != null
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
}

/** Los parámetros y unidades que la aplicación sabe nombrar. */
object ParametrosDePago {
    const val RETRASO_DE_VUELO = "flight_delay"
    const val MINUTOS = "minutes"
}
