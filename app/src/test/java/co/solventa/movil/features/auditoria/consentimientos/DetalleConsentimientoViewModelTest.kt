package co.solventa.movil.features.auditoria.consentimientos

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** El estado de M8b · Detalle del consentimiento (HU-M1.3-01). */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleConsentimientoViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private val revocaciones = RevocacionesFalsas()

    private fun vm(id: String, repositorio: RepositorioFalso) = DetalleConsentimientoViewModel(
        SavedStateHandle(mapOf(DetalleConsentimientoViewModel.ARGUMENTO_ID to id)),
        repositorio,
        revocaciones,
    ) { AHORA }

    @Test
    fun `lee el detalle de la copia local, tambien sin conexion`() = runTest(despachador) {
        val repositorio = RepositorioFalso(CopiaDeConsentimientos(listOf(consentimiento("cns-1")), AHORA, sinConexion = true))

        val vm = vm("cns-1", repositorio)
        assertTrue(vm.estado.value.cargando)
        testScheduler.advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals("cns-1", estado.consentimiento?.id)
        assertEquals("Banco Aliado", estado.consentimiento?.nombreDeEntidad)
        assertTrue("revocar visible en un activo", estado.revocarVisible)
        assertFalse(estado.noDisponible)
        assertEquals("no pide nada a la red", 0, repositorio.actualizaciones)
    }

    @Test
    fun `revocar no aparece en revocados ni vencidos`() = runTest(despachador) {
        val repositorio = RepositorioFalso(
            CopiaDeConsentimientos(
                listOf(
                    consentimiento("revocado", EstadosDeConsentimiento.REVOCADO, finalizadoEn = AHORA - 1, comprobante = "REV-1"),
                    consentimiento("vencido-en-la-copia", venceEn = AHORA - 1),
                ),
                AHORA,
                sinConexion = false,
            ),
        )

        val revocado = vm("revocado", repositorio)
        val vencido = vm("vencido-en-la-copia", repositorio)
        testScheduler.advanceUntilIdle()

        assertFalse(revocado.estado.value.revocarVisible)
        assertEquals(EstadosDeConsentimiento.VENCIDO, vencido.estado.value.consentimiento?.estado)
        assertFalse(vencido.estado.value.revocarVisible)
    }

    @Test
    fun `el ultimo paso del historial de un activo sigue la conexion`() = runTest(despachador) {
        val repositorio = RepositorioFalso(CopiaDeConsentimientos(listOf(consentimiento("cns-1")), AHORA, sinConexion = false))

        val vm = vm("cns-1", repositorio)
        testScheduler.advanceUntilIdle()
        assertFalse(vm.estado.value.sinConexion)
        assertEquals(PasoFinalDelHistorial.Activo(sincronizado = true), vm.estado.value.pasoFinal)

        repositorio.red.value = false
        testScheduler.advanceUntilIdle()
        assertTrue(vm.estado.value.sinConexion)
        assertEquals(PasoFinalDelHistorial.Activo(sincronizado = false), vm.estado.value.pasoFinal)

        repositorio.red.value = true
        testScheduler.advanceUntilIdle()
        assertEquals(PasoFinalDelHistorial.Activo(sincronizado = true), vm.estado.value.pasoFinal)
        assertEquals("seguir la conexion no pide nada a la red", 0, repositorio.actualizaciones)
    }

    @Test
    fun `un activo que vencio desde la ultima copia termina en su vencimiento, sin paso final`() = runTest(despachador) {
        val repositorio = RepositorioFalso(
            CopiaDeConsentimientos(listOf(consentimiento("vencido-en-la-copia", venceEn = AHORA - 1)), AHORA, sinConexion = false),
        )

        val vm = vm("vencido-en-la-copia", repositorio)
        testScheduler.advanceUntilIdle()

        assertEquals(CambiosDeEstado.VENCIDO, vm.estado.value.consentimiento?.historial?.last()?.estado)
        assertNull(vm.estado.value.pasoFinal)
    }

    @Test
    fun `un consentimiento que ya no esta en la copia no esta disponible`() = runTest(despachador) {
        val vm = vm("cns-borrado", RepositorioFalso())
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.noDisponible)
        assertNull(vm.estado.value.consentimiento)
        assertFalse(vm.estado.value.revocarVisible)
        assertNull(vm.estado.value.pasoFinal)
    }

    // --- HU-M1.3-02 ---

    @Test
    fun `con su revocacion en cola no ofrece revocar y termina en pendiente de envio`() = runTest(despachador) {
        val repositorio = RepositorioFalso(CopiaDeConsentimientos(listOf(consentimiento("cns-1")), AHORA, false))
        revocaciones.cola.value = listOf(RevocacionPendiente("cns-1", "Banco Aliado", AHORA - 1_000L, 0))
        val vm = vm("cns-1", repositorio)
        advanceUntilIdle()

        val estado = vm.estado.value
        assertEquals("cns-1", estado.pendiente?.consentimientoId)
        assertFalse("criterio 6", estado.revocarVisible)
        assertEquals(PasoFinalDelHistorial.RevocacionPendiente(AHORA - 1_000L), estado.pasoFinal)
    }

    @Test
    fun `un revocado parcial muestra la revocacion como paso final sin confirmar`() = runTest(despachador) {
        val revocacion = MapeadorDeConsentimientos.revocacionDesdeDto(
            revocacionDto("cns-1", estado = "partially_revoked", aviso = "partial", entidad = "not_confirmed"),
        )
        val parcial = MapeadorDeConsentimientos.conRevocacion(consentimiento("cns-1"), revocacion).copy(
            historial = MapeadorDeConsentimientos.conRevocacion(consentimiento("cns-1"), revocacion).historial +
                CambioDeEstado(CambiosDeEstado.REVOCACION_NO_CONFIRMADA, revocacion.efectivaEn + 300_000L, null, null, "aliado-banco-1"),
        )
        val vm = vm("cns-1", RepositorioFalso(CopiaDeConsentimientos(listOf(parcial), AHORA, false)))
        advanceUntilIdle()

        val estado = vm.estado.value
        assertTrue(estado.parcial)
        assertFalse(estado.revocarVisible)
        assertEquals(
            "el revocado y los avisos de cada entidad no son pasos con visto",
            listOf(CambiosDeEstado.OTORGADO, CambiosDeEstado.REVOCACION_SOLICITADA),
            estado.historialVisible.map { it.estado },
        )
        assertEquals(
            PasoFinalDelHistorial.RevocadoSinConfirmar(revocacion.efectivaEn, "REV-2026-0088", listOf("Banco Aliado")),
            estado.pasoFinal,
        )
    }
}
