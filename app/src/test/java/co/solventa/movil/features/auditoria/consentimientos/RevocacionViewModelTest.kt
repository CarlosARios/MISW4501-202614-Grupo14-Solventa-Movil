package co.solventa.movil.features.auditoria.consentimientos

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import co.solventa.movil.core.auth.biometria.ResultadoBiometrico
import co.solventa.movil.core.auth.sesion.VerificacionDeContrasena
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * La revocacion desde el detalle (HU-M1.3-02): la hoja con el efecto y la
 * confirmacion (criterio 1), cancelar o fallar sin escribir nada (criterio
 * 2), el resultado en linea o en cola (criterios 3 y 5), la accion ausente en
 * lo que no se puede revocar (criterio 6) y el comprobante.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RevocacionViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val consentimientos = RepositorioFalso()
    private val revocaciones = RevocacionesFalsas()
    private val biometria = BiometriaFalsa()
    private val sesion = SesionFalsa(biometria = true)
    private val contrasena = VerificadorFalso()
    private val guardados = mutableListOf<Pair<String, ByteArray>>()
    private var guardar = true
    private val idiomas = IdiomasFalsos()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun conCopia(vararg lista: Consentimiento) {
        consentimientos.copia.value = CopiaDeConsentimientos(lista.toList(), AHORA, false)
    }

    private fun vm(id: String = "cns-1") = RevocacionViewModel(
        SavedStateHandle(mapOf(DetalleConsentimientoViewModel.ARGUMENTO_ID to id)),
        consentimientos,
        revocaciones,
        ConfirmadorDeRevocacion(biometria, sesion, contrasena),
        { destino, bytes ->
            guardados += destino to bytes
            guardar
        },
        idiomas,
    ) { AHORA }

    private val revocacion = MapeadorDeConsentimientos.revocacionDesdeDto(revocacionDto("cns-1"))

    @Test
    fun `la hoja se abre con la huella si esta habilitada y se cancela sin escribir nada`() = runTest(despachador) {
        conCopia(consentimiento("cns-1"))
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.estado.value.revocarDisponible)

        vm.abrirHoja()
        advanceUntilIdle()
        assertEquals(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.BIOMETRIA), vm.estado.value.paso)

        vm.cancelar()

        assertEquals(PasoDeRevocacion.Oculto, vm.estado.value.paso)
        assertTrue("criterio 2: nada se escribe ni se envia", revocaciones.revocados.isEmpty())
        assertEquals(0, biometria.llamadas)
    }

    @Test
    fun `sin biometria habilitada la hoja pide la contraseña`() = runTest(despachador) {
        sesion.biometria = false
        conCopia(consentimiento("cns-1"))
        val vm = vm()
        advanceUntilIdle()

        vm.abrirHoja()
        advanceUntilIdle()

        assertEquals(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.CONTRASENA), vm.estado.value.paso)
    }

    @Test
    fun `una huella reconocida revoca en linea y abre la pantalla de revocado`() = runTest(despachador) {
        conCopia(consentimiento("cns-1"))
        revocaciones.resultado = ResultadoDeRevocacion.Revocado(revocacion)
        val vm = vm()
        advanceUntilIdle()
        vm.abrirHoja()
        advanceUntilIdle()

        vm.estado.test {
            assertEquals(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.BIOMETRIA), awaitItem().paso)
            vm.confirmarConBiometria()
            assertEquals(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.BIOMETRIA, verificando = true), awaitItem().paso)
            assertEquals(PasoDeRevocacion.Enviando, awaitItem().paso)
            assertEquals(PasoDeRevocacion.Revocado(revocacion), awaitItem().paso)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("cns-1" to MetodoDeConfirmacion.BIOMETRIA), revocaciones.revocados)

        vm.alMostrarRevocado()
        assertEquals(PasoDeRevocacion.Oculto, vm.estado.value.paso)
    }

    @Test
    fun `una huella no reconocida, cancelada o bloqueada deja el detalle como estaba y solo la no reconocida lo avisa`() = runTest(despachador) {
        conCopia(consentimiento("cns-1"))
        val vm = vm()
        advanceUntilIdle()
        mapOf(
            ResultadoBiometrico.NoReconocido to AvisoDeLaRevocacion.HUELLA_NO_RECONOCIDA,
            ResultadoBiometrico.Cancelado to null,
            ResultadoBiometrico.NoDisponible to null,
        ).forEach { (resultado, aviso) ->
            biometria.resultado = resultado
            vm.abrirHoja()
            advanceUntilIdle()
            vm.confirmarConBiometria()
            advanceUntilIdle()
            assertEquals(resultado.toString(), PasoDeRevocacion.Oculto, vm.estado.value.paso)
            assertEquals(resultado.toString(), aviso, vm.estado.value.aviso)
        }
        assertTrue(revocaciones.revocados.isEmpty())
    }

    @Test
    fun `la contraseña correcta revoca y la incorrecta vuelve al detalle sin escribir y lo avisa`() = runTest(despachador) {
        sesion.biometria = false
        conCopia(consentimiento("cns-1"))
        val vm = vm()
        advanceUntilIdle()

        contrasena.resultado = VerificacionDeContrasena.Incorrecta
        vm.abrirHoja()
        advanceUntilIdle()
        vm.confirmarConContrasena("claveMala123")
        advanceUntilIdle()
        assertEquals(PasoDeRevocacion.Oculto, vm.estado.value.paso)
        assertEquals(AvisoDeLaRevocacion.CONTRASENA_INCORRECTA, vm.estado.value.aviso)
        assertTrue(revocaciones.revocados.isEmpty())

        contrasena.resultado = VerificacionDeContrasena.Correcta
        vm.abrirHoja()
        advanceUntilIdle()
        vm.confirmarConContrasena("claveBuena123")
        advanceUntilIdle()
        assertEquals(listOf("cns-1" to MetodoDeConfirmacion.CONTRASENA), revocaciones.revocados)
        assertEquals(listOf("claveMala123", "claveBuena123"), contrasena.recibidas)
    }

    @Test
    fun `sin red y sin biometria la hoja lo explica y no se escribe nada`() = runTest(despachador) {
        sesion.biometria = false
        conCopia(consentimiento("cns-1"))
        contrasena.resultado = VerificacionDeContrasena.SinConexion
        val vm = vm()
        advanceUntilIdle()
        vm.abrirHoja()
        advanceUntilIdle()

        vm.confirmarConContrasena("claveBuena123")
        advanceUntilIdle()

        assertEquals(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.CONTRASENA, sinConexion = true), vm.estado.value.paso)
        assertTrue(revocaciones.revocados.isEmpty())
        vm.confirmarConContrasena("")
        advanceUntilIdle()
        assertEquals("una contraseña vacia no se verifica", 1, contrasena.recibidas.size)
    }

    @Test
    fun `sin red queda pendiente, la hoja se cierra y el detalle ofrece reintentar`() = runTest(despachador) {
        conCopia(consentimiento("cns-1"))
        revocaciones.resultado = ResultadoDeRevocacion.PendienteDeEnvio
        revocaciones.alRevocar = {
            revocaciones.cola.value = listOf(RevocacionPendiente("cns-1", "Banco Aliado", AHORA, 0))
            revocaciones.red.value = false
        }
        val vm = vm()
        advanceUntilIdle()
        vm.abrirHoja()
        advanceUntilIdle()

        vm.confirmarConBiometria()
        advanceUntilIdle()

        val estado = vm.estado.value
        assertEquals(PasoDeRevocacion.Oculto, estado.paso)
        assertEquals("cns-1", estado.pendiente?.consentimientoId)
        assertFalse("criterio 6", estado.revocarDisponible)
        assertTrue(estado.reintentarVisible)
        assertFalse("sin señal", estado.reintentarHabilitado)
        vm.reintentarAhora()
        advanceUntilIdle()
        assertEquals(0, revocaciones.envios)

        revocaciones.red.value = true
        advanceUntilIdle()
        assertTrue(vm.estado.value.reintentarHabilitado)
        vm.reintentarAhora()
        assertTrue(vm.estado.value.reenviando)
        advanceUntilIdle()
        assertEquals(1, revocaciones.envios)
        assertFalse(vm.estado.value.reenviando)
    }

    @Test
    fun `si ya estaba revocado o vencio lo avisa`() = runTest(despachador) {
        conCopia(consentimiento("cns-1"))
        val vm = vm()
        advanceUntilIdle()
        mapOf(
            ResultadoDeRevocacion.YaRevocado to AvisoDeLaRevocacion.YA_REVOCADO,
            ResultadoDeRevocacion.YaNoActivo to AvisoDeLaRevocacion.YA_NO_ACTIVO,
            ResultadoDeRevocacion.Error to AvisoDeLaRevocacion.NO_SE_PUDO,
            ResultadoDeRevocacion.SinSesion to AvisoDeLaRevocacion.NO_SE_PUDO,
        ).forEach { (resultado, aviso) ->
            revocaciones.resultado = resultado
            vm.abrirHoja()
            advanceUntilIdle()
            vm.confirmarConBiometria()
            advanceUntilIdle()
            assertEquals(resultado.toString(), aviso, vm.estado.value.aviso)
            assertEquals(PasoDeRevocacion.Oculto, vm.estado.value.paso)
            vm.descartarAviso()
            assertNull(vm.estado.value.aviso)
        }
    }

    @Test
    fun `un revocado, un vencido o uno ajeno no ofrecen revocar`() = runTest(despachador) {
        conCopia(
            consentimiento("cns-revocado", EstadosDeConsentimiento.REVOCADO, finalizadoEn = AHORA - 1_000L, comprobante = "REV-1"),
            consentimiento("cns-vencido", venceEn = AHORA - 1L),
        )
        listOf("cns-revocado", "cns-vencido", "cns-no-existe").forEach { id ->
            val vm = vm(id)
            advanceUntilIdle()
            assertFalse(id, vm.estado.value.revocarDisponible)
            vm.abrirHoja()
            advanceUntilIdle()
            assertEquals(id, PasoDeRevocacion.Oculto, vm.estado.value.paso)
        }
    }

    @Test
    fun `el comprobante se descarga en el idioma de la aplicacion y se guarda donde se eligio`() = runTest(despachador) {
        val revocado = MapeadorDeConsentimientos.conRevocacion(consentimiento("cns-1"), revocacion)
        conCopia(revocado)
        idiomas.actual.value = Idioma.INGLES
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.estado.value.comprobanteDisponible)
        assertEquals("REV-2026-0088.pdf", vm.estado.value.nombreDelComprobante)

        vm.guardarComprobante("content://destino/1")
        advanceUntilIdle()

        assertEquals("en", revocaciones.idiomaDelComprobante)
        assertEquals("content://destino/1", guardados.single().first)
        assertEquals(AvisoDeLaRevocacion.COMPROBANTE_GUARDADO, vm.estado.value.aviso)
        assertFalse(vm.estado.value.descargando)
    }

    @Test
    fun `el comprobante sin red, con error o sin poder guardarse lo avisa`() = runTest(despachador) {
        conCopia(MapeadorDeConsentimientos.conRevocacion(consentimiento("cns-1"), revocacion))
        val vm = vm()
        advanceUntilIdle()

        revocaciones.comprobante = ResultadoDeComprobante.SinConexion
        vm.guardarComprobante("content://destino/1")
        advanceUntilIdle()
        assertEquals(AvisoDeLaRevocacion.COMPROBANTE_SIN_CONEXION, vm.estado.value.aviso)

        revocaciones.comprobante = ResultadoDeComprobante.Error
        vm.guardarComprobante("content://destino/1")
        advanceUntilIdle()
        assertEquals(AvisoDeLaRevocacion.COMPROBANTE_ERROR, vm.estado.value.aviso)

        revocaciones.comprobante = ResultadoDeComprobante.Descargado("%PDF".toByteArray(), "x.pdf")
        guardar = false
        vm.guardarComprobante("content://destino/1")
        advanceUntilIdle()
        assertEquals(AvisoDeLaRevocacion.COMPROBANTE_ERROR, vm.estado.value.aviso)
        assertEquals("es", revocaciones.idiomaDelComprobante)

        vm.guardarComprobante(null)
        advanceUntilIdle()
        assertEquals("el selector cancelado no descarga", 1, guardados.size)
    }

    @Test
    fun `una revocacion de la cola que el servicio ya no acepto se avisa y se descarta`() = runTest(despachador) {
        conCopia(consentimiento("cns-1"))
        revocaciones.descartes.value = listOf(AvisoDeRevocacion("cns-1", "Banco Aliado", MotivoDeDescarte.VENCIO))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(MotivoDeDescarte.VENCIO, vm.estado.value.descarte?.motivo)

        vm.descartarAviso()
        advanceUntilIdle()

        assertEquals(listOf("cns-1"), revocaciones.avisosDescartados)
        assertNull(vm.estado.value.descarte)
    }

    private class IdiomasFalsos : IdiomaRepositorio {
        override val actual = MutableStateFlow(Idioma.ESPANOL)
        override val elegidoPorElUsuario: Boolean = false
        override fun elegir(idioma: Idioma) {
            actual.value = idioma
        }
    }
}
