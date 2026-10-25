package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * M8b · Detalle del consentimiento (HU-M1.3-01): los campos del criterio 2,
 * el historial como linea de tiempo (frame 37:1418) con su paso final, la
 * revocacion con su comprobante y el boton Revocar presente y deshabilitado
 * solo en un activo (D-8 b).
 */
class DetalleConsentimientoPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private fun montar(consentimiento: Consentimiento?, cargando: Boolean = false, sinConexion: Boolean = false) {
        composicion.setContent {
            SolventaTheme {
                ContenidoDetalleConsentimiento(
                    estado = DetalleConsentimientoUiState(
                        cargando = cargando,
                        consentimiento = consentimiento,
                        noDisponible = consentimiento == null && !cargando,
                        sinConexion = sinConexion,
                    ),
                    idioma = Idioma.ESPANOL,
                    onAtras = {},
                )
            }
        }
    }

    private fun fecha(instante: Long) = FormatoDeFecha.fecha(instante, Idioma.ESPANOL)
    private fun hora(instante: Long) = FormatoDeFecha.hora(instante, Idioma.ESPANOL)

    @Test
    fun muestra_todo_lo_del_criterio_2() {
        montar(DatosDePantalla.activo)
        val otorgado = DatosDePantalla.OTORGADO

        composicion.onNodeWithText("Banco Aliado").assertIsDisplayed()
        composicion.onNodeWithText("cns-activo").assertIsDisplayed()
        // «Activo» esta en la insignia de la cabecera y en el ultimo paso del historial.
        assertEquals(2, composicion.onAllNodesWithText(texto(R.string.auditoria_consentimientos_estado_activo)).fetchSemanticsNodes().size)
        composicion.onAllNodesWithText(texto(R.string.auditoria_consentimientos_estado_activo)).onFirst().assertIsDisplayed()
        // La fecha, la hora y el canal salen en el campo Otorgado y en la
        // primera entrada del historial.
        val otorgadoTexto = texto(
            R.string.auditoria_detalle_fecha_hora_canal, fecha(otorgado), hora(otorgado), texto(R.string.auditoria_consentimientos_canal_aliado),
        )
        assertEquals(2, composicion.onAllNodesWithText(otorgadoTexto).fetchSemanticsNodes().size)
        composicion.onAllNodesWithText(otorgadoTexto).onFirst().assertIsDisplayed()
        composicion.onNodeWithText(fecha(DatosDePantalla.VENCE)).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(
            texto(
                R.string.auditoria_detalle_fuente,
                texto(R.string.auditoria_consentimientos_fuente_open_finance),
                texto(R.string.auditoria_consentimientos_categoria_productos_de_credito) + " · " +
                    texto(R.string.auditoria_consentimientos_categoria_saldos),
                texto(R.string.auditoria_consentimientos_vigencia_12_meses),
            ),
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_finalidad_perfil_y_oferta))
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_detalle_historial).uppercase()).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_historial_otorgado)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun el_revocado_dice_cuando_y_muestra_el_comprobante_en_el_historial() {
        montar(DatosDePantalla.revocado)
        val fin = DatosDePantalla.REVOCADO_EN

        composicion.onNodeWithText(texto(R.string.auditoria_detalle_revocado_el).uppercase()).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_detalle_fecha_hora, fecha(fin), hora(fin))).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText("REV-2026-0012").performScrollTo().assertIsDisplayed()
        // «Revocado» esta en la insignia y en el historial: se busca la entrada del historial.
        composicion.onNode(hasContentDescription(texto(R.string.auditoria_consentimientos_historial_revocado) + ". ", substring = true))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun el_vencido_dice_cuando_vencio() {
        montar(DatosDePantalla.vencido)

        composicion.onNodeWithText(texto(R.string.auditoria_detalle_vencio_el).uppercase()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun revocar_esta_presente_y_deshabilitado_en_un_activo() {
        montar(DatosDePantalla.activo)

        composicion.onNodeWithText(texto(R.string.auditoria_detalle_revocar)).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun revocar_no_aparece_en_un_revocado() {
        montar(DatosDePantalla.revocado)

        assertEquals(0, composicion.onAllNodes(hasText(texto(R.string.auditoria_detalle_revocar))).fetchSemanticsNodes().size)
    }

    @Test
    fun revocar_no_aparece_en_un_vencido() {
        montar(DatosDePantalla.vencido)

        assertEquals(0, composicion.onAllNodes(hasText(texto(R.string.auditoria_detalle_revocar))).fetchSemanticsNodes().size)
    }

    @Test
    fun no_hay_ningun_campo_editable() {
        montar(DatosDePantalla.activo)

        assertEquals(0, composicion.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    /** La parte de arriba de un paso de la linea de tiempo, buscado por su descripcion. */
    private fun arribaDe(descripcion: String, substring: Boolean = false) =
        composicion.onNode(hasContentDescription(descripcion, substring = substring)).performScrollTo()
            .getUnclippedBoundsInRoot().top

    private fun pasoNumerado(numero: Int) =
        composicion.onAllNodes(hasText(numero.toString()), useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun el_activo_termina_en_el_paso_activo_vigente_y_sincronizado() {
        montar(DatosDePantalla.activo)
        val otorgado = texto(R.string.auditoria_consentimientos_historial_otorgado) + ". "
        val activo = texto(R.string.auditoria_consentimientos_estado_activo) + ". " +
            texto(R.string.auditoria_detalle_paso_vigente_sincronizado)

        composicion.onNodeWithContentDescription(activo).performScrollTo().assertIsDisplayed()
        // El paso final lleva su numero: el segundo, despues del otorgamiento.
        assertEquals(1, pasoNumerado(2))
        assertTrue("Otorgado va antes que Activo", arribaDe(otorgado, substring = true) < arribaDe(activo))
    }

    @Test
    fun sin_conexion_el_paso_activo_dice_solo_vigente() {
        montar(DatosDePantalla.activo, sinConexion = true)
        val activo = texto(R.string.auditoria_consentimientos_estado_activo) + ". " + texto(R.string.auditoria_detalle_paso_vigente)

        composicion.onNodeWithContentDescription(activo).performScrollTo().assertIsDisplayed()
        assertEquals(
            0,
            composicion.onAllNodes(hasText(texto(R.string.auditoria_detalle_paso_vigente_sincronizado))).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun el_revocado_termina_en_su_revocacion_sin_paso_final() {
        montar(DatosDePantalla.revocado)
        val otorgado = texto(R.string.auditoria_consentimientos_historial_otorgado) + ". "
        val revocado = texto(R.string.auditoria_consentimientos_historial_revocado) + ". "

        assertTrue("Otorgado va antes que Revocado", arribaDe(otorgado, substring = true) < arribaDe(revocado, substring = true))
        assertEquals("sin paso numerado", 0, pasoNumerado(3))
        assertEquals(
            0,
            composicion.onAllNodes(
                hasContentDescription(texto(R.string.auditoria_consentimientos_estado_activo) + ". ", substring = true),
            ).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun el_vencido_que_termina_en_su_vencimiento_no_agrega_paso() {
        montar(DatosDePantalla.vencido)

        composicion.onNode(hasContentDescription(texto(R.string.auditoria_consentimientos_historial_vencido) + ". ", substring = true))
            .performScrollTo().assertIsDisplayed()
        assertEquals("sin paso numerado", 0, pasoNumerado(3))
    }

    @Test
    fun el_vencido_con_una_revocacion_posterior_no_repite_el_vencimiento() {
        val revocadoDespues = DatosDePantalla.VENCE + 3_600_000L
        montar(
            DatosDePantalla.vencido.copy(
                historial = DatosDePantalla.vencido.historial +
                    CambioDeEstado(CambiosDeEstado.REVOCADO, revocadoDespues, "REV-2027-0001", "mobile", null),
            ),
        )
        val vencio = texto(R.string.auditoria_consentimientos_historial_vencido) + ". "
        val revocado = texto(R.string.auditoria_consentimientos_historial_revocado) + ". "
        val pasoVencido = texto(R.string.auditoria_consentimientos_estado_vencido) + ". " + fecha(DatosDePantalla.VENCE)

        composicion.onNode(hasContentDescription(revocado, substring = true)).performScrollTo().assertIsDisplayed()
        assertEquals(
            "el vencimiento aparece una sola vez",
            1,
            composicion.onAllNodes(hasContentDescription(vencio, substring = true)).fetchSemanticsNodes().size,
        )
        assertEquals("sin paso final Vencido", 0, composicion.onAllNodes(hasContentDescription(pasoVencido)).fetchSemanticsNodes().size)
        assertEquals("sin paso numerado", 0, pasoNumerado(4))
        assertTrue("el vencimiento va antes que la revocacion", arribaDe(vencio, substring = true) < arribaDe(revocado, substring = true))
    }

    @Test
    fun un_consentimiento_que_ya_no_esta_lo_dice() {
        montar(null)

        composicion.onNodeWithText(texto(R.string.auditoria_detalle_no_disponible)).assertIsDisplayed()
    }
}
