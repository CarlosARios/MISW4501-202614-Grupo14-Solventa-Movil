package co.solventa.movil.core.api.auditoria

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Respuesta de `GET /v1/consents`: la lista completa, sin paginacion. */
@Serializable
data class ListaDeConsentimientosDto(
    @SerialName("items") val elementos: List<ConsentimientoDto> = emptyList(),
    @SerialName("generated_at") val generadoEn: String? = null,
)

/**
 * Un consentimiento tal como lo ve su titular. Las fechas viajan en RFC 3339
 * (UTC) y el mapeador del dominio las convierte; aqui quedan como texto para
 * que la red no dependa de la zona del dispositivo.
 */
@Serializable
data class ConsentimientoDto(
    @SerialName("consent_id") val id: String,
    @SerialName("status") val estado: String,
    @SerialName("current") val vigente: Boolean,
    @SerialName("entities") val entidades: List<EntidadDto> = emptyList(),
    @SerialName("purpose") val finalidad: String,
    @SerialName("channel") val canal: String,
    @SerialName("terms_version") val versionDeTerminos: String,
    @SerialName("scope") val alcance: List<FuenteDto> = emptyList(),
    @SerialName("granted_at") val otorgadoEn: String,
    @SerialName("expires_at") val venceEn: String,
    @SerialName("ended_at") val finalizadoEn: String? = null,
    @SerialName("revocation_receipt_id") val comprobanteDeRevocacion: String? = null,
    @SerialName("history") val historial: List<CambioDeEstadoDto> = emptyList(),
    /** Solo un activo se puede revocar (HU-M1.3-02, criterio 6). */
    @SerialName("revocable") val revocable: Boolean = false,
    /** La revocacion que lo termino, con el aviso a cada entidad; null si no fue revocado. */
    @SerialName("revocation") val revocacion: RevocacionDto? = null,
)

/** Entidad financiera autorizada: el aliado y su nombre visible. */
@Serializable
data class EntidadDto(
    @SerialName("code") val codigo: String,
    @SerialName("name") val nombre: String,
)

/** Una fuente del alcance: que datos lee y por cuanto tiempo. */
@Serializable
data class FuenteDto(
    @SerialName("code") val codigo: String,
    @SerialName("data_categories") val categorias: List<String> = emptyList(),
    @SerialName("validity") val vigencia: String,
)

/** Un cambio de estado del historial, con su fecha y hora. */
@Serializable
data class CambioDeEstadoDto(
    @SerialName("status") val estado: String,
    @SerialName("occurred_at") val ocurrioEn: String,
    @SerialName("receipt_id") val comprobante: String? = null,
    @SerialName("channel") val canal: String? = null,
    @SerialName("entity_code") val entidad: String? = null,
)

/** Cuerpo de error del servicio de auditoria. */
@Serializable
data class ErrorDeAuditoriaDto(
    @SerialName("error") val error: String,
    @SerialName("message") val mensaje: String? = null,
)
