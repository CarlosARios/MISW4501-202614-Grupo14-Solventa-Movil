package co.solventa.movil.features.siniestros.seguimiento

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

/**
 * M9 y M10 con lo que agrega HU-M2.4-01: el estimado de la evaluación
 * (criterio 2), la línea de tiempo automática (criterio 3) y la versión
 * guardada en el teléfono sin red (criterio 5), distinta del aviso de la caché
 * del servicio (criterio 4).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SinConexionYResolucionViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun detalleVm(consulta: ConsultaFalsa, id: String) =
        DetalleSiniestroViewModel(SavedStateHandle(mapOf(DetalleSiniestroViewModel.ARGUMENTO_ID to id)), consulta, ESPERA_MS)

    @Test
    fun `en evaluacion el detalle lleva el tiempo estimado del triage`() = runTest(despachador) {
        val estimacion = EstimacionDeResolucion(nivel = 2, diasHabiles = 5)
        val consulta = ConsultaFalsa().apply {
            detalle = ResultadoDeConsulta.Exito(DetalleDeSiniestro(siniestro(), emptyList(), estimacion), Frescura(false, AHORA))
        }

        val vm = detalleVm(consulta, siniestro().id)
        testScheduler.runCurrent()

        assertEquals(estimacion, vm.estado.value.estimacion)
        assertFalse(vm.estado.value.sinConexion)
    }

    @Test
    fun `un siniestro parametrico tiene la linea de tiempo automatica`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply {
            detalle = ResultadoDeConsulta.Exito(
                DetalleDeSiniestro(parametrico(), listOf(CambioDeEstado(EstadoDeSiniestro.PAGADO, AHORA - HORA))),
                Frescura(false, AHORA),
            )
        }

        val vm = detalleVm(consulta, parametrico().id)
        testScheduler.runCurrent()

        val estado = vm.estado.value
        assertEquals(LineaDeTiempo.AUTOMATICAS, estado.etapas.map { it.etapa })
        assertTrue(estado.etapas.all { it.avance == Avance.HECHA })
        assertEquals(1f, estado.progreso)
        assertEquals(RESOLUCION, estado.siniestro?.resolucionAutomatica)
    }

    @Test
    fun `sin red el detalle muestra la version guardada con su hora y no el aviso de la cache`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply {
            detalle = ResultadoDeConsulta.CopiaLocal(DetalleDeSiniestro(siniestro(), emptyList()), AHORA - 3 * HORA)
        }

        val vm = detalleVm(consulta, siniestro().id)
        testScheduler.runCurrent()

        val estado = vm.estado.value
        assertEquals(siniestro(), estado.siniestro)
        assertEquals(AHORA - 3 * HORA, estado.copiaLocalDe)
        assertTrue(estado.sinConexion)
        assertFalse(estado.desdeCache)
        assertNull(estado.frescura)
        assertNull(estado.fallo)
        assertEquals(LineaDeTiempo.MANUALES, estado.etapas.map { it.etapa })
    }

    @Test
    fun `al volver la red el reintento reemplaza la version guardada`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply {
            detalle = ResultadoDeConsulta.CopiaLocal(DetalleDeSiniestro(siniestro(), emptyList()), AHORA - 3 * HORA)
        }
        val vm = detalleVm(consulta, siniestro().id)
        testScheduler.runCurrent()

        consulta.detalle = ResultadoDeConsulta.Exito(DetalleDeSiniestro(siniestro(), emptyList()), Frescura(false, AHORA))
        vm.reintentar()
        testScheduler.runCurrent()

        assertNull(vm.estado.value.copiaLocalDe)
        assertEquals(Frescura(false, AHORA), vm.estado.value.frescura)
    }

    @Test
    fun `sin red la lista muestra la version guardada separada en curso y cerrados`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply {
            lista = ResultadoDeConsulta.CopiaLocal(listOf(siniestro(), parametrico()), AHORA - HORA)
        }

        val vm = MisSiniestrosViewModel(consulta, ESPERA_MS)
        testScheduler.runCurrent()

        val estado = vm.estado.value
        assertEquals(listOf(siniestro()), estado.enCurso)
        assertEquals(listOf(parametrico()), estado.cerrados)
        assertEquals(AHORA - HORA, estado.copiaLocalDe)
        assertNull(estado.frescura)
        assertFalse(estado.vacio)
    }

    private companion object {
        const val ESPERA_MS = 30_000L
    }
}
