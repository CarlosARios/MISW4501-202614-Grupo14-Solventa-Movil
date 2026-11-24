package co.solventa.movil.features.notificaciones.centro

import co.solventa.movil.R
import co.solventa.movil.core.navegacion.Ruta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * La notificación de cambio de etapa (HU-M3.3-01) en la lógica pura: lo que se
 * lee del servicio, lo que se guarda, los textos de cada etapa y a dónde lleva.
 */
class CambioDeEtapaTest {

    private fun instante(texto: String) = Instant.parse(texto).toEpochMilli()

    @Test
    fun `del servicio sale la etapa con el siniestro, la hora y el monto`() {
        val n = MapeadorDeNotificaciones.desdeDto(etapaDto(etapa = "paid", pagado = "2026-09-20T14:40:00Z"))

        assertEquals(TiposDeNotificacion.CAMBIO_DE_ETAPA, n.tipo)
        assertNull(n.pago)
        assertEquals(
            CambioDeEtapa(
                cambio = 42, siniestroId = "8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90", radicado = "SIN-2026-01655",
                poliza = "HG-2025-880231", etapa = EtapasDeSiniestro.PAGADO, cambioEn = instante("2026-09-20T14:40:05Z"),
                monto = 4_132_500.0, moneda = "COP", pagadoEn = instante("2026-09-20T14:40:00Z"),
            ),
            n.etapa,
        )
    }

    @Test
    fun `la etapa sobrevive a la copia local y una ilegible se pierde sin romper la notificacion`() {
        val n = MapeadorDeNotificaciones.desdeDto(etapaDto())

        val fila = MapeadorDeNotificaciones.aEntidad(n, "sub-a")
        assertEquals(n, MapeadorDeNotificaciones.desdeEntidad(fila))
        assertNull(fila.pago)

        assertNull(MapeadorDeNotificaciones.desdeEntidad(fila.copy(etapa = "{roto")).etapa)
    }

    @Test
    fun `tocarla lleva a la linea de tiempo del siniestro y la de pago a su detalle (criterio 5)`() {
        val etapa = MapeadorDeNotificaciones.desdeDto(etapaDto())
        val pago = notificacion("ntf-ppo-1")

        assertEquals(DestinoDeNotificacion.Siniestro("8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90", "ntf-stage-42"), etapa.destino)
        assertEquals(DestinoDeNotificacion.Pago("ntf-ppo-1"), pago.destino)
        assertEquals(Ruta.DetalleSiniestro("8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90"), rutaDe(etapa.destino))
        assertEquals(Ruta.DetallePagoParametrico("ntf-ppo-1"), rutaDe(pago.destino))
    }

    @Test
    fun `cada etapa tiene su titulo, con el monto cuando lo trae (criterios 1, 2 y 3)`() {
        assertEquals(R.string.notificaciones_etapa_titulo_evaluacion, TextosDeNotificacion.tituloDeEtapa(EtapasDeSiniestro.EN_EVALUACION, false))
        assertEquals(R.string.notificaciones_etapa_titulo_aprobado_monto, TextosDeNotificacion.tituloDeEtapa(EtapasDeSiniestro.APROBADO, true))
        assertEquals(R.string.notificaciones_etapa_titulo_aprobado, TextosDeNotificacion.tituloDeEtapa(EtapasDeSiniestro.APROBADO, false))
        assertEquals(R.string.notificaciones_etapa_titulo_pagado_monto, TextosDeNotificacion.tituloDeEtapa(EtapasDeSiniestro.PAGADO, true))
        assertEquals(R.string.notificaciones_etapa_titulo_pagado, TextosDeNotificacion.tituloDeEtapa(EtapasDeSiniestro.PAGADO, false))
        assertEquals(R.string.notificaciones_etapa_titulo_generico, TextosDeNotificacion.tituloDeEtapa("rejected", false))
    }

    @Test
    fun `cada etapa tiene su cuerpo e icono, y una desconocida el generico`() {
        assertEquals(R.string.notificaciones_etapa_cuerpo_evaluacion, TextosDeNotificacion.cuerpoDeEtapa(EtapasDeSiniestro.EN_EVALUACION))
        assertEquals(R.string.notificaciones_etapa_cuerpo_aprobado, TextosDeNotificacion.cuerpoDeEtapa(EtapasDeSiniestro.APROBADO))
        assertEquals(R.string.notificaciones_etapa_cuerpo_pagado, TextosDeNotificacion.cuerpoDeEtapa(EtapasDeSiniestro.PAGADO))
        assertEquals(R.string.notificaciones_etapa_cuerpo_generico, TextosDeNotificacion.cuerpoDeEtapa("rejected"))
        assertEquals(R.drawable.ic_reloj, TextosDeNotificacion.iconoDeEtapa(EtapasDeSiniestro.EN_EVALUACION))
        assertEquals(R.drawable.ic_circulo_check, TextosDeNotificacion.iconoDeEtapa(EtapasDeSiniestro.APROBADO))
        assertEquals(R.drawable.ic_escudo_check, TextosDeNotificacion.iconoDeEtapa(EtapasDeSiniestro.PAGADO))
    }

    @Test
    fun `el pago cuenta la fecha del desembolso y las demas la del cambio`() {
        val pagado = MapeadorDeNotificaciones.desdeDto(etapaDto(etapa = "paid", pagado = "2026-09-20T14:40:00Z")).etapa!!
        val sinFecha = pagado.copy(pagadoEn = null)
        val aprobado = MapeadorDeNotificaciones.desdeDto(etapaDto()).etapa!!

        assertEquals(instante("2026-09-20T14:40:00Z"), TextosDeNotificacion.instanteDeEtapa(pagado))
        assertEquals(pagado.cambioEn, TextosDeNotificacion.instanteDeEtapa(sinFecha))
        assertEquals(aprobado.cambioEn, TextosDeNotificacion.instanteDeEtapa(aprobado))
    }
}
