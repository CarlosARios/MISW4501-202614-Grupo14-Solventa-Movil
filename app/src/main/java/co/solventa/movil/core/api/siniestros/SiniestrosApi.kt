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

    /**
     * Pide la URL firmada con la que se sube una evidencia (HU-M2.1-01). El
     * archivo no pasa por aquí: va del teléfono al repositorio.
     */
    @POST("v1/claims/evidence/upload-url")
    suspend fun urlDeSubida(
        @Header(Correlacion.ENCABEZADO) correlacion: String,
        @Body peticion: SolicitudDeSubida,
    ): Response<RespuestaDeSubida>

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
    /** Lo capturado en el lugar, ya subido al repositorio de evidencia. */
    @SerialName("evidence") val evidencia: List<EvidenciaRadicada> = emptyList(),
    @SerialName("declaration_accepted") val declaracionAceptada: Boolean,
    /**
     * El monto estimado en centavos de COP (HU-M2.2-01). Sin monto no viaja:
     * el cuerpo es el de antes y el servicio no lo cuenta en la huella.
     */
    @SerialName("estimated_amount_minor") val montoEstimadoMinor: Long? = null,
)

/** Lo que el asegurado declara antes de subir una evidencia. */
@Serializable
data class SolicitudDeSubida(
    @SerialName("kind") val tipo: String,
    @SerialName("content_type") val tipoDeContenido: String,
    @SerialName("byte_size") val tamanoBytes: Long,
    @SerialName("sha256") val sha256: String,
)

/**
 * La subida firmada. Los encabezados son los que el `PUT` tiene que llevar,
 * exactamente esos: la firma los cubre.
 */
@Serializable
data class RespuestaDeSubida(
    @SerialName("object_key") val llaveDelObjeto: String,
    @SerialName("upload_url") val url: String,
    @SerialName("upload_headers") val encabezados: Map<String, String> = emptyMap(),
    @SerialName("expires_at") val venceEn: String,
)

/** Una evidencia que viaja con la radicación: su llave y su captura. */
@Serializable
data class EvidenciaRadicada(
    @SerialName("object_key") val llaveDelObjeto: String,
    @SerialName("kind") val tipo: String,
    @SerialName("captured_at") val capturadaEn: String,
    @SerialName("latitude") val latitud: Double,
    @SerialName("longitude") val longitud: Double,
    @SerialName("altitude_m") val altitud: Double? = null,
    @SerialName("accuracy_m") val precisionM: Double,
    @SerialName("location_trusted") val ubicacionConfiable: Boolean,
    @SerialName("sha256") val sha256: String,
    @SerialName("byte_size") val tamanoBytes: Long,
    @SerialName("duration_s") val duracionS: Int = 0,
)

/** Solo lo que la aplicación usa; el resto de la respuesta se ignora. */
@Serializable
data class RespuestaDeRadicacion(
    @SerialName("claim_id") val casoId: String,
    /** El número que el asegurado le dicta a soporte: `SIN-<año>-<cinco dígitos>`. */
    @SerialName("filing_number") val radicado: String,
    @SerialName("status") val estado: String,
    @SerialName("filed_at") val radicadoEn: String,
    /** Lo que quedó asociado al caso, contado por el servicio. */
    @SerialName("evidence_count") val evidencia: ConteoDeEvidencia = ConteoDeEvidencia(),
)

@Serializable
data class ConteoDeEvidencia(
    @SerialName("photos") val fotos: Int = 0,
    @SerialName("videos") val videos: Int = 0,
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
