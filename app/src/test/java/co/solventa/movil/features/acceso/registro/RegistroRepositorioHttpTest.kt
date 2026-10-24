package co.solventa.movil.features.acceso.registro

import co.solventa.movil.core.api.originacion.OriginacionApi
import co.solventa.movil.core.errores.Correlacion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * El registro contra el API Gateway, con el servicio simulado. Lo que se
 * comprueba es el contrato de `POST /v1/registrations`: qué se envía y a
 * dónde lleva cada código de estado.
 */
class RegistroRepositorioHttpTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: RegistroRepositorioHttp

    @Before
    fun antes() {
        servidor = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OriginacionApi::class.java)
        repositorio = RegistroRepositorioHttp(api)
    }

    @After
    fun despues() {
        servidor.close()
    }

    @Test
    fun `un registro aceptado devuelve la cuenta del user pool`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        val resultado = repositorio.registrar(DATOS, CONTRASENA)

        assertEquals(ResultadoRegistro.Exitoso("sub-carolina"), resultado)
        val peticion = servidor.takeRequest()
        assertEquals("/v1/registrations", peticion.url.encodedPath)
        assertEquals("POST", peticion.method)
    }

    /**
     * La pantalla muestra el documento con puntos, el nombre en dos campos y
     * la fecha como se escribe en Colombia; el contrato recibe otra cosa.
     */
    @Test
    fun `el cuerpo se traduce a lo que el contrato recibe`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        repositorio.registrar(DATOS, CONTRASENA)

        val enviado = servidor.takeRequest().body?.utf8().orEmpty()
        assertTrue(enviado, enviado.contains("\"document_id\":\"1020345678\""))
        assertTrue(enviado, enviado.contains("\"full_name\":\"Carolina Pérez Ruiz\""))
        assertTrue(enviado, enviado.contains("\"birth_date\":\"1991-05-14\""))
        assertTrue(enviado, enviado.contains("\"email\":\"carolina.perez.4821@example.com\""))
        assertTrue(enviado, enviado.contains("\"password\":\"$CONTRASENA\""))
    }

    /** Un día o un mes de una cifra tienen que salir con dos. */
    @Test
    fun `la fecha se completa con ceros`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        repositorio.registrar(DATOS.copy(fechaNacimiento = "5/9/1991"), CONTRASENA)

        val enviado = servidor.takeRequest().body?.utf8().orEmpty()
        assertTrue(enviado, enviado.contains("\"birth_date\":\"1991-09-05\""))
    }

    /**
     * La referencia que M1e le muestra al usuario es el identificador con el
     * que soporte encuentra la petición en los registros del backend, así que
     * tiene que ser el mismo que viajó en el encabezado.
     */
    @Test
    fun `la referencia de la falla es el identificador que se envio`() = runTest {
        servidor.enqueue(MockResponse(code = 503, body = """{"error":"registration_unavailable"}"""))

        val resultado = repositorio.registrar(DATOS, CONTRASENA)

        val enviado = servidor.takeRequest().headers[Correlacion.ENCABEZADO]
        assertTrue(Correlacion.esValido(enviado.orEmpty()))
        assertEquals(ResultadoRegistro.NoDisponible(enviado.orEmpty()), resultado)
    }

    @Test
    fun `un documento ya registrado es conflicto y no una falla`() = runTest {
        servidor.enqueue(MockResponse(code = 409, body = """{"error":"already_registered"}"""))

        assertEquals(ResultadoRegistro.YaRegistrado, repositorio.registrar(DATOS, CONTRASENA))
    }

    @Test
    fun `los campos rechazados se traducen a los del formulario`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = CUERPO_INVALIDO))

        val resultado = repositorio.registrar(DATOS, CONTRASENA)

        assertEquals(
            ResultadoRegistro.DatosInvalidos(
                setOf(CampoDeRegistro.CORREO, CampoDeRegistro.CONTRASENA),
            ),
            resultado,
        )
    }

    /** Si no se entiende qué campo sobra, se marca el formulario completo. */
    @Test
    fun `un rechazo sin campos reconocibles marca todo el formulario`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = """{"error":"validation_failed"}"""))

        val resultado = repositorio.registrar(DATOS, CONTRASENA)

        assertEquals(
            ResultadoRegistro.DatosInvalidos(CampoDeRegistro.entries.toSet()),
            resultado,
        )
    }

    /**
     * Un `201` ilegible no permite afirmar que la cuenta existe, y mandar al
     * usuario a entrar con credenciales que quizá no se crearon es peor que
     * pedirle que reintente.
     */
    @Test
    fun `una respuesta creada pero ilegible se trata como falla`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = "{"))

        val resultado = repositorio.registrar(DATOS, CONTRASENA)

        assertTrue(resultado is ResultadoRegistro.NoDisponible)
    }

    @Test
    fun `sin red el registro queda reintentable`() = runTest {
        servidor.close()

        val resultado = repositorio.registrar(DATOS, CONTRASENA)

        assertTrue(resultado is ResultadoRegistro.NoDisponible)
    }

    private companion object {
        const val CONTRASENA = "Solventa.2026.4821"

        const val CUERPO_CREADO = """
            {"customer_id":"cus-1","document_id":"1020345678","subject":"sub-carolina",
             "registered_at":"2026-10-22T15:00:00Z"}
        """

        const val CUERPO_INVALIDO = """
            {"error":"validation_failed","message":"one or more fields are missing or invalid",
             "fields":[{"field":"email","code":"invalid_format"},
                       {"field":"password","code":"out_of_range"}]}
        """

        val DATOS = DatosDocumento(
            nombres = "Carolina",
            apellidos = "Pérez Ruiz",
            tipoDocumento = "CC",
            numeroDocumento = "1.020.345.678",
            fechaNacimiento = "14/05/1991",
            correo = "carolina.perez.4821@example.com",
            celular = "+57 301 234 5678",
        )
    }
}
