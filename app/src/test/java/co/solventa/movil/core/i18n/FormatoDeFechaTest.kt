package co.solventa.movil.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** Fechas y horas en el formato de cada idioma, en la zona dada (HU-M1.3-01). */
class FormatoDeFechaTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val instante = Instant.parse("2026-09-16T20:52:00Z").toEpochMilli()

    @Test
    fun `en espanol dia mes y hora de 24 horas`() {
        assertEquals("16/09/2026", FormatoDeFecha.fecha(instante, Idioma.ESPANOL, bogota))
        assertEquals("15:52", FormatoDeFecha.hora(instante, Idioma.ESPANOL, bogota))
    }

    @Test
    fun `en ingles mes dia y hora de 12 horas`() {
        assertEquals("09/16/2026", FormatoDeFecha.fecha(instante, Idioma.INGLES, bogota))
        assertEquals("3:52 PM", FormatoDeFecha.hora(instante, Idioma.INGLES, bogota))
    }

    @Test
    fun `usa la zona del dispositivo y no la UTC`() {
        val casiMedianocheUtc = Instant.parse("2026-09-17T02:30:00Z").toEpochMilli()

        assertEquals("16/09/2026", FormatoDeFecha.fecha(casiMedianocheUtc, Idioma.ESPANOL, bogota))
        assertEquals("21:30", FormatoDeFecha.hora(casiMedianocheUtc, Idioma.ESPANOL, bogota))
    }
}
