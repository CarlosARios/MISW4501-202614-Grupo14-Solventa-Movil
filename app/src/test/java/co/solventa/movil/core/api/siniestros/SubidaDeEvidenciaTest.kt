package co.solventa.movil.core.api.siniestros

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File

/**
 * La subida de una evidencia (HU-M2.1-01) con el servicio y el almacenamiento
 * simulados. Son dos peticiones y cada una tiene su regla: la primera pide la
 * URL firmada al servicio, con la sesión; la segunda lleva el archivo al
 * almacenamiento, sin sesión y con los encabezados que la firma cubre.
 */
class SubidaDeEvidenciaTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private lateinit var servidor: MockWebServer
    private lateinit var api: SiniestrosApi
    private lateinit var subidor: SubidorDeArchivos

    @Before
    fun antes() {
        servidor = MockWebServer().apply { start() }
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiniestrosApi::class.java)
        subidor = SubidorDeArchivos(OkHttpClient())
    }

    @After
    fun despues() {
        servidor.close()
    }

    @Test
    fun `la declaracion viaja con el tipo el tamano y el resumen`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_FIRMADO))

        val respuesta = api.urlDeSubida(
            Correlacion.nuevo(),
            SolicitudDeSubida(
                tipo = "photo",
                tipoDeContenido = "image/jpeg",
                tamanoBytes = 312_044,
                sha256 = SHA256,
            ),
        )

        val peticion = servidor.takeRequest()
        assertEquals("/v1/claims/evidence/upload-url", peticion.url.encodedPath)
        assertEquals("POST", peticion.method)
        assertTrue(Correlacion.esValido(peticion.headers[Correlacion.ENCABEZADO].orEmpty()))
        val enviado = peticion.body?.utf8().orEmpty()
        assertTrue(enviado.contains("\"kind\":\"photo\""))
        assertTrue(enviado.contains("\"content_type\":\"image/jpeg\""))
        assertTrue(enviado.contains("\"byte_size\":312044"))
        assertTrue(enviado.contains("\"sha256\":\"$SHA256\""))

        val firmada = respuesta.body()
        assertEquals("evidence/sub-carolina/$SHA256.jpg", firmada?.llaveDelObjeto)
        assertEquals("image/jpeg", firmada?.encabezados?.get("Content-Type"))
        assertEquals("2026-10-23T15:10:00Z", firmada?.venceEn)
    }

    /**
     * El `PUT` lleva los encabezados que la firma cubre y el archivo tal cual.
     * `Content-Length` no se repite: OkHttp lo calcula y un encabezado
     * duplicado invalida la firma.
     */
    @Test
    fun `el archivo va al almacenamiento con los encabezados firmados`() = runTest {
        servidor.enqueue(MockResponse(code = 200))
        val archivo = conContenido()

        val quedo = subidor.subir(
            archivo,
            servidor.url("/evidence/sub-carolina/$SHA256.jpg").toString(),
            mapOf(
                "Content-Type" to "image/jpeg",
                "x-amz-checksum-sha256" to "resumen-en-base64",
                "Content-Length" to "4",
            ),
        )

        assertTrue(quedo)
        val peticion = servidor.takeRequest()
        assertEquals("PUT", peticion.method)
        assertEquals("image/jpeg", peticion.headers["Content-Type"])
        assertEquals("resumen-en-base64", peticion.headers["x-amz-checksum-sha256"])
        assertEquals(1, peticion.headers.values("Content-Length").size)
        assertNull(peticion.headers["Authorization"])
        assertEquals(4, peticion.body?.size)
    }

    /** Una URL vencida o una firma que no corresponde se resuelve pidiendo otra. */
    @Test
    fun `un rechazo del almacenamiento no es una subida`() = runTest {
        servidor.enqueue(MockResponse(code = 403))

        val quedo = subidor.subir(conContenido(), servidor.url("/evidencia.jpg").toString(), emptyMap())

        assertFalse(quedo)
    }

    /** Sin red la subida no queda, y el trabajador la reintenta después. */
    @Test
    fun `un almacenamiento que no responde no es una subida`() = runTest {
        val url = servidor.url("/evidencia.jpg").toString()
        servidor.close()

        assertFalse(subidor.subir(conContenido(), url, emptyMap()))
    }

    private fun conContenido(): File =
        carpeta.newFile().apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }

    private companion object {
        const val SHA256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

        val CUERPO_FIRMADO = """
            {
              "object_key": "evidence/sub-carolina/$SHA256.jpg",
              "upload_url": "https://bucket.s3.amazonaws.com/evidence/sub-carolina/$SHA256.jpg?X-Amz-Signature=abc",
              "upload_headers": {
                "Content-Type": "image/jpeg",
                "Content-Length": "312044",
                "x-amz-checksum-sha256": "qqqq"
              },
              "expires_at": "2026-10-23T15:10:00Z"
            }
        """.trimIndent()
    }
}
