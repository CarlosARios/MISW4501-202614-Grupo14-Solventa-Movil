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
    /** `parametric_payout`; un tipo que la aplicación no conoce se ignora. */
    @SerialName("kind") val tipo: String,
    @SerialName("occurred_at") val ocurrioEn: String,
    @SerialName("created_at") val creadaEn: String,
    @SerialName("read_at") val leidaEn: String? = null,
    @SerialName("payout") val pago: PagoParametricoDto? = null,
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
