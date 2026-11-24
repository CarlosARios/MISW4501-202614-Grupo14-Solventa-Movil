package co.solventa.movil.features.siniestros.seguimiento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

/** El estado de M10 · Detalle del siniestro (HU-M2.4-01, HA-L02). */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleSiniestroViewModelTest {

    // Con una respuesta desde la caché la consulta se reprograma sola cada
    // 30 s, así que estas pruebas avanzan el reloj virtual a mano
    // (runCurrent, advanceTimeBy) y nunca con advanceUntilIdle, que no
    // terminaría.
    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private val caso = siniestro()
    private val cambios = listOf(
        CambioDeEstado(EstadoDeSiniestro.RADICADO, AHORA - 48 * HORA),
        CambioDeEstado(EstadoDeSiniestro.EN_EVALUACION, AHORA - 24 * HORA),
    )

    private fun vm(consulta: ConsultaFalsa) =
        DetalleSiniestroViewModel(SavedStateHandle(mapOf(DetalleSiniestroViewModel.ARGUMENTO_ID to caso.id)), consulta, ESPERA)

    @Test
    fun `pide el siniestro de la ruta y arma su linea de tiempo`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply {
            detalle = ResultadoDeConsulta.Exito(DetalleDeSiniestro(caso, cambios), Frescura(false, AHORA))
        }
        val vm = vm(consulta)
        testScheduler.runCurrent()

        val estado = vm.estado.value
        assertEquals(caso.id, consulta.pedido)
        assertEquals(caso, estado.siniestro)
        assertEquals(Avance.ACTUAL, estado.etapas[2].avance)
        assertEquals(0.5f, estado.progreso, 0.001f)
        assertFalse(estado.desdeCache)
        assertNull(estado.fallo)
    }

    @Test
    fun `desde la cache avisa y vuelve a consultar sola`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply {
            detalle = ResultadoDeConsulta.Exito(DetalleDeSiniestro(caso, cambios), Frescura(true, AHORA - HORA))
        }
        val vm = vm(consulta)
        testScheduler.runCurrent()
        assertTrue(vm.estado.value.desdeCache)
        assertEquals(AHORA - HORA, vm.estado.value.frescura?.leidoEn)

        advanceTimeBy(ESPERA + 1)
        testScheduler.runCurrent()

        assertEquals(2, consulta.consultas)
        // Cerrar la pantalla detiene los reintentos; sin esto runTest esperaría para siempre.
        vm.viewModelScope.cancel()
    }

    @Test
    fun `un siniestro que no existe o es ajeno`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply { detalle = ResultadoDeConsulta.NoEncontrado }
        val vm = vm(consulta)
        testScheduler.runCurrent()

        assertEquals(FalloDeConsulta.NO_ENCONTRADO, vm.estado.value.fallo)
        assertNull(vm.estado.value.siniestro)
    }

    @Test
    fun `un fallo al reintentar conserva lo que se veia`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply {
            detalle = ResultadoDeConsulta.Exito(DetalleDeSiniestro(caso, cambios), Frescura(false, AHORA))
        }
        val vm = vm(consulta)
        testScheduler.runCurrent()

        consulta.detalle = ResultadoDeConsulta.SinConexion
        vm.reintentar()
        testScheduler.runCurrent()

        assertEquals(caso, vm.estado.value.siniestro)
        assertEquals(FalloDeConsulta.SIN_CONEXION, vm.estado.value.fallo)
    }

    private companion object {
        const val ESPERA = 30_000L
    }
}
