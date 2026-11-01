package co.solventa.movil.features.originacion.polizas

import co.solventa.movil.R
import co.solventa.movil.core.i18n.Idioma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Los textos e íconos de los códigos del servicio (HU-M1.2-01). */
class EtiquetasDePolizaTest {

    @Test
    fun `cada tipo de seguro del contrato tiene texto e icono`() {
        assertEquals(R.string.originacion_polizas_linea_vida, EtiquetasDePoliza.linea("life"))
        assertEquals(R.string.originacion_polizas_linea_autos, EtiquetasDePoliza.linea("auto"))
        assertEquals(R.string.originacion_polizas_linea_hogar, EtiquetasDePoliza.linea("home"))
        assertEquals(R.string.originacion_polizas_linea_viaje, EtiquetasDePoliza.linea("travel"))
        assertNull(EtiquetasDePoliza.linea("pets"))

        assertEquals(R.drawable.ic_escudo, EtiquetasDePoliza.icono("life"))
        assertEquals(R.drawable.ic_carro, EtiquetasDePoliza.icono("auto"))
        assertEquals(R.drawable.ic_inicio, EtiquetasDePoliza.icono("home"))
        assertEquals(R.drawable.ic_avion, EtiquetasDePoliza.icono("travel"))
        assertEquals(R.drawable.ic_escudo, EtiquetasDePoliza.icono("pets"))
    }

    @Test
    fun `cada cobertura del contrato tiene texto`() {
        listOf(
            "death", "total_permanent_disability", "collision", "theft", "third_party_damage", "glass", "other",
            "water_damage", "flight_delay",
        ).forEach { assertNotNull(it, EtiquetasDePoliza.cobertura(it)) }
        assertNull(EtiquetasDePoliza.cobertura("earthquake"))
    }

    @Test
    fun `cada situacion y cada cambio tienen texto`() {
        Situacion.entries.forEach { assertNotNull(EtiquetasDePoliza.situacion(it)) }
        assertEquals(R.string.originacion_polizas_cambio_renovada, EtiquetasDePoliza.cambio(TipoDeCambio.RENOVADA))
        assertEquals(R.string.originacion_polizas_cambio_cancelada, EtiquetasDePoliza.cambio(TipoDeCambio.CANCELADA))
        assertEquals(R.string.originacion_polizas_cambio_modificada, EtiquetasDePoliza.cambio(TipoDeCambio.MODIFICADA))
    }

    @Test
    fun `el monto sigue el formato del idioma como el prototipo`() {
        assertEquals("$ 72.500.000", EtiquetasDePoliza.monto(72_500_000.0, "COP", Idioma.ESPANOL.aLocale()))
        assertEquals("$ 72,500,000", EtiquetasDePoliza.monto(72_500_000.0, "COP", Idioma.INGLES.aLocale()))
        assertEquals("$ 180.000", EtiquetasDePoliza.monto(180_000.4, "COP", Idioma.ESPANOL.aLocale()))
    }

    @Test
    fun `otra moneda lleva su codigo`() {
        assertEquals("$ 1.500 MXN", EtiquetasDePoliza.monto(1_500.0, "MXN", Idioma.ESPANOL.aLocale()))
    }
}
