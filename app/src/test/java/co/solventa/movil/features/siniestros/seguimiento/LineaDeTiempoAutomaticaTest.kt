package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** La línea de tiempo del siniestro paramétrico resuelto automáticamente (HU-M2.4-01, criterio 3). */
class LineaDeTiempoAutomaticaTest {

    private val aprobadoEn = AHORA - 2 * HORA
    private val pagadoEn = AHORA - HORA

    @Test
    fun `pagado como en M10b el evento, la validacion y el pago hechos con sus horas`() {
        val cambios = listOf(
            CambioDeEstado(EstadoDeSiniestro.EN_EVALUACION, aprobadoEn),
            CambioDeEstado(EstadoDeSiniestro.APROBADO, aprobadoEn),
            CambioDeEstado(EstadoDeSiniestro.PAGADO, pagadoEn),
        )

        val etapas = LineaDeTiempo.armar(parametrico(), cambios)

        assertEquals(
            listOf(
                EtapaDeLinea(Etapa.EVENTO_DETECTADO, Avance.HECHA, RESOLUCION.disparador.observadoEn),
                EtapaDeLinea(Etapa.VALIDACION_AUTOMATICA, Avance.HECHA, RESOLUCION.resueltaEn),
                EtapaDeLinea(Etapa.PAGO_EJECUTADO, Avance.HECHA, pagadoEn),
            ),
            etapas,
        )
        assertEquals(1f, LineaDeTiempo.progreso(parametrico()))
    }

    @Test
    fun `aprobado y sin pagar el pago es la etapa actual desde la aprobacion`() {
        val aprobado = parametrico(EstadoDeSiniestro.APROBADO)
        val etapas = LineaDeTiempo.armar(aprobado, listOf(CambioDeEstado(EstadoDeSiniestro.APROBADO, aprobadoEn)))

        assertEquals(EtapaDeLinea(Etapa.PAGO_EJECUTADO, Avance.ACTUAL, aprobadoEn), etapas.last())
        assertEquals(2.5f / 3, LineaDeTiempo.progreso(aprobado))
    }

    @Test
    fun `con el pago en novedad el pago sigue actual`() {
        val etapas = LineaDeTiempo.armar(parametrico(EstadoDeSiniestro.PAGO_CON_NOVEDAD), emptyList())

        assertEquals(Avance.ACTUAL, etapas.last().avance)
        assertNull(etapas.last().desde)
    }

    @Test
    fun `en un estado inesperado el pago queda pendiente`() {
        val etapas = LineaDeTiempo.armar(parametrico(EstadoDeSiniestro.EN_EVALUACION), emptyList())

        assertEquals(EtapaDeLinea(Etapa.PAGO_EJECUTADO, Avance.PENDIENTE, null), etapas.last())
    }

    @Test
    fun `un siniestro evaluado por un analista conserva la linea de cinco etapas`() {
        val etapas = LineaDeTiempo.armar(siniestro(), emptyList())

        assertEquals(LineaDeTiempo.MANUALES, etapas.map { it.etapa })
        assertEquals(LineaDeTiempo.progreso(EstadoDeSiniestro.EN_EVALUACION), LineaDeTiempo.progreso(siniestro()))
    }

    @Test
    fun `el parametro, la unidad y el tipo del siniestro parametrico tienen texto y lo desconocido no`() {
        assertEquals(R.string.siniestros_seguimiento_parametro_retraso_vuelo, EtiquetasDeSiniestro.parametro("flight_delay"))
        assertNull(EtiquetasDeSiniestro.parametro("rainfall"))
        assertEquals(R.string.siniestros_seguimiento_unidad_minutos, EtiquetasDeSiniestro.unidad("minutes"))
        assertNull(EtiquetasDeSiniestro.unidad("mm"))
        assertEquals(R.string.siniestros_seguimiento_incidente_retraso_vuelo, EtiquetasDeSiniestro.incidente("flight_delay"))
        assertEquals(
            R.string.siniestros_seguimiento_etapa_pagado_pendiente,
            EtiquetasDeSiniestro.etapaPendiente(Etapa.PAGO_EJECUTADO),
        )
    }

    @Test
    fun `los numeros del disparador se muestran sin decimales cuando no los tienen`() {
        assertEquals("215", PresentacionDeSiniestros.numero(215.0))
        assertEquals("2.5", PresentacionDeSiniestros.numero(2.5))
    }
}
