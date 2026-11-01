package co.solventa.movil.features.originacion.polizas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El estado de M4 · Mis pólizas (HU-M1.2-01). */
@OptIn(ExperimentalCoroutinesApi::class)
class PolizasViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun copiaCon(vararg lista: Poliza, sinConexion: Boolean = false, sincronizadoEn: Long? = AHORA) =
        CopiaDePolizas(lista.toList(), sincronizadoEn, sinConexion)

    private fun vm(repositorio: RepositorioFalso, ahora: Long = AHORA) = PolizasViewModel(repositorio) { ahora }

    @Test
    fun `empieza cargando, sincroniza al abrir y muestra las tarjetas`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon())
        val vm = vm(repositorio)
        assertTrue(vm.estado.value.cargando)

        repositorio.copia.value = copiaCon(poliza(), poliza(id = "AU-2024-093377", estado = EstadosDePoliza.CANCELADA))
        testScheduler.advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertFalse(estado.sincronizando)
        assertEquals(1, repositorio.sincronizaciones)
        assertEquals(listOf(Situacion.ACTIVA, Situacion.CANCELADA), estado.polizas.map { it.situacion })
        assertEquals(AHORA, estado.sincronizadoEn)
    }

    @Test
    fun `sin conexion muestra la copia con su hora (criterio 1)`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon(poliza(), sinConexion = true)).apply {
            red.value = false
            resultado = ResultadoDeSincronizacion.SinConexion
        }

        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        val estado = vm.estado.value
        assertTrue(estado.sinConexion)
        assertEquals(AHORA, estado.sincronizadoEn)
        assertEquals(1, estado.polizas.size)
        assertFalse(estado.vacio)
    }

    @Test
    fun `al volver la red sincroniza sola (criterio 3)`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon(poliza(), sinConexion = true)).apply { red.value = false }
        vm(repositorio)
        testScheduler.advanceUntilIdle()
        assertEquals(1, repositorio.sincronizaciones)

        repositorio.red.value = true
        testScheduler.advanceUntilIdle()

        assertEquals(2, repositorio.sincronizaciones)
    }

    @Test
    fun `muestra los cambios y los descarta (criterio 4)`() = runTest(despachador) {
        val renovada = poliza(fin = AHORA + 365 * DIA)
        val repositorio = RepositorioFalso(copiaCon(renovada))
        repositorio.cambiosFijados.value = listOf(CambioDePoliza(renovada, TipoDeCambio.RENOVADA))
        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        assertEquals(1, vm.estado.value.cambios.size)
        assertTrue("la tarjeta va marcada como renovada", vm.estado.value.polizas.single().renovada)

        vm.descartarCambios()
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.cambios.isEmpty())
    }

    @Test
    fun `una copia de mas de 30 dias se advierte sin esconderla (criterio 5)`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon(poliza(fin = AHORA + 400 * DIA), sincronizadoEn = AHORA - 31 * DIA))

        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.desactualizada)
        assertEquals(1, vm.estado.value.polizas.size)
    }

    @Test
    fun `una copia reciente no se advierte`() = runTest(despachador) {
        val vm = vm(RepositorioFalso(copiaCon(poliza(), sincronizadoEn = AHORA - DIA)))
        testScheduler.advanceUntilIdle()

        assertFalse(vm.estado.value.desactualizada)
    }

    @Test
    fun `un error sin copia ofrece reintentar y el reintento sincroniza`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon(sincronizadoEn = null)).apply { resultado = ResultadoDeSincronizacion.Error }
        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.error)
        assertFalse(vm.estado.value.hayContenido)
        assertFalse(vm.estado.value.vacio)

        repositorio.resultado = ResultadoDeSincronizacion.Sincronizado(emptyList())
        vm.reintentar()
        testScheduler.advanceUntilIdle()

        assertEquals(2, repositorio.sincronizaciones)
        assertFalse(vm.estado.value.error)
        assertTrue(vm.estado.value.vacio)
    }

    @Test
    fun `no lanza una segunda sincronizacion mientras corre la primera`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon())
        val vm = vm(repositorio)

        vm.reintentar()
        testScheduler.advanceUntilIdle()

        assertEquals(1, repositorio.sincronizaciones)
    }
}
