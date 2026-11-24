package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.core.api.siniestros.EvidenciaRadicada
import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.features.siniestros.evidencia.EvidenciaDelReporte
import co.solventa.movil.features.siniestros.evidencia.EvidenciaRepositorio
import co.solventa.movil.features.siniestros.evidencia.TipoDeEvidencia
import co.solventa.movil.features.siniestros.radicacion.BorradorEnCurso
import co.solventa.movil.features.siniestros.radicacion.BorradorRepositorio
import co.solventa.movil.features.siniestros.radicacion.BorradorResumen
import co.solventa.movil.features.siniestros.radicacion.CasoRadicado
import co.solventa.movil.features.siniestros.radicacion.EnvioDelBorrador
import co.solventa.movil.features.siniestros.radicacion.Etapa
import co.solventa.movil.features.siniestros.radicacion.TipoDeIncidente
import co.solventa.movil.features.siniestros.radicacion.envio.ColaDeRadicacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** Los borradores de *Mis siniestros* (HU-M2.2-02, M13b–M13d). */
@OptIn(ExperimentalCoroutinesApi::class)
class BorradoresViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val borradores = BorradoresDelUsuario()
    private val evidencias = EvidenciaLimpiable()
    private val cola = ColaDePrueba()
    private val recordatorio = RecordatorioDeBorrador()

    @Before
    fun antes() = Dispatchers.setMain(dispatcher)

    @After
    fun despues() = Dispatchers.resetMain()

    private fun crear() = BorradoresViewModel(borradores, evidencias, cola, recordatorio)

    @Test
    fun `muestra los borradores y ofrece retomar el que sigue en edicion una sola vez`() = runTest(dispatcher) {
        borradores.lista.value = listOf(resumen("b1", EnvioDelBorrador.EDICION))

        val vm = crear()
        advanceUntilIdle()

        assertEquals(listOf("b1"), vm.estado.value.borradores.map { it.id })
        assertEquals("b1", vm.estado.value.hoja?.id)

        // Al volver a la pestaña ya no se ofrece sola.
        val otra = crear()
        advanceUntilIdle()
        assertNull(otra.estado.value.hoja)
    }

    @Test
    fun `al abrir vuelve a encolar lo que esperaba su envio`() = runTest(dispatcher) {
        borradores.lista.value = listOf(
            resumen("b1", EnvioDelBorrador.EN_COLA),
            resumen("b2", EnvioDelBorrador.ENVIANDO),
            resumen("b3", EnvioDelBorrador.FALLIDO),
        )

        crear()
        advanceUntilIdle()

        assertEquals(listOf("b1", "b2"), cola.encolados)
    }

    @Test
    fun `reintentar ahora pone los fallidos en cola con los intentos en cero`() = runTest(dispatcher) {
        borradores.lista.value = listOf(resumen("b1", EnvioDelBorrador.FALLIDO, intentos = 3))
        val vm = crear()
        advanceUntilIdle()
        assertEquals(listOf("b1"), vm.estado.value.fallidos.map { it.id })

        vm.reintentarEnvios()
        advanceUntilIdle()

        assertEquals(listOf("b1"), borradores.encolados)
        assertEquals(listOf("b1"), cola.reintentados)
    }

    @Test
    fun `el radicado de un envio en segundo plano sale como aviso y no como tarjeta`() = runTest(dispatcher) {
        borradores.lista.value = listOf(
            resumen("b1", EnvioDelBorrador.RADICADO).copy(radicado = "SIN-2026-01877", radicadoEn = 5L),
        )
        val vm = crear()
        advanceUntilIdle()

        assertTrue(vm.estado.value.borradores.isEmpty())
        assertEquals("SIN-2026-01877", vm.estado.value.radicado?.radicado)

        vm.cerrarAvisoDeRadicado()
        advanceUntilIdle()
        assertEquals(listOf("b1"), borradores.descartados)
    }

    @Test
    fun `continuar retoma el borrador y pide abrir el asistente`() = runTest(dispatcher) {
        borradores.lista.value = listOf(resumen("b1", EnvioDelBorrador.FALLIDO))
        val vm = crear()
        advanceUntilIdle()

        vm.abrir("b1")
        assertEquals("b1", vm.estado.value.hoja?.id)
        vm.continuar()
        advanceUntilIdle()

        assertEquals(listOf("b1"), borradores.retomados)
        assertEquals(listOf("b1"), cola.cancelados)
        assertTrue(vm.estado.value.abrirAsistente)
        assertNull(vm.estado.value.hoja)

        vm.asistenteAbierto()
        assertTrue(!vm.estado.value.abrirAsistente)
    }

    @Test
    fun `un borrador en cola no abre la hoja`() = runTest(dispatcher) {
        recordatorio.mostrado = true
        borradores.lista.value = listOf(resumen("b1", EnvioDelBorrador.EN_COLA))
        val vm = crear()
        advanceUntilIdle()

        vm.abrir("b1")

        assertNull(vm.estado.value.hoja)
    }

    @Test
    fun `descartar borra el borrador, su evidencia y su envio`() = runTest(dispatcher) {
        borradores.lista.value = listOf(resumen("b1", EnvioDelBorrador.EDICION))
        val vm = crear()
        advanceUntilIdle()

        vm.descartar()
        advanceUntilIdle()

        assertEquals(listOf("b1"), borradores.descartados)
        assertEquals("b1", evidencias.limpiado)
        assertEquals(listOf("b1"), cola.cancelados)
        assertNull(vm.estado.value.hoja)
    }

    private fun resumen(id: String, envio: EnvioDelBorrador, intentos: Int = 0) = BorradorResumen(
        id = id,
        etapa = Etapa.EVIDENCIAS,
        tipo = TipoDeIncidente.COLISION,
        poliza = "AU-2025-118204",
        actualizadoEn = 1_000L,
        envio = envio,
        intentos = intentos,
        fotos = 4,
        videos = 1,
    )
}

