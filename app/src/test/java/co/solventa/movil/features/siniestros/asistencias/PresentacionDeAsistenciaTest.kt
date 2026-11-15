package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.R
import co.solventa.movil.core.ubicacion.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/**
 * Cómo se leen los datos de la solicitud en M14 (HU-M2.3-01): «4,7712 N ·
 * 74,0452 O», «8 m» y «16:31 (UTC 21:31)» del prototipo, en los dos idiomas, y
 * el texto de cada estado.
 */
class PresentacionDeAsistenciaTest {

    private val espanol = Locale.forLanguageTag("es-CO")
    private val bogota = ZoneId.of("America/Bogota")

    private fun lugar(latitud: Double, longitud: Double, precision: Float = 8f) =
        Ubicacion(latitud, longitud, altitud = null, precisionM = precision, leidaEn = 0)

    @Test
    fun `las coordenadas del prototipo con coma decimal y hemisferio`() {
        assertEquals(
            CoordenadasVisibles("4,7712", Hemisferio.NORTE, "74,0452", Hemisferio.OESTE),
            PresentacionDeAsistencia.coordenadas(lugar(4.7712, -74.0452), espanol),
        )
        assertEquals(
            CoordenadasVisibles("33.8688", Hemisferio.SUR, "151.2093", Hemisferio.ESTE),
            PresentacionDeAsistencia.coordenadas(lugar(-33.86882, 151.20929), Locale.ENGLISH),
        )
        assertEquals(
            CoordenadasVisibles("0,0000", Hemisferio.NORTE, "0,0000", Hemisferio.ESTE),
            PresentacionDeAsistencia.coordenadas(lugar(0.0, 0.0), espanol),
        )
    }

    @Test
    fun `la precision en metros enteros y nunca cero`() {
        assertEquals("8", PresentacionDeAsistencia.precision(lugar(0.0, 0.0, 8f), espanol))
        assertEquals("5", PresentacionDeAsistencia.precision(lugar(0.0, 0.0, 4.7f), espanol))
        assertEquals("1", PresentacionDeAsistencia.precision(lugar(0.0, 0.0, 0.2f), espanol))
        assertEquals("1.500", PresentacionDeAsistencia.precision(lugar(0.0, 0.0, 1_500f), espanol))
        assertEquals("1,500", PresentacionDeAsistencia.precision(lugar(0.0, 0.0, 1_500f), Locale.ENGLISH))
    }

    @Test
    fun `la hora local y la UTC del prototipo`() {
        // 2026-11-14 21:31:08 UTC es 16:31:08 en Bogotá.
        val instante = 1_794_691_868_000L

        assertEquals("16:31:08", PresentacionDeAsistencia.horaConSegundos(instante, espanol, bogota))
        assertEquals("16:31" to "21:31", PresentacionDeAsistencia.horaLocalYUtc(instante, espanol, bogota))
        assertEquals("4:31:08 PM", PresentacionDeAsistencia.horaConSegundos(instante, Locale.ENGLISH, bogota))
        assertEquals("4:31 PM" to "9:31 PM", PresentacionDeAsistencia.horaLocalYUtc(instante, Locale.ENGLISH, bogota))
    }

    @Test
    fun `cada estado motivo dialogo y hemisferio tiene su texto`() {
        assertEquals(
            PasoDelEstado.entries.size,
            PasoDelEstado.entries.map(TextosDeAsistencia::estado).toSet().size,
        )
        assertEquals(R.string.siniestros_asistencia_estado_llamada, TextosDeAsistencia.estado(PasoDelEstado.LLAMADA_EN_CURSO))
        assertEquals(MotivoSinUbicacion.entries.size, MotivoSinUbicacion.entries.map(TextosDeAsistencia::sinUbicacion).toSet().size)
        assertEquals(MotivoDeDescarte.entries.size, MotivoDeDescarte.entries.map(TextosDeAsistencia::descarte).toSet().size)
        assertEquals(TipoDeDialogo.entries.size, TipoDeDialogo.entries.map(TextosDeAsistencia::dialogo).toSet().size)
        assertEquals(Hemisferio.entries.size, Hemisferio.entries.map(TextosDeAsistencia::hemisferio).toSet().size)
    }
}
