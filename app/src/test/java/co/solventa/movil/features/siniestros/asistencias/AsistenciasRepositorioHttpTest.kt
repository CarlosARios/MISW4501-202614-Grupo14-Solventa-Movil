package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.core.api.siniestros.SiniestrosApi
import co.solventa.movil.core.errores.Correlacion
import co.solventa.movil.core.persistencia.entidades.SolicitudDeAsistenciaEntidad
import co.solventa.movil.features.siniestros.UsuarioActual
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.AHORA
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.compartida
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.poliza
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
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
import java.util.concurrent.TimeUnit

/**
 * La solicitud de asistencia contra el contrato de
 * `POST /v1/claims/assistance-requests` (HU-M2.3-01), con el servicio simulado:
 * qué cuerpo viaja, que la clave se reusa en los reintentos y a dónde lleva
 * cada código (S5).
 */
class AsistenciasRepositorioHttpTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: SiniestrosApi
    private val cola = ColaEnMemoria()
    private val programador = ProgramadorContador()
    private val avisador = AvisadorRegistro()
    private val registros = mutableListOf<String>()
    private var ahora = AHORA

    @Before
    fun antes() {
        servidor = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiniestrosApi::class.java)
    }

    @After
    fun despues() {
        servidor.close()
    }

    private fun repositorio(lugar: String? = "Autopista Norte km 12, Bogotá") = AsistenciasRepositorioImpl(
        api = api,
        dao = cola,
        cifrador = DatosDeAsistencia.cifrador(),
        usuario = UsuarioActual { "sub-a" },
        conectividad = ConectividadFija(),
        programador = programador,
        traductor = TraductorFijo(lugar),
        avisador = avisador,
        reloj = { ahora },
        nuevaClave = { "clave-1" },
        registrar = { registros += it },
    )

    @Test
    fun `una solicitud sent viaja con el cuerpo exacto del contrato y la confirmacion se guarda`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 201, body = CREADA))

        val id = repositorio.solicitar(poliza, compartida)!!
        assertEquals(ResultadoDelEnvio.COMPLETO, repositorio.enviarPendientes())

        val peticion = servidor.takeRequest()
        assertEquals("/v1/claims/assistance-requests", peticion.url.encodedPath)
        assertEquals("POST", peticion.method)
        assertEquals("clave-1", peticion.headers[SiniestrosApi.ENCABEZADO_IDEMPOTENCIA])
        assertTrue(Correlacion.esValido(peticion.headers[Correlacion.ENCABEZADO].orEmpty()))
        assertEquals(
            """{"policy_id":"AU-2025-118204","location":{"status":"sent","latitude":4.7712,"longitude":-74.0452,""" +
                """"accuracy_m":8.0,"captured_at":"2026-11-15T02:39:51Z"},"place":"Autopista Norte km 12, Bogotá"}""",
            peticion.body?.utf8(),
        )

        val guardada = repositorio.observarUltima().first()!!
        assertEquals(id, guardada.id)
        assertEquals(EstadoDeEnvio.Confirmada, guardada.envio)
        assertEquals("AST-2026-0001", guardada.numero)
        assertEquals("5004f477-d43b-4b8a-80b0-b045207c5d11", guardada.requestId)
        assertEquals(1_794_710_337_043L, guardada.registradaEn)
        assertEquals(EstadoDeLaSolicitud.NUEVA, guardada.estado)
        assertEquals("Autopista Norte km 12, Bogotá", guardada.lugar)
    }

    @Test
    fun `una solicitud unavailable viaja sin coordenadas ni lugar`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 201, body = CREADA))

        repositorio.solicitar(poliza, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO))
        repositorio.enviarPendientes()

        assertEquals(
            """{"policy_id":"AU-2025-118204","location":{"status":"unavailable","unavailable_reason":"permission_denied"}}""",
            servidor.takeRequest().body?.utf8(),
        )
    }

    @Test
    fun `dos intentos llevan la misma clave y el mismo cuerpo`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 503, body = """{"error":"policy_directory_unavailable","message":"x"}"""))
        servidor.enqueue(MockResponse(code = 200, body = CREADA))

        val id = repositorio.solicitar(poliza, compartida)!!
        assertEquals(ResultadoDelEnvio.REINTENTAR, repositorio.enviarPendientes())
        assertEquals(1, cola.una(id).intentos)
        assertEquals(ResultadoDelEnvio.COMPLETO, repositorio.enviarPendientes())

        val primera = servidor.takeRequest()
        val segunda = servidor.takeRequest()
        assertEquals(primera.headers[SiniestrosApi.ENCABEZADO_IDEMPOTENCIA], segunda.headers[SiniestrosApi.ENCABEZADO_IDEMPOTENCIA])
        assertEquals(primera.body?.utf8(), segunda.body?.utf8())
        assertEquals(SolicitudDeAsistenciaEntidad.CONFIRMADA, cola.una(id).estadoDeEnvio)
    }

    @Test
    fun `los codigos reintentables dejan la solicitud en la cola`() = runTest {
        listOf(401, 404, 500, 502, 503).forEach { codigo ->
            servidor.enqueue(MockResponse(code = codigo, body = """{"error":"x","message":"y"}"""))
            val repositorio = repositorio()
            val id = repositorio.solicitar(poliza, compartida)!!

            assertEquals("http $codigo", ResultadoDelEnvio.REINTENTAR, repositorio.enviarPendientes())
            assertEquals("http $codigo", SolicitudDeAsistenciaEntidad.EN_COLA, cola.una(id).estadoDeEnvio)
            cola.filas.value = emptyList()
        }
        assertTrue(avisador.avisos.isEmpty())
    }

    @Test
    fun `una poliza no elegible se descarta con su motivo y se avisa`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 422, body = """{"error":"policy_not_eligible","reason":"policy_not_active"}"""))

        val id = repositorio.solicitar(poliza, compartida)!!
        assertEquals(ResultadoDelEnvio.COMPLETO, repositorio.enviarPendientes())

        assertEquals(SolicitudDeAsistenciaEntidad.DESCARTADA, cola.una(id).estadoDeEnvio)
        assertEquals(EstadoDeEnvio.Descartada(MotivoDeDescarte.POLIZA_NO_ACTIVA), repositorio.observarUltima().first()!!.envio)
        assertEquals(listOf(MotivoDeDescarte.POLIZA_NO_ACTIVA), avisador.avisos)
        assertTrue(registros.isEmpty())
    }

    @Test
    fun `los errores de la aplicacion se descartan y dejan la correlacion en el registro`() = runTest {
        val casos = listOf(
            MockResponse(code = 422, body = """{"error":"validation_failed","fields":[]}"""),
            MockResponse(code = 422, body = """{"error":"idempotency_key_reused"}"""),
            MockResponse(code = 422, body = "no es json"),
            MockResponse(code = 400, body = """{"error":"invalid_request"}"""),
            MockResponse(code = 403, body = """{"error":"forbidden"}"""),
        )
        casos.forEach { respuesta ->
            servidor.enqueue(respuesta)
            val repositorio = repositorio()
            val id = repositorio.solicitar(poliza, compartida)!!

            assertEquals(ResultadoDelEnvio.COMPLETO, repositorio.enviarPendientes())
            assertEquals(SolicitudDeAsistenciaEntidad.DESCARTADA, cola.una(id).estadoDeEnvio)
            assertEquals(MotivoDeDescarte.RECHAZADA.codigo, cola.una(id).motivoDeDescarte)
            cola.filas.value = emptyList()
        }
        assertEquals(5, registros.size)
        registros.forEach { linea ->
            assertTrue(linea, linea.contains("correlacion="))
            listOf("4.7712", "74.0452", "Autopista", "AU-2025-118204", "sub-a").forEach { dato ->
                assertTrue("el registro no debe llevar $dato", !linea.contains(dato))
            }
        }
    }

    @Test
    fun `con la pantalla a la vista el descarte no se notifica`() = runTest {
        val repositorio = repositorio()
        repositorio.pantallaVisible(true)
        servidor.enqueue(MockResponse(code = 403, body = """{"error":"forbidden"}"""))

        repositorio.solicitar(poliza, compartida)
        repositorio.enviarPendientes()

        assertTrue(avisador.avisos.isEmpty())
        repositorio.pantallaVisible(false)
    }

    @Test
    fun `sin red la solicitud sigue en la cola con un intento mas`() = runTest {
        val repositorio = repositorio()
        val id = repositorio.solicitar(poliza, compartida)!!
        // Sin servidor que conteste, la conexión se rechaza como sin red.
        servidor.close()

        assertEquals(ResultadoDelEnvio.REINTENTAR, repositorio.enviarPendientes())
        assertEquals(SolicitudDeAsistenciaEntidad.EN_COLA, cola.una(id).estadoDeEnvio)
        assertEquals(1, cola.una(id).intentos)
    }

    @Test
    fun `un cuerpo ilegible en la confirmacion se reintenta`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 201, body = "{ilegible"))

        val id = repositorio.solicitar(poliza, compartida)!!

        assertEquals(ResultadoDelEnvio.REINTENTAR, repositorio.enviarPendientes())
        assertEquals(SolicitudDeAsistenciaEntidad.EN_COLA, cola.una(id).estadoDeEnvio)
    }

    @Test
    fun `una respuesta exitosa sin cuerpo se reintenta`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 204))

        val id = repositorio.solicitar(poliza, compartida)!!

        assertEquals(ResultadoDelEnvio.REINTENTAR, repositorio.enviarPendientes())
        assertEquals(SolicitudDeAsistenciaEntidad.EN_COLA, cola.una(id).estadoDeEnvio)
    }

    @Test
    fun `una hora de registro ilegible usa la del telefono`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 201, body = CREADA.replace("2026-11-15T02:38:57.043927Z", "ayer")))

        repositorio.solicitar(poliza, compartida)
        repositorio.enviarPendientes()

        assertEquals(AHORA, repositorio.observarUltima().first()!!.registradaEn)
    }

    @Test
    fun `consultar el estado lo lee del servicio y lo guarda`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 201, body = CREADA))
        servidor.enqueue(MockResponse(code = 200, body = CREADA.replace("\"new\"", "\"dispatched\"")))
        repositorio.solicitar(poliza, compartida)
        repositorio.enviarPendientes()
        servidor.takeRequest()

        val estado = repositorio.consultarEstado("5004f477-d43b-4b8a-80b0-b045207c5d11")

        assertEquals(EstadoDeLaSolicitud.DESPACHADA, estado)
        assertEquals("/v1/claims/assistance-requests/5004f477-d43b-4b8a-80b0-b045207c5d11", servidor.takeRequest().url.encodedPath)
        assertEquals(EstadoDeLaSolicitud.DESPACHADA, repositorio.observarUltima().first()!!.estado)
    }

    @Test
    fun `consultar el estado sin exito devuelve nulo`() = runTest {
        val repositorio = repositorio()
        servidor.enqueue(MockResponse(code = 404, body = """{"error":"assistance_request_not_found"}"""))
        servidor.enqueue(MockResponse(code = 200, body = "{ilegible"))

        assertNull(repositorio.consultarEstado("a"))
        assertNull(repositorio.consultarEstado("b"))
        servidor.close()
        assertNull(repositorio.consultarEstado("c"))
    }

    @Test
    fun `sin sesion no se solicita ni se envia ni se consulta`() = runTest {
        val repositorio = AsistenciasRepositorioImpl(
            api, cola, DatosDeAsistencia.cifrador(), UsuarioActual { "" }, ConectividadFija(), programador,
            TraductorFijo(), avisador, { ahora }, { "clave-1" }, { registros += it },
        )

        assertNull(repositorio.solicitar(poliza, compartida))
        assertEquals(ResultadoDelEnvio.COMPLETO, repositorio.enviarPendientes())
        assertNull(repositorio.consultarEstado("a"))
        assertNull(repositorio.observarUltima().first())
        repositorio.reanudarPendientes()
        assertEquals(0, programador.veces)
        assertEquals(0, servidor.requestCount)
    }

    private companion object {
        val CREADA = """
            {
              "request_id": "5004f477-d43b-4b8a-80b0-b045207c5d11",
              "request_number": "AST-2026-0001",
              "status": "new",
              "policy": { "policy_id": "AU-2025-118204", "line": "auto", "product": "Autos", "insured_object": "KJT-482" },
              "location": { "status": "sent", "latitude": 4.7712, "longitude": -74.0452, "accuracy_m": 8,
                            "captured_at": "2026-11-15T02:39:51Z", "unavailable_reason": null },
              "place": "Autopista Norte km 12, Bogotá",
              "requested_at": "2026-11-15T02:38:57.043927Z"
            }
        """.trimIndent()
    }
}
