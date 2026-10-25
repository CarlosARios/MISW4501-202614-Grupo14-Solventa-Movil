package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.core.i18n.Idioma
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * «Efectivo en 2,1 segundos» (frame 38:1414): la demora en segundos con un
 * decimal y el separador del idioma de la aplicacion (HU-M1.3-02).
 */
class SegundosDeLaRevocacionTest {

    @Test
    fun `en español el decimal va con coma`() {
        assertEquals("2,1", segundos(2_100L, Idioma.ESPANOL))
    }

    @Test
    fun `en ingles el decimal va con punto`() {
        assertEquals("2.1", segundos(2_100L, Idioma.INGLES))
    }

    @Test
    fun `se redondea a un decimal`() {
        assertEquals("0,0", segundos(0L, Idioma.ESPANOL))
        assertEquals("4,0", segundos(3_960L, Idioma.ESPANOL))
    }
}
