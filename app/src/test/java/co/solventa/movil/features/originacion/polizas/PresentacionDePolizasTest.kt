package co.solventa.movil.features.originacion.polizas

import co.solventa.movil.features.originacion.polizas.PresentacionDePolizas.Subtitulo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El orden y la presentación de la billetera (HU-M1.2-01). */
class PresentacionDePolizasTest {

    @Test
    fun `la situacion sale del estado y de cuanto falta para el fin`() {
        assertEquals(Situacion.ACTIVA, PresentacionDePolizas.situacion(poliza(fin = AHORA + 31 * DIA), AHORA))
        assertEquals(Situacion.POR_VENCER, PresentacionDePolizas.situacion(poliza(fin = AHORA + 16 * DIA), AHORA))
        assertEquals(Situacion.POR_VENCER, PresentacionDePolizas.situacion(poliza(fin = AHORA + 30 * DIA), AHORA))
        assertEquals(Situacion.VENCIDA, PresentacionDePolizas.situacion(poliza(fin = AHORA - 1), AHORA))
        assertEquals(Situacion.VENCIDA, PresentacionDePolizas.situacion(poliza(estado = EstadosDePoliza.VENCIDA), AHORA))
        assertEquals(Situacion.CANCELADA, PresentacionDePolizas.situacion(poliza(estado = EstadosDePoliza.CANCELADA), AHORA))
    }

    @Test
    fun `las vigentes van primero y se respeta el orden del servicio`() {
        val cancelada = poliza(id = "AU-2024-093377", estado = EstadosDePoliza.CANCELADA)
        val vida = poliza(id = "VH-2026-004512", linea = LineasDePoliza.VIDA)
        val autos = poliza(fin = AHORA + 16 * DIA)

        val tarjetas = PresentacionDePolizas.tarjetas(listOf(cancelada, vida, autos), emptyList(), AHORA)

        assertEquals(listOf("VH-2026-004512", "AU-2025-118204", "AU-2024-093377"), tarjetas.map { it.poliza.id })
        assertEquals(listOf(Situacion.ACTIVA, Situacion.POR_VENCER, Situacion.CANCELADA), tarjetas.map { it.situacion })
    }

    @Test
    fun `una renovacion con numero nuevo reemplaza a la anterior y va marcada`() {
        val anterior = poliza(id = "pol-1", fin = AHORA + 10 * DIA)
        val nueva = poliza(id = "pol-2", renovacionDe = "pol-1", inicio = AHORA + 10 * DIA, fin = AHORA + 375 * DIA)

        val tarjetas = PresentacionDePolizas.tarjetas(listOf(anterior, nueva), emptyList(), AHORA)

        assertEquals(listOf(PolizaEnLista(nueva, Situacion.ACTIVA, renovada = true)), tarjetas)
    }

    @Test
    fun `la renovada en la ultima sincronizacion va marcada aunque conserve el numero`() {
        val renovada = poliza()

        val tarjetas = PresentacionDePolizas.tarjetas(listOf(renovada), listOf(CambioDePoliza(renovada, TipoDeCambio.RENOVADA)), AHORA)

        assertTrue(tarjetas.single().renovada)
    }

    @Test
    fun `el subtitulo dice lo que la situacion dice de la vigencia`() {
        fun subtitulo(situacion: Situacion, renovada: Boolean = false) =
            PresentacionDePolizas.subtitulo(PolizaEnLista(poliza(), situacion, renovada))

        assertEquals(Subtitulo.VIGENCIA, subtitulo(Situacion.ACTIVA))
        assertEquals(Subtitulo.VENCE, subtitulo(Situacion.POR_VENCER))
        assertEquals(Subtitulo.RENOVADA, subtitulo(Situacion.ACTIVA, renovada = true))
        assertEquals(Subtitulo.RENOVADA, subtitulo(Situacion.POR_VENCER, renovada = true))
        assertEquals(Subtitulo.VENCIO, subtitulo(Situacion.VENCIDA, renovada = true))
        assertEquals(Subtitulo.CANCELADA, subtitulo(Situacion.CANCELADA, renovada = true))
    }

    @Test
    fun `una cancelada no cubre ni se anuncia como renovada`() {
        val cancelada = PolizaEnLista(poliza(), Situacion.CANCELADA, renovada = true)
        val renovada = PolizaEnLista(poliza(), Situacion.ACTIVA, renovada = true)

        assertTrue(PresentacionDePolizas.sinCobertura(cancelada))
        assertTrue(PresentacionDePolizas.sinCobertura(PolizaEnLista(poliza(), Situacion.VENCIDA)))
        assertFalse(PresentacionDePolizas.sinCobertura(renovada))
        assertFalse(PresentacionDePolizas.insigniaRenovada(cancelada))
        assertTrue(PresentacionDePolizas.insigniaRenovada(renovada))
    }

    @Test
    fun `la copia se advierte desactualizada solo despues de los dias dados`() {
        assertFalse(PresentacionDePolizas.desactualizada(null, AHORA, 30))
        assertFalse(PresentacionDePolizas.desactualizada(AHORA - 30 * DIA, AHORA, 30))
        assertTrue(PresentacionDePolizas.desactualizada(AHORA - 30 * DIA - 1, AHORA, 30))
    }
}
