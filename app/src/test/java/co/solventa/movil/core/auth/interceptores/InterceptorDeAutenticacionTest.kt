package co.solventa.movil.core.auth.interceptores

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

/**
 * El interceptor adjunta el *id token* de la sesión a cada petición del API
 * Gateway (HA-S01).
 */
class InterceptorDeAutenticacionTest {

    private lateinit var servidor: MockWebServer

    @Before
    fun antes() {
        servidor = MockWebServer().apply { start() }
    }

    @After
    fun despues() {
        servidor.close()
    }

    private fun clienteCon(token: String?): OkHttpClient {
        val proveedor = ProveedorDeToken { token }
        return OkHttpClient.Builder()
            .addInterceptor(InterceptorDeAutenticacion(Provider { proveedor }))
            .build()
    }

    private fun pedir(cliente: OkHttpClient) {
        servidor.enqueue(MockResponse(code = 200))
        cliente.newCall(Request.Builder().url(servidor.url("/v1/policies")).build()).execute().close()
    }

    @Test
    fun `con sesion la peticion lleva el id token como portador`() {
        pedir(clienteCon("token-de-identidad"))

        assertEquals("Bearer token-de-identidad", servidor.takeRequest().headers["Authorization"])
    }

    /**
     * No le corresponde a esta capa decidir qué rutas necesitan sesión: la
     * petición sale sin encabezado y el backend responde 401 si hacía falta.
     */
    @Test
    fun `sin sesion la peticion sale sin encabezado`() {
        pedir(clienteCon(null))

        assertNull(servidor.takeRequest().headers["Authorization"])
    }

    @Test
    fun `un token en blanco se trata como ausencia de sesion`() {
        pedir(clienteCon("   "))

        assertNull(servidor.takeRequest().headers["Authorization"])
    }
}
