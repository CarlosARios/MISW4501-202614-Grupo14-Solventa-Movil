package co.solventa.movil.features.siniestros.radicacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

/**
 * La fecha del hecho entre lo que se escribe y lo que se envía. La zona se
 * fija en las pruebas: el contrato recibe UTC y quien reporta escribe la hora
 * de su reloj.
 */
class FechaDelHechoTest {

    private val bogota = ZoneId.of("America/Bogota")

    @Test
    fun `una fecha completa se convierte al instante de esa hora local`() {
        val instante = FechaDelHecho.aInstante("20/10/2026 · 18:30", bogota)

        // 18:30 en Bogotá (UTC-5) son las 23:30 UTC.
        assertEquals(1_792_539_000_000L, instante)
    }

    @Test
    fun `el instante vuelve al texto con el que se escribio`() {
        assertEquals("20/10/2026 · 18:30", FechaDelHecho.aTexto(1_792_539_000_000L, bogota))
    }

    /** Mientras se teclea, lo escrito todavía no es una fecha. */
    @Test
    fun `una fecha a medias no es un instante`() {
        assertNull(FechaDelHecho.aInstante("20/10", bogota))
        assertNull(FechaDelHecho.aInstante("20/10/2026", bogota))
        assertNull(FechaDelHecho.aInstante("", bogota))
    }

    @Test
    fun `una fecha que no existe se rechaza`() {
        assertNull(FechaDelHecho.aInstante("31/02/2026 · 10:00", bogota))
        assertNull(FechaDelHecho.aInstante("20/10/2026 · 25:00", bogota))
    }

    @Test
    fun `sin instante no hay texto`() {
        assertEquals("", FechaDelHecho.aTexto(null, bogota))
    }

    /** El selector devuelve el dia en UTC y la hora por separado. */
    @Test
    fun `el dia del selector se combina con la hora elegida`() {
        // 20/10/2026 a medianoche UTC, que es lo que devuelve el selector.
        val dia = 1_792_454_400_000L

        assertEquals(1_792_539_000_000L, FechaDelHecho.combinar(dia, 18, 30, bogota))
    }

    @Test
    fun `la fecha de radicacion se lee del instante que devuelve el servicio`() {
        assertEquals("14/09/2026", FechaDelHecho.diaDeRfc3339("2026-09-14T23:55:00Z", bogota))
        assertEquals("18:55", FechaDelHecho.horaDeRfc3339("2026-09-14T23:55:00Z", bogota))
    }

    @Test
    fun `un instante ilegible no rompe la pantalla`() {
        assertEquals("", FechaDelHecho.diaDeRfc3339("ayer", bogota))
        assertEquals("", FechaDelHecho.horaDeRfc3339("", bogota))
    }

    @Test
    fun `los espacios de los extremos no estorban`() {
        assertEquals(
            FechaDelHecho.aInstante("20/10/2026 · 18:30", bogota),
            FechaDelHecho.aInstante("  20/10/2026 · 18:30  ", bogota),
        )
    }
}
