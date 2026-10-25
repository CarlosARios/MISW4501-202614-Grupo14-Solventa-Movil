package co.solventa.movil.core.api.auditoria

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cuerpo de `POST /v1/consents/{id}/revocations` (HU-M1.3-02). La fecha es la
 * de la confirmacion en el telefono, en RFC 3339 con su desfase: se conserva
 * aunque la solicitud salga horas despues de la cola sin conexion
 * (criterio 5).
 */
@Serializable
data class SolicitudDeRevocacionDto(
    @SerialName("requested_at") val solicitadaEn: String,
    @SerialName("device") val dispositivo: String,
    @SerialName("confirmation_method") val metodoDeConfirmacion: String,
)

/**
 * La revocacion de un consentimiento como la ve su titular: el comprobante,
 * la hora de la solicitud y la hora en que termino la autorizacion, y el
 * aviso a cada entidad (decision D-1 a: «Revocado» desde que se registra).
 */
@Serializable
data class RevocacionDto(
    @SerialName("consent_id") val consentimientoId: String,
    @SerialName("receipt_number") val comprobante: String,
    @SerialName("status") val estado: String,
    @SerialName("requested_at") val solicitadaEn: String,
    @SerialName("effective_at") val efectivaEn: String,
    @SerialName("channel") val canal: String,
    @SerialName("device") val dispositivo: String? = null,
    @SerialName("notice_status") val estadoDelAviso: String,
    @SerialName("entities") val entidades: List<EntidadDeRevocacionDto> = emptyList(),
)

/** El aviso de la revocacion a una entidad financiera. */
@Serializable
data class EntidadDeRevocacionDto(
    @SerialName("code") val codigo: String,
    @SerialName("name") val nombre: String,
    @SerialName("status") val estado: String,
    @SerialName("confirmed_at") val confirmadaEn: String? = null,
    @SerialName("deadline_at") val plazo: String? = null,
)
