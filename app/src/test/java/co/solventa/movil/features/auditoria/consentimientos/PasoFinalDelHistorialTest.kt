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
    fun `un vencido con una revocacion posterior no repite el vencimiento`() {
        val vencido = consentimiento("cns-1", EstadosDeConsentimiento.VENCIDO, finalizadoEn = VENCE).copy(
            historial = listOf(
                cambio(CambiosDeEstado.OTORGADO, OTORGADO),
                cambio(CambiosDeEstado.VENCIDO, VENCE),
                cambio(CambiosDeEstado.REVOCADO, VENCE + 1_000L),
            ),
        )

        assertNull(pasoFinalDelHistorial(vencido, sinConexion = true))
        assertNull(pasoFinalDelHistorial(vencido, sinConexion = false))
    }

    @Test
    fun `un vencido cuyo historial no trae el vencimiento termina en Vencido con su fecha`() {
        val vencido = consentimiento("cns-1", EstadosDeConsentimiento.VENCIDO, finalizadoEn = VENCE).copy(
            historial = listOf(cambio(CambiosDeEstado.OTORGADO, OTORGADO), cambio(CambiosDeEstado.REVOCADO, VENCE + 1_000L)),
        )

        assertEquals(PasoFinalDelHistorial.Vencido(vencioEn = VENCE), pasoFinalDelHistorial(vencido, sinConexion = false))
    }

    @Test
    fun `un vencido visible en la copia ya trae el vencimiento y no agrega paso`() {
        val vencidoEnLaCopia = consentimiento("cns-1").visibleEn(VENCE)

        assertEquals(EstadosDeConsentimiento.VENCIDO, vencidoEnLaCopia.estado)
        assertNull(pasoFinalDelHistorial(vencidoEnLaCopia, sinConexion = true))
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
    fun `el estado reservado revocation_pending no agrega paso`() {
        assertNull(pasoFinalDelHistorial(consentimiento("cns-1", EstadosDeConsentimiento.REVOCACION_PENDIENTE), sinConexion = false))
    }

    // HU-M1.3-02: la revocacion en cola y la revocacion parcial (frames 1379:3583 y 1379:3687).

    @Test
    fun `un activo con su revocacion en cola termina en pendiente de envio`() {
        val pendiente = RevocacionPendiente("cns-1", "Banco Aliado", AHORA, 0)

        assertEquals(
            PasoFinalDelHistorial.RevocacionPendiente(AHORA),
            pasoFinalDelHistorial(consentimiento("cns-1"), sinConexion = true, pendiente = pendiente),
        )
    }

    @Test
    fun `un revocado parcial termina en su revocacion sin confirmar con las entidades que faltan`() {
        val revocacion = MapeadorDeConsentimientos.revocacionDesdeDto(
            revocacionDto("cns-1", estado = "partially_revoked", aviso = "partial", entidad = "not_confirmed"),
        )
        val parcial = MapeadorDeConsentimientos.conRevocacion(consentimiento("cns-1"), revocacion)

        assertEquals(
            PasoFinalDelHistorial.RevocadoSinConfirmar(revocacion.efectivaEn, "REV-2026-0088", listOf("Banco Aliado")),
            pasoFinalDelHistorial(parcial, sinConexion = false),
        )
    }

    @Test
    fun `un parcial sin la revocacion en la copia usa su fin, su comprobante y su entidad`() {
        val parcial = consentimiento("cns-1", EstadosDeConsentimiento.REVOCADO_PARCIAL, finalizadoEn = AHORA, comprobante = "REV-7")

        assertEquals(
            PasoFinalDelHistorial.RevocadoSinConfirmar(AHORA, "REV-7", listOf("Banco Aliado")),
            pasoFinalDelHistorial(parcial, sinConexion = false),
        )
    }

    @Test
    fun `un revocado completo no agrega paso aunque traiga su revocacion`() {
        val revocacion = MapeadorDeConsentimientos.revocacionDesdeDto(revocacionDto("cns-1", aviso = "completed", entidad = "confirmed"))

        assertNull(pasoFinalDelHistorial(MapeadorDeConsentimientos.conRevocacion(consentimiento("cns-1"), revocacion), sinConexion = false))
    }

    @Test
    fun `el estado de la pantalla expone el paso del consentimiento que muestra`() {
        val estado = DetalleConsentimientoUiState(cargando = false, consentimiento = consentimiento("cns-1"), sinConexion = true)

        assertEquals(PasoFinalDelHistorial.Activo(sincronizado = false), estado.pasoFinal)
        assertNull(DetalleConsentimientoUiState().pasoFinal)
    }
}
