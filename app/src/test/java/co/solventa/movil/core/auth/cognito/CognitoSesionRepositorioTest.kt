package co.solventa.movil.core.auth.cognito

import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.TokensDeSesion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Base64

/**
 * La sesión contra el *user pool* de Cognito (HA-S01), con la API de Cognito
 * simulada. Lo que se comprueba es el contrato: qué se envía, qué queda
 * guardado y qué ocurre cuando el *pool* responde mal.
 */
class CognitoSesionRepositorioTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: CognitoApi
    private lateinit var almacen: AlmacenDeSesionEnMemoria

    @Before
    fun antes() {
        servidor = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CognitoApi::class.java)
        almacen = AlmacenDeSesionEnMemoria()
    }

    @After
    fun despues() {
        servidor.close()
    }

    private fun repositorio(limpiezas: Set<AlCerrarSesion> = emptySet()) = CognitoSesionRepositorio(api, almacen, limpiezas)

    @Test
    fun `el ingreso usa USER_PASSWORD_AUTH y guarda la sesion del token`() = runTest {
        servidor.enqueue(respuestaConTokens(idTokenDe("asegurado@example.com", "Carolina", VENCE_EN_S)))

        val resultado = repositorio().ingresarConContrasena("asegurado@example.com", "unaClave")

        assertTrue(resultado is ResultadoIngreso.Exitoso)
        assertEquals("Carolina", (resultado as ResultadoIngreso.Exitoso).sesion.nombre)
        val enviado = servidor.takeRequest().body?.utf8().orEmpty()
        assertTrue(enviado.contains("USER_PASSWORD_AUTH"))
        assertTrue(enviado.contains("asegurado@example.com"))
        val guardado = almacen.tokens()
        assertNotNull(guardado)
        assertEquals("asegurado@example.com", guardado?.correo)
        // El vencimiento sale del propio token, no del reloj del dispositivo.
        assertEquals(VENCE_EN_S * 1_000L, guardado?.venceEn)
    }

    /**
     * Cognito responde 400 tanto para una contraseña equivocada como para un
     * usuario que no existe, y la aplicación tampoco los distingue: hacerlo
     * le diría a un atacante qué correos están registrados.
     */
    @Test
    fun `un rechazo del pool es credenciales invalidas y no guarda nada`() = runTest {
        servidor.enqueue(MockResponse(code = 400, body = CUERPO_NO_AUTORIZADO))

        val resultado = repositorio().ingresarConContrasena("asegurado@example.com", "mala")

        assertEquals(ResultadoIngreso.CredencialesInvalidas, resultado)
        assertNull(almacen.tokens())
    }

    @Test
    fun `sin red el ingreso informa falta de conexion`() = runTest {
        servidor.close()

        val resultado = repositorio().ingresarConContrasena("asegurado@example.com", "unaClave")

        assertEquals(ResultadoIngreso.SinConexion, resultado)
    }

    /**
     * Un desafío —por ejemplo cambiar la contraseña temporal del alta
     * administrativa— no se resuelve en la aplicación: la cuenta la crea el
     * backend tras el KYC.
     */
    @Test
    fun `un desafio sin tokens no abre sesion`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = CUERPO_DESAFIO))

        val resultado = repositorio().ingresarConContrasena("asegurado@example.com", "temporal")

        assertEquals(ResultadoIngreso.CredencialesInvalidas, resultado)
        assertNull(almacen.tokens())
    }

    @Test
    fun `un token todavia vigente no gasta una renovacion`() = runTest {
        almacen.guardarTokens(tokensGuardados(venceEn = AHORA + 5 * 60_000L))

        val token = repositorio().tokenVigente(ahora = AHORA)

        assertEquals("id-guardado", token)
        assertEquals(0, servidor.requestCount)
    }

    /**
     * Los tokens del *pool* duran cinco minutos y una sesión de trabajo dura
     * mucho más: sin renovar, la aplicación quedaría inservible a los cinco
     * minutos de entrar.
     */
    @Test
    fun `un token por vencer se renueva con REFRESH_TOKEN_AUTH`() = runTest {
        almacen.guardarTokens(tokensGuardados(venceEn = AHORA + 30_000L))
        val nuevo = idTokenDe("asegurado@example.com", "Carolina", VENCE_EN_S)
        servidor.enqueue(respuestaConTokens(nuevo, conRenovacion = false))

        val token = repositorio().tokenVigente(ahora = AHORA)

        assertEquals(nuevo, token)
        val enviado = servidor.takeRequest().body?.utf8().orEmpty()
        assertTrue(enviado.contains("REFRESH_TOKEN_AUTH"))
        assertTrue(enviado.contains("renovacion-guardada"))
        // Al renovar, Cognito no reenvía el de renovación: hay que conservar
        // el que el dispositivo ya tenía o la sesión muere en la siguiente.
        assertEquals("renovacion-guardada", almacen.tokens()?.tokenDeRenovacion)
    }

    @Test
    fun `un token de renovacion rechazado descarta la sesion`() = runTest {
        almacen.guardarTokens(tokensGuardados(venceEn = AHORA - 1))
        servidor.enqueue(MockResponse(code = 400, body = CUERPO_NO_AUTORIZADO))

        val token = repositorio().tokenVigente(ahora = AHORA)

        assertNull(token)
        assertNull(almacen.tokens())
    }

    /**
     * Sin red no se descarta la sesión: el token sigue sirviendo cuando la
     * conexión vuelva, si todavía no ha vencido.
     */
    @Test
    fun `sin red la renovacion falla pero conserva la sesion`() = runTest {
        almacen.guardarTokens(tokensGuardados(venceEn = AHORA + 30_000L))
        servidor.close()

        val token = repositorio().tokenVigente(ahora = AHORA)

        assertNull(token)
        assertNotNull(almacen.tokens())
    }

    @Test
    fun `cerrar la sesion borra los tokens del dispositivo`() = runTest {
        almacen.guardarTokens(tokensGuardados(venceEn = AHORA + 60_000L))

        repositorio().cerrar()

        assertNull(almacen.tokens())
    }

    /**
     * HU-M1.3-01: cada dominio borra lo suyo antes de que se borren los
     * tokens, y una limpieza que falla no deja la sesion abierta.
     */
    @Test
    fun `cerrar ejecuta cada limpieza antes de borrar los tokens`() = runTest {
        almacen.guardarTokens(tokensGuardados(venceEn = AHORA + 60_000L))
        val tokensAlLimpiar = mutableListOf<Boolean>()
        val limpieza = AlCerrarSesion { tokensAlLimpiar += almacen.tokens() != null }
        val fallida = AlCerrarSesion { error("la base no respondio") }

        repositorio(setOf(fallida, limpieza)).cerrar()

        assertEquals(listOf(true), tokensAlLimpiar)
        assertNull(almacen.tokens())
    }

    @Test
    fun `la sesion trae el sub del token`() = runTest {
        servidor.enqueue(respuestaConTokens(idTokenDe("asegurado@example.com", "Carolina", VENCE_EN_S, sub = "sub-123")))

        val resultado = repositorio().ingresarConContrasena("asegurado@example.com", "unaClave")

        assertEquals("sub-123", (resultado as ResultadoIngreso.Exitoso).sesion.sub)
        assertEquals("sub-123", repositorio().sesionGuardada()?.sub)
    }

    @Test
    fun `sin sub en el token la sesion lo deja vacio`() = runTest {
        almacen.guardarTokens(tokensGuardados(venceEn = AHORA + 60_000L))

        assertEquals("", repositorio().sesionGuardada()?.sub)
    }

    /** El desbloqueo del criterio 2 es local: no hay petición al proveedor. */
    @Test
    fun `desbloquear no llama al pool`() = runTest {
        almacen.guardarTokens(tokensGuardados(venceEn = AHORA + 60_000L))

        val sesion = repositorio().desbloquear()

        assertEquals("Carolina", sesion?.nombre)
        assertEquals(0, servidor.requestCount)
    }

    private fun tokensGuardados(venceEn: Long) = TokensDeSesion(
        idToken = "id-guardado",
        accessToken = "acceso-guardado",
        tokenDeRenovacion = "renovacion-guardada",
        venceEn = venceEn,
        correo = "asegurado@example.com",
        nombre = "Carolina",
    )

    private fun respuestaConTokens(idToken: String, conRenovacion: Boolean = true): MockResponse {
        val renovacion = if (conRenovacion) "\"RefreshToken\":\"renovacion-nueva\"," else ""
        val cuerpo = "{\"AuthenticationResult\":{\"IdToken\":\"$idToken\"," +
            "\"AccessToken\":\"acceso\",$renovacion\"ExpiresIn\":300}}"
        return MockResponse(code = 200, body = cuerpo)
    }

    /** Un JWT con la forma que devuelve Cognito: aquí solo importa la carga. */
    private fun idTokenDe(correo: String, nombre: String, exp: Long, sub: String? = null): String {
        val conSub = if (sub != null) ",\"sub\":\"$sub\"" else ""
        val carga = "{\"email\":\"$correo\",\"name\":\"$nombre\",\"exp\":$exp$conSub}"
        val codificada = Base64.getUrlEncoder().withoutPadding().encodeToString(carga.toByteArray())
        return "encabezado.$codificada.firma"
    }

    private companion object {
        const val AHORA = 1_760_000_000_000L
        const val VENCE_EN_S = 1_760_000_300L
        const val CUERPO_NO_AUTORIZADO = "{\"__type\":\"NotAuthorizedException\"}"
        const val CUERPO_DESAFIO = "{\"ChallengeName\":\"NEW_PASSWORD_REQUIRED\"}"
    }
}
