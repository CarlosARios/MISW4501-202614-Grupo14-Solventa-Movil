package co.solventa.movil.features.originacion.polizas

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

/** El estado de M5 · Detalle de la póliza (HU-M1.2-01, criterio 2). */
@OptIn(ExperimentalCoroutinesApi::class)
class DetallePolizaViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun vm(repositorio: RepositorioFalso, id: String = "AU-2025-118204") =
        DetallePolizaViewModel(SavedStateHandle(mapOf(DetallePolizaViewModel.ARGUMENTO_ID to id)), repositorio) { AHORA }

    @Test
    fun `sin conexion muestra la poliza de la copia con la hora de la sincronizacion`() = runTest(despachador) {
        val repositorio = RepositorioFalso(CopiaDePolizas(listOf(poliza(fin = AHORA + 16 * DIA)), AHORA, sinConexion = true))

        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals("AU-2025-118204", estado.poliza?.poliza?.id)
        assertEquals(Situacion.POR_VENCER, estado.poliza?.situacion)
        assertTrue(estado.sinConexion)
        assertEquals(AHORA, estado.sincronizadoEn)
        assertNull(estado.cambio)
    }

    @Test
    fun `una poliza que ya no esta en la copia no esta disponible`() = runTest(despachador) {
        val vm = vm(RepositorioFalso(CopiaDePolizas(listOf(poliza()), AHORA, sinConexion = false)), id = "HG-2025-880231")
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.noDisponible)
        assertNull(vm.estado.value.poliza)
    }

    @Test
    fun `muestra el cambio que la ultima sincronizacion detecto sobre ella`() = runTest(despachador) {
        val renovada = poliza(fin = AHORA + 365 * DIA)
        val otra = poliza(id = "HG-2025-880231", estado = EstadosDePoliza.CANCELADA)
        val repositorio = RepositorioFalso(CopiaDePolizas(listOf(renovada, otra), AHORA, sinConexion = false))
        repositorio.cambiosFijados.value =
            listOf(CambioDePoliza(otra, TipoDeCambio.CANCELADA), CambioDePoliza(renovada, TipoDeCambio.RENOVADA))

        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        assertEquals(TipoDeCambio.RENOVADA, vm.estado.value.cambio?.tipo)
        assertTrue(vm.estado.value.poliza?.renovada == true)
    }
}
