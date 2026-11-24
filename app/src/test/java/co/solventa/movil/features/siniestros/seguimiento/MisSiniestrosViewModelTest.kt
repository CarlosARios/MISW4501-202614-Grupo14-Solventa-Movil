package co.solventa.movil.features.siniestros.seguimiento

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El estado de M9 · Mis siniestros y de su estado degradado M9c (HU-M2.4-01, HA-L02). */
@OptIn(ExperimentalCoroutinesApi::class)
class MisSiniestrosViewModelTest {

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

    private val enCurso = siniestro(id = "a", estado = EstadoDeSiniestro.EN_EVALUACION)
    private val devuelto = siniestro(id = "b", estado = EstadoDeSiniestro.DEVUELTO)
    private val pagado = siniestro(id = "c", estado = EstadoDeSiniestro.PAGADO)

    private fun exito(desdeCache: Boolean = false) =
        ResultadoDeConsulta.Exito(listOf(enCurso, pagado, devuelto), Frescura(desdeCache, AHORA))

    @Test
    fun `consulta al abrir y separa los casos en curso de los cerrados`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply { lista = exito() }
        val vm = MisSiniestrosViewModel(consulta, ESPERA)
        assertTrue(vm.estado.value.cargando)

        testScheduler.runCurrent()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals(listOf("a", "b"), estado.enCurso.map { it.id })
        assertEquals(listOf("c"), estado.cerrados.map { it.id })
        assertFalse(estado.desdeCache)
        assertEquals(1, consulta.consultas)
    }

    @Test
    fun `desde la cache avisa y vuelve a consultar sola (M9c)`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply { lista = exito(desdeCache = true) }
        val vm = MisSiniestrosViewModel(consulta, ESPERA)
        testScheduler.runCurrent()
        assertTrue(vm.estado.value.desdeCache)
        assertEquals(1, consulta.consultas)

        consulta.lista = exito(desdeCache = false)
        advanceTimeBy(ESPERA + 1)
        testScheduler.runCurrent()

        assertEquals(2, consulta.consultas)
        assertFalse(vm.estado.value.desdeCache)
        advanceTimeBy(ESPERA * 3)
        assertEquals("con la respuesta al dia no se reintenta", 2, consulta.consultas)
    }

    @Test
    fun `reintentar ahora consulta en seguida y no deja dos reintentos programados`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply { lista = exito(desdeCache = true) }
        val vm = MisSiniestrosViewModel(consulta, ESPERA)
        testScheduler.runCurrent()

        vm.reintentar()
        testScheduler.runCurrent()
        assertEquals(2, consulta.consultas)

        advanceTimeBy(ESPERA + 1)
        testScheduler.runCurrent()
        assertEquals("solo el reintento de la ultima consulta", 3, consulta.consultas)
        // Cerrar la pantalla detiene los reintentos; sin esto runTest esperaría para siempre.
        vm.viewModelScope.cancel()
    }

    @Test
    fun `un fallo despues de mostrar la lista la conserva y avisa`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply { lista = exito() }
        val vm = MisSiniestrosViewModel(consulta, ESPERA)
        testScheduler.runCurrent()

        consulta.lista = ResultadoDeConsulta.Error
        vm.reintentar()
        assertTrue(vm.estado.value.recargando)
        testScheduler.runCurrent()

        val estado = vm.estado.value
        assertEquals(FalloDeConsulta.ERROR, estado.fallo)
        assertTrue(estado.hayContenido)
        assertFalse(estado.recargando)
    }

    @Test
    fun `sin red y sin nada que mostrar`() = runTest(despachador) {
        val consulta = ConsultaFalsa().apply { lista = ResultadoDeConsulta.SinConexion }
        val vm = MisSiniestrosViewModel(consulta, ESPERA)
        testScheduler.runCurrent()

        assertEquals(FalloDeConsulta.SIN_CONEXION, vm.estado.value.fallo)
        assertFalse(vm.estado.value.vacio)
    }

    @Test
    fun `sin siniestros es el estado vacio`() = runTest(despachador) {
        val vm = MisSiniestrosViewModel(ConsultaFalsa(), ESPERA)
        testScheduler.runCurrent()

        assertTrue(vm.estado.value.vacio)
    }

    @Test
    fun `cada resultado fallido tiene su fallo`() {
        assertEquals(FalloDeConsulta.SIN_CONEXION, falloDe(ResultadoDeConsulta.SinConexion))
        assertEquals(FalloDeConsulta.NO_ENCONTRADO, falloDe(ResultadoDeConsulta.NoEncontrado))
        assertEquals(FalloDeConsulta.ERROR, falloDe(ResultadoDeConsulta.Error))
        assertEquals(30_000L, ConsultaAutomatica.ESPERA_MS)
    }

    private companion object {
        const val ESPERA = 30_000L
    }
}
