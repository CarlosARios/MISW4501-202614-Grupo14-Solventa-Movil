package co.solventa.movil.features.notificaciones.centro

import co.solventa.movil.R
import co.solventa.movil.core.navegacion.Ruta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * La alerta de vencimiento de una póliza (HU-M3.2-01) en la lógica pura: lo que
 * se lee del servicio, lo que se guarda, cómo se nombra la póliza y a dónde
 * lleva.
 */
class VencimientoDePolizaTest {

    private fun instante(texto: String) = Instant.parse(texto).toEpochMilli()

    @Test
    fun `del servicio sale la alerta con la poliza, la fecha exacta del vencimiento y el hito (criterio 1)`() {
        val n = MapeadorDeNotificaciones.desdeDto(vencimientoDto())

        assertEquals(TiposDeNotificacion.VENCIMIENTO_DE_POLIZA, n.tipo)
        assertNull(n.pago)
        assertNull(n.etapa)
        assertEquals(
            AlertaDeVencimiento(
                poliza = "AU-2025-118204", linea = LineasDeVencimiento.AUTOS, producto = "Autos", objetoAsegurado = "KJT-482",
                venceEn = instante("2026-09-26T05:00:00Z"), hito = 15, diasRestantes = 15,
            ),
            n.vencimiento,
        )
    }

    @Test
    fun `la alerta sobrevive a la copia local y una ilegible se pierde sin romper la notificacion`() {
        val n = MapeadorDeNotificaciones.desdeDto(vencimientoDto(producto = null, objeto = null))

        val fila = MapeadorDeNotificaciones.aEntidad(n, "sub-a")
        assertEquals(n, MapeadorDeNotificaciones.desdeEntidad(fila))
        assertNull(fila.pago)
        assertNull(fila.etapa)

        assertNull(MapeadorDeNotificaciones.desdeEntidad(fila.copy(vencimiento = "{roto")).vencimiento)
    }

    @Test
    fun `tocarla lleva al detalle de la poliza y queda leida`() {
        val n = MapeadorDeNotificaciones.desdeDto(vencimientoDto())

        assertEquals(DestinoDeNotificacion.Poliza("AU-2025-118204", "ntf-expiry-AU-2025-118204-20260926-15"), n.destino)
        assertEquals(Ruta.DetallePoliza("AU-2025-118204"), rutaDe(n.destino))
        assertEquals("ntf-expiry-AU-2025-118204-20260926-15", notificacionPorMarcar(n.destino))
    }

    @Test
    fun `solo se marcan al abrir las que no llevan a una pantalla de notificaciones`() {
        assertNull(notificacionPorMarcar(DestinoDeNotificacion.Pago("ntf-ppo-1")))
        assertEquals("ntf-stage-42", notificacionPorMarcar(DestinoDeNotificacion.Siniestro("cl-1", "ntf-stage-42")))
    }

    @Test
    fun `con cero dias dice que vence hoy y con dias que faltan, cuantos`() {
        val alerta = MapeadorDeNotificaciones.desdeDto(vencimientoDto()).vencimiento!!

        assertFalse(TextosDeNotificacion.venceHoy(alerta))
        assertFalse(TextosDeNotificacion.venceHoy(alerta.copy(diasRestantes = 1)))
        assertTrue(TextosDeNotificacion.venceHoy(alerta.copy(diasRestantes = 0)))
        assertTrue(TextosDeNotificacion.venceHoy(alerta.copy(diasRestantes = -1)))
    }

    @Test
    fun `cada tipo de seguro tiene su nombre y uno desconocido el generico`() {
        assertEquals(R.string.notificaciones_vencimiento_linea_vida, TextosDeNotificacion.lineaDeVencimiento(LineasDeVencimiento.VIDA))
        assertEquals(R.string.notificaciones_vencimiento_linea_autos, TextosDeNotificacion.lineaDeVencimiento(LineasDeVencimiento.AUTOS))
        assertEquals(R.string.notificaciones_vencimiento_linea_hogar, TextosDeNotificacion.lineaDeVencimiento(LineasDeVencimiento.HOGAR))
        assertEquals(R.string.notificaciones_vencimiento_linea_viaje, TextosDeNotificacion.lineaDeVencimiento(LineasDeVencimiento.VIAJE))
        assertEquals(R.string.notificaciones_vencimiento_linea_generica, TextosDeNotificacion.lineaDeVencimiento("pet"))
    }

    @Test
    fun `la poliza se nombra con el producto y lo asegurado, o con el tipo de seguro si no hay producto`() {
        val alerta = MapeadorDeNotificaciones.desdeDto(vencimientoDto()).vencimiento!!

        assertEquals("Autos · KJT-482", TextosDeNotificacion.descripcionDePoliza(alerta, "Auto"))
        assertEquals("Auto · KJT-482", TextosDeNotificacion.descripcionDePoliza(alerta.copy(producto = " "), "Auto"))
        assertEquals("Vida", TextosDeNotificacion.descripcionDePoliza(alerta.copy(producto = null, objetoAsegurado = null), "Vida"))
    }
}
