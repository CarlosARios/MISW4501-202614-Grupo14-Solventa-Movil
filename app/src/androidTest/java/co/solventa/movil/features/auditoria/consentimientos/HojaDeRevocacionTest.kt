package co.solventa.movil.features.auditoria.consentimientos

import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.i18n.ConIdioma
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * La hoja de confirmacion de la revocacion (HU-M1.3-02, frame 37:1519):
 * explica el efecto (criterio 1), confirma con la huella o con la
 * contraseña y cancelar no confirma nada (criterio 2).
 */
class HojaDeRevocacionTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private var biometrias = 0
    private val contrasenas = mutableListOf<String>()
    private var cancelaciones = 0

    private fun montar(hoja: PasoDeRevocacion.Hoja) {
        composicion.setContent {
            SolventaTheme {
                ContenidoHojaDeRevocacion(
                    entidad = "Banco Aliado",
                    hoja = hoja,
                    onConfirmarConBiometria = { biometrias++ },
                    onConfirmarConContrasena = { contrasenas += it },
                    onCancelar = { cancelaciones++ },
                )
            }
        }
    }

    /** El texto en el idioma dado, sin importar el del telefono. */
    private fun textoEn(idioma: Idioma, id: Int, vararg args: Any) =
        contexto.createConfigurationContext(Configuration(contexto.resources.configuration).apply { setLocale(idioma.aLocale()) })
            .getString(id, *args)

    /** La hoja completa, en su propia ventana, dentro de la aplicacion con el idioma elegido. */
    private fun montarHoja(idioma: Idioma) {
        composicion.setContent {
            SolventaTheme {
                ConIdioma(idioma) {
                    HojaDeRevocacion(
                        entidad = "Banco Aliado",
                        hoja = PasoDeRevocacion.Hoja(MetodoDeConfirmacion.BIOMETRIA),
                        idioma = idioma,
                        onConfirmarConBiometria = { biometrias++ },
                        onConfirmarConContrasena = { contrasenas += it },
                        onCancelar = { cancelaciones++ },
                    )
                }
            }
        }
    }

    // Uno de los dos idiomas es distinto del telefono: ese es el que prueba que
    // la ventana de la hoja no vuelve al idioma del dispositivo.
    @Test
    fun la_hoja_sale_en_espanol_si_la_aplicacion_esta_en_espanol() {
        montarHoja(Idioma.ESPANOL)

        composicion.onNodeWithText(textoEn(Idioma.ESPANOL, R.string.auditoria_revocacion_hoja_titulo, "Banco Aliado")).assertIsDisplayed()
    }

    @Test
    fun la_hoja_sale_en_ingles_si_la_aplicacion_esta_en_ingles() {
        montarHoja(Idioma.INGLES)

        composicion.onNodeWithText(textoEn(Idioma.INGLES, R.string.auditoria_revocacion_hoja_titulo, "Banco Aliado")).assertIsDisplayed()
    }

    @Test
    fun explica_el_efecto_y_confirma_con_la_huella() {
        montar(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.BIOMETRIA))

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_hoja_titulo, "Banco Aliado")).assertIsDisplayed()
        composicion.onNodeWithText(
            texto(R.string.auditoria_revocacion_hoja_efecto) + " " + texto(R.string.auditoria_revocacion_hoja_con_biometria),
        ).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_hoja_icono_biometria)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_hoja_confirmar_biometria)).assertIsEnabled().performClick()

        assertEquals(1, biometrias)
        assertEquals(0, cancelaciones)
    }

    @Test
    fun cancelar_cierra_sin_confirmar() {
        montar(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.BIOMETRIA))

        composicion.onNodeWithText(texto(R.string.comun_cancelar)).performClick()

        assertEquals(1, cancelaciones)
        assertEquals(0, biometrias)
        assertTrue(contrasenas.isEmpty())
    }

    @Test
    fun sin_huella_pide_la_contrasena_de_al_menos_diez_caracteres() {
        montar(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.CONTRASENA))
        val confirmar = composicion.onNodeWithText(texto(R.string.auditoria_revocacion_hoja_confirmar_contrasena))

        composicion.onNodeWithText(
            texto(R.string.auditoria_revocacion_hoja_efecto) + " " + texto(R.string.auditoria_revocacion_hoja_con_contrasena),
        ).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_hoja_contrasena_minimo, MINIMO_DE_CONTRASENA))
            .assertIsDisplayed()
        confirmar.assertIsNotEnabled()

        composicion.onNode(hasSetTextAction()).performTextInput("corta")
        confirmar.assertIsNotEnabled()
        composicion.onNode(hasSetTextAction()).performTextInput("-y-larga")
        confirmar.assertIsEnabled().performClick()

        assertEquals(listOf("corta-y-larga"), contrasenas)
        assertEquals(0, biometrias)
    }

    @Test
    fun mientras_verifica_no_se_puede_confirmar_ni_cancelar() {
        montar(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.BIOMETRIA, verificando = true))

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_hoja_verificando)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_hoja_confirmar_biometria)).assertIsNotEnabled()
        composicion.onNodeWithText(texto(R.string.comun_cancelar)).assertIsNotEnabled()
    }

    @Test
    fun sin_red_la_contrasena_no_se_puede_verificar_y_lo_dice() {
        montar(PasoDeRevocacion.Hoja(MetodoDeConfirmacion.CONTRASENA, sinConexion = true))

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_hoja_sin_conexion)).assertIsDisplayed()
    }
}
