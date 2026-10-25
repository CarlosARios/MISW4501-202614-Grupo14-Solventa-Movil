package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Cada codigo conocido del servicio tiene su texto y un codigo desconocido
 * queda sin texto para mostrarse tal cual (HU-M1.3-01).
 */
class EtiquetasDeConsentimientoTest {

    @Test
    fun `los estados conocidos tienen texto`() {
        listOf("active", "expired", "revoked", "revocation_pending", "partially_revoked").forEach {
            assertNotNull(it, EtiquetasDeConsentimiento.estado(it))
        }
        assertNull(EtiquetasDeConsentimiento.estado("suspended"))
    }

    @Test
    fun `los cambios del historial tienen texto y los estados sirven de respaldo`() {
        assertEquals(R.string.auditoria_consentimientos_historial_otorgado, EtiquetasDeConsentimiento.cambio("granted"))
        assertEquals(R.string.auditoria_consentimientos_historial_vencido, EtiquetasDeConsentimiento.cambio("expired"))
        assertNotNull(EtiquetasDeConsentimiento.cambio("revocation_requested"))
        assertNotNull(EtiquetasDeConsentimiento.cambio("revoked"))
        assertEquals(R.string.auditoria_consentimientos_estado_activo, EtiquetasDeConsentimiento.cambio("active"))
        assertNull(EtiquetasDeConsentimiento.cambio("frozen"))
    }

    @Test
    fun `fuentes categorias vigencias finalidad y canal`() {
        listOf("open_finance", "open_data", "kyc").forEach { assertNotNull(it, EtiquetasDeConsentimiento.fuente(it)) }
        listOf("credit_products", "balances", "payment_behavior", "public_records", "identity_verification").forEach {
            assertNotNull(it, EtiquetasDeConsentimiento.categoria(it))
        }
        listOf("12_months", "single_use").forEach { assertNotNull(it, EtiquetasDeConsentimiento.vigencia(it)) }
        assertNotNull(EtiquetasDeConsentimiento.finalidad("risk_profiling_mortgage_life_offer"))
        assertNotNull(EtiquetasDeConsentimiento.canal("embedded"))
        assertNotNull(EtiquetasDeConsentimiento.canal("mobile"))

        assertNull(EtiquetasDeConsentimiento.fuente("satellite"))
        assertNull(EtiquetasDeConsentimiento.categoria("salary"))
        assertNull(EtiquetasDeConsentimiento.vigencia("forever"))
        assertNull(EtiquetasDeConsentimiento.finalidad("marketing"))
        assertNull(EtiquetasDeConsentimiento.canal("assisted"))
    }
}
