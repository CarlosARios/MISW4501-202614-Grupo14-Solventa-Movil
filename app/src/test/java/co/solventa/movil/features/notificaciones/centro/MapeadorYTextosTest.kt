package co.solventa.movil.features.notificaciones.centro

import co.solventa.movil.R
import co.solventa.movil.core.i18n.Idioma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** La traducción de las notificaciones y cómo se presentan (HU-M3.1-01). */
class MapeadorYTextosTest {

    @Test
    fun `la notificacion del servicio trae el pago con todo lo que muestra M10b`() {
        val n = MapeadorDeNotificaciones.desdeDto(notificacionDto(leida = "2026-09-11T22:00:00Z"))

        assertEquals("ntf-ppo-1", n.id)
        assertEquals(PAGADO, n.ocurrioEn)
        assertEquals(PAGADO + 20 * 60_000, n.leidaEn)
        assertTrue(n.leida)
        val p = n.pago!!
        assertEquals(180_000.0, p.monto, 0.0)
        assertEquals("COP", p.moneda)
        assertEquals(Disparador("flight_delay", "AV-8421", 180.0, 215.0, "minutes", PAGADO - 60_000), p.disparador)
        assertEquals(MedioDePago("visa", "1111"), p.medioDePago)
        assertEquals("pay-ppo-1", p.claveDeIdempotencia)
        assertEquals("VJ-2026-000931", p.poliza)
        assertEquals("SIN-2026-01702", p.siniestro)
    }

    @Test(expected = RuntimeException::class)
    fun `una fecha que no se entiende hace fallar la lista`() {
        MapeadorDeNotificaciones.desdeDto(notificacionDto(ocurrio = "ayer"))
    }

    @Test
    fun `lo que se guarda vuelve igual y un pago ilegible no rompe la notificacion`() {
        val n = notificacion(leidaEn = PAGADO + 1)

        val entidad = MapeadorDeNotificaciones.aEntidad(n, "sub-a")
        assertEquals("sub-a", entidad.propietario)
        assertEquals(n, MapeadorDeNotificaciones.desdeEntidad(entidad))

        assertNull(MapeadorDeNotificaciones.desdeEntidad(entidad.copy(pago = "{roto")).pago)
        assertNull(MapeadorDeNotificaciones.desdeEntidad(entidad.copy(pago = null)).pago)
    }

    @Test
    fun `el monto y el medio de pago se escriben como el prototipo`() {
        assertEquals("$ 180.000 COP", TextosDeNotificacion.monto(180_000.0, "COP", Idioma.ESPANOL.aLocale()))
        assertEquals("$ 180,000 COP", TextosDeNotificacion.monto(180_000.0, "COP", Idioma.INGLES.aLocale()))
        assertEquals("Visa •••• 1111", TextosDeNotificacion.medioDePago(MedioDePago("visa", "1111"), Idioma.ESPANOL.aLocale()))
        assertEquals("215", TextosDeNotificacion.numero(215.0, Idioma.ESPANOL.aLocale()))
        assertEquals("2,5", TextosDeNotificacion.numero(2.5, Idioma.ESPANOL.aLocale()))
    }

    @Test
    fun `el retraso de vuelo tiene sus textos y otro parametro los genericos`() {
        assertEquals(R.string.notificaciones_evento_retraso_de_vuelo, TextosDeNotificacion.evento("flight_delay"))
        assertEquals(R.string.notificaciones_evento_generico, TextosDeNotificacion.evento("rainfall"))
        assertEquals(R.string.notificaciones_aviso_cuerpo_retraso_de_vuelo, TextosDeNotificacion.cuerpoDelAviso("flight_delay"))
        assertEquals(R.string.notificaciones_aviso_cuerpo_generico, TextosDeNotificacion.cuerpoDelAviso("rainfall"))
        assertEquals(R.string.notificaciones_centro_cuerpo_retraso_de_vuelo, TextosDeNotificacion.cuerpoDelCentro("flight_delay"))
        assertEquals(R.string.notificaciones_centro_cuerpo_generico, TextosDeNotificacion.cuerpoDelCentro("rainfall"))
        assertEquals(R.string.notificaciones_parametro_retraso_de_vuelo, TextosDeNotificacion.parametro("flight_delay"))
        assertNull(TextosDeNotificacion.parametro("rainfall"))
        assertEquals(R.string.notificaciones_unidad_minutos, TextosDeNotificacion.unidad("minutes"))
        assertNull(TextosDeNotificacion.unidad("mm"))
    }

    @Test
    fun `hoy es el mismo dia en la zona del telefono`() {
        val bogota = ZoneId.of("America/Bogota")
        // 21:40 UTC es 16:40 en Bogotá: el mismo día hasta las 05:00 UTC del siguiente.
        assertTrue(TextosDeNotificacion.esDeHoy(PAGADO, PAGADO + 7 * 3_600_000, bogota))
        assertFalse(TextosDeNotificacion.esDeHoy(PAGADO, PAGADO + 8 * 3_600_000, bogota))
        assertFalse(TextosDeNotificacion.esDeHoy(PAGADO, PAGADO + DIA, bogota))
    }

    @Test
    fun `los estados derivados del centro y del detalle`() {
        assertTrue(CentroUiState(cargando = false).vacio)
        assertFalse(CentroUiState(cargando = false, sinConexion = true).vacio)
        assertFalse(CentroUiState(cargando = false, sinLeer = 0).puedeMarcarTodas)
        assertTrue(CentroUiState(cargando = false, hoy = listOf(notificacion()), sinLeer = 1).puedeMarcarTodas)

        assertTrue(DetallePagoUiState(cargando = false, notificacion = notificacion()).superoElUmbral)
        assertFalse(DetallePagoUiState(cargando = false).superoElUmbral)
    }
}
