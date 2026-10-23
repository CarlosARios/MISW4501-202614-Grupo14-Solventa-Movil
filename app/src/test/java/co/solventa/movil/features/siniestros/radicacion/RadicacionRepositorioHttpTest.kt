package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.core.api.siniestros.SiniestrosApi
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
 * La radicación contra el API Gateway, con el servicio simulado. Lo que se
 * comprueba es el contrato de `POST /v1/claims`: qué se envía y a dónde lleva
 * cada código de estado.
 */
class RadicacionRepositorioHttpTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: RadicacionRepositorioHttp

    @Before
    fun antes() {
        servidor = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiniestrosApi::class.java)
        repositorio = RadicacionRepositorioHttp(api)
    }

    @After
    fun despues() {
        servidor.close()
    }

    @Test
    fun `una radicacion aceptada devuelve el numero de radicado`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        assertTrue(resultado is ResultadoRadicacion.Exitoso)
        val caso = (resultado as ResultadoRadicacion.Exitoso).caso
        assertEquals("SIN-2026-00001", caso.radicado)
        assertEquals("cl-1", caso.casoId)

        val peticion = servidor.takeRequest()
        assertEquals("/v1/claims", peticion.url.encodedPath)
        assertEquals("POST", peticion.method)
    }

    /** El identificador del borrador es la clave: por eso el reintento no duplica. */
    @Test
    fun `el borrador viaja como clave de idempotencia`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        repositorio.radicar(BORRADOR, radicacion())

        val peticion = servidor.takeRequest()
        assertEquals(BORRADOR, peticion.headers[SiniestrosApi.ENCABEZADO_IDEMPOTENCIA])
        assertTrue(Correlacion.esValido(peticion.headers[Correlacion.ENCABEZADO].orEmpty()))
    }

    @Test
    fun `el cuerpo se traduce a lo que el contrato recibe`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        repositorio.radicar(BORRADOR, radicacion())

        val enviado = servidor.takeRequest().body?.utf8().orEmpty()
        assertTrue(enviado, enviado.contains("\"policy_id\":\"AU-2025-118204\""))
        assertTrue(enviado, enviado.contains("\"incident_type\":\"collision\""))
        assertTrue(enviado, enviado.contains("\"occurred_at\":\"2026-10-20T23:30:00Z\""))
        assertTrue(enviado, enviado.contains("\"declaration_accepted\":true"))
        assertTrue(enviado, enviado.contains("\"otro_vehiculo\":\"si\""))
    }

    /** Un reintento con la misma clave responde 200 y es un éxito, no un conflicto. */
    @Test
    fun `un reintento devuelve el caso que ya existe`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = CUERPO_CREADO))

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        assertTrue(resultado is ResultadoRadicacion.Exitoso)
        assertEquals("SIN-2026-00001", (resultado as ResultadoRadicacion.Exitoso).caso.radicado)
    }

    @Test
    fun `los campos rechazados se traducen a los del asistente`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = CUERPO_INVALIDO))

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        assertEquals(
            ResultadoRadicacion.DatosInvalidos(
                setOf(CampoDeRadicacion.FECHA, CampoDeRadicacion.DESCRIPCION),
            ),
            resultado,
        )
    }

    @Test
    fun `un rechazo sin campos reconocibles marca el formulario completo`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = """{"error":"validation_failed"}"""))

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        assertEquals(
            ResultadoRadicacion.DatosInvalidos(CampoDeRadicacion.entries.toSet()),
            resultado,
        )
    }

    /**
     * Reutilizar la clave con otra radicación no es un dato que el usuario
     * pueda corregir en una etapa: se trata como falla del envío.
     */
    @Test
    fun `una clave reutilizada no se presenta como campo invalido`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = """{"error":"idempotency_key_reused"}"""))

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        assertTrue(resultado is ResultadoRadicacion.NoDisponible)
    }

    @Test
    fun `una falla del servicio deja la referencia que se envio`() = runTest {
        servidor.enqueue(MockResponse(code = 503, body = """{"error":"filing_unavailable"}"""))

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        val enviado = servidor.takeRequest().headers[Correlacion.ENCABEZADO].orEmpty()
        assertEquals(ResultadoRadicacion.NoDisponible(enviado), resultado)
    }

    @Test
    fun `sin sesion el servicio responde 401 y el envio queda reintentable`() = runTest {
        servidor.enqueue(MockResponse(code = 401, body = """{"error":"unauthorized"}"""))

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        assertTrue(resultado is ResultadoRadicacion.NoDisponible)
    }

    @Test
    fun `sin red el envio queda reintentable`() = runTest {
        servidor.close()

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        assertTrue(resultado is ResultadoRadicacion.NoDisponible)
    }

    private fun radicacion() = Radicacion(
        poliza = "AU-2025-118204",
        tipo = TipoDeIncidente.COLISION,
        // 2026-10-20T23:30:00Z
        ocurrioEn = 1_792_539_000_000L,
        lugar = "Calle 80 con Carrera 15, Bogotá",
        descripcion = "Choque por alcance en el semáforo.",
        respuestas = mapOf("otro_vehiculo" to "si"),
        declaracionAceptada = true,
    )

    private companion object {
        const val BORRADOR = "9d1f6b52-0b2e-4a0c-9d9b-6f1a2c3d4e5f"

        const val CUERPO_CREADO = """
            {"claim_id":"cl-1","filing_number":"SIN-2026-00001","status":"active",
             "policy_id":"AU-2025-118204","incident_type":"collision",
             "occurred_at":"2026-10-20T23:30:00Z","filed_at":"2026-10-23T03:10:00Z"}
        """

        const val CUERPO_INVALIDO = """
            {"error":"validation_failed","message":"one or more fields are missing or invalid",
             "fields":[{"field":"occurred_at","code":"out_of_range"},
                       {"field":"description","code":"out_of_range"}]}
        """
    }
}
