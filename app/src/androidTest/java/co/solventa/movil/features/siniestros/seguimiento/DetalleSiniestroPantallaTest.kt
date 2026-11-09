package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
 * M10 · Detalle del siniestro con su línea de tiempo (HU-M2.4-01) y el aviso
 * de versión en caché (HA-L02, criterio 5: respuesta normal y respuesta desde
 * la caché), con el caso del prototipo.
 */
class DetalleSiniestroPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    /** El título de la tarjeta en el idioma del dispositivo: «Hogar · Daño por agua». */
    private fun titulo(linea: Int, incidente: Int) =
        texto(R.string.siniestros_seguimiento_titulo_caso, texto(linea), texto(incidente))

    private val hogar get() = titulo(R.string.siniestros_seguimiento_linea_hogar, R.string.siniestros_seguimiento_incidente_agua)

    private var atras = 0
    private var reintentos = 0

    private fun montar(estado: DetalleSiniestroUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoDetalleSiniestro(
                    estado = estado,
                    idioma = Idioma.ESPANOL,
                    ahora = DatosDeConsulta.AHORA,
                    onAtras = { atras++ },
                    onReintentar = { reintentos++ },
                )
            }
        }
    }

    private fun hoy() = texto(R.string.siniestros_seguimiento_hoy, FormatoDeFecha.hora(DatosDeConsulta.AHORA, Idioma.ESPANOL))

    @Test
    fun respuesta_normal_muestra_la_linea_de_tiempo_y_cuando_se_actualizo() {
        montar(DatosDeConsulta.detalle(desdeCache = false))

        composicion.onNodeWithText("SIN-2026-01655").assertIsDisplayed()
        composicion.onNodeWithText(hogar).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_etapa_evaluacion), substring = true)
            .performScrollTo().assertIsDisplayed()
        // Cada etapa se anuncia como una unidad: nombre, avance y fecha o lo que viene.
        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_seguimiento_etapa_evaluacion)}. ${texto(R.string.siniestros_seguimiento_etapa_actual)}",
            substring = true,
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_que_sigue_en_evaluacion)).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_pie_actualizado, hoy()))
            .performScrollTo().assertIsDisplayed()
        assertEquals(
            0,
            composicion.onAllNodesWithContentDescription(texto(R.string.siniestros_seguimiento_cache_titulo), substring = true)
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun respuesta_desde_cache_avisa_y_dice_de_que_hora_es_la_version() {
        montar(DatosDeConsulta.detalle(desdeCache = true))

        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_seguimiento_cache_titulo)}. ${texto(R.string.siniestros_seguimiento_cache_texto, hoy())}",
        ).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_pie_cache, hoy()))
            .performScrollTo().assertIsDisplayed()

        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_reintentar_ahora)).performClick()
        assertEquals(1, reintentos)
    }

    @Test
    fun las_etapas_pendientes_dicen_lo_que_viene() {
        montar(DatosDeConsulta.detalle(desdeCache = false))

        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_etapa_aprobado_pendiente), substring = true)
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_etapa_pagado_pendiente), substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun un_siniestro_que_no_existe_no_ofrece_reintentar() {
        montar(DetalleSiniestroUiState(cargando = false, fallo = FalloDeConsulta.NO_ENCONTRADO))

        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_no_encontrado)).assertIsDisplayed()
        assertEquals(0, composicion.onAllNodesWithText(texto(R.string.siniestros_seguimiento_reintentar)).fetchSemanticsNodes().size)
    }

    @Test
    fun atras_vuelve() {
        montar(DatosDeConsulta.detalle(desdeCache = false))

        composicion.onNodeWithContentDescription(texto(R.string.comun_atras)).performClick()

        assertEquals(1, atras)
    }
}
