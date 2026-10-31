package co.solventa.movil.features.siniestros.radicacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * El monto estimado de la radicación (HU-M2.2-01), con las reglas de la web
 * (HU-W2.2-01): pesos enteros, opcional, de 1 a 100.000.000.000, con o sin
 * separadores de miles.
 */
class MontoEstimadoTest {

    @Test
    fun `vacio no es un error porque el campo es opcional`() {
        assertEquals(MontoEstimado.Lectura.Vacio, MontoEstimado.leer(""))
        assertEquals(MontoEstimado.Lectura.Vacio, MontoEstimado.leer("   "))
        assertNull(MontoEstimado.centavos(""))
    }

    @Test
    fun `acepta pesos enteros con o sin separadores de miles`() {
        for (texto in listOf("4800000", "4.800.000", "4,800,000", "4 800 000", " 4.800.000 ")) {
            assertEquals(texto, MontoEstimado.Lectura.Valido(4_800_000), MontoEstimado.leer(texto))
        }
        assertEquals(480_000_000L, MontoEstimado.centavos("4.800.000"))
        assertEquals(MontoEstimado.Lectura.Valido(1), MontoEstimado.leer("1"))
        assertEquals(MontoEstimado.Lectura.Valido(100_000_000_000), MontoEstimado.leer("100.000.000.000"))
    }

    @Test
    fun `un decimal letras o separadores mal puestos son un formato invalido`() {
        for (texto in listOf("4800000,50", "4.8", "abc", "-100", "48.00.000", "$ 4.800.000")) {
            assertEquals(
                texto,
                MontoEstimado.Lectura.Invalido(MontoEstimado.Problema.FORMATO),
                MontoEstimado.leer(texto),
            )
            assertNull(texto, MontoEstimado.centavos(texto))
        }
    }

    @Test
    fun `fuera de 1 a 100 mil millones es un monto fuera de rango`() {
        for (texto in listOf("0", "000", "100.000.000.001", "999999999999999999999")) {
            assertEquals(
                texto,
                MontoEstimado.Lectura.Invalido(MontoEstimado.Problema.RANGO),
                MontoEstimado.leer(texto),
            )
        }
    }

    @Test
    fun `se muestra con los separadores del idioma de la aplicacion`() {
        assertEquals("$ 4.800.000 COP", MontoEstimado.aTexto(4_800_000, Locale.forLanguageTag("es-CO")))
        assertEquals("$ 4,800,000 COP", MontoEstimado.aTexto(4_800_000, Locale.forLanguageTag("en-US")))
    }

    @Test
    fun `en los hechos solo falta si se escribio algo que no es un monto`() {
        val hechos = RadicacionUiState(
            etapa = Etapa.HECHOS,
            radicacion = Radicacion(
                poliza = "AU-2025-118204",
                tipo = TipoDeIncidente.OTRO,
                ocurrioEn = 1_792_539_000_000L,
                lugar = "Autopista Norte km 12, Bogotá",
                descripcion = "Choque por alcance en el semáforo.",
            ),
        )

        assertFalse(CampoDeRadicacion.MONTO in hechos.faltantesDe(Etapa.HECHOS))
        val valido = hechos.copy(radicacion = hechos.radicacion.copy(montoEstimado = "4.800.000"))
        assertFalse(CampoDeRadicacion.MONTO in valido.faltantesDe(Etapa.HECHOS))
        val invalido = hechos.copy(radicacion = hechos.radicacion.copy(montoEstimado = "4,5"))
        assertTrue(CampoDeRadicacion.MONTO in invalido.faltantesDe(Etapa.HECHOS))
        assertEquals(Etapa.HECHOS, CampoDeRadicacion.MONTO.etapa)
    }
}
