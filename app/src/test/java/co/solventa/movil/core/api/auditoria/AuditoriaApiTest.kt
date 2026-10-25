package co.solventa.movil.core.api.auditoria

import co.solventa.movil.core.auth.interceptores.InterceptorDeAutenticacion
import co.solventa.movil.core.auth.interceptores.ProveedorDeToken
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Provider

/**
 * El cliente del servicio de auditoria contra un servidor simulado: la ruta,
 * el token del titular que agrega el interceptor y cada respuesta que la
 * pantalla distingue (HU-M1.3-01).
 */
class AuditoriaApiTest {

    private lateinit var servidor: MockWebServer
    private lateinit var api: AuditoriaApi
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun antes() {
        servidor = MockWebServer().apply { start() }
        val proveedor = ProveedorDeToken { "token-del-titular" }
        val cliente = OkHttpClient.Builder()
            .addInterceptor(InterceptorDeAutenticacion(Provider { proveedor }))
            .build()
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuditoriaApi::class.java)
    }

    @After
    fun despues() {
        servidor.close()
    }

    @Test
    fun `la lista viaja con el token del titular y trae el detalle completo`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = LISTA))

        val respuesta = api.consentimientos()

        val peticion = servidor.takeRequest()
        assertEquals("/v1/consents", peticion.url.encodedPath)
        assertEquals("GET", peticion.method)
        assertEquals("Bearer token-del-titular", peticion.headers["Authorization"])
        assertTrue(respuesta.isSuccessful)
        val lista = respuesta.body()!!
        assertEquals(2, lista.elementos.size)
        val activo = lista.elementos[0]
        assertEquals("cns-1", activo.id)
        assertEquals("active", activo.estado)
        assertTrue(activo.vigente)
        assertEquals(EntidadDto("aliado-banco-1", "Banco Aliado"), activo.entidades.single())
        assertEquals(listOf("credit_products", "balances"), activo.alcance.single().categorias)
        assertEquals("2026-09-16T15:52:00Z", activo.otorgadoEn)
        assertNull(activo.finalizadoEn)
        assertNull(activo.comprobanteDeRevocacion)
        assertEquals("granted", activo.historial.single().estado)
        assertNull(activo.historial.single().comprobante)
        val revocado = lista.elementos[1]
        assertFalse(revocado.vigente)
        assertEquals("REV-0001", revocado.comprobanteDeRevocacion)
        assertEquals("REV-0001", revocado.historial.last().comprobante)
    }

    @Test
    fun `el detalle pide el consentimiento por su identificador`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = UNO))

        val respuesta = api.consentimiento("cns-1")

        assertEquals("/v1/consents/cns-1", servidor.takeRequest().url.encodedPath)
        assertEquals("cns-1", respuesta.body()?.id)
    }

    @Test
    fun `una cuenta sin vincular responde 403 holder_not_linked`() = runTest {
        servidor.enqueue(MockResponse(code = 403, body = """{"error":"holder_not_linked","message":"later"}"""))

        val respuesta = api.consentimientos()

        assertEquals(403, respuesta.code())
        val error = json.decodeFromString<ErrorDeAuditoriaDto>(respuesta.errorBody()!!.string())
        assertEquals(AuditoriaApi.ERROR_CUENTA_SIN_VINCULAR, error.error)
    }

    @Test
    fun `una sesion vencida y el servicio caido llegan como codigos`() = runTest {
        servidor.enqueue(MockResponse(code = 401, body = """{"error":"unauthorized","message":"x"}"""))
        servidor.enqueue(MockResponse(code = 503, body = """{"error":"service_unavailable","message":"x"}"""))

        assertEquals(401, api.consentimientos().code())
        assertEquals(503, api.consentimiento("cns-1").code())
    }

    @Test
    fun `los campos opcionales pueden faltar o ser nulos`() = runTest {
        servidor.enqueue(MockResponse(code = 200, body = MINIMO))

        val minimo = api.consentimientos().body()!!.elementos.single()

        assertTrue(minimo.entidades.isEmpty())
        assertTrue(minimo.alcance.isEmpty())
        assertTrue(minimo.historial.isEmpty())
        assertNull(minimo.finalizadoEn)
    }

    private companion object {
        val ACTIVO = """
            {"consent_id":"cns-1","status":"active","current":true,
             "entities":[{"code":"aliado-banco-1","name":"Banco Aliado"}],
             "purpose":"risk_profiling_mortgage_life_offer","channel":"embedded","terms_version":"v1",
             "scope":[{"code":"open_finance","data_categories":["credit_products","balances"],"validity":"12_months"}],
             "granted_at":"2026-09-16T15:52:00Z","expires_at":"2027-09-16T15:52:00Z","ended_at":null,
             "revocation_receipt_id":null,
             "history":[{"status":"granted","occurred_at":"2026-09-16T15:52:00Z","receipt_id":null,"channel":"embedded","entity_code":null}],
             "a_future_field":1}
        """.trimIndent()
        val REVOCADO = """
            {"consent_id":"cns-2","status":"revoked","current":false,
             "entities":[{"code":"aliado-banco-1","name":"Banco Aliado"}],
             "purpose":"risk_profiling_mortgage_life_offer","channel":"embedded","terms_version":"v1","scope":[],
             "granted_at":"2025-03-03T10:00:00Z","expires_at":"2026-03-03T10:00:00Z","ended_at":"2025-10-01T10:00:00Z",
             "revocation_receipt_id":"REV-0001",
             "history":[{"status":"granted","occurred_at":"2025-03-03T10:00:00Z"},
                        {"status":"revoked","occurred_at":"2025-10-01T10:00:00Z","receipt_id":"REV-0001","channel":"mobile"}]}
        """.trimIndent()
        val LISTA = """{"items":[$ACTIVO,$REVOCADO],"generated_at":"2026-10-24T20:00:00Z"}"""
        val UNO = ACTIVO
        val MINIMO = """
            {"items":[{"consent_id":"cns-3","status":"expired","current":false,"purpose":"p","channel":"embedded",
             "terms_version":"v1","granted_at":"2025-01-01T00:00:00Z","expires_at":"2026-01-01T00:00:00Z"}]}
        """.trimIndent()
    }
}
