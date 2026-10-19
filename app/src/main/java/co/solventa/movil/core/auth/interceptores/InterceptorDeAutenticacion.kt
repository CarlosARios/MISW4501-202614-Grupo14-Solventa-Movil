package co.solventa.movil.core.auth.interceptores

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Adjunta el *id token* de la sesión a cada petición del API Gateway.
 *
 * **Va el *id token* y no el de acceso.** El backend verifica `token_use` y
 * la plataforma no define `AUTH_TOKEN_USE`, así que toma su valor por
 * omisión, `id` (`pkg/security`). Un *access token* se rechazaría con 401.
 *
 * Sin sesión la petición sale sin encabezado y el backend responde 401, que
 * es lo correcto: no le corresponde a esta capa decidir qué rutas necesitan
 * sesión.
 *
 * **Solo toca el API Gateway.** La llamada al propio Cognito no lleva
 * `Authorization`, y además pedirle un token a quien lo emite sería una
 * recursión: por eso el proveedor se inyecta perezosamente.
 */
@Singleton
class InterceptorDeAutenticacion @Inject constructor(
    private val sesion: Provider<ProveedorDeToken>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp llama a los interceptores desde un hilo propio y bloqueante;
        // la renovación es una suspensión, así que aquí se puentea. No corre
        // en el hilo principal.
        val token = runBlocking { sesion.get().tokenVigente() }
        val peticion = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().header(ENCABEZADO, "$PREFIJO$token").build()
        }
        return chain.proceed(peticion)
    }

    private companion object {
        const val ENCABEZADO = "Authorization"
        const val PREFIJO = "Bearer "
    }
}

/**
 * Lo único que el interceptor necesita de la sesión. Existe para que el
 * interceptor no dependa del repositorio completo y para poder probarlo con
 * un doble de dos líneas.
 */
fun interface ProveedorDeToken {
    suspend fun tokenVigente(): String?
}
