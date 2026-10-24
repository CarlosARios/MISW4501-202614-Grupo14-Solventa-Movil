package co.solventa.movil.core.api.originacion

import co.solventa.movil.core.errores.Correlacion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Registro del asegurado en originación (HU-M1.1-01). El contrato vive en
 * `docs/contratos/HU-M1.1-01-api.md` del repositorio de servicios.
 *
 * **Es la única llamada del canal que sale sin sesión**, porque la cuenta
 * todavía no existe. El interceptor de autenticación no estorba: sin tokens
 * guardados la petición sale sin encabezado.
 *
 * Devuelve `Response` y no el cuerpo directamente: el código de estado es
 * parte del contrato. Un dato inválido, un documento ya registrado y una
 * falla del servicio llevan al usuario a tres sitios distintos.
 *
 * El identificador de correlación lo pone quien llama para poder
 * devolvérselo al usuario como referencia cuando el registro falle; el
 * interceptor respeta el que ya venga puesto.
 */
interface OriginacionApi {

    @POST("v1/registrations")
    suspend fun registrar(
        @Header(Correlacion.ENCABEZADO) correlacion: String,
        @Body peticion: SolicitudDeRegistro,
    ): Response<RespuestaDeRegistro>
}

/**
 * Cuerpo del registro. La contraseña viaja al *user pool* y no se almacena
 * en ninguna base, ni aquí ni en el servicio.
 *
 * Las dos caras del documento van en el mismo cuerpo, en base64: así un
 * `201` significa que el cliente, la cuenta y el documento quedaron, sin
 * estados a medias entre dos llamadas.
 */
@Serializable
data class SolicitudDeRegistro(
    @SerialName("document_id") val documento: String,
    @SerialName("full_name") val nombreCompleto: String,
    @SerialName("email") val correo: String,
    @SerialName("birth_date") val fechaNacimiento: String,
    @SerialName("password") val contrasena: String,
    @SerialName("document_front") val frenteDelDocumento: String,
    @SerialName("document_back") val reversoDelDocumento: String,
)

/** Solo lo que la aplicación usa; el resto del `201` se ignora. */
@Serializable
data class RespuestaDeRegistro(
    @SerialName("customer_id") val clienteId: String,
    /** Identificador de la cuenta en el *user pool*: el `sub` del token. */
    @SerialName("subject") val cuenta: String,
)

@Serializable
data class ErrorDeRegistro(
    @SerialName("error") val error: String,
    @SerialName("fields") val campos: List<CampoInvalido> = emptyList(),
)

@Serializable
data class CampoInvalido(
    @SerialName("field") val campo: String,
    @SerialName("code") val codigo: String,
)
