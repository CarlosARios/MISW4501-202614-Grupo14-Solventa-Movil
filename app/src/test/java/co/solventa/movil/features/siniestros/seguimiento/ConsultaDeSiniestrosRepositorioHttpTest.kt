package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.core.api.siniestros.PolizaDelSiniestroDto
import co.solventa.movil.core.api.siniestros.SiniestrosApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant

/**
 * La consulta contra el API Gateway, con el servicio simulado (HU-M2.4-01,
 * HA-L02): lo que se lee de cada respuesta, la marca de la caché y a dónde
 * lleva cada código de estado.
 */
class ConsultaDeSiniestrosRepositorioHttpTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: ConsultaDeSiniestrosRepositorioHttp
    private val respuestas = mutableMapOf<String, MockResponse>()

    @Before
    fun antes() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                respuestas[request.url.encodedPath] ?: MockResponse(code = 500)
        }
        servidor.start()
        repositorio = ConsultaDeSiniestrosRepositorioHttp(apiHacia(servidor.url("/"))) { AHORA }
    }

    private fun apiHacia(base: HttpUrl): SiniestrosApi = Retrofit.Builder()
        .baseUrl(base)
        .client(OkHttpClient())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SiniestrosApi::class.java)

    @After
    fun despues() {
        servidor.close()
    }

    private fun responder(ruta: String, codigo: Int, cuerpo: String = "{}") {
        respuestas[ruta] = MockResponse(code = codigo, body = cuerpo)
    }

    @Test
    fun `la lista se lee con su frescura`() = runTest {
        responder("/v1/claims", 200, LISTA.replace("STALE", "false"))

        val resultado = repositorio.listar() as ResultadoDeConsulta.Exito

        val caso = resultado.valor.single()
        assertEquals("SIN-2026-01655", caso.radicado)
        assertEquals(EstadoDeSiniestro.EN_EVALUACION, caso.estado)
        assertEquals("home", caso.linea)
        assertEquals("water_damage", caso.tipoDeIncidente)
        assertEquals(Instant.parse("2026-09-02T03:15:00Z").toEpochMilli(), caso.ocurrioEn)
        assertEquals(Frescura(false, Instant.parse(LEIDO_EN).toEpochMilli()), resultado.frescura)
    }

    @Test
    fun `una lista desde la cache del servicio es un exito marcado (HA-L02)`() = runTest {
        responder("/v1/claims", 200, LISTA.replace("STALE", "true"))

        val resultado = repositorio.listar() as ResultadoDeConsulta.Exito

        assertTrue(resultado.frescura.desdeCache)
        assertEquals(Instant.parse(LEIDO_EN).toEpochMilli(), resultado.frescura.leidoEn)
    }

    @Test
    fun `sin as_of se toma la hora de la respuesta`() = runTest {
        responder("/v1/claims", 200, """{"items":[]}""")

        val resultado = repositorio.listar() as ResultadoDeConsulta.Exito

        assertEquals(Frescura(false, AHORA), resultado.frescura)
    }

    @Test
    fun `el detalle junta el siniestro y su historial, y la cache de cualquiera marca el todo`() = runTest {
        responder("/v1/claims/$ID", 200, DETALLE.replace("STALE", "false"))
        responder("/v1/claims/$ID/history", 200, HISTORIAL.replace("STALE", "true"))

        val resultado = repositorio.detalle(ID) as ResultadoDeConsulta.Exito

        assertEquals("Carrera 15 # 93-40, apto 402", resultado.valor.siniestro.lugar)
        assertEquals(
            listOf(EstadoDeSiniestro.RADICADO, EstadoDeSiniestro.EN_EVALUACION),
            resultado.valor.cambios.map { it.estado },
        )
        assertTrue(resultado.frescura.desdeCache)
    }

    @Test
    fun `un siniestro ajeno o inexistente es no encontrado`() = runTest {
        responder("/v1/claims/$ID", 404, """{"error":"claim_not_found"}""")
        responder("/v1/claims/$ID/history", 404, """{"error":"claim_not_found"}""")

        assertEquals(ResultadoDeConsulta.NoEncontrado, repositorio.detalle(ID))
    }

    @Test
    fun `un 503 sin base ni cache es un error`() = runTest {
        responder("/v1/claims", 503, """{"error":"claims_unavailable"}""")

        assertEquals(ResultadoDeConsulta.Error, repositorio.listar())
    }

    @Test
    fun `un cuerpo que no se puede leer es un error`() = runTest {
        responder("/v1/claims", 200, """{"items":[{"claim_id":"x"}]}""")

        assertEquals(ResultadoDeConsulta.Error, repositorio.listar())
    }

    @Test
    fun `sin red es sin conexion`() = runTest {
        // Un servidor que ya se apagó: la conexión se rechaza.
        val apagado = MockWebServer().apply { start() }
        val url = apagado.url("/")
        apagado.close()

        assertEquals(ResultadoDeConsulta.SinConexion, ConsultaDeSiniestrosRepositorioHttp(apiHacia(url)) { AHORA }.listar())
    }

    @Test
    fun `un caso radicado desde el movil deduce la linea del numero de la poliza`() {
        val dto = PolizaDelSiniestroDto("AU-2026-000001")
        assertEquals("auto", MapeadorDeSiniestros.lineaDe(dto))
        assertEquals("travel", MapeadorDeSiniestros.lineaDe(dto.copy(id = "VJ-2026-000003")))
        assertEquals("life", MapeadorDeSiniestros.lineaDe(dto.copy(id = "VH-2026-004512")))
        assertNull(MapeadorDeSiniestros.lineaDe(dto.copy(id = "XX-1")))
        assertEquals("home", MapeadorDeSiniestros.lineaDe(dto.copy(id = "AU-1", linea = "home")))
    }

    private companion object {
        const val ID = "8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90"
        const val LEIDO_EN = "2026-09-16T19:30:00Z"
        const val POLIZA = """{"policy_id":"HG-2025-880231","line":null,"product":null,"insured_object":null}"""
        const val LISTA = """{"items":[{"claim_id":"$ID","filing_number":"SIN-2026-01655","status":"in_evaluation",
            "channel":"mobile","policy":$POLIZA,"incident_type":"water_damage","occurred_at":"2026-09-02T03:15:00Z",
            "filed_at":"2026-09-04T00:20:00Z"}],"next_page_token":null,"stale":STALE,"as_of":"$LEIDO_EN"}"""
        const val DETALLE = """{"claim_id":"$ID","filing_number":"SIN-2026-01655","status":"in_evaluation",
            "channel":"mobile","policy":$POLIZA,"incident_type":"water_damage","occurred_at":"2026-09-02T03:15:00Z",
            "filed_at":"2026-09-04T00:20:00Z","place":"Carrera 15 # 93-40, apto 402","description":"Fuga en la cocina",
            "supports":{},"evidence":[],"stale":STALE,"as_of":"$LEIDO_EN"}"""
        const val HISTORIAL = """{"claim_id":"$ID","filing_number":"SIN-2026-01655","status":"in_evaluation",
            "last_changed_at":"2026-09-05T15:02:00Z","changes":[
            {"id":9,"status":"in_evaluation","previous_status":"active","changed_at":"2026-09-05T15:02:00Z"},
            {"id":7,"status":"active","previous_status":null,"changed_at":"2026-09-04T00:20:00Z"}],
            "reviews":[],"last_return":null,"stale":STALE,"as_of":"$LEIDO_EN"}"""
    }
}
