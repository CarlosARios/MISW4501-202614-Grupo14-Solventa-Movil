package co.solventa.movil.core.api.notificaciones

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Respuesta de `GET /v1/notifications` (HU-M3.1-01). */
@Serializable
data class ListaDeNotificacionesDto(
    @SerialName("items") val elementos: List<NotificacionDto> = emptyList(),
    @SerialName("unread_count") val sinLeer: Int = 0,
)

/**
 * Una notificación del centro. Las fechas viajan en RFC 3339 (UTC) y el
 * mapeador del dominio las convierte. Los textos los escribe la aplicación en
 * su idioma a partir de estos datos.
 */
@Serializable
data class NotificacionDto(
    @SerialName("notification_id") val id: String,
    /** `parametric_payout`, `claim_stage_changed` o `policy_expiry`; un tipo que la aplicación no conoce se ignora. */
    @SerialName("kind") val tipo: String,
    @SerialName("occurred_at") val ocurrioEn: String,
    @SerialName("created_at") val creadaEn: String,
    @SerialName("read_at") val leidaEn: String? = null,
    @SerialName("payout") val pago: PagoParametricoDto? = null,
    @SerialName("stage") val etapa: CambioDeEtapaDto? = null,
    @SerialName("expiry") val vencimiento: AlertaDeVencimientoDto? = null,
)

/**
 * La alerta de vencimiento de una póliza (HU-M3.2-01): a 30, 15 o 5 días del
 * fin de la vigencia. `coverage_end` es la fecha exacta del vencimiento;
 * `days_left`, los días que faltaban al enviarla.
 */
@Serializable
data class AlertaDeVencimientoDto(
    @SerialName("policy_id") val poliza: String,
    /** `life`, `auto`, `home` o `travel`. */
    @SerialName("line") val linea: String,
    @SerialName("product") val producto: String? = null,
    @SerialName("insured_object") val objetoAsegurado: String? = null,
    @SerialName("coverage_end") val venceEn: String,
    /** 30, 15 o 5. */
    @SerialName("milestone_days") val hito: Int,
    @SerialName("days_left") val diasRestantes: Int,
)

/**
 * El cambio de etapa de un siniestro (HU-M3.3-01): la etapa, el siniestro y
 * cuándo, y en la aprobación y el pago, el monto. `claim_id` es el id con el
 * que se abre la línea de tiempo; `filing_number`, el radicado que se muestra.
 */
@Serializable
data class CambioDeEtapaDto(
    @SerialName("change_id") val cambio: Long,
    @SerialName("claim_id") val siniestroId: String,
    @SerialName("filing_number") val radicado: String,
    @SerialName("policy_id") val poliza: String = "",
    /** `in_evaluation`, `approved` o `paid`. */
    @SerialName("stage") val etapa: String,
    @SerialName("changed_at") val cambioEn: String,
    /** En unidades de la moneda (pesos), ya sin el deducible. */
    @SerialName("amount") val monto: Double? = null,
    @SerialName("currency") val moneda: String? = null,
    @SerialName("paid_at") val pagadoEn: String? = null,
)

/** El pago paramétrico automático: lo que muestra M10b. */
@Serializable
data class PagoParametricoDto(
    @SerialName("payout_id") val id: String,
    @SerialName("claim_id") val siniestro: String,
    @SerialName("policy_id") val poliza: String,
    /** En unidades de la moneda (pesos). */
    @SerialName("amount") val monto: Double,
    @SerialName("currency") val moneda: String,
    @SerialName("trigger") val disparador: DisparadorDto,
    @SerialName("payment_method") val medioDePago: MedioDePagoDto,
    @SerialName("idempotency_key") val claveDeIdempotencia: String,
    @SerialName("paid_at") val pagadoEn: String,
)

/** El evento externo que superó el umbral de la póliza. */
@Serializable
data class DisparadorDto(
    /** `flight_delay`… */
    @SerialName("parameter") val parametro: String,
    /** El evento en pocas palabras: el vuelo `AV-8421`. */
    @SerialName("event_reference") val evento: String,
    @SerialName("threshold") val umbral: Double,
    @SerialName("observed") val observado: Double,
    /** `minutes`… */
    @SerialName("unit") val unidad: String,
    @SerialName("observed_at") val observadoEn: String? = null,
)

/** A dónde fue el pago, sin el número. */
@Serializable
data class MedioDePagoDto(
    @SerialName("brand") val marca: String,
    @SerialName("last4") val ultimos4: String,
)

/** Cuerpo de `PUT /v1/devices/{installation_id}`. */
@Serializable
data class DispositivoDto(
    @SerialName("platform") val plataforma: String = "android",
    @SerialName("token") val token: String? = null,
    @SerialName("push_enabled") val pushHabilitado: Boolean,
    @SerialName("locale") val idioma: String,
)
