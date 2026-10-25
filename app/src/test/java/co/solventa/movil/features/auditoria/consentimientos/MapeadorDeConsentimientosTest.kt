package co.solventa.movil.features.auditoria.consentimientos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La traduccion entre red, copia local y dominio, y la regla del estado
 * visible con un instante fijo (HU-M1.3-01).
 */
class MapeadorDeConsentimientosTest {

    @Test
    fun `del servicio al dominio con fechas en milisegundos`() {
        val c = MapeadorDeConsentimientos.desdeDto(dto("cns-2", "revoked", finalizado = "2026-10-01T10:00:00Z", comprobante = "REV-1"))

        assertEquals("cns-2", c.id)
        assertEquals(EstadosDeConsentimiento.REVOCADO, c.estado)
        assertFalse(c.vigente)
        assertEquals(OTORGADO, c.otorgadoEn)
        assertEquals(VENCE, c.venceEn)
        assertEquals(1_790_848_800_000L, c.finalizadoEn)
        assertEquals("REV-1", c.comprobante)
        assertEquals("Banco Aliado", c.nombreDeEntidad)
        assertEquals(listOf("credit_products", "balances"), c.alcance.single().categorias)
        assertEquals(listOf(CambiosDeEstado.OTORGADO, CambiosDeEstado.REVOCADO), c.historial.map { it.estado })
        assertEquals("REV-1", c.historial.last().comprobante)
    }

    @Test
    fun `ida y vuelta por la copia local sin perder nada`() {
        val original = MapeadorDeConsentimientos.desdeDto(dto("cns-2", "revoked", finalizado = "2026-10-01T10:00:00Z", comprobante = "REV-1"))

        val fila = MapeadorDeConsentimientos.aEntidad(original, "sub-a")
        val leido = MapeadorDeConsentimientos.desdeEntidad(fila)

        assertEquals("sub-a", fila.propietario)
        assertEquals(original, leido)
    }

    @Test
    fun `una fecha o un texto guardado mal formado no tumba la lectura`() {
        val c = MapeadorDeConsentimientos.desdeDto(dto("cns-1", otorgado = "ayer"))
        assertEquals(0L, c.otorgadoEn)

        val fila = MapeadorDeConsentimientos.aEntidad(consentimiento("cns-1"), "sub-a").copy(historial = "{no es json")
        assertTrue(MapeadorDeConsentimientos.desdeEntidad(fila).historial.isEmpty())
    }

    @Test
    fun `un activo se ve activo hasta justo antes de vencer y vencido desde vencer`() {
        assertEquals(EstadosDeConsentimiento.ACTIVO, estadoVisible(EstadosDeConsentimiento.ACTIVO, VENCE, VENCE - 1))
        assertEquals(EstadosDeConsentimiento.VENCIDO, estadoVisible(EstadosDeConsentimiento.ACTIVO, VENCE, VENCE))
        assertEquals(EstadosDeConsentimiento.REVOCADO, estadoVisible(EstadosDeConsentimiento.REVOCADO, VENCE, VENCE + 1))
    }

    @Test
    fun `una copia vieja de un activo ya vencido se muestra vencida con su fin y el vencimiento en el historial`() {
        val viejo = consentimiento("cns-1")

        val visto = viejo.visibleEn(VENCE + 1)

        assertEquals(EstadosDeConsentimiento.VENCIDO, visto.estado)
        assertFalse(visto.vigente)
        assertEquals(VENCE, visto.finalizadoEn)
        assertEquals(CambiosDeEstado.VENCIDO, visto.historial.last().estado)
        assertEquals(VENCE, visto.historial.last().ocurrioEn)
        assertSame(viejo, viejo.visibleEn(VENCE - 1))
        assertEquals(1, visto.visibleEn(VENCE + 2).historial.count { it.estado == CambiosDeEstado.VENCIDO })
    }

    @Test
    fun `agrupa activos por otorgamiento y el historial por fin`() {
        val grupos = agrupar(
            listOf(
                consentimiento("viejo", otorgadoEn = OTORGADO - 10_000),
                consentimiento("nuevo", otorgadoEn = OTORGADO),
                consentimiento("revocado", EstadosDeConsentimiento.REVOCADO, finalizadoEn = AHORA - 1_000, comprobante = "REV-1"),
                consentimiento("vencido-hace-poco", venceEn = AHORA - 500),
                consentimiento("vencido-antes", EstadosDeConsentimiento.VENCIDO, venceEn = AHORA - 50_000, finalizadoEn = AHORA - 50_000),
            ),
            AHORA,
        )

        assertEquals(listOf("nuevo", "viejo"), grupos.activos.map { it.id })
        assertEquals(listOf("vencido-hace-poco", "revocado", "vencido-antes"), grupos.historial.map { it.id })
        assertNull(grupos.activos.first().finalizadoEn)
    }
}
