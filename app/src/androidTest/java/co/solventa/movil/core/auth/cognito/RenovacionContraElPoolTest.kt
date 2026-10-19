package co.solventa.movil.core.auth.cognito

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.BuildConfig
import co.solventa.movil.core.auth.sesion.AlmacenDeSesion
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.TokensDeSesion
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Ingreso y renovación contra el **user pool real** de Cognito.
 *
 * Es la única prueba que recorre el camino completo: las demás simulan la
 * API. Lo que comprueba es lo que más riesgo tiene para una demostración
 * larga —que `REFRESH_TOKEN_AUTH` funcione de verdad contra el *pool*—,
 * porque los tokens duran cinco minutos y una sustentación dura cuarenta.
 *
 * **La contraseña no está en el repositorio ni se registra.** Entra como
 * argumento de la instrumentación, así que la escribe quien corre la prueba:
 *
 * ```powershell
 * $env:CLAVE_POOL_SOLVENTA = "la contrasena del pool"
 * .\gradlew :app:connectedDebugAndroidTest
 * ```
 *
 * El `build.gradle.kts` toma esa variable del entorno y la pasa como
 * argumento de la instrumentacion, de modo que la contrasena no aparece en
 * la linea de comandos ni queda escrita en ningun archivo del repositorio.
 *
 * Sin ese argumento la prueba **se omite**, no falla: así el pipeline sigue
 * en verde sin tener que conocer la contraseña.
 *
 * Usa `runBlocking` y no `runTest` a propósito. En el dispositivo, el
 * artefacto `kotlinx-coroutines-test` resuelve a una versión más nueva que el
 * `kotlinx-coroutines-core` que la aplicación empaqueta, y `runTest` llama a
 * un método que ese núcleo no tiene. Aquí no hace falta tiempo virtual: las
 * llamadas al *pool* son de red real y hay que esperarlas de verdad.
 */
@RunWith(AndroidJUnit4::class)
class RenovacionContraElPoolTest {

    private lateinit var api: CognitoApi
    private lateinit var almacen: AlmacenEnMemoria
    private var clave: String? = null

    @Before
    fun antes() {
        clave = InstrumentationRegistry.getArguments().getString(ARGUMENTO_CLAVE)
        assumeFalse("sin $ARGUMENTO_CLAVE no se consulta el pool real", clave.isNullOrBlank())

        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl("https://cognito-idp.${BuildConfig.COGNITO_REGION}.amazonaws.com/")
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CognitoApi::class.java)
        almacen = AlmacenEnMemoria()
    }

    @Test
    fun ingresaYRenuevaContraElPoolReal() = runBlocking {
        val repositorio = CognitoSesionRepositorio(api, almacen)

        val ingreso = repositorio.ingresarConContrasena(USUARIO, clave!!)

        assertEquals(ResultadoIngreso.Exitoso::class.java, ingreso.javaClass)
        val primeros = almacen.tokens()
        assertNotNull("el ingreso debió dejar la sesión guardada", primeros)
        assertEquals(USUARIO, primeros?.correo)

        // Se fuerza la renovación pidiendo el token en el instante en que el
        // actual vence: es lo que hará el interceptor dentro de cinco minutos.
        val renovado = repositorio.tokenVigente(ahora = primeros!!.venceEn)

        assertNotNull("el pool debió emitir un token nuevo", renovado)
        assertNotEquals("el token renovado tiene que ser otro", primeros.idToken, renovado)
        // Cognito no reenvía el de renovación: el dispositivo conserva el suyo
        // o la sesión moriría en la renovación siguiente.
        assertEquals(primeros.tokenDeRenovacion, almacen.tokens()?.tokenDeRenovacion)
    }

    private class AlmacenEnMemoria : AlmacenDeSesion {
        private var guardados: TokensDeSesion? = null
        private var biometria = true

        override fun guardarTokens(tokens: TokensDeSesion) {
            guardados = tokens
        }

        override fun tokens(): TokensDeSesion? = guardados

        override fun borrarTokens() {
            guardados = null
        }

        override fun biometriaHabilitada(): Boolean = biometria

        override fun habilitarBiometria(habilitada: Boolean) {
            biometria = habilitada
        }
    }

    private companion object {
        const val ARGUMENTO_CLAVE = "claveDePrueba"
        const val USUARIO = "asegurado@example.com"
    }
}
