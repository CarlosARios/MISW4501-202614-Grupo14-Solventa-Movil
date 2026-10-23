package co.solventa.movil.core.api.siniestros

import co.solventa.movil.core.errores.Correlacion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Radicación del siniestro en el servicio de siniestros (HU-M2.2-01). El
 * contrato está en `docs/contratos/HU-M2.2-01-api.md` del repositorio de
 * servicios.
 *
 * Lleva la sesión del asegurado: el caso queda a nombre del `sub` del token,
 * y el encabezado lo pone el interceptor de autenticación.
 *
 * **La clave de idempotencia es el identificador del borrador.** La petición
 * puede reintentarse sin radicar dos veces: con la misma clave el servicio
 * devuelve el caso que ya existe.
 */
interface SiniestrosApi {

    @POST("v1/claims")
    suspend fun radicar(
        @Header(ENCABEZADO_IDEMPOTENCIA) borrador: String,
        @Header(Correlacion.ENCABEZADO) correlacion: String,
        @Body peticion: SolicitudDeRadicacion,
    ): Response<RespuestaDeRadicacion>

    companion object {
        const val ENCABEZADO_IDEMPOTENCIA = "Idempotency-Key"
    }
}

/** Cuerpo de la radicación: lo que el asistente reunió en sus cuatro etapas. */
@Serializable
data class SolicitudDeRadicacion(
    @SerialName("policy_id") val poliza: String,
    @SerialName("incident_type") val tipoDeIncidente: String,
    /** Instante del hecho, en RFC 3339 y en UTC. */
    @SerialName("occurred_at") val ocurrioEn: String,
    @SerialName("place") val lugar: String,
    @SerialName("description") val descripcion: String,
    /** Respuestas de las preguntas que dependen del tipo de incidente. */
    @SerialName("details") val detalles: Map<String, String> = emptyMap(),
    @SerialName("declaration_accepted") val declaracionAceptada: Boolean,
)

/** Solo lo que la aplicación usa; el resto de la respuesta se ignora. */
@Serializable
data class RespuestaDeRadicacion(
    @SerialName("claim_id") val casoId: String,
    /** El número que el asegurado le dicta a soporte: `SIN-<año>-<cinco dígitos>`. */
    @SerialName("filing_number") val radicado: String,
    @SerialName("status") val estado: String,
    @SerialName("filed_at") val radicadoEn: String,
)

@Serializable
data class ErrorDeRadicacion(
    @SerialName("error") val error: String,
    @SerialName("fields") val campos: List<CampoInvalido> = emptyList(),
)

@Serializable
data class CampoInvalido(
    @SerialName("field") val campo: String,
    @SerialName("code") val codigo: String,
)
