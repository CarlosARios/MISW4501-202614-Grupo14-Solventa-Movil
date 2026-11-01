package co.solventa.movil.core.navegacion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * La barra inferior (HU-M1.3-01, D-14 b): Siniestros en espera y Perfil
 * seleccionada en Privacidad y consentimientos. Pólizas navega a M4 desde
 * HU-M1.2-01.
 */
class BarraInferiorTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int) = contexto.getString(id)

    private val elegidas = mutableListOf<PestanaInferior>()

    private fun montar(ruta: Ruta) {
        composicion.setContent {
            SolventaTheme {
                BarraInferior(seleccionada = pestanaDe(ruta)!!, onElegir = { elegidas += it })
            }
        }
    }

    @Test
    fun siniestros_esta_en_espera_y_deshabilitada() {
        montar(Ruta.Inicio)

        composicion.onNodeWithContentDescription(texto(R.string.navegacion_pestana_siniestros_en_espera)).assertIsDisplayed().assertIsNotEnabled()
    }

    /** HU-M1.2-01: la pestaña Pólizas lleva a M4 y queda seleccionada allí. */
    @Test
    fun polizas_navega_y_queda_seleccionada_en_mis_polizas() {
        montar(Ruta.Polizas)

        composicion.onNodeWithText(texto(R.string.navegacion_pestana_polizas).uppercase()).assertIsSelected()
        composicion.onNodeWithText(texto(R.string.navegacion_pestana_inicio).uppercase()).performClick()
        assertEquals(listOf(PestanaInferior.INICIO), elegidas)
    }

    @Test
    fun en_consentimientos_perfil_queda_seleccionada() {
        montar(Ruta.Consentimientos)

        composicion.onNodeWithText(texto(R.string.navegacion_pestana_perfil).uppercase()).assertIsSelected()
    }

    @Test
    fun inicio_y_perfil_navegan() {
        montar(Ruta.Perfil)

        composicion.onNodeWithText(texto(R.string.navegacion_pestana_inicio).uppercase()).assertIsEnabled().performClick()

        assertEquals(listOf(PestanaInferior.INICIO), elegidas)
    }
}
