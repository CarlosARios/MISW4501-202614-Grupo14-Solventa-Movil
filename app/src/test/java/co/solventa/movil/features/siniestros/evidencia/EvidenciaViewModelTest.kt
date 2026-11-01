package co.solventa.movil.features.siniestros.evidencia

import co.solventa.movil.core.ubicacion.Localizador
import co.solventa.movil.core.ubicacion.Ubicacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import java.io.File

/**
 * La captura de evidencia (HU-M2.1-01): un caso por criterio de los que esta
 * pantalla gobierna.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EvidenciaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repositorio: RepositorioFalso
    private lateinit var localizador: LocalizadorFalso

    @Before
    fun antes() {
        Dispatchers.setMain(dispatcher)
        repositorio = RepositorioFalso()
        localizador = LocalizadorFalso()
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = EvidenciaViewModel(repositorio, localizador)

    @Test
    fun `al abrir se leen las evidencias del borrador y la ubicacion`() = runTest(dispatcher) {
        val viewModel = crearViewModel()

        viewModel.abrir(BORRADOR)
        advanceUntilIdle()

        assertEquals(BORRADOR, viewModel.estado.value.borradorId)
        assertEquals(8f, viewModel.estado.value.ubicacion?.precisionM)
    }

    // Criterio 3: con precisión suficiente se captura sin preguntar nada.
    @Test
    fun `con buena precision se captura de una vez`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()

        val puede = viewModel.intentarCapturar(TipoDeEvidencia.FOTO)

        assertTrue(puede)
        assertNull(viewModel.estado.value.confirmandoPrecision)
    }

    @Test
    fun `con precision insuficiente se avisa antes de capturar`() = runTest(dispatcher) {
        localizador.ubicacion = ubicacion(precision = Ubicacion.PRECISION_MAXIMA_M + 70)
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()

        val puede = viewModel.intentarCapturar(TipoDeEvidencia.FOTO)

        assertFalse(puede)
        assertEquals(TipoDeEvidencia.FOTO, viewModel.estado.value.confirmandoPrecision)
    }

    @Test
    fun `sin lectura de ubicacion tambien se avisa`() = runTest(dispatcher) {
        localizador.ubicacion = null
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()

        assertFalse(viewModel.intentarCapturar(TipoDeEvidencia.VIDEO))
        assertEquals(TipoDeEvidencia.VIDEO, viewModel.estado.value.confirmandoPrecision)
    }

    @Test
    fun `esperar mejor senal cierra el aviso y vuelve a leer`() = runTest(dispatcher) {
        localizador.ubicacion = ubicacion(precision = 120f)
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()
        viewModel.intentarCapturar(TipoDeEvidencia.FOTO)

        localizador.ubicacion = ubicacion(precision = 6f)
        viewModel.esperarMejorSenal()
        advanceUntilIdle()

        assertNull(viewModel.estado.value.confirmandoPrecision)
        assertEquals(6f, viewModel.estado.value.ubicacion?.precisionM)
    }

    @Test
    fun `capturar de todas formas devuelve que se iba a capturar`() = runTest(dispatcher) {
        localizador.ubicacion = ubicacion(precision = 120f)
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()
        viewModel.intentarCapturar(TipoDeEvidencia.VIDEO)

        val tipo = viewModel.capturarDeTodasFormas()

        assertEquals(TipoDeEvidencia.VIDEO, tipo)
        assertNull(viewModel.estado.value.confirmandoPrecision)
    }

    // Criterio 4: lo capturado se registra con el lugar y la hora del momento.
    @Test
    fun `lo capturado se registra con la ubicacion del momento`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()

        viewModel.registrar(File("/tmp/evidencia.jpg"), TipoDeEvidencia.FOTO)
        advanceUntilIdle()

        assertEquals(BORRADOR, repositorio.borradorRecibido)
        assertEquals(TipoDeEvidencia.FOTO, repositorio.tipoRecibido)
        assertEquals(8f, repositorio.ubicacionRecibida?.precisionM)
    }

    @Test
    fun `un video se registra con su duracion`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()

        viewModel.registrar(File("/tmp/evidencia.mp4"), TipoDeEvidencia.VIDEO, duracionS = 22)
        advanceUntilIdle()

        assertEquals(22, repositorio.duracionRecibida)
    }

    @Test
    fun `el contador y los topes salen de lo capturado`() = runTest(dispatcher) {
        repositorio.publicar(
            List(LimitesDeEvidencia.MAXIMO_DE_FOTOS) { evidencia("ev-$it", TipoDeEvidencia.FOTO) },
        )
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(LimitesDeEvidencia.MAXIMO_DE_FOTOS, estado.fotos)
        assertFalse("ya no caben más fotos", estado.puedeTomarFoto)
        assertTrue("el video sigue disponible", estado.puedeGrabarVideo)
        assertEquals(LimitesDeEvidencia.MAXIMO_DE_FOTOS, estado.numeroDeLaSiguiente)
    }

    @Test
    fun `un solo video por reporte`() = runTest(dispatcher) {
        repositorio.publicar(listOf(evidencia("ev-video", TipoDeEvidencia.VIDEO)))
        val viewModel = crearViewModel()
        viewModel.abrir(BORRADOR)
        advanceUntilIdle()

        assertFalse(viewModel.estado.value.puedeGrabarVideo)
    }

    private fun ubicacion(precision: Float) =
        Ubicacion(4.7712, -74.0452, null, precision, 1_792_000_000_000)

    private fun evidencia(id: String, tipo: TipoDeEvidencia) = EvidenciaDelReporte(
        id = id,
        tipo = tipo,
        rutaArchivo = "/tmp/$id",
        capturadaEn = 1_792_000_000_000,
        ubicacion = ubicacion(precision = 8f),
        duracionS = 0,
        estado = EstadoDeEvidencia.SUBIDA,
    )

    private companion object {
        const val BORRADOR = "borrador-1"
    }

    private class LocalizadorFalso : Localizador {
        var ubicacion: Ubicacion? = Ubicacion(4.7712, -74.0452, null, 8f, 1_792_000_000_000)

        override suspend fun ubicacionActual(): Ubicacion? = ubicacion
    }

    private class RepositorioFalso : EvidenciaRepositorio {
        private val capturadas = MutableStateFlow<List<EvidenciaDelReporte>>(emptyList())

        var borradorRecibido: String? = null
            private set
        var tipoRecibido: TipoDeEvidencia? = null
            private set
        var ubicacionRecibida: Ubicacion? = null
            private set
        var duracionRecibida: Int = 0
            private set

        fun publicar(evidencias: List<EvidenciaDelReporte>) {
            capturadas.value = evidencias
        }

        override fun observar(borradorId: String): Flow<List<EvidenciaDelReporte>> = capturadas

        override suspend fun capturar(
            borradorId: String,
            archivo: File,
            tipo: TipoDeEvidencia,
            ubicacion: Ubicacion?,
            duracionS: Int,
        ): Boolean {
            borradorRecibido = borradorId
            tipoRecibido = tipo
            ubicacionRecibida = ubicacion
            duracionRecibida = duracionS
            return true
        }

        override suspend fun descartar(id: String) {
            capturadas.value = capturadas.value.filterNot { it.id == id }
        }

        var reintentos = 0
            private set

        override suspend fun reintentarEnvios(borradorId: String) {
            reintentos++
        }

        override suspend fun paraRadicar(borradorId: String) = emptyList<co.solventa.movil.core.api.siniestros.EvidenciaRadicada>()

        override suspend fun todoSubido(borradorId: String): Boolean = true

        override suspend fun limpiar(borradorId: String) = Unit
    }
}
