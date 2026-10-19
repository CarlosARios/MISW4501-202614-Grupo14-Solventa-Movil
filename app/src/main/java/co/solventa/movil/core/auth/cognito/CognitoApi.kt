package co.solventa.movil.core.auth.cognito

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * El pedazo de la API de Cognito que este canal usa: iniciar sesión y renovar
 * la sesión (HA-S01, decisión D-5).
 *
 * **Por qué no se usa Amplify ni el SDK de AWS.** `InitiateAuth` es un único
 * `POST` de JSON y el cliente de la aplicación es público, sin secreto, así
 * que no hay nada que firmar con SigV4. Traer el SDK añadiría varios
 * megabytes a un APK de 2,9 MB, su propia capa de almacenamiento de tokens y
 * reglas de R8 que mantener, a cambio de dos llamadas que caben en este
 * archivo. Retrofit y kotlinx.serialization ya estaban en el proyecto.
 *
 * La API usa el protocolo JSON 1.1 de AWS: la operación va en el encabezado
 * `X-Amz-Target`, no en la ruta, y por eso todas las llamadas son a la raíz.
 */
interface CognitoApi {

    @Headers(
        "Content-Type: application/x-amz-json-1.1",
        "X-Amz-Target: AWSCognitoIdentityProviderService.InitiateAuth",
    )
    @POST("/")
    suspend fun iniciarAutenticacion(@Body peticion: PeticionDeAutenticacion): RespuestaDeAutenticacion
}

/**
 * Cuerpo de `InitiateAuth`. Los dos flujos que el *pool* habilita para este
 * cliente viajan en el mismo tipo porque solo cambian los parámetros.
 */
@Serializable
data class PeticionDeAutenticacion(
    @SerialName("AuthFlow") val flujo: String,
    @SerialName("ClientId") val clienteId: String,
    @SerialName("AuthParameters") val parametros: Map<String, String>,
) {
    companion object {
        const val FLUJO_CONTRASENA = "USER_PASSWORD_AUTH"
        const val FLUJO_RENOVACION = "REFRESH_TOKEN_AUTH"

        fun conContrasena(clienteId: String, correo: String, contrasena: String) = PeticionDeAutenticacion(
            flujo = FLUJO_CONTRASENA,
            clienteId = clienteId,
            parametros = mapOf("USERNAME" to correo, "PASSWORD" to contrasena),
        )

        fun conRenovacion(clienteId: String, tokenDeRenovacion: String) = PeticionDeAutenticacion(
            flujo = FLUJO_RENOVACION,
            clienteId = clienteId,
            parametros = mapOf("REFRESH_TOKEN" to tokenDeRenovacion),
        )
    }
}

@Serializable
data class RespuestaDeAutenticacion(
    @SerialName("AuthenticationResult") val resultado: ResultadoDeAutenticacion? = null,
    /**
     * Cognito responde con un desafío en lugar de tokens cuando la cuenta
     * exige algo más, por ejemplo cambiar la contraseña temporal del alta
     * administrativa. No se resuelve en la aplicación: el alta la hace el
     * backend tras el KYC.
     */
    @SerialName("ChallengeName") val desafio: String? = null,
)

@Serializable
data class ResultadoDeAutenticacion(
    /** El que viaja en `Authorization`: el backend valida `token_use = id`. */
    @SerialName("IdToken") val idToken: String,
    @SerialName("AccessToken") val accessToken: String,
    /** Ausente al renovar: Cognito conserva el que ya tenía el dispositivo. */
    @SerialName("RefreshToken") val tokenDeRenovacion: String? = null,
    @SerialName("ExpiresIn") val vigenciaEnSegundos: Long,
)
