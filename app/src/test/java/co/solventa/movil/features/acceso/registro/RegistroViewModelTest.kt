package co.solventa.movil.features.acceso.registro

import app.cash.turbine.test
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Una prueba por criterio de aceptación de HU-M1.1-01, más lo que el
 * servicio de registro puede responder.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var kyc: KycFalso
    private lateinit var registro: RegistroFalso
    private lateinit var sesion: SesionFalsa

    @Before
    fun antes() {
        Dispatchers.setMain(dispatcher)
        kyc = KycFalso()
        registro = RegistroFalso()
        sesion = SesionFalsa()
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() =
        RegistroViewModel(kyc, registro, GeneradorDeDatosDeDemostracion(), sesion)

    // Criterio 1 — guiar la captura del frente y del reverso y confirmar los
    // datos antes de enviarlos.
    @Test
    fun `capturar las dos caras deja el formulario listo para confirmar`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()

            viewModel.capturar(ImagenDocumento(CaraDocumento.FRENTE, "/tmp/frente.jpg"))
            advanceUntilIdle()
            assertNull("El formulario aparece al cerrar la captura", viewModel.estado.value.datos)

            viewModel.capturar(ImagenDocumento(CaraDocumento.REVERSO, "/tmp/reverso.jpg"))
            advanceUntilIdle()

            val estado = viewModel.estado.value
            assertNotNull(estado.datos)
            assertTrue("Viene con contraseña puesta", estado.contrasena.isNotBlank())
            assertFalse("Faltan los términos", estado.puedeVerificar)
        }

    /** Volver a M1b no puede borrar lo que el usuario ya corrigió en M1c. */
    @Test
    fun `volver a capturar el reverso conserva los datos del formulario`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            capturarAmbasCaras(viewModel)
            val primeros = viewModel.estado.value.datos

            viewModel.capturar(ImagenDocumento(CaraDocumento.REVERSO, "/tmp/otro.jpg"))
            advanceUntilIdle()

            assertEquals(primeros, viewModel.estado.value.datos)
        }

    // Criterio 4 — la poca luz la mide la cámara y el aviso sale antes de
    // tomar la foto, no después de enviarla.
    @Test
    fun `la camara reporta poca luz y se avisa sin llegar a capturar`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()

            viewModel.reportarLuzInsuficiente()

            assertEquals(
                ProblemaCalidad.ILUMINACION_INSUFICIENTE,
                viewModel.estado.value.problemaCalidad,
            )
            assertEquals("No se debe analizar una foto que no se tomó", 0, kyc.capturasAnalizadas)
            assertNull(viewModel.estado.value.frente)
        }

    @Test
    fun `el aviso de poca luz se retira al volver a intentarlo`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            viewModel.reportarLuzInsuficiente()

            viewModel.descartarProblemaCalidad()

            assertNull(viewModel.estado.value.problemaCalidad)
        }

    // Criterio 4 — la calidad insuficiente se informa durante la captura y el
    // flujo no avanza con esa imagen.
    @Test
    fun `una imagen borrosa se reporta y no se guarda la captura`() =
        runTest(dispatcher) {
            kyc.resultado = ResultadoCaptura.CalidadInsuficiente(ProblemaCalidad.IMAGEN_BORROSA)
            val viewModel = crearViewModel()

            viewModel.capturar(ImagenDocumento(CaraDocumento.FRENTE, "/tmp/borrosa.jpg"))
            advanceUntilIdle()

            val estado = viewModel.estado.value
            assertEquals(ProblemaCalidad.IMAGEN_BORROSA, estado.problemaCalidad)
            assertNull("La captura no debe quedar registrada", estado.frente)
        }

    // Criterio 2 — el registro exitoso activa la cuenta.
    @Test
    fun `el registro exitoso activa la cuenta`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        prepararParaRegistrar(viewModel)

        viewModel.registrar()
        registro.resultado.complete(ResultadoRegistro.Exitoso("sub-carolina"))
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(EstadoVerificacion.Exitosa, estado.verificacion)
        assertTrue(estado.cuentaActiva)
    }

    /**
     * El servicio acaba de crear la cuenta con esas credenciales: pedirle al
     * usuario que las teclee otra vez no aporta nada.
     */
    @Test
    fun `tras registrarse la sesion se abre con las credenciales enviadas`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            prepararParaRegistrar(viewModel)
            val enviado = viewModel.estado.value

            viewModel.registrar()
            registro.resultado.complete(ResultadoRegistro.Exitoso("sub-carolina"))
            advanceUntilIdle()

            assertEquals(enviado.datos?.correo, sesion.correo)
            assertEquals(enviado.contrasena, sesion.contrasena)
        }

    @Test
    fun `lo que se envia es lo que quedo en el formulario`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        prepararParaRegistrar(viewModel)
        val corregidos = viewModel.estado.value.datos!!.copy(nombres = "Ana María")
        viewModel.actualizarDatos(corregidos)
        viewModel.actualizarContrasena("OtraClave.2026!")

        viewModel.registrar()
        registro.resultado.complete(ResultadoRegistro.Exitoso("sub-ana"))
        advanceUntilIdle()

        assertEquals(corregidos, registro.datosEnviados)
        assertEquals("OtraClave.2026!", registro.contrasenaEnviada)
    }

    /** Un dato rechazado se corrige en M1c: el flujo no sale de la pantalla. */
    @Test
    fun `un dato rechazado deja el campo marcado y el registro sin iniciar`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            prepararParaRegistrar(viewModel)

            viewModel.registrar()
            registro.resultado.complete(
                ResultadoRegistro.DatosInvalidos(setOf(CampoDeRegistro.CORREO)),
            )
            advanceUntilIdle()

            val estado = viewModel.estado.value
            assertEquals(EstadoVerificacion.NoIniciada, estado.verificacion)
            assertEquals(setOf(CampoDeRegistro.CORREO), estado.camposInvalidos)
            assertFalse(estado.cuentaActiva)
        }

    @Test
    fun `corregir el dato retira la marca del campo`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        prepararParaRegistrar(viewModel)
        viewModel.registrar()
        registro.resultado.complete(
            ResultadoRegistro.DatosInvalidos(setOf(CampoDeRegistro.CORREO)),
        )
        advanceUntilIdle()

        viewModel.actualizarDatos(viewModel.estado.value.datos!!.copy(correo = "otro@example.com"))

        assertTrue(viewModel.estado.value.camposInvalidos.isEmpty())
    }

    @Test
    fun `un documento ya registrado se avisa en el formulario`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        prepararParaRegistrar(viewModel)

        viewModel.registrar()
        registro.resultado.complete(ResultadoRegistro.YaRegistrado)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado.yaRegistrado)
        assertEquals(EstadoVerificacion.NoIniciada, estado.verificacion)
    }

    // Criterio 3 — el fallo no revela el motivo técnico; solo una referencia
    // con la que soporte puede rastrear el intento.
    @Test
    fun `una falla del servicio entrega una referencia y ningun motivo`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            prepararParaRegistrar(viewModel)

            viewModel.registrar()
            registro.resultado.complete(ResultadoRegistro.NoDisponible("a1b2c3"))
            advanceUntilIdle()

            val verificacion = viewModel.estado.value.verificacion
            assertTrue(verificacion is EstadoVerificacion.NoSuperada)
            assertEquals("a1b2c3", (verificacion as EstadoVerificacion.NoSuperada).referencia)
        }

    // Criterio 5 — mientras el registro no termine bien, la cuenta no queda
    // activa en ningún estado intermedio.
    @Test
    fun `la cuenta no queda activa en ningun estado distinto del exito`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            assertFalse(viewModel.estado.value.cuentaActiva)

            prepararParaRegistrar(viewModel)
            assertFalse(viewModel.estado.value.cuentaActiva)

            viewModel.registrar()
            runCurrent()
            assertEquals(EstadoVerificacion.EnProceso, viewModel.estado.value.verificacion)
            assertFalse(viewModel.estado.value.cuentaActiva)

            registro.resultado.complete(ResultadoRegistro.NoDisponible("a1b2c3"))
            advanceUntilIdle()
            assertFalse(viewModel.estado.value.cuentaActiva)
        }

    // Criterio 6 — pasados 30 segundos sin respuesta se le avisa al usuario
    // que el resultado le llega por notificación.
    @Test
    fun `a los treinta segundos sin respuesta el estado pasa a demorada`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            prepararParaRegistrar(viewModel)

            viewModel.estado.test {
                assertEquals(EstadoVerificacion.NoIniciada, awaitItem().verificacion)

                viewModel.registrar()
                assertEquals(EstadoVerificacion.EnProceso, awaitItem().verificacion)

                advanceTimeBy(RegistroViewModel.LIMITE_DE_ESPERA_MS - 1)
                runCurrent()
                expectNoEvents()

                advanceTimeBy(2)
                runCurrent()
                assertEquals(EstadoVerificacion.Demorada, awaitItem().verificacion)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // Criterio 6 — un resultado que llega después del aviso no le cambia la
    // pantalla al usuario.
    @Test
    fun `un resultado tardio no contradice el aviso de notificacion`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            prepararParaRegistrar(viewModel)

            viewModel.registrar()
            advanceTimeBy(RegistroViewModel.LIMITE_DE_ESPERA_MS + 1)
            runCurrent()
            assertEquals(EstadoVerificacion.Demorada, viewModel.estado.value.verificacion)

            registro.resultado.complete(ResultadoRegistro.Exitoso("sub-carolina"))
            advanceUntilIdle()

            assertEquals(EstadoVerificacion.Demorada, viewModel.estado.value.verificacion)
            assertFalse(viewModel.estado.value.cuentaActiva)
        }

    @Test
    fun `el usuario puede pedir que le avisen por notificacion antes del limite`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            prepararParaRegistrar(viewModel)

            viewModel.registrar()
            runCurrent()
            viewModel.avisarPorNotificacion()

            assertEquals(EstadoVerificacion.Demorada, viewModel.estado.value.verificacion)
            assertFalse(viewModel.estado.value.cuentaActiva)
        }

    @Test
    fun `pedir aviso por notificacion no hace nada si no hay registro en curso`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()

            viewModel.avisarPorNotificacion()

            assertEquals(EstadoVerificacion.NoIniciada, viewModel.estado.value.verificacion)
        }

    @Test
    fun `sin aceptar los terminos no se registra`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        capturarAmbasCaras(viewModel)

        assertFalse(viewModel.estado.value.puedeVerificar)

        viewModel.registrar()
        advanceUntilIdle()
        assertEquals(EstadoVerificacion.NoIniciada, viewModel.estado.value.verificacion)
        assertNull("No se debe llamar al servicio", registro.datosEnviados)
    }

    @Test
    fun `sin contrasena no se registra`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        prepararParaRegistrar(viewModel)

        viewModel.actualizarContrasena("")
        assertFalse(viewModel.estado.value.puedeVerificar)

        viewModel.registrar()
        advanceUntilIdle()
        assertEquals(EstadoVerificacion.NoIniciada, viewModel.estado.value.verificacion)
    }

    @Test
    fun `reiniciar deja el flujo como al principio`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        prepararParaRegistrar(viewModel)

        viewModel.reiniciar()

        assertEquals(RegistroUiState(), viewModel.estado.value)
    }

    /** Otro intento no puede reenviar el documento que ya recibió respuesta. */
    @Test
    fun `tras reiniciar los datos se generan de nuevo`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        capturarAmbasCaras(viewModel)
        val primeros = viewModel.estado.value.datos

        viewModel.reiniciar()
        capturarAmbasCaras(viewModel)

        assertNotNull(viewModel.estado.value.datos)
        assertNotEquals(primeros, viewModel.estado.value.datos)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.capturarAmbasCaras(
        viewModel: RegistroViewModel,
    ) {
        viewModel.capturar(ImagenDocumento(CaraDocumento.FRENTE, "/tmp/frente.jpg"))
        advanceUntilIdle()
        viewModel.capturar(ImagenDocumento(CaraDocumento.REVERSO, "/tmp/reverso.jpg"))
        advanceUntilIdle()
    }

    private suspend fun kotlinx.coroutines.test.TestScope.prepararParaRegistrar(
        viewModel: RegistroViewModel,
    ) {
        capturarAmbasCaras(viewModel)
        viewModel.aceptarTerminos(true)
    }
}

