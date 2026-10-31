package co.solventa.movil.core.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.features.acceso.ingreso.HojaDeSesionExpirada
import co.solventa.movil.features.siniestros.radicacion.HojaDeBorrador
import co.solventa.movil.features.siniestros.radicacion.RadicacionUiState
import co.solventa.movil.features.siniestros.radicacion.SelectorDeFechaYHora
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Rule
import org.junit.Test

/**
 * Las hojas inferiores y los diálogos abren su propia ventana, que vuelve al
 * idioma del teléfono si nadie le lleva el de la aplicación. Cada prueba
 * dibuja la ventana en el idioma **contrario** al del teléfono: si sale en el
 * del teléfono, la ventana perdió el idioma que el usuario eligió.
 */
class IdiomaEnVentanasTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val elegido: Idioma =
        if (contexto.resources.configuration.locales[0].language == "es") Idioma.INGLES else Idioma.ESPANOL

    /** El texto de [id] en el idioma elegido, no en el del teléfono. */
    private fun enElElegido(id: Int): String {
        val configuracion = Configuration(contexto.resources.configuration).apply { setLocale(elegido.aLocale()) }
        return contexto.createConfigurationContext(configuracion).getString(id)
    }

    private fun dibujar(contenido: @Composable () -> Unit) {
        composicion.setContent { SolventaTheme { ConIdioma(elegido) { contenido() } } }
    }

    // HU-M1.1-02 · M2d
    @Test
    fun la_hoja_de_sesion_expirada_sale_en_el_idioma_de_la_aplicacion() {
        dibujar { HojaDeSesionExpirada(onConHuella = {}, onConContrasena = {}) }

        composicion.onNodeWithText(enElElegido(R.string.sesion_expirada_titulo)).assertIsDisplayed()
    }

    // HU-M2.2-01 · M13b
    @Test
    fun la_hoja_del_borrador_sale_en_el_idioma_de_la_aplicacion() {
        dibujar { HojaDeBorrador(estado = RadicacionUiState(), onContinuar = {}, onDescartar = {}) }

        composicion.onNodeWithText(enElElegido(R.string.radicacion_borrador_titulo)).assertIsDisplayed()
    }

    // HU-M2.2-01 · fecha del hecho, primer paso: el día
    @Test
    fun el_selector_del_dia_sale_en_el_idioma_de_la_aplicacion() {
        dibujar { SelectorDeFechaYHora(inicial = null, onElegida = {}, onCerrar = {}) }

        composicion.onNodeWithText(enElElegido(R.string.comun_cancelar)).assertIsDisplayed()
    }
}
