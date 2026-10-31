package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.core.api.siniestros.EvidenciaRadicada
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
import org.junit.Assert.assertFalse
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

    /** HU-M2.2-01: el monto estimado viaja en centavos y solo si se declaró. */
    @Test
    fun `el monto estimado viaja en centavos solo cuando se declaro`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        repositorio.radicar(BORRADOR, radicacion().copy(montoEstimado = "4.800.000"))
        repositorio.radicar(BORRADOR, radicacion())

        val conMonto = servidor.takeRequest().body?.utf8().orEmpty()
        assertTrue(conMonto, conMonto.contains("\"estimated_amount_minor\":480000000"))
        val sinMonto = servidor.takeRequest().body?.utf8().orEmpty()
        assertFalse(sinMonto, sinMonto.contains("estimated_amount_minor"))
    }

    @Test
    fun `el monto rechazado por el servicio se marca en los hechos`() = runTest {
        servidor.enqueue(
            MockResponse(
                code = 422,
                body = """{"error":"validation_failed","message":"x",
                    "fields":[{"field":"estimated_amount_minor","code":"out_of_range"}]}""",
            ),
        )

        val resultado = repositorio.radicar(BORRADOR, radicacion())

        assertEquals(ResultadoRadicacion.DatosInvalidos(setOf(CampoDeRadicacion.MONTO)), resultado)
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
        const val CUERPO_EVIDENCIA_INVALIDA = """
            {"error":"validation_failed","message":"one or more fields are missing or invalid",
             "fields":[{"field":"evidence.0.object_key","code":"not_stored"}]}
        """

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

    // La evidencia de HU-M2.1-01 viaja con la radicación: sus llaves y lo que
    // era cierto al capturarla.

    @Test
    fun `la evidencia viaja con sus llaves y sus metadatos`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        repositorio.radicar(BORRADOR, radicacion(), listOf(evidencia()))

        val enviado = servidor.takeRequest().body?.utf8().orEmpty()
        assertTrue(enviado, enviado.contains("\"object_key\":\"evidence/sub/una.jpg\""))
        assertTrue(enviado, enviado.contains("\"kind\":\"photo\""))
        assertTrue(enviado, enviado.contains("\"location_trusted\":true"))
        assertTrue(enviado, enviado.contains("\"accuracy_m\":8.0"))
    }

    /** Sin evidencia el campo no viaja, y el servicio lo lee como vacío. */
    @Test
    fun `sin evidencia el cuerpo no lleva el campo`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        repositorio.radicar(BORRADOR, radicacion())

        val enviado = servidor.takeRequest().body?.utf8().orEmpty()
        assertFalse(enviado, enviado.contains("evidence"))
    }

    /**
     * El servicio nombra cada evidencia rechazada con su posición, y todas se
     * corrigen en el mismo sitio: volviendo a capturar.
     */
    @Test
    fun `una evidencia rechazada devuelve al paso de evidencias`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = CUERPO_EVIDENCIA_INVALIDA))

        val resultado = repositorio.radicar(BORRADOR, radicacion(), listOf(evidencia()))

        assertEquals(
            ResultadoRadicacion.DatosInvalidos(setOf(CampoDeRadicacion.EVIDENCIAS)),
            resultado,
        )
    }

    private fun evidencia() = EvidenciaRadicada(
        llaveDelObjeto = "evidence/sub/una.jpg",
        tipo = "photo",
        capturadaEn = "2026-10-20T23:32:14Z",
        latitud = 4.7712,
        longitud = -74.0452,
        precisionM = 8.0,
        ubicacionConfiable = true,
        sha256 = "a".repeat(64),
        tamanoBytes = 312_044,
    )
}
