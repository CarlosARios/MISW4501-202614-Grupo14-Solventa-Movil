package co.solventa.movil.features.siniestros.seguimiento

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
 * HU-M2.4-01 contra el servicio simulado: la resolución automática del
 * siniestro paramétrico (criterio 3), el tiempo estimado del triage en
 * evaluación (criterio 2) y la copia en el teléfono que se muestra sin red
 * (criterio 5).
 */
class ConsultaConResolucionYCopiaTest {

    private lateinit var servidor: MockWebServer
    private val copia = CopiaFalsa()
    private val respuestas = mutableMapOf<String, MockResponse>()
    private val pedidos = mutableListOf<String>()

    @Before
    fun antes() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                pedidos += request.url.encodedPath
                return respuestas[request.url.encodedPath] ?: MockResponse(code = 500)
            }
        }
        servidor.start()
    }

    @After
    fun despues() {
        servidor.close()
    }

    private fun repositorio(base: HttpUrl = servidor.url("/")) = ConsultaDeSiniestrosRepositorioHttp(apiHacia(base), copia) { AHORA }

    private fun apiHacia(base: HttpUrl): SiniestrosApi = Retrofit.Builder()
        .baseUrl(base)
        .client(OkHttpClient())
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SiniestrosApi::class.java)

    private fun responder(ruta: String, codigo: Int, cuerpo: String) {
        respuestas[ruta] = MockResponse(code = codigo, body = cuerpo)
    }

    /** Un servidor que ya se apagó: la conexión se rechaza, como sin red. */
    private fun sinRed(): HttpUrl {
        val apagado = MockWebServer().apply { start() }
        return apagado.url("/").also { apagado.close() }
    }

    @Test
    fun `la lista trae la resolucion automatica del siniestro parametrico y la de los demas es nula`() = runTest {
        responder("/v1/claims", 200, """{"items":[$PARAMETRICO,$MANUAL],"stale":false,"as_of":"$LEIDO_EN"}""")

        val resultado = repositorio().listar() as ResultadoDeConsulta.Exito

        val (automatico, manual) = resultado.valor
        assertEquals(
            ResolucionAutomatica(
                resueltaEn = instante("2026-09-11T21:39:30Z"),
                version = "parametric-rules-v1",
                disparador = Disparador("flight_delay", "AV-8421", 180.0, 215.0, "minutes", instante("2026-09-11T21:39:00Z")),
            ),
            automatico.resolucionAutomatica,
        )
        assertTrue(automatico.automatico)
        assertNull(manual.resolucionAutomatica)
    }

    @Test
    fun `en evaluacion el detalle trae el tiempo estimado del triage`() = runTest {
        responderDetalle(ID, "in_evaluation")
        responder("/v1/claims/$ID/triage", 200, TRIAJE)

        val resultado = repositorio().detalle(ID) as ResultadoDeConsulta.Exito

        assertEquals(EstimacionDeResolucion(nivel = 2, diasHabiles = 5), resultado.valor.estimacion)
    }

    @Test
    fun `un triage sin calcular o que falla deja el detalle sin estimado, no lo hace fallar`() = runTest {
        responderDetalle(ID, "in_evaluation")
        responder("/v1/claims/$ID/triage", 200, """{"view":"insured","claim_id":"$ID","state":"pending","level":null,
            "estimated_resolution_business_days":null,"assessed_at":null}""")
        assertNull((repositorio().detalle(ID) as ResultadoDeConsulta.Exito).valor.estimacion)

        responder("/v1/claims/$ID/triage", 503, """{"error":"claims_unavailable"}""")
        assertNull((repositorio().detalle(ID) as ResultadoDeConsulta.Exito).valor.estimacion)
    }

    @Test
    fun `fuera de evaluacion el triage no se pide`() = runTest {
        responderDetalle(ID, "paid")

        val resultado = repositorio().detalle(ID) as ResultadoDeConsulta.Exito

        assertNull(resultado.valor.estimacion)
        assertTrue(pedidos.none { it.endsWith("/triage") })
    }

    @Test
    fun `cada respuesta se guarda con la hora de sus datos`() = runTest {
        responder("/v1/claims", 200, """{"items":[$MANUAL],"stale":true,"as_of":"$LEIDO_EN"}""")
        responderDetalle(ID, "in_evaluation")

        repositorio().listar()
        repositorio().detalle(ID)

        assertEquals(instante(LEIDO_EN), copia.lista?.guardadoEn)
        assertEquals(listOf(ID), copia.lista?.valor?.map { it.id })
        assertEquals(instante(LEIDO_EN), copia.detalles[ID]?.guardadoEn)
    }

    @Test
    fun `sin red se muestra la ultima version guardada, con su hora`() = runTest {
        val guardado = DetalleDeSiniestro(siniestro(id = ID), emptyList())
        copia.guardarLista(listOf(siniestro(id = ID)), AHORA - HORA)
        copia.guardarDetalle(guardado, AHORA - 2 * HORA)
        val offline = repositorio(sinRed())

        assertEquals(ResultadoDeConsulta.CopiaLocal(listOf(siniestro(id = ID)), AHORA - HORA), offline.listar())
        assertEquals(ResultadoDeConsulta.CopiaLocal(guardado, AHORA - 2 * HORA), offline.detalle(ID))
    }

    @Test
    fun `sin red y sin copia es sin conexion`() = runTest {
        val offline = repositorio(sinRed())

        assertEquals(ResultadoDeConsulta.SinConexion, offline.listar())
        assertEquals(ResultadoDeConsulta.SinConexion, offline.detalle(ID))
    }

    @Test
    fun `un error del servicio no reemplaza la copia ni la muestra como sin conexion`() = runTest {
        copia.guardarLista(listOf(siniestro(id = ID)), AHORA - HORA)
        responder("/v1/claims", 503, """{"error":"claims_unavailable"}""")

        assertEquals(ResultadoDeConsulta.Error, repositorio().listar())
        assertEquals(AHORA - HORA, copia.lista?.guardadoEn)
    }

    private fun responderDetalle(id: String, estado: String) {
        responder(
            "/v1/claims/$id",
            200,
            """{"claim_id":"$id","filing_number":"SIN-2026-01655","status":"$estado","channel":"web","policy":$POLIZA,
                "incident_type":"water_damage","occurred_at":"2026-09-02T03:15:00Z","filed_at":"2026-09-04T00:20:00Z",
                "place":"Carrera 15 # 93-40, apto 402","automatic_resolution":null,"stale":false,"as_of":"$LEIDO_EN"}""",
        )
        responder(
            "/v1/claims/$id/history",
            200,
            """{"claim_id":"$id","status":"$estado","changes":[
                {"status":"active","previous_status":null,"changed_at":"2026-09-04T00:20:00Z"}],"stale":false,"as_of":"$LEIDO_EN"}""",
        )
    }

    private fun instante(texto: String) = Instant.parse(texto).toEpochMilli()

    private companion object {
        const val ID = "8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90"
        const val LEIDO_EN = "2026-09-16T19:30:00Z"
        const val POLIZA = """{"policy_id":"HG-2025-880231","line":"home","product":"Hogar","insured_object":null}"""
        const val MANUAL = """{"claim_id":"$ID","filing_number":"SIN-2026-01655","status":"in_evaluation","channel":"web",
            "policy":$POLIZA,"incident_type":"water_damage","occurred_at":"2026-09-02T03:15:00Z",
            "filed_at":"2026-09-04T00:20:00Z","automatic_resolution":null}"""
        const val PARAMETRICO = """{"claim_id":"6f1c2a7e-3b4d-4e5f-8a9b-0c1d2e3f4a5b","filing_number":"SIN-2026-01702",
            "status":"paid","channel":"parametric",
            "policy":{"policy_id":"VJ-2026-000931","line":"travel","product":"Viaje","insured_object":"Viajes nacionales"},
            "incident_type":"flight_delay","occurred_at":"2026-09-11T21:39:00Z","filed_at":"2026-09-11T21:39:30Z",
            "automatic_resolution":{"resolved_at":"2026-09-11T21:39:30Z","rule_version":"parametric-rules-v1",
            "trigger":{"parameter":"flight_delay","event_reference":"AV-8421","threshold":180,"observed":215,
            "unit":"minutes","observed_at":"2026-09-11T21:39:00Z"}}}"""
        const val TRIAJE = """{"view":"insured","claim_id":"$ID","state":"assessed","level":2,
            "estimated_resolution_business_days":5,"assessed_at":"2026-09-05T15:03:00Z"}"""
    }
}
