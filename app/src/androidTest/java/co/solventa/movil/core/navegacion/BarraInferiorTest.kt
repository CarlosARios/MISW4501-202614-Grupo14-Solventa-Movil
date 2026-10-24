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
 * La barra inferior (HU-M1.3-01, D-14 b): Pólizas y Siniestros en espera y
 * Perfil seleccionada en Privacidad y consentimientos.
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
    fun polizas_y_siniestros_estan_en_espera_y_deshabilitadas() {
        montar(Ruta.Inicio)

        composicion.onNodeWithContentDescription(texto(R.string.navegacion_pestana_polizas_en_espera)).assertIsDisplayed().assertIsNotEnabled()
        composicion.onNodeWithContentDescription(texto(R.string.navegacion_pestana_siniestros_en_espera)).assertIsDisplayed().assertIsNotEnabled()
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
