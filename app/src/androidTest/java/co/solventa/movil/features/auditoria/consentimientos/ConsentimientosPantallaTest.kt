package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * M8 · Privacidad y consentimientos (HU-M1.3-01) con datos de prueba: las
 * secciones y sus conteos, los campos del criterio 1, el comprobante, el
 * aviso sin conexion, la cuenta sin vincular y que no hay nada que editar.
 *
 * HU-M1.3-02 (38:1503, criterio 5): la seccion «Pendientes de envio» con su
 * insignia, el aviso de la cola y «Reintentar envio ahora».
 */
class ConsentimientosPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private var abierto: String? = null
    private var reintentos = 0
    private var reenvios = 0
    private val avisosCerrados = mutableListOf<String>()

    private fun montar(estado: ConsentimientosUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoConsentimientos(
                    estado = estado,
                    idioma = Idioma.ESPANOL,
                    onAtras = {},
                    onAbrir = { abierto = it },
                    onReintentar = { reintentos++ },
                    onReintentarEnvio = { reenvios++ },
                    onCerrarAviso = { avisosCerrados += it },
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
    fun el_comprobante_lleva_su_icono_descrito_para_el_lector_de_pantalla() {
        montar(conContenido())

        // Solo el revocado tiene comprobante (frame 37:1233).
        val iconos = composicion.onAllNodes(
            hasContentDescription(texto(R.string.auditoria_detalle_comprobante)),
            useUnmergedTree = true,
        )
        assertEquals(1, iconos.fetchSemanticsNodes().size)
        composicion.onNodeWithContentDescription(texto(R.string.auditoria_detalle_comprobante), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sin_conexion_el_aviso_va_antes_del_texto_introductorio() {
        montar(conContenido().copy(sinConexion = true, sincronizadoEn = DatosDePantalla.SINCRONIZADO))
        val aviso = texto(
            R.string.auditoria_consentimientos_sin_conexion,
            FormatoDeFecha.fecha(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL),
            FormatoDeFecha.hora(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL),
        )

        val arribaDelAviso = composicion.onNodeWithContentDescription(aviso).getUnclippedBoundsInRoot().top
        val arribaDeLaIntro = composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_intro))
            .getUnclippedBoundsInRoot().top
        assertTrue("el aviso va justo bajo el encabezado (frame 38:1503)", arribaDelAviso < arribaDeLaIntro)
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
        // Y la barra superior muestra el icono de sin red (frame 38:1503).
        composicion.onNodeWithContentDescription(texto(R.string.auditoria_consentimientos_sin_conexion_icono)).assertIsDisplayed()
        composicion.onNodeWithText("Banco Aliado").assertIsDisplayed()
    }

    @Test
    fun en_linea_no_muestra_el_icono_de_sin_conexion() {
        montar(conContenido())

        assertEquals(
            0,
            composicion.onAllNodes(hasContentDescription(texto(R.string.auditoria_consentimientos_sin_conexion_icono)))
                .fetchSemanticsNodes().size,
        )
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

    // --- HU-M1.3-02 ---

    private fun conCola(sinConexion: Boolean = true) = ConsentimientosUiState(
        cargando = false,
        historial = listOf(DatosDePantalla.vencido),
        sinConexion = sinConexion,
        sincronizadoEn = DatosDePantalla.SINCRONIZADO,
        pendientes = listOf(ConsentimientoPendiente(DatosDePantalla.activo, DatosDePantalla.enCola)),
    )

    @Test
    fun la_revocacion_en_cola_va_en_pendientes_de_envio_con_su_insignia() {
        montar(conCola())
        val solicitada = texto(
            R.string.auditoria_revocacion_solicitada_el,
            FormatoDeFecha.fecha(DatosDePantalla.SOLICITADA, Idioma.ESPANOL),
            FormatoDeFecha.hora(DatosDePantalla.SOLICITADA, Idioma.ESPANOL),
        )

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_seccion_pendientes, 1).uppercase())
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(solicitada).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_pendiente_de_envio)).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_se_reintenta)).performScrollTo().assertIsDisplayed()
        // Ya no esta entre los activos.
        composicion.onNodeWithText(texto(R.string.auditoria_consentimientos_seccion_activos, 0).uppercase())
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sin_senal_el_aviso_de_la_cola_reemplaza_al_de_sin_conexion() {
        montar(conCola())
        val enCola = texto(R.string.auditoria_revocacion_en_cola_titulo) + ". " + texto(
            R.string.auditoria_revocacion_en_cola,
            "Banco Aliado",
            FormatoDeFecha.fecha(DatosDePantalla.SOLICITADA, Idioma.ESPANOL),
            FormatoDeFecha.hora(DatosDePantalla.SOLICITADA, Idioma.ESPANOL),
        )
        val sinConexion = texto(
            R.string.auditoria_consentimientos_sin_conexion,
            FormatoDeFecha.fecha(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL),
            FormatoDeFecha.hora(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL),
        )

        composicion.onNodeWithContentDescription(enCola).assertIsDisplayed()
        assertEquals(0, composicion.onAllNodes(hasContentDescription(sinConexion)).fetchSemanticsNodes().size)
        composicion.onNodeWithContentDescription(texto(R.string.auditoria_consentimientos_sin_conexion_icono)).assertIsDisplayed()
    }

    @Test
    fun sin_senal_reintentar_envio_esta_deshabilitado() {
        montar(conCola(sinConexion = true))

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_reintentar)).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun con_senal_reintentar_envio_responde() {
        montar(conCola(sinConexion = false))

        composicion.onNodeWithText(texto(R.string.auditoria_revocacion_reintentar)).assertIsEnabled().performClick()

        assertEquals(1, reenvios)
    }

    @Test
    fun sin_revocaciones_en_cola_no_hay_reintento() {
        montar(conContenido())

        assertEquals(0, composicion.onAllNodes(hasText(texto(R.string.auditoria_revocacion_reintentar))).fetchSemanticsNodes().size)
        assertEquals(0, composicion.onAllNodes(hasText(texto(R.string.auditoria_revocacion_pendiente_de_envio))).fetchSemanticsNodes().size)
    }

    @Test
    fun la_tarjeta_pendiente_abre_su_detalle() {
        montar(conCola())

        composicion.onNodeWithText("Banco Aliado").performScrollTo().performClick()

        assertEquals("cns-activo", abierto)
    }

    @Test
    fun una_revocacion_que_el_servicio_ya_no_acepto_se_avisa_y_se_cierra() {
        montar(
            conContenido().copy(descartes = listOf(AvisoDeRevocacion("cns-vencido", "Banco Central", MotivoDeDescarte.VENCIO))),
        )

        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_descarte_vencio, "Banco Central"))
            .assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.auditoria_revocacion_cerrar_aviso)).performClick()

        assertEquals(listOf("cns-vencido"), avisosCerrados)
    }
}
