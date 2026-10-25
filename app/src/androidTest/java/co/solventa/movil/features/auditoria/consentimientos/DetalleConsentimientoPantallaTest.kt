package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import androidx.compose.ui.test.performClick
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
 * el historial como linea de tiempo (frame 37:1418) con su paso final y la
 * revocacion con su comprobante.
 *
 * HU-M1.3-02 (criterio 6): «Revocar consentimiento» activo solo en un activo
 * revocable; ausente en revocados, vencidos y con la revocacion en cola, que
 * ofrece «Reintentar envio ahora»; la revocacion parcial con su aviso y su
 * comprobante.
 */
class DetalleConsentimientoPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private var revocaciones = 0
    private var reintentos = 0
    private var descargas = 0
    private var avisosCerrados = 0

    private fun montar(
        consentimiento: Consentimiento?,
        cargando: Boolean = false,
        sinConexion: Boolean = false,
        pendiente: RevocacionPendiente? = null,
        revocacion: RevocacionUiState = RevocacionUiState(consentimiento = consentimiento, pendiente = pendiente),
    ) {
        composicion.setContent {
            SolventaTheme {
                ContenidoDetalleConsentimiento(
                    estado = DetalleConsentimientoUiState(
                        cargando = cargando,
                        consentimiento = consentimiento,
                        noDisponible = consentimiento == null && !cargando,
                        sinConexion = sinConexion,
                        pendiente = pendiente,
                    ),
                    idioma = Idioma.ESPANOL,
                    onAtras = {},
                    revocacion = revocacion,
                    onRevocar = { revocaciones++ },
                    onReintentar = { reintentos++ },
                    onDescargarComprobante = { descargas++ },
                    onCerrarAviso = { avisosCerrados++ },
                )
            }
        }
    }

    private fun cuantos(id: Int) = composicion.onAllNodes(hasText(texto(id))).fetchSemanticsNodes().size

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
    fun revocar_esta_activo_en_un_activo_y_abre_la_confirmacion() {
        montar(DatosDePantalla.activo)

        composicion.onNodeWithText(texto(R.string.auditoria_detalle_revocar)).assertIsDisplayed().assertIsEnabled().performClick()

        assertEquals(1, revocaciones)
        assertEquals("sin cola no hay reintento", 0, cuantos(R.string.auditoria_revocacion_reintentar))
    }

    @Test
    fun revocar_no_aparece_en_un_activo_que_el_servicio_no_deja_revocar() {
        montar(DatosDePantalla.activo.copy(revocable = false))

        assertEquals(0, cuantos(R.string.auditoria_detalle_revocar))
    }

    @Test
    fun mientras_se_envia_revocar_se_apaga_y_lo_dice() {
        montar(
            DatosDePantalla.activo,
            revocacion = RevocacionUiState(consentimiento = DatosDePantalla.activo, paso = PasoDeRevocacion.Enviando),
        )

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_enviando)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_detalle_revocar)).assertIsNotEnabled()
    }

    @Test
    fun con_la_revocacion_en_cola_no_hay_revocar_y_se_ofrece_reintentar_sin_senal_deshabilitado() {
        montar(
            DatosDePantalla.activo,
            sinConexion = true,
            pendiente = DatosDePantalla.enCola,
            revocacion = RevocacionUiState(
                consentimiento = DatosDePantalla.activo,
                pendiente = DatosDePantalla.enCola,
                conectado = false,
            ),
        )
        val aviso = texto(R.string.auditoria_revocacion_en_cola_titulo) + ". " + texto(
            R.string.auditoria_revocacion_en_cola,
            "Banco Aliado",
            fecha(DatosDePantalla.SOLICITADA),
            hora(DatosDePantalla.SOLICITADA),
        )

        assertEquals("criterio 6", 0, cuantos(R.string.auditoria_detalle_revocar))
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_reintentar)).assertIsDisplayed().assertIsNotEnabled()
        composicion.onNodeWithContentDescription(aviso).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_pendiente_de_envio)).performScrollTo().assertIsDisplayed()
        composicion.onNode(hasContentDescription(texto(R.string.auditoria_revocacion_paso_pendiente) + ". ", substring = true))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun con_senal_reintentar_envio_responde() {
        montar(
            DatosDePantalla.activo,
            pendiente = DatosDePantalla.enCola,
            revocacion = RevocacionUiState(consentimiento = DatosDePantalla.activo, pendiente = DatosDePantalla.enCola, conectado = true),
        )

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_reintentar)).assertIsEnabled().performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun la_revocacion_parcial_avisa_la_entidad_que_falta_y_ofrece_el_comprobante() {
        montar(DatosDePantalla.parcial)
        val aviso = texto(R.string.auditoria_revocacion_parcial_titulo, "Banco Aliado") + ". " + texto(
            R.string.auditoria_revocacion_parcial,
            fecha(DatosDePantalla.EFECTIVA),
            FormatoDeFecha.horaConSegundos(DatosDePantalla.EFECTIVA, Idioma.ESPANOL),
        )

        composicion.onNodeWithContentDescription(aviso).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_estado_revocado_parcial)).performScrollTo().assertIsDisplayed()
        composicion.onNode(
            hasContentDescription(texto(R.string.auditoria_revocacion_paso_sin_confirmar, "Banco Aliado") + ". ", substring = true),
        ).performScrollTo().assertIsDisplayed()
        assertEquals("sin boton de accion", 0, cuantos(R.string.auditoria_detalle_revocar))
        assertEquals(0, cuantos(R.string.auditoria_revocacion_reintentar))

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_descargar_detalle)).performScrollTo().performClick()
        assertEquals(1, descargas)
    }

    @Test
    fun la_contrasena_incorrecta_se_avisa_en_el_detalle_que_sigue_activo() {
        montar(
            DatosDePantalla.activo,
            revocacion = RevocacionUiState(consentimiento = DatosDePantalla.activo, aviso = AvisoDeLaRevocacion.CONTRASENA_INCORRECTA),
        )

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_aviso_contrasena_incorrecta)).assertIsDisplayed()
        // «Activo» esta en la insignia y en el paso final del historial.
        composicion.onAllNodesWithText(texto(R.string.auditoria_consentimientos_estado_activo)).onFirst().assertIsDisplayed()
    }

    @Test
    fun la_huella_no_reconocida_se_avisa_en_el_detalle() {
        montar(
            DatosDePantalla.activo,
            revocacion = RevocacionUiState(consentimiento = DatosDePantalla.activo, aviso = AvisoDeLaRevocacion.HUELLA_NO_RECONOCIDA),
        )

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_aviso_huella_no_reconocida)).assertIsDisplayed()
    }

    @Test
    fun un_aviso_de_la_revocacion_se_muestra_y_se_cierra() {
        montar(
            DatosDePantalla.activo,
            revocacion = RevocacionUiState(consentimiento = DatosDePantalla.activo, aviso = AvisoDeLaRevocacion.NO_SE_PUDO),
        )

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_aviso_no_se_pudo)).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_cerrar_aviso)).performClick()

        assertEquals(1, avisosCerrados)
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
