package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Consentimiento revocado y comprobante (HU-M1.3-02, frame 38:1414): cuanto
 * tardo en hacerse efectiva (criterio 3), los datos del criterio 4, la
 * descarga del PDF y volver a la lista.
 */
class ConsentimientoRevocadoPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private var descargas = 0
    private var vueltas = 0
    private var avisosCerrados = 0

    private fun montar(estado: RevocacionUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoConsentimientoRevocado(
                    estado = estado,
                    idioma = Idioma.ESPANOL,
                    onDescargar = { descargas++ },
                    onVolver = { vueltas++ },
                    onCerrarAviso = { avisosCerrados++ },
                )
            }
        }
    }

    private fun conSegundos(instante: Long) = texto(
        R.string.auditoria_revocacion_fecha_hora_segundos,
        FormatoDeFecha.fecha(instante, Idioma.ESPANOL),
        FormatoDeFecha.horaConSegundos(instante, Idioma.ESPANOL),
    )

    @Test
    fun dice_en_cuantos_segundos_fue_efectiva() {
        montar(RevocacionUiState(consentimiento = DatosDePantalla.revocadoEnLinea))

        // El titulo esta en la barra superior y bajo el escudo.
        assertEquals(2, composicion.onAllNodesWithText(texto(R.string.auditoria_revocacion_revocado_titulo)).fetchSemanticsNodes().size)
        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_revocado_icono)).assertIsDisplayed()
        composicion.onNodeWithText(
            texto(R.string.auditoria_revocacion_revocado_efectivo_en, segundos(2_100L, Idioma.ESPANOL), "Banco Aliado"),
        ).assertIsDisplayed()
    }

    @Test
    fun muestra_los_datos_del_criterio_4() {
        montar(RevocacionUiState(consentimiento = DatosDePantalla.revocadoEnLinea))

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_consentimiento_y_entidad, "cns-activo", "Banco Aliado"))
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_campo_estado).uppercase()).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_estado_revocado)).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(conSegundos(DatosDePantalla.SOLICITADA)).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(conSegundos(DatosDePantalla.EFECTIVA)).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_comprobante_detalle, "REV-2026-0088"))
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            texto(R.string.auditoria_revocacion_que_cambia_titulo) + ". " + texto(R.string.auditoria_revocacion_que_cambia),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun con_el_reloj_del_telefono_adelantado_dice_desde_cuando() {
        val adelantado = DatosDePantalla.revocadoEnLinea.copy(
            revocacion = DatosDePantalla.revocacion().copy(solicitadaEn = DatosDePantalla.EFECTIVA + 60_000L),
        )
        montar(RevocacionUiState(consentimiento = adelantado))

        composicion.onNodeWithText(
            texto(
                R.string.auditoria_revocacion_revocado_efectivo_desde,
                FormatoDeFecha.horaConSegundos(DatosDePantalla.EFECTIVA, Idioma.ESPANOL),
                "Banco Aliado",
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun descargar_el_comprobante_y_volver_responden() {
        montar(RevocacionUiState(consentimiento = DatosDePantalla.revocadoEnLinea))

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_descargar)).assertIsEnabled().performClick()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_volver)).performClick()

        assertEquals(1, descargas)
        assertEquals(1, vueltas)
    }

    @Test
    fun mientras_descarga_el_boton_se_apaga() {
        montar(RevocacionUiState(consentimiento = DatosDePantalla.revocadoEnLinea, descargando = true))

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_descargar)).assertIsNotEnabled()
    }

    @Test
    fun el_comprobante_guardado_se_avisa_y_el_aviso_se_cierra() {
        montar(
            RevocacionUiState(consentimiento = DatosDePantalla.revocadoEnLinea, aviso = AvisoDeLaRevocacion.COMPROBANTE_GUARDADO),
        )

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_aviso_comprobante_guardado))
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_cerrar_aviso)).performScrollTo().performClick()

        assertEquals(1, avisosCerrados)
    }
}
