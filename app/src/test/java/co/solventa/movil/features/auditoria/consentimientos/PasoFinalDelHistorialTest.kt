package co.solventa.movil.features.auditoria.consentimientos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * El ultimo paso de la linea de tiempo de M8b (HU-M1.3-01, frame 37:1418):
 * sale del estado visible y solo aparece cuando ningun hecho del historial
 * lo dice ya.
 */
class PasoFinalDelHistorialTest {

    private val revocadoEn = OTORGADO + 1_000L

    private fun cambio(estado: String, en: Long) = CambioDeEstado(estado, en, comprobante = null, canal = null, entidad = null)

    @Test
    fun `un activo termina en Activo, sincronizado en linea y sin sincronizar sin conexion`() {
        val activo = consentimiento("cns-1")

        assertEquals(PasoFinalDelHistorial.Activo(sincronizado = true), pasoFinalDelHistorial(activo, sinConexion = false))
        assertEquals(PasoFinalDelHistorial.Activo(sincronizado = false), pasoFinalDelHistorial(activo, sinConexion = true))
    }

    @Test
    fun `un vencido cuyo ultimo hecho es el vencimiento no agrega paso`() {
        val vencido = consentimiento("cns-1", EstadosDeConsentimiento.VENCIDO, finalizadoEn = VENCE).copy(
            historial = listOf(cambio(CambiosDeEstado.OTORGADO, OTORGADO), cambio(CambiosDeEstado.VENCIDO, VENCE)),
        )

        assertNull(pasoFinalDelHistorial(vencido, sinConexion = false))
    }

    @Test
    fun `un vencido con una revocacion posterior termina en Vencido con su fecha`() {
        val vencido = consentimiento("cns-1", EstadosDeConsentimiento.VENCIDO, finalizadoEn = VENCE).copy(
            historial = listOf(
                cambio(CambiosDeEstado.OTORGADO, OTORGADO),
                cambio(CambiosDeEstado.VENCIDO, VENCE),
                cambio(CambiosDeEstado.REVOCADO, VENCE + 1_000L),
            ),
        )

        assertEquals(PasoFinalDelHistorial.Vencido(vencioEn = VENCE), pasoFinalDelHistorial(vencido, sinConexion = true))
    }

    @Test
    fun `un vencido sin fecha de fin usa la de vencimiento`() {
        val vencido = consentimiento("cns-1", EstadosDeConsentimiento.VENCIDO, finalizadoEn = null)

        assertEquals(PasoFinalDelHistorial.Vencido(vencioEn = VENCE), pasoFinalDelHistorial(vencido, sinConexion = false))
    }

    @Test
    fun `un revocado termina en su revocacion y no agrega paso`() {
        val revocado = consentimiento("cns-1", EstadosDeConsentimiento.REVOCADO, finalizadoEn = revocadoEn, comprobante = "REV-1").copy(
            historial = listOf(cambio(CambiosDeEstado.OTORGADO, OTORGADO), cambio(CambiosDeEstado.REVOCADO, revocadoEn)),
        )

        assertNull(pasoFinalDelHistorial(revocado, sinConexion = false))
    }

    @Test
    fun `los estados de la revocacion en curso no agregan paso`() {
        listOf(EstadosDeConsentimiento.REVOCACION_PENDIENTE, EstadosDeConsentimiento.REVOCADO_PARCIAL).forEach { estado ->
            assertNull(estado, pasoFinalDelHistorial(consentimiento("cns-1", estado), sinConexion = false))
        }
    }

    @Test
    fun `el estado de la pantalla expone el paso del consentimiento que muestra`() {
        val estado = DetalleConsentimientoUiState(cargando = false, consentimiento = consentimiento("cns-1"), sinConexion = true)

        assertEquals(PasoFinalDelHistorial.Activo(sincronizado = false), estado.pasoFinal)
        assertNull(DetalleConsentimientoUiState().pasoFinal)
    }
}