private class BorradoresDelUsuario : BorradorRepositorio {
    val lista = MutableStateFlow<List<BorradorResumen>>(emptyList())
    val encolados = mutableListOf<String>()
    val retomados = mutableListOf<String>()
    val descartados = mutableListOf<String>()

    override suspend fun enCurso(): BorradorEnCurso? = null
    override suspend fun guardar(borrador: BorradorEnCurso) = Unit
    override suspend fun descartar(id: String) {
        descartados += id
    }

    override fun nuevoIdentificador(): String = "nuevo"
    override fun delUsuario(): Flow<List<BorradorResumen>> = lista
    override suspend fun retomar(id: String) {
        retomados += id
    }

    override suspend fun encolar(id: String) {
        encolados += id
    }

    override suspend fun paraEnviar(id: String): BorradorEnCurso? = null
    override suspend fun marcarEnviando(id: String) = Unit
    override suspend fun registrarFallo(id: String, envio: EnvioDelBorrador) = Unit
    override suspend fun marcarRadicado(id: String, caso: CasoRadicado) = Unit
}

private class ColaDePrueba : ColaDeRadicacion {
    val encolados = mutableListOf<String>()
    val reintentados = mutableListOf<String>()
    val cancelados = mutableListOf<String>()
    override fun encolar(borradorId: String) {
        encolados += borradorId
    }

    override fun reintentar(borradorId: String) {
        reintentados += borradorId
    }

    override fun cancelar(borradorId: String) {
        cancelados += borradorId
    }
}

private class EvidenciaLimpiable : EvidenciaRepositorio {
    var limpiado: String? = null
    override fun observar(borradorId: String): Flow<List<EvidenciaDelReporte>> = flowOf(emptyList())
    override suspend fun capturar(
        borradorId: String,
        archivo: File,
        tipo: TipoDeEvidencia,
        ubicacion: Ubicacion?,
        duracionS: Int,
    ): Boolean = true

    override suspend fun descartar(id: String) = Unit
    override suspend fun reintentarEnvios(borradorId: String) = Unit
    override suspend fun paraRadicar(borradorId: String): List<EvidenciaRadicada> = emptyList()
    override suspend fun todoSubido(borradorId: String): Boolean = true
    override suspend fun limpiar(borradorId: String) {
        limpiado = borradorId
    }
}
