package co.solventa.movil.features.acceso.registro

import co.solventa.movil.core.api.originacion.OriginacionApi
import co.solventa.movil.core.errores.Correlacion
import co.solventa.movil.core.imagen.PreparadorDeCaptura
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
    private lateinit var preparador: PreparadorFalso

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
        preparador = PreparadorFalso()
        repositorio = RegistroRepositorioHttp(api, preparador)
    }

    private suspend fun registrar(datos: DatosDocumento = DATOS) =
        repositorio.registrar(datos, CONTRASENA, FRENTE, REVERSO)

    /**
     * El preparador reduce la captura y la codifica; aquí basta con saber qué
     * ruta se le pidió y qué devolvió.
     */
    private class PreparadorFalso : PreparadorDeCaptura {
        var ilegibles: Set<String> = emptySet()
        val descartados = mutableListOf<String>()

        override suspend fun aBase64(rutaArchivo: String): String? = when {
            rutaArchivo in ilegibles -> null
            rutaArchivo == FRENTE.rutaArchivo -> FRENTE_EN_BASE64
            else -> REVERSO_EN_BASE64
        }

        override suspend fun descartar(rutaArchivo: String) {
            descartados += rutaArchivo
        }
    }

    @After
    fun despues() {
        servidor.close()
    }

    @Test
    fun `un registro aceptado devuelve la cuenta del user pool`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        val resultado = registrar()

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

        registrar()

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

        registrar(DATOS.copy(fechaNacimiento = "5/9/1991"))

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

        val resultado = registrar()

        val enviado = servidor.takeRequest().headers[Correlacion.ENCABEZADO]
        assertTrue(Correlacion.esValido(enviado.orEmpty()))
        assertEquals(ResultadoRegistro.NoDisponible(enviado.orEmpty()), resultado)
    }

    @Test
    fun `un documento ya registrado es conflicto y no una falla`() = runTest {
        servidor.enqueue(MockResponse(code = 409, body = """{"error":"already_registered"}"""))

        assertEquals(ResultadoRegistro.YaRegistrado, registrar())
    }

    @Test
    fun `los campos rechazados se traducen a los del formulario`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = CUERPO_INVALIDO))

        val resultado = registrar()

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

        val resultado = registrar()

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

        val resultado = registrar()

        assertTrue(resultado is ResultadoRegistro.NoDisponible)
    }

    @Test
    fun `sin red el registro queda reintentable`() = runTest {
        servidor.close()

        val resultado = registrar()

        assertTrue(resultado is ResultadoRegistro.NoDisponible)
    }

    // Las dos caras del documento son parte del cuerpo: es lo que hace que un
    // 201 signifique cliente, cuenta y documento guardados.
    @Test
    fun `las dos caras viajan en el cuerpo`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        registrar()

        val enviado = servidor.takeRequest().body?.utf8().orEmpty()
        assertTrue(enviado, enviado.contains("\"document_front\":\"$FRENTE_EN_BASE64\""))
        assertTrue(enviado, enviado.contains("\"document_back\":\"$REVERSO_EN_BASE64\""))
    }

    /**
     * La caché del sistema se puede vaciar en cualquier momento. Sin la imagen
     * no hay nada que verificar, así que no se llama al servicio.
     */
    @Test
    fun `una captura que ya no se puede leer no se envia`() = runTest {
        preparador.ilegibles = setOf(FRENTE.rutaArchivo)

        val resultado = registrar()

        assertTrue(resultado is ResultadoRegistro.DocumentoNoLegible)
        assertEquals("no se llamó al servicio", 0, servidor.requestCount)
    }

    /**
     * Una cara rechazada no se corrige en M1c: el camino es volver a capturar,
     * que es lo que ofrece M1e (criterio 3).
     */
    @Test
    fun `una cara rechazada lleva a volver a capturar`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = CUERPO_CARA_INVALIDA))

        val resultado = registrar()

        val enviado = servidor.takeRequest().headers[Correlacion.ENCABEZADO]
        assertEquals(ResultadoRegistro.DocumentoNoLegible(enviado.orEmpty()), resultado)
    }

    /**
     * Si el servicio rechaza una cara y además un campo del formulario, manda
     * la cara: corregir el correo no arregla la foto.
     */
    @Test
    fun `la cara rechazada manda sobre los campos del formulario`() = runTest {
        servidor.enqueue(MockResponse(code = 422, body = CUERPO_CARA_Y_CORREO))

        val resultado = registrar()

        assertTrue(resultado.toString(), resultado is ResultadoRegistro.DocumentoNoLegible)
    }

    /**
     * Borrar las capturas del teléfono es del ViewModel, que es el que sabe
     * cuándo una imagen dejó de hacer falta. El repositorio solo las envía.
     */
    // Criterio 3 — el proveedor de identidad no aprobó: M1e, sin motivo.
    @Test
    fun `una identidad no verificada lleva a M1e con la referencia`() = runTest {
        servidor.enqueue(
            MockResponse(
                code = 422,
                body = """{"error":"identity_not_verified","message":"the identity could not be verified"}""",
            ),
        )

        val resultado = registrar()

        val enviado = servidor.takeRequest()
        assertEquals(
            ResultadoRegistro.IdentidadNoVerificada(enviado.headers[Correlacion.ENCABEZADO]!!),
            resultado,
        )
    }

    // Criterio 1 — el proveedor lee el documento y M1c lo muestra.
    @Test
    fun `la lectura devuelve los datos como los muestra M1c`() = runTest {
        servidor.enqueue(
            MockResponse(
                code = 200,
                body = """{"document_type":"CC","document_id":"1020345678","given_names":"Carolina",
                    "surnames":"Pérez Ruiz","birth_date":"1992-04-18"}""",
            ),
        )

        val resultado = repositorio.leerDocumento(FRENTE, REVERSO)

        assertEquals(
            ResultadoLectura.Leido(
                DatosDocumento(
                    nombres = "Carolina",
                    apellidos = "Pérez Ruiz",
                    tipoDocumento = "CC",
                    numeroDocumento = "1020345678",
                    fechaNacimiento = "18/04/1992",
                ),
            ),
            resultado,
        )
        val enviado = servidor.takeRequest()
        assertEquals("/v1/registrations/document-reads", enviado.url.encodedPath)
        val cuerpo = enviado.body!!.utf8()
        assertTrue(cuerpo.contains(FRENTE_EN_BASE64))
        assertTrue(cuerpo.contains(REVERSO_EN_BASE64))
    }

    @Test
    fun `una lectura que falla deja una referencia`() = runTest {
        servidor.enqueue(MockResponse(code = 503, body = """{"error":"document_read_unavailable"}"""))

        val resultado = repositorio.leerDocumento(FRENTE, REVERSO)

        val enviado = servidor.takeRequest()
        assertEquals(ResultadoLectura.NoLeido(enviado.headers[Correlacion.ENCABEZADO]!!), resultado)
    }

    @Test
    fun `una captura ilegible no se manda a leer`() = runTest {
        preparador.ilegibles = setOf(REVERSO.rutaArchivo)

        val resultado = repositorio.leerDocumento(FRENTE, REVERSO)

        assertTrue(resultado is ResultadoLectura.NoLeido)
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun `el repositorio no borra las capturas`() = runTest {
        servidor.enqueue(MockResponse(code = 201, body = CUERPO_CREADO))

        registrar()

        assertTrue(preparador.descartados.isEmpty())
    }

    private companion object {
        const val CONTRASENA = "Solventa.2026.4821"

        val FRENTE = ImagenDocumento(CaraDocumento.FRENTE, "/cache/documento-frente-1.jpg")
        val REVERSO = ImagenDocumento(CaraDocumento.REVERSO, "/cache/documento-reverso-1.jpg")
        const val FRENTE_EN_BASE64 = "ZnJlbnRl"
        const val REVERSO_EN_BASE64 = "cmV2ZXJzbw=="

        const val CUERPO_CREADO = """
            {"customer_id":"cus-1","document_id":"1020345678","subject":"sub-carolina",
             "registered_at":"2026-10-22T15:00:00Z"}
        """

        const val CUERPO_CARA_INVALIDA = """
            {"error":"validation_failed","message":"one or more fields are missing or invalid",
             "fields":[{"field":"document_front","code":"invalid_format"}]}
        """

        const val CUERPO_CARA_Y_CORREO = """
            {"error":"validation_failed","message":"one or more fields are missing or invalid",
             "fields":[{"field":"email","code":"invalid_format"},
                       {"field":"document_back","code":"out_of_range"}]}
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
