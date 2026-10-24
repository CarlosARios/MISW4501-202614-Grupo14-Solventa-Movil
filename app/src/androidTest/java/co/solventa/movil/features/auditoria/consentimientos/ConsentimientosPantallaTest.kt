package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
 * M8 · Privacidad y consentimientos (HU-M1.3-01) con datos de prueba: las
 * secciones y sus conteos, los campos del criterio 1, el comprobante, el
 * aviso sin conexion, la cuenta sin vincular y que no hay nada que editar.
 */
class ConsentimientosPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private var abierto: String? = null
    private var reintentos = 0

    private fun montar(estado: ConsentimientosUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoConsentimientos(
                    estado = estado,
                    idioma = Idioma.ESPANOL,
                    onAtras = {},
                    onAbrir = { abierto = it },
                    onReintentar = { reintentos++ },
                )
            }
        }
    }

    private fun conContenido() = ConsentimientosUiState(
        cargando = false,
        activos = listOf(DatosDePantalla.activo),
        historial = listOf(DatosDePantalla.revocado, DatosDePantalla.vencido),
    )

    @Test
    fun separa_activos_de_revocados_y_vencidos_con_su_conteo() {
        montar(conContenido())

        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_seccion_activos, 1).uppercase()).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_seccion_historial, 2).uppercase())
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cada_tarjeta_muestra_entidad_otorgamiento_vigencia_y_estado() {
        montar(conContenido())
        val otorgado = FormatoDeFecha.fecha(DatosDePantalla.OTORGADO, Idioma.ESPANOL)
        val vence = FormatoDeFecha.fecha(DatosDePantalla.VENCE, Idioma.ESPANOL)

        composicion.onNodeWithText("Banco Aliado").assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_otorgado_vence, otorgado, vence)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_estado_activo)).assertIsDisplayed()
    }

    @Test
    fun el_revocado_muestra_cuando_termino_y_su_comprobante() {
        montar(conContenido())
        val otorgado = FormatoDeFecha.fecha(DatosDePantalla.OTORGADO, Idioma.ESPANOL)
        val revocado = FormatoDeFecha.fecha(DatosDePantalla.REVOCADO_EN, Idioma.ESPANOL)

        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_otorgado_revocado, otorgado, revocado))
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_comprobante, "REV-2026-0012"))
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_estado_vencido)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tocar_una_tarjeta_abre_su_detalle() {
        montar(conContenido())

        composicion.onNodeWithText("Banco Aliado").performClick()

        assertEquals("cns-activo", abierto)
    }

    @Test
    fun sin_conexion_avisa_con_la_fecha_y_hora_de_la_sincronizacion() {
        montar(conContenido().copy(sinConexion = true, sincronizadoEn = DatosDePantalla.SINCRONIZADO))
        val aviso = texto(
            R.string.auditoria_consentimientos_sin_conexion,
            FormatoDeFecha.fecha(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL),
            FormatoDeFecha.hora(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL),
        )

        // El aviso expone su texto como descripcion para el lector de pantalla.
        composicion.onNodeWithContentDescription(aviso).assertIsDisplayed()
        composicion.onNodeWithText("Banco Aliado").assertIsDisplayed()
    }

    @Test
    fun una_cuenta_sin_vincular_lo_dice_y_permite_reintentar() {
        montar(ConsentimientosUiState(cargando = false, cuentaSinVincular = true))

        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_sin_vincular)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_reintentar)).performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun sin_consentimientos_muestra_el_estado_vacio() {
        montar(ConsentimientosUiState(cargando = false))

        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_vacio)).assertIsDisplayed()
    }

    @Test
    fun la_carga_tiene_descripcion_para_el_lector_de_pantalla() {
        montar(ConsentimientosUiState(cargando = true))

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_consentimientos_cargando)).assertIsDisplayed()
    }

    @Test
    fun no_hay_ningun_control_de_edicion() {
        montar(conContenido())

        assertEquals(0, composicion.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        // Lo unico que se toca son las tarjetas (abren el detalle) y volver.
        val tocables = composicion.onAllNodes(hasClickAction()).fetchSemanticsNodes().size
        assertEquals(4, tocables)
        assertEquals(0, composicion.onAllNodes(hasText(texto(R.string.auditoria_detalle_revocar))).fetchSemanticsNodes().size)
    }
}
