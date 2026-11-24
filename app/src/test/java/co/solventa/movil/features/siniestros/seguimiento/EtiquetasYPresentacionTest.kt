package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.shared.ui.TipoInsignia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/** Los textos, tonos y horas de la consulta de siniestros (HU-M2.4-01, HA-L02). */
class EtiquetasYPresentacionTest {

    @Test
    fun `cada estado conocido tiene su texto y uno nuevo se muestra tal cual`() {
        EstadoDeSiniestro.entries.filter { it != EstadoDeSiniestro.DESCONOCIDO }.forEach {
            assertNotNull(it.name, EtiquetasDeSiniestro.estado(it))
            assertEquals(it, EstadoDeSiniestro.de(it.codigo))
        }
        assertNull(EtiquetasDeSiniestro.estado(EstadoDeSiniestro.DESCONOCIDO))
        assertEquals(EstadoDeSiniestro.DESCONOCIDO, EstadoDeSiniestro.de(""))
    }

    @Test
    fun `solo pagado y rechazado son casos cerrados`() {
        assertEquals(
            setOf(EstadoDeSiniestro.PAGADO, EstadoDeSiniestro.RECHAZADO),
            EstadoDeSiniestro.entries.filter { it.cerrado }.toSet(),
        )
    }

    @Test
    fun `el tono avisa lo que pide algo al asegurado`() {
        assertEquals(TipoInsignia.ADVERTENCIA, EtiquetasDeSiniestro.tono(EstadoDeSiniestro.DEVUELTO))
        assertEquals(TipoInsignia.ADVERTENCIA, EtiquetasDeSiniestro.tono(EstadoDeSiniestro.PAGO_CON_NOVEDAD))
        assertEquals(TipoInsignia.INFORMACION, EtiquetasDeSiniestro.tono(EstadoDeSiniestro.EN_EVALUACION))
        assertEquals(TipoInsignia.EXITO, EtiquetasDeSiniestro.tono(EstadoDeSiniestro.PAGADO))
        assertEquals(TipoInsignia.EXITO, EtiquetasDeSiniestro.tono(EstadoDeSiniestro.APROBADO))
        assertEquals(TipoInsignia.NEUTRO, EtiquetasDeSiniestro.tono(EstadoDeSiniestro.RADICADO))
        assertEquals(TipoInsignia.NEUTRO, EtiquetasDeSiniestro.tono(EstadoDeSiniestro.RECHAZADO))
    }

    @Test
    fun `lineas, incidentes e iconos conocidos y desconocidos`() {
        listOf("auto", "home", "travel", "life").forEach { assertNotNull(it, EtiquetasDeSiniestro.linea(it)) }
        assertNull(EtiquetasDeSiniestro.linea(null))
        assertNull(EtiquetasDeSiniestro.linea("pets"))
        listOf("collision", "theft", "third_party_damage", "glass", "water_damage", "other").forEach {
            assertNotNull(it, EtiquetasDeSiniestro.incidente(it))
        }
        assertNull(EtiquetasDeSiniestro.incidente("flood"))
        assertEquals(EtiquetasDeSiniestro.icono(null), EtiquetasDeSiniestro.icono("life"))
        assertTrue(EtiquetasDeSiniestro.icono("auto") != EtiquetasDeSiniestro.icono("home"))
    }

    @Test
    fun `cada etapa y cada estado tienen texto, y la decision cambia con el rechazo`() {
        Etapa.entries.forEach { etapa ->
            EtiquetasDeSiniestro.etapa(EtapaDeLinea(etapa, Avance.PENDIENTE, null))
        }
        val aprobado = EtiquetasDeSiniestro.etapa(EtapaDeLinea(Etapa.DECISION, Avance.HECHA, AHORA))
        val rechazado = EtiquetasDeSiniestro.etapa(EtapaDeLinea(Etapa.DECISION, Avance.HECHA, AHORA, rechazada = true))
        assertTrue(aprobado != rechazado)
        assertNotNull(EtiquetasDeSiniestro.etapaPendiente(Etapa.DECISION))
        assertNotNull(EtiquetasDeSiniestro.etapaPendiente(Etapa.PAGO))
        assertNull(EtiquetasDeSiniestro.etapaPendiente(Etapa.RADICADO))
        assertEquals(
            EstadoDeSiniestro.entries.size,
            EstadoDeSiniestro.entries.map(EtiquetasDeSiniestro::queSigue).toSet().size,
        )
    }

    @Test
    fun `hoy se decide en la zona del telefono`() {
        val bogota = ZoneId.of("America/Bogota")
        // 2026-09-16 14:30 UTC = 09:30 en Bogotá.
        assertTrue(PresentacionDeSiniestros.esDeHoy(AHORA - 9 * HORA, AHORA, bogota))
        assertFalse("ayer en Bogotá", PresentacionDeSiniestros.esDeHoy(AHORA - 10 * HORA, AHORA, bogota))
    }

    @Test
    fun `la frescura de varias respuestas viene de la cache si alguna vino y vale la hora mas antigua`() {
        val combinada = Frescura.combinar(Frescura(false, AHORA), Frescura(true, AHORA - HORA))
        assertEquals(Frescura(true, AHORA - HORA), combinada)
        assertEquals(Frescura(false, AHORA - 1), Frescura.combinar(Frescura(false, AHORA), Frescura(false, AHORA - 1)))
    }
}
