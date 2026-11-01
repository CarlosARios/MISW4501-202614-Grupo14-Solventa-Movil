package co.solventa.movil.features.auditoria.consentimientos

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La caja del icono de la entidad en la cabecera de M8b (HU-M1.3-02): azul en
 * un activo y en una revocacion parcial, gris en los demas estados.
 */
class EntidadResaltadaTest {

    @Test
    fun `un activo y una revocacion parcial resaltan la entidad`() {
        assertTrue(entidadResaltada(EstadosDeConsentimiento.ACTIVO))
        assertTrue(entidadResaltada(EstadosDeConsentimiento.REVOCADO_PARCIAL))
    }

    @Test
    fun `revocados, vencidos y la revocacion en curso la atenuan`() {
        listOf(
            EstadosDeConsentimiento.REVOCADO,
            EstadosDeConsentimiento.VENCIDO,
            EstadosDeConsentimiento.REVOCACION_PENDIENTE,
        ).forEach { estado -> assertFalse(estado, entidadResaltada(estado)) }
    }
}
