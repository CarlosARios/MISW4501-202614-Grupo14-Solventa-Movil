package co.solventa.movil.features.notificaciones.centro

import androidx.lifecycle.SavedStateHandle
import co.solventa.movil.core.i18n.Idioma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El revisor del centro, el sondeo, M15 y M10b (HU-M3.1-01). */
@OptIn(ExperimentalCoroutinesApi::class)
class RevisorYViewModelsTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun copia(vararg n: Notificacion, sinConexion: Boolean = false) =
        CopiaDeNotificaciones(n.toList(), PAGADO, sinConexion)

    // --- Revisor ---

    @Test
    fun `revisar registra el dispositivo, sincroniza y avisa las nuevas en el idioma elegido`() = runTest(despachador) {
        val repositorio = RepositorioFalso().apply {
            resultado = ResultadoDeSincronizacion.Sincronizado(listOf(notificacion("ntf-1"), notificacion("ntf-2")))
        }
        val avisador = AvisadorFalso()
        val revisor = RevisorDeNotificaciones(repositorio, avisador, PermisoFalso(true), IdiomasFalsos(Idioma.INGLES))

        revisor.revisar()

        assertEquals(listOf(true to "en"), repositorio.registros)
        assertEquals(listOf(listOf("ntf-1", "ntf-2") to Idioma.INGLES), avisador.avisadas)
        assertEquals(listOf("ntf-1", "ntf-2"), repositorio.avisadas)
    }

    @Test
    fun `sin permiso se registra asi y las nuevas quedan en el centro (criterio 5)`() = runTest(despachador) {
        val repositorio = RepositorioFalso().apply {
            resultado = ResultadoDeSincronizacion.Sincronizado(listOf(notificacion("ntf-1")))
        }
        val revisor = RevisorDeNotificaciones(repositorio, AvisadorFalso(), PermisoFalso(false), IdiomasFalsos())

        revisor.revisar()

        assertEquals(listOf(false to "es"), repositorio.registros)
        assertEquals("se marca avisada para no inundar al conceder el permiso", listOf("ntf-1"), repositorio.avisadas)
    }

    @Test
    fun `sin novedades o sin red no se avisa nada`() = runTest(despachador) {
        val repositorio = RepositorioFalso().apply { resultado = ResultadoDeSincronizacion.SinConexion }
        val avisador = AvisadorFalso()

        RevisorDeNotificaciones(repositorio, avisador, PermisoFalso(), IdiomasFalsos()).revisar()

        assertTrue(avisador.avisadas.isEmpty())
    }

    // --- Sondeo ---

    @Test
    fun `el sondeo programa la revision periodica y revisa cada intervalo mientras esta visible`() = runTest(despachador) {
        val repositorio = RepositorioFalso()
        val programador = ProgramadorFalso()
        val revisor = RevisorDeNotificaciones(repositorio, AvisadorFalso(), PermisoFalso(), IdiomasFalsos())
        val vm = SondeoDeNotificacionesViewModel(revisor, programador, AperturaDesdeNotificacion(), 20_000)

        val visible = launch { vm.mientrasVisible() }
        runCurrent()
        assertEquals(1, programador.programado)
        assertEquals(1, repositorio.sincronizaciones)
        advanceTimeBy(20_001)
        assertEquals(2, repositorio.sincronizaciones)
        visible.cancel()
        advanceTimeBy(60_000)
        assertEquals("deja de revisar al ocultarse", 2, repositorio.sincronizaciones)
    }

    @Test
    fun `responder al permiso registra enseguida y la notificacion tocada se entrega una vez`() = runTest(despachador) {
        val repositorio = RepositorioFalso()
        val revisor = RevisorDeNotificaciones(repositorio, AvisadorFalso(), PermisoFalso(), IdiomasFalsos())
        val apertura = AperturaDesdeNotificacion()
        val vm = SondeoDeNotificacionesViewModel(revisor, ProgramadorFalso(), apertura, 20_000)

        vm.permisoRespondido()
        testScheduler.advanceUntilIdle()
        assertEquals(1, repositorio.registros.size)

        assertNull(vm.abrirPendiente())
    }

    // --- M15 ---

    @Test
    fun `el centro separa hoy de anteriores, cuenta las no leidas y marca avisadas las que ve`() = runTest(despachador) {
        val hoy = notificacion("ntf-hoy", ocurrioEn = PAGADO)
        val ayer = notificacion("ntf-ayer", ocurrioEn = PAGADO - 2 * DIA, leidaEn = PAGADO - DIA)
        val repositorio = RepositorioFalso(copia(hoy, ayer)).apply {
            resultado = ResultadoDeSincronizacion.Sincronizado(listOf(hoy))
        }

        val vm = CentroViewModel(repositorio) { PAGADO + 60_000 }
        assertTrue(vm.estado.value.cargando)
        testScheduler.advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals(listOf("ntf-hoy"), estado.hoy.map { it.id })
        assertEquals(listOf("ntf-ayer"), estado.anteriores.map { it.id })
        assertEquals(1, estado.sinLeer)
        assertTrue(estado.puedeMarcarTodas)
        assertEquals("lo que se ve aqui ya no se avisa", listOf("ntf-hoy"), repositorio.avisadas)

        vm.marcarTodasLeidas()
        testScheduler.advanceUntilIdle()
        assertEquals(1, repositorio.todasLeidas)
    }

    @Test
    fun `el centro sin conexion o con error lo dice y se actualiza al volver la red`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copia(sinConexion = true)).apply {
            red.value = false
            resultado = ResultadoDeSincronizacion.Error
        }
        val vm = CentroViewModel(repositorio) { PAGADO }
        testScheduler.advanceUntilIdle()
        assertTrue(vm.estado.value.sinConexion)
        assertTrue(vm.estado.value.error)
        assertFalse(vm.estado.value.vacio)

        repositorio.red.value = true
        testScheduler.advanceUntilIdle()
        assertEquals(2, repositorio.sincronizaciones)

        vm.reintentar()
        testScheduler.advanceUntilIdle()
        assertEquals(3, repositorio.sincronizaciones)
    }

    // --- M10b ---

    private fun detalle(repositorio: RepositorioFalso, id: String = "ntf-ppo-1") =
        DetallePagoViewModel(SavedStateHandle(mapOf(DetallePagoViewModel.ARGUMENTO_ID to id)), repositorio)

    @Test
    fun `el detalle muestra el pago de la copia y lo marca leido una vez (criterio 4)`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copia(notificacion("ntf-ppo-1")))

        val vm = detalle(repositorio)
        testScheduler.advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals("AV-8421", estado.notificacion?.pago?.disparador?.evento)
        assertTrue(estado.superoElUmbral)
        assertEquals(listOf("ntf-ppo-1"), repositorio.leidas)
        assertEquals("no sincroniza lo que ya tiene", 0, repositorio.sincronizaciones)
    }

    @Test
    fun `abierto desde un aviso que la copia no tiene, lo trae del servicio`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copia())
        repositorio.alSincronizar = { repositorio.copia.value = copia(notificacion("ntf-ppo-1")) }

        val vm = detalle(repositorio)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repositorio.sincronizaciones)
        assertEquals("ntf-ppo-1", vm.estado.value.notificacion?.id)
        assertFalse(vm.estado.value.noDisponible)
    }

    @Test
    fun `una notificacion que no existe no esta disponible`() = runTest(despachador) {
        val repositorio = RepositorioFalso(copia())

        val vm = detalle(repositorio, "ntf-otra")
        testScheduler.advanceUntilIdle()

        assertTrue(vm.estado.value.noDisponible)
        assertFalse(vm.estado.value.cargando)
        assertEquals(1, repositorio.sincronizaciones)
    }
}
