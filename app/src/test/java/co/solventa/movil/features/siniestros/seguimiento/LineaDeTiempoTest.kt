package co.solventa.movil.features.siniestros.seguimiento

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La línea de tiempo de M10 (HU-M2.4-01). */
class LineaDeTiempoTest {

    private val radicadoEn = AHORA - 48 * HORA
    private val revisadoEn = AHORA - 24 * HORA

    private val historial = listOf(
        CambioDeEstado(EstadoDeSiniestro.RADICADO, radicadoEn),
        CambioDeEstado(EstadoDeSiniestro.EN_EVALUACION, revisadoEn),
    )

    @Test
    fun `en evaluacion como en M10 las dos primeras hechas, la tercera actual y las demas pendientes`() {
        val etapas = LineaDeTiempo.armar(EstadoDeSiniestro.EN_EVALUACION, historial)

        assertEquals(Etapa.entries, etapas.map { it.etapa })
        assertEquals(
            listOf(Avance.HECHA, Avance.HECHA, Avance.ACTUAL, Avance.PENDIENTE, Avance.PENDIENTE),
            etapas.map { it.avance },
        )
        assertEquals(listOf(radicadoEn, revisadoEn, revisadoEn, null, null), etapas.map { it.desde })
    }

    @Test
    fun `recien radicado espera la revision del asesor`() {
        val etapas = LineaDeTiempo.armar(EstadoDeSiniestro.RADICADO, historial.take(1))

        assertEquals(Avance.HECHA, etapas[0].avance)
        assertEquals(Avance.ACTUAL, etapas[1].avance)
        assertNull("todavia no hay revision", etapas[1].desde)
    }

    @Test
    fun `devuelto queda en la revision desde la devolucion`() {
        val devueltoEn = AHORA - 2 * HORA
        val etapas = LineaDeTiempo.armar(
            EstadoDeSiniestro.DEVUELTO,
            historial.take(1) + CambioDeEstado(EstadoDeSiniestro.DEVUELTO, devueltoEn),
        )

        assertEquals(Avance.ACTUAL, etapas[1].avance)
        assertEquals(devueltoEn, etapas[1].desde)
    }

    @Test
    fun `aprobado espera el pago y pagado cierra todas las etapas`() {
        val aprobadoEn = AHORA - HORA
        val aprobado = LineaDeTiempo.armar(
            EstadoDeSiniestro.APROBADO,
            historial + CambioDeEstado(EstadoDeSiniestro.APROBADO, aprobadoEn),
        )
        assertEquals(Avance.HECHA, aprobado[3].avance)
        assertEquals(aprobadoEn, aprobado[3].desde)
        assertEquals(Avance.ACTUAL, aprobado[4].avance)

        val pagado = LineaDeTiempo.armar(EstadoDeSiniestro.PAGADO, historial)
        assertTrue(pagado.all { it.avance == Avance.HECHA })
    }

    @Test
    fun `rechazado termina en la decision marcada como rechazo y sin pago`() {
        val etapas = LineaDeTiempo.armar(
            EstadoDeSiniestro.RECHAZADO,
            historial + CambioDeEstado(EstadoDeSiniestro.RECHAZADO, AHORA),
        )

        assertEquals(listOf(Etapa.RADICADO, Etapa.REVISION, Etapa.EVALUACION, Etapa.DECISION), etapas.map { it.etapa })
        assertTrue(etapas.all { it.avance == Avance.HECHA })
        assertTrue(etapas.last().rechazada)
        assertEquals(AHORA, etapas.last().desde)
    }

    @Test
    fun `un pago con novedad sigue en la etapa del pago`() {
        val etapas = LineaDeTiempo.armar(EstadoDeSiniestro.PAGO_CON_NOVEDAD, historial)
        assertEquals(Avance.ACTUAL, etapas.last().avance)
    }

    @Test
    fun `el progreso cuenta la etapa actual a medias y un caso cerrado completo`() {
        assertEquals(0.3f, LineaDeTiempo.progreso(EstadoDeSiniestro.RADICADO), 0.001f)
        assertEquals(0.5f, LineaDeTiempo.progreso(EstadoDeSiniestro.EN_EVALUACION), 0.001f)
        assertEquals(0.9f, LineaDeTiempo.progreso(EstadoDeSiniestro.APROBADO), 0.001f)
        assertEquals(1f, LineaDeTiempo.progreso(EstadoDeSiniestro.PAGADO), 0.001f)
        assertEquals(1f, LineaDeTiempo.progreso(EstadoDeSiniestro.RECHAZADO), 0.001f)
    }

    @Test
    fun `un estado desconocido se trata como en revision`() {
        assertEquals(Etapa.REVISION, LineaDeTiempo.etapaActual(EstadoDeSiniestro.de("nuevo_estado")))
    }
}
