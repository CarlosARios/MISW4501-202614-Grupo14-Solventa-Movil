package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.features.originacion.polizas.CopiaDePolizas
import co.solventa.movil.features.originacion.polizas.EstadosDePoliza
import co.solventa.movil.features.originacion.polizas.LineasDePoliza
import co.solventa.movil.features.originacion.polizas.RepositorioFalso
import co.solventa.movil.features.originacion.polizas.poliza
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PolizasDeLaBilleteraTest {

    @Test
    fun `ofrece solo las polizas activas del nucleo, que son las que claims puede cubrir`() = runTest {
        val billetera = RepositorioFalso(
            CopiaDePolizas(
                listOf(
                    poliza(id = "AU-2026-000001"),
                    poliza(id = "AU-2025-000002", estado = EstadosDePoliza.VENCIDA),
                    poliza(id = "pol-emitida", linea = LineasDePoliza.VIDA).copy(origen = "origination"),
                ),
                sincronizadoEn = 1L,
                sinConexion = false,
            ),
        )

        val polizas = PolizasDeLaBilletera(billetera).activas()

        assertEquals(listOf(PolizaElegible("AU-2026-000001", "Autos · AU-2026-000001 · KJT-482")), polizas)
        assertEquals("con la copia llena no se sincroniza", 0, billetera.sincronizaciones)
    }

    @Test
    fun `con la billetera vacia la sincroniza una vez antes de ofrecer polizas`() = runTest {
        val billetera = RepositorioFalso()

        val polizas = PolizasDeLaBilletera(billetera).activas()

        assertEquals(emptyList<PolizaElegible>(), polizas)
        assertEquals(1, billetera.sincronizaciones)
    }
}