private class KycFalso : RegistroKycRepositorio {

    var resultado: ResultadoCaptura = ResultadoCaptura.Aceptada

    /** Cuántas fotos se llegaron a analizar. Ninguna si no se tomó ninguna. */
    var capturasAnalizadas = 0
        private set

    override suspend fun analizarCaptura(imagen: ImagenDocumento): ResultadoCaptura {
        capturasAnalizadas++
        return resultado
    }
}

/**
 * Doble del registro con control explícito del momento en que responde: es lo
 * que permite probar el límite de 30 segundos sin esperar 30 segundos reales.
 */
private class RegistroFalso : RegistroRepositorio {

    val resultado = CompletableDeferred<ResultadoRegistro>()

    var datosEnviados: DatosDocumento? = null
        private set
    var contrasenaEnviada: String? = null
        private set

    override suspend fun registrar(datos: DatosDocumento, contrasena: String): ResultadoRegistro {
        datosEnviados = datos
        contrasenaEnviada = contrasena
        return resultado.await()
    }
}

private class SesionFalsa : SesionRepositorio {

    var correo: String? = null
        private set
    var contrasena: String? = null
        private set

    override suspend fun sesionGuardada(): Sesion? = null

    override suspend fun biometriaHabilitada(): Boolean = false

    override suspend fun desbloquear(): Sesion? = null

    override suspend fun ingresarConContrasena(
        correo: String,
        contrasena: String,
    ): ResultadoIngreso {
        this.correo = correo
        this.contrasena = contrasena
        return ResultadoIngreso.Exitoso(Sesion(correo = correo, nombre = "Carolina"))
    }

    override suspend fun habilitarBiometria(habilitada: Boolean) = Unit

    override suspend fun cerrar() = Unit
}
