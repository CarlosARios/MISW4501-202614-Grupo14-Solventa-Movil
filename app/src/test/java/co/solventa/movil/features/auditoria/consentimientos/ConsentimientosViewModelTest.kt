package co.solventa.movil.features.auditoria.consentimientos

import app.cash.turbine.test
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

/** El estado de M8 · Privacidad y consentimientos (HU-M1.3-01). */
@OptIn(ExperimentalCoroutinesApi::class)
class ConsentimientosViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun copiaCon(vararg lista: Consentimiento, sinConexion: Boolean = false, sincronizadoEn: Long? = AHORA) =
        CopiaDeConsentimientos(lista.toList(), sincronizadoEn, sinConexion)

    private fun vm(repositorio: RepositorioFalso) = ConsentimientosViewModel(repositorio) { AHORA }

    @Test
    fun `empieza cargando y luego agrupa activos e historial en orden`() = runTest(despachador) {
        val repositorio = RepositorioFalso(
            copiaCon(
                consentimiento("viejo", otorgadoEn = OTORGADO - 10_000),
                consentimiento("revocado", EstadosDeConsentimiento.REVOCADO, finalizadoEn = AHORA - 1_000, comprobante = "REV-1"),
                consentimiento("nuevo"),
                consentimiento("vencido", venceEn = AHORA - 1),
            ),
        )

        val vm = vm(repositorio)
        assertTrue(vm.estado.value.cargando)
        testScheduler.advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals(listOf("nuevo", "viejo"), estado.activos.map { it.id })
        assertEquals(listOf("vencido", "revocado"), estado.historial.map { it.id })
        assertEquals(EstadosDeConsentimiento.VENCIDO, estado.historial.first().estado)
        assertFalse(estado.vacio)
        assertEquals(1, repositorio.actualizaciones, "actualiza al abrir")
    }

    @Test
    fun `sin consentimientos es el estado vacio`() = runTest(despachador) {
        val vm = vm(RepositorioFalso())
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.vacio)
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun `un error se muestra y el reintento vuelve a pedir la lista`() = runTest(despachador) {
        val repositorio = RepositorioFalso().apply { resultado = ResultadoDeActualizacion.Error }
        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()
        assertTrue(vm.estado.value.error)
        assertFalse(vm.estado.value.vacio)

        repositorio.resultado = ResultadoDeActualizacion.Actualizado
        repositorio.alActualizar = { repositorio.copia.value = copiaCon(consentimiento("cns-1")) }
        vm.reintentar()
        testScheduler.advanceUntilIdle()

        assertFalse(vm.estado.value.error)
        assertEquals(listOf("cns-1"), vm.estado.value.activos.map { it.id })
        assertEquals(2, repositorio.actualizaciones)
    }

    @Test
    fun `sin conexion muestra la copia con la hora de sincronizacion`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon(consentimiento("cns-1"), sinConexion = true, sincronizadoEn = AHORA - 60_000))
            .apply { resultado = ResultadoDeActualizacion.SinConexion; red.value = false }

        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        val estado = vm.estado.value
        assertTrue(estado.sinConexion)
        assertEquals(AHORA - 60_000, estado.sincronizadoEn)
        assertEquals(listOf("cns-1"), estado.activos.map { it.id })
        assertFalse(estado.error)
    }

    @Test
    fun `sin conexion y sin copia no es el estado vacio`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon(sinConexion = true, sincronizadoEn = null))
            .apply { resultado = ResultadoDeActualizacion.SinConexion }

        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.sinConexion)
        assertNull(vm.estado.value.sincronizadoEn)
        assertFalse(vm.estado.value.vacio)
    }

    @Test
    fun `una cuenta sin vincular se muestra con su reintento`() = runTest(despachador) {
        val repositorio = RepositorioFalso().apply { resultado = ResultadoDeActualizacion.CuentaSinVincular }

        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.cuentaSinVincular)
        assertFalse(vm.estado.value.vacio)
    }

    @Test
    fun `al volver la red se actualiza sola mientras la pantalla esta abierta`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copiaCon(consentimiento("cns-1"), sinConexion = true))
            .apply { resultado = ResultadoDeActualizacion.SinConexion; red.value = false }
        val vm = vm(repositorio)
        testScheduler.advanceUntilIdle()
        assertEquals(1, repositorio.actualizaciones)

        vm.estado.test {
            skipItems(1)
            repositorio.resultado = ResultadoDeActualizacion.Actualizado
            repositorio.alActualizar = {
                repositorio.copia.value = copiaCon(consentimiento("cns-1"), consentimiento("cns-nuevo", otorgadoEn = OTORGADO + 1))
            }
            repositorio.red.value = true
            testScheduler.advanceUntilIdle()

            val ultimo = expectMostRecentItem()
            assertEquals(listOf("cns-nuevo", "cns-1"), ultimo.activos.map { it.id })
            assertFalse(ultimo.sinConexion)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, repositorio.actualizaciones)

        repositorio.red.value = true
        testScheduler.advanceUntilIdle()
        assertEquals("sin transicion no se repite", 2, repositorio.actualizaciones)
    }

    private fun assertEquals(esperado: Int, real: Int, mensaje: String) = org.junit.Assert.assertEquals(mensaje, esperado.toLong(), real.toLong())
}
