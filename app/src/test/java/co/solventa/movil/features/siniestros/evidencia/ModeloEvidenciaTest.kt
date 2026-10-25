package co.solventa.movil.features.siniestros.evidencia

import co.solventa.movil.core.ubicacion.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lo que el contrato nombra y lo que la pantalla muestra (HU-M2.1-01). */
class ModeloEvidenciaTest {

    @Test
    fun `cada tipo viaja con el nombre y el formato del contrato`() {
        assertEquals("photo", TipoDeEvidencia.FOTO.delContrato)
        assertEquals("image/jpeg", TipoDeEvidencia.FOTO.tipoDeContenido)
        assertEquals("video", TipoDeEvidencia.VIDEO.delContrato)
        assertEquals("video/mp4", TipoDeEvidencia.VIDEO.tipoDeContenido)
    }

    @Test
    fun `el estado se guarda y se vuelve a leer`() {
        EstadoDeEvidencia.entries.forEach { estado ->
            assertEquals(estado, EstadoDeEvidencia.de(estado.guardado))
        }
    }

    /** Una base de una versión anterior puede traer otra cosa. */
    @Test
    fun `un estado desconocido se trata como pendiente`() {
        assertEquals(EstadoDeEvidencia.PENDIENTE, EstadoDeEvidencia.de("lo-que-sea"))
        assertEquals(EstadoDeEvidencia.PENDIENTE, EstadoDeEvidencia.de(null))
    }

    // Criterio 3: el límite de precisión decide si la ubicación respalda algo.
    @Test
    fun `la ubicacion es confiable hasta el limite`() {
        assertTrue(ubicacion(precision = Ubicacion.PRECISION_MAXIMA_M).esConfiable)
        assertTrue(ubicacion(precision = 8f).esConfiable)
        assertFalse(ubicacion(precision = Ubicacion.PRECISION_MAXIMA_M + 1).esConfiable)
    }

    /** Una precisión de cero no es perfecta: es que no hay lectura. */
    @Test
    fun `sin precision no hay ubicacion confiable`() {
        assertFalse(ubicacion(precision = 0f).esConfiable)
        assertFalse(evidencia(ubicacion = null).ubicacionConfiable)
    }

    @Test
    fun `una evidencia sabe si su ubicacion respalda el lugar`() {
        assertTrue(evidencia(ubicacion(precision = 8f)).ubicacionConfiable)
        assertFalse(evidencia(ubicacion(precision = 120f)).ubicacionConfiable)
    }

    @Test
    fun `los limites son los que el contrato exige`() {
        assertEquals(4, LimitesDeEvidencia.MINIMO_DE_FOTOS)
        assertEquals(6, LimitesDeEvidencia.MAXIMO_DE_FOTOS)
        assertEquals(1, LimitesDeEvidencia.MAXIMO_DE_VIDEOS)
        assertEquals(30, LimitesDeEvidencia.MAXIMO_DE_SEGUNDOS)
    }

    private fun ubicacion(precision: Float) =
        Ubicacion(4.7712, -74.0452, null, precision, 1_792_000_000_000)

    private fun evidencia(ubicacion: Ubicacion?) = EvidenciaDelReporte(
        id = "ev-1",
        tipo = TipoDeEvidencia.FOTO,
        rutaArchivo = "/tmp/ev-1.jpg",
        capturadaEn = 1_792_000_000_000,
        ubicacion = ubicacion,
        duracionS = 0,
        estado = EstadoDeEvidencia.PENDIENTE,
    )
}
