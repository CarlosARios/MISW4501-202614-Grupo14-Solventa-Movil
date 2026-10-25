package co.solventa.movil.features.siniestros.evidencia

import co.solventa.movil.core.imagen.ReductorDeFoto
import co.solventa.movil.core.persistencia.dao.EvidenciaCapturadaDao
import co.solventa.movil.core.persistencia.entidades.EvidenciaCapturada
import co.solventa.movil.core.ubicacion.Ubicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Las evidencias de un reporte en el dispositivo (HU-M2.1-01): qué se guarda
 * al capturar y qué puede viajar con la radicación.
 */
class EvidenciaRepositorioLocalTest {

    private val dao = DaoEnMemoria()
    private val envios = EnvioFalso()
    private val reductor = ReductorFalso()
    private val repositorio = EvidenciaRepositorioLocal(dao, HuellaFalsa(), reductor, envios)

    private val ubicacionBuena = Ubicacion(4.7712, -74.0452, 2601.0, 8f, 1_792_000_000_000)

    @Test
    fun `una captura queda pendiente y pide su envio`() = runTest {
        val guardada = repositorio.capturar(BORRADOR, archivo("frente.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)

        assertTrue(guardada)
        val fila = dao.filas.single()
        assertEquals("pendiente", fila.estado)
        assertEquals("photo", fila.tipo)
        assertEquals(0, fila.posicion)
        assertEquals(1, envios.pedidos)
    }

    // Criterio 5: la fotografía se acota antes de medirla. Si se midiera
    // primero, el tamaño y el resumen firmados no serían los del archivo que
    // se sube y el almacenamiento lo rechazaría.
    @Test
    fun `la fotografia se acota antes de calcular su huella`() = runTest {
        repositorio.capturar(BORRADOR, archivo("frente.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)

        assertEquals(1, reductor.reducidos.size)
        assertTrue(reductor.reducidos.single().endsWith("frente.jpg"))
    }

    /** El video sale de la cámara en 720p y 30 s: no hay nada que acotar. */
    @Test
    fun `el video no se reduce`() = runTest {
        repositorio.capturar(BORRADOR, archivo("choque.mp4"), TipoDeEvidencia.VIDEO, ubicacionBuena, duracionS = 12)

        assertTrue(reductor.reducidos.isEmpty())
    }

    @Test
    fun `cada captura guarda donde y cuando se tomo`() = runTest {
        repositorio.capturar(BORRADOR, archivo("frente.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)

        val fila = dao.filas.single()
        assertEquals(4.7712, fila.latitud, 0.0001)
        assertEquals(-74.0452, fila.longitud, 0.0001)
        assertEquals(2601.0, fila.altitud!!, 0.1)
        assertEquals(8f, fila.precisionM)
        assertTrue(fila.ubicacionConfiable)
        assertEquals(HUELLA, fila.sha256)
        assertEquals(TAMANO, fila.tamanoBytes)
    }

    // Criterio 3: la evidencia se conserva, marcada como no confiable.
    @Test
    fun `una ubicacion imprecisa se guarda marcada`() = runTest {
        val imprecisa = ubicacionBuena.copy(precisionM = Ubicacion.PRECISION_MAXIMA_M + 70)

        repositorio.capturar(BORRADOR, archivo("frente.jpg"), TipoDeEvidencia.FOTO, imprecisa)

        assertFalse(dao.filas.single().ubicacionConfiable)
    }

    @Test
    fun `sin lectura de ubicacion la evidencia igual se guarda`() = runTest {
        repositorio.capturar(BORRADOR, archivo("frente.jpg"), TipoDeEvidencia.FOTO, ubicacion = null)

        val fila = dao.filas.single()
        assertFalse(fila.ubicacionConfiable)
        assertEquals(0f, fila.precisionM)
    }

    @Test
    fun `las capturas se numeran en el orden en que se toman`() = runTest {
        repeat(3) { repositorio.capturar(BORRADOR, archivo("f$it.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena) }

        assertEquals(listOf(0, 1, 2), dao.filas.map { it.posicion })
    }

    @Test
    fun `un archivo ilegible no se registra`() = runTest {
        val repositorio = EvidenciaRepositorioLocal(dao, HuellaFalsa(ilegible = true), reductor, envios)

        val guardada = repositorio.capturar(BORRADOR, archivo("rota.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)

        assertFalse(guardada)
        assertTrue(dao.filas.isEmpty())
        assertEquals(0, envios.pedidos)
    }

    /**
     * El servicio comprueba cada llave contra el repositorio, así que lo que
     * todavía no subió no puede viajar con la radicación.
     */
    @Test
    fun `solo lo ya subido viaja con la radicacion`() = runTest {
        repositorio.capturar(BORRADOR, archivo("una.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)
        repositorio.capturar(BORRADOR, archivo("otra.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)
        dao.marcarSubida(dao.filas.first().id, "evidence/sub/una.jpg")

        val paraRadicar = repositorio.paraRadicar(BORRADOR)

        assertEquals(1, paraRadicar.size)
        assertEquals("evidence/sub/una.jpg", paraRadicar.single().llaveDelObjeto)
        assertEquals("2026-10-14T17:46:40Z", paraRadicar.single().capturadaEn)
        assertFalse(repositorio.todoSubido(BORRADOR))
    }

    @Test
    fun `al radicar se limpian las evidencias del borrador`() = runTest {
        repositorio.capturar(BORRADOR, archivo("una.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)

        repositorio.limpiar(BORRADOR)

        assertTrue(dao.filas.isEmpty())
    }

    @Test
    fun `lo observado llega con su estado de envio`() = runTest {
        repositorio.capturar(BORRADOR, archivo("una.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)
        dao.marcar(dao.filas.single().id, EstadoDeEvidencia.FALLIDA.guardado)

        val evidencias = repositorio.observar(BORRADOR).first()

        assertEquals(EstadoDeEvidencia.FALLIDA, evidencias.single().estado)
        assertTrue(evidencias.single().ubicacionConfiable)
    }

    private fun archivo(nombre: String) = File(nombre)

    // Al volver a la pantalla se reintenta lo que quedó pendiente: tras
    // varios fallos, la espera entre reintentos crece hasta horas.
    @Test
    fun `lo pendiente se vuelve a intentar al abrir`() = runTest {
        repositorio.capturar(BORRADOR, archivo("una.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)

        repositorio.reintentarEnvios(BORRADOR)

        assertEquals(1, envios.reintentos)
    }

    @Test
    fun `sin nada pendiente no se reintenta`() = runTest {
        repositorio.capturar(BORRADOR, archivo("una.jpg"), TipoDeEvidencia.FOTO, ubicacionBuena)
        dao.marcarSubida(dao.filas.single().id, "evidence/sub/una.jpg")

        repositorio.reintentarEnvios(BORRADOR)

        assertEquals(0, envios.reintentos)
    }

    private companion object {
        const val BORRADOR = "borrador-1"
        const val HUELLA = "abc123"
        const val TAMANO = 312_044L
    }

    private class HuellaFalsa(private val ilegible: Boolean = false) : HuellaDeArchivo {
        override suspend fun de(archivo: File): Huella? =
            if (ilegible) null else Huella(HUELLA, TAMANO)
    }

    private class EnvioFalso : EnvioDeEvidencia {
        var pedidos = 0
            private set

        var reintentos = 0
            private set

        override fun pedir() {
            pedidos++
        }

        override fun reintentar() {
            reintentos++
        }
    }

    /** El DAO de Room, con las filas en una lista. */
    private class DaoEnMemoria : EvidenciaCapturadaDao {
        val filas = mutableListOf<EvidenciaCapturada>()
        private val flujo = MutableStateFlow<List<EvidenciaCapturada>>(emptyList())

        override suspend fun guardar(evidencia: EvidenciaCapturada) {
            filas.removeAll { it.id == evidencia.id }
            filas += evidencia.copy(capturadaEn = INSTANTE)
            publicar()
        }

        override fun deBorrador(borradorId: String): Flow<List<EvidenciaCapturada>> = flujo

        override suspend fun listarDeBorrador(borradorId: String): List<EvidenciaCapturada> =
            filas.filter { it.borradorId == borradorId }.sortedBy { it.posicion }

        override suspend fun pendientes(): List<EvidenciaCapturada> =
            filas.filterNot { it.estado == EstadoDeEvidencia.SUBIDA.guardado }

        override suspend fun marcar(id: String, estado: String) {
            reemplazar(id) { it.copy(estado = estado) }
        }

        override suspend fun marcarSubida(id: String, llave: String) {
            reemplazar(id) {
                it.copy(estado = EstadoDeEvidencia.SUBIDA.guardado, llaveDelObjeto = llave)
            }
        }

        override suspend fun borrar(id: String) {
            filas.removeAll { it.id == id }
            publicar()
        }

        override suspend fun borrarDeBorrador(borradorId: String) {
            filas.removeAll { it.borradorId == borradorId }
            publicar()
        }

        private fun reemplazar(id: String, cambio: (EvidenciaCapturada) -> EvidenciaCapturada) {
            val indice = filas.indexOfFirst { it.id == id }
            if (indice >= 0) filas[indice] = cambio(filas[indice])
            publicar()
        }

        private fun publicar() {
            flujo.value = filas.sortedBy { it.posicion }
        }

        companion object {
            /** 2026-10-14T17:46:40Z, para que la fecha enviada sea estable. */
            const val INSTANTE = 1_792_000_000_000L
        }
    }
}

/** ReductorFalso anota cada archivo que se le pidió acotar. */
private class ReductorFalso : ReductorDeFoto {
    val reducidos = mutableListOf<String>()

    override suspend fun reducir(archivo: File): Boolean {
        reducidos += archivo.absolutePath
        return true
    }
}
