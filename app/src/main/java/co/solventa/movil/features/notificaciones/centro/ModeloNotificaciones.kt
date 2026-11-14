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
    /** La alerta, para una notificación de vencimiento de póliza (HU-M3.2-01). */
    val vencimiento: AlertaDeVencimiento? = null,
) {
    val leida: Boolean get() = leidaEn != null

    /**
     * A dónde lleva tocarla: el detalle del pago (M10b); la de un cambio de
     * etapa, la línea de tiempo del siniestro (M10, HU-M3.3-01 criterio 5), y
     * la de un vencimiento, el detalle de la póliza (M5, HU-M3.2-01).
     */
    val destino: DestinoDeNotificacion
        get() = etapa?.let { DestinoDeNotificacion.Siniestro(it.siniestroId, id) }
            ?: vencimiento?.let { DestinoDeNotificacion.Poliza(it.poliza, id) }
            ?: DestinoDeNotificacion.Pago(id)
}

/**
 * La alerta de que una póliza está por vencer (HU-M3.2-01): la envía el
 * servicio a 30, 15 y 5 días del fin de la vigencia, una vez por hito y nunca
 * si la póliza ya se renovó (criterios 1 a 3). Dice la fecha exacta del
 * vencimiento (criterio 1) y los días que faltaban cuando se envió.
 */
data class AlertaDeVencimiento(
    /** El número de la póliza, con el que se abre su detalle. */
    val poliza: String,
    /** Uno de los tipos de seguro del contrato: `life`, `auto`, `home`, `travel`. */
    val linea: String,
    /** El nombre del producto que da el núcleo («Autos»), si lo da. */
    val producto: String?,
    /** Lo asegurado: la placa, la dirección…, si lo hay. */
    val objetoAsegurado: String?,
    /** El fin de la vigencia, en milisegundos desde época. */
    val venceEn: Long,
    /** El hito que se avisa: 30, 15 o 5 días. */
    val hito: Int,
    /** Los días que faltaban al enviarla. */
    val diasRestantes: Int,
)

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

    /**
     * El detalle de la póliza (M5) de una alerta de vencimiento (HU-M3.2-01);
     * [notificacionId] se marca leída al abrirla. Cuando exista la renovación
     * desde la alerta (M17, HU-M3.2-02) este destino pasa a ser esa pantalla.
     */
    data class Poliza(val polizaId: String, val notificacionId: String) : DestinoDeNotificacion
}

/**
 * La notificación que hay que marcar leída al abrir [destino]: la de los
 * destinos que no son pantallas de notificaciones (la línea de tiempo y el
 * detalle de la póliza). El detalle del pago (M10b) la marca solo.
 */
internal fun notificacionPorMarcar(destino: DestinoDeNotificacion): String? = when (destino) {
    is DestinoDeNotificacion.Pago -> null
    is DestinoDeNotificacion.Siniestro -> destino.notificacionId
    is DestinoDeNotificacion.Poliza -> destino.notificacionId
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
    const val VENCIMIENTO_DE_POLIZA = "policy_expiry"
}

/** Los tipos de seguro que la alerta de vencimiento sabe nombrar (HU-M3.2-01). */
object LineasDeVencimiento {
    const val VIDA = "life"
    const val AUTOS = "auto"
    const val HOGAR = "home"
    const val VIAJE = "travel"
}

/** Los parámetros y unidades que la aplicación sabe nombrar. */
object ParametrosDePago {
    const val RETRASO_DE_VUELO = "flight_delay"
    const val MINUTOS = "minutes"
}
