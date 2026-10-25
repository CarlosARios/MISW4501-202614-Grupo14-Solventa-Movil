package co.solventa.movil.features.perfil

import android.content.res.Configuration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * M19 · Perfil (HU-M1.3-01): las filas en espera dicen
 * «Próximamente» y no responden; las activas si; Idioma abre la hoja.
 * HU-M1.3-02 (D-7 a): con revocaciones en cola avisa antes de cerrar sesion.
 */
class PerfilPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private var consentimientos = 0
    private var cierres = 0
    private var cierresConfirmados = 0
    private var cierresCancelados = 0
    private val biometria = mutableListOf<Boolean>()

    private fun montar(estado: PerfilUiState = estadoBase()) {
        composicion.setContent {
            SolventaTheme {
                ContenidoPerfil(
                    estado = estado,
                    onAlternarBiometria = { biometria += it },
                    onElegirIdioma = {},
                    onConsentimientos = { consentimientos++ },
                    onCerrarSesion = { cierres++ },
                    onConfirmarCierre = { cierresConfirmados++ },
                    onCancelarCierre = { cierresCancelados++ },
                )
            }
        }
    }

    private fun estadoBase() = PerfilUiState(
        nombre = "Carolina Pérez Ruiz",
        correo = "carolina.perez@correo.com",
        iniciales = "CP",
        biometriaHabilitada = false,
        biometriaDisponible = true,
        idioma = Idioma.ESPANOL,
        activos = 2,
        version = "1.0",
        minutosDeCierre = 5,
    )

    @Test
    fun muestra_la_identidad_y_la_version() {
        montar()

        composicion.onNodeWithText("Carolina Pérez Ruiz").assertIsDisplayed()
        composicion.onNodeWithText("carolina.perez@correo.com").assertIsDisplayed()
        composicion.onNodeWithText("CP").assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.perfil_version, "1.0")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun las_filas_en_espera_estan_deshabilitadas_y_dicen_proximamente() {
        montar()

        listOf(
            R.string.perfil_datos_personales,
            R.string.perfil_medios_de_pago,
            R.string.perfil_cambiar_contrasena,
            R.string.perfil_descargar_datos,
            R.string.perfil_notificaciones,
            R.string.perfil_sincronizacion,
        ).forEach { fila ->
            composicion.onNodeWithContentDescription(texto(R.string.perfil_en_espera_descripcion, texto(fila)))
                .performScrollTo()
                .assertIsDisplayed()
                .assertIsNotEnabled()
        }
    }

    @Test
    fun privacidad_y_consentimientos_muestra_los_activos_y_abre_la_consulta() {
        montar()

        composicion.onNodeWithText(contexto.resources.getQuantityString(R.plurals.perfil_consentimientos_activos, 2, 2))
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.perfil_privacidad_consentimientos)).performScrollTo().performClick()

        assertEquals(1, consentimientos)
    }

    @Test
    fun cierre_automatico_informa_los_minutos() {
        montar()

        composicion.onNodeWithText(texto(R.string.perfil_cierre_minutos, 5)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun la_biometria_se_alterna_con_el_interruptor() {
        montar()

        composicion.onNodeWithContentDescription(texto(R.string.perfil_biometria)).performScrollTo().performClick()

        assertEquals(listOf(true), biometria)
    }

    @Test
    fun el_interruptor_de_la_biometria_va_antes_del_texto_como_en_el_frame() {
        montar()

        val interruptor = composicion.onNodeWithContentDescription(texto(R.string.perfil_biometria))
            .performScrollTo().getUnclippedBoundsInRoot()
        val titulo = composicion.onNodeWithText(texto(R.string.perfil_biometria)).getUnclippedBoundsInRoot()

        assertTrue("el interruptor queda a la izquierda del texto", interruptor.right <= titulo.left)
    }

    @Test
    fun sin_biometria_en_el_dispositivo_el_interruptor_esta_deshabilitado() {
        montar(estadoBase().copy(biometriaDisponible = false))

        composicion.onNodeWithContentDescription(texto(R.string.perfil_biometria)).performScrollTo().assertIsNotEnabled()
        composicion.onNodeWithText(texto(R.string.perfil_biometria_no_disponible)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun idioma_muestra_el_actual_y_abre_la_hoja() {
        montar()

        composicion.onNodeWithText(Idioma.ESPANOL.etiqueta).performScrollTo().performClick()

        // La hoja se dibuja en el idioma elegido en la aplicacion, no en el
        // del dispositivo.
        val enEspanol = contexto.createConfigurationContext(
            Configuration(contexto.resources.configuration).apply { setLocale(Idioma.ESPANOL.aLocale()) },
        )
        composicion.onNodeWithText(enEspanol.getString(R.string.bienvenida_idioma_titulo)).assertIsDisplayed()
    }

    @Test
    fun cerrar_sesion_responde() {
        montar()

        composicion.onNodeWithText(texto(R.string.perfil_cerrar_sesion)).performScrollTo().performClick()

        assertTrue(cierres == 1)
    }

    @Test
    fun cerrar_sesion_no_lleva_flecha_y_las_filas_que_abren_otra_pantalla_si() {
        montar()

        fun flechasDe(fila: Int) = composicion.onNodeWithText(texto(fila), useUnmergedTree = true)
            .performScrollTo()
            .onParent()
            .onChildren()
            .filter(hasTestTag(ETIQUETA_FLECHA_DE_FILA))

        // Cerrar sesion no abre otra pantalla, asi que no lleva flecha.
        flechasDe(R.string.perfil_cerrar_sesion).assertCountEquals(0)
        flechasDe(R.string.perfil_idioma).assertCountEquals(1)
        flechasDe(R.string.perfil_privacidad_consentimientos).assertCountEquals(1)
    }

    @Test
    fun con_revocaciones_en_cola_avisa_antes_de_cerrar_sesion() {
        montar(estadoBase().copy(revocacionesPendientesAlCerrar = 2))

        composicion.onNodeWithText(texto(R.string.perfil_cerrar_sesion_pendientes_titulo)).assertIsDisplayed()
        composicion.onNodeWithText(contexto.resources.getQuantityString(R.plurals.perfil_cerrar_sesion_pendientes, 2, 2))
            .assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.perfil_cerrar_sesion_pendientes_cancelar)).performClick()
        composicion.onNodeWithText(texto(R.string.perfil_cerrar_sesion_pendientes_confirmar)).performClick()

        assertEquals(1, cierresCancelados)
        assertEquals(1, cierresConfirmados)
    }

    @Test
    fun sin_revocaciones_en_cola_no_hay_aviso_al_cerrar_sesion() {
        montar()

        composicion.onAllNodesWithText(texto(R.string.perfil_cerrar_sesion_pendientes_titulo)).assertCountEquals(0)
    }
}
