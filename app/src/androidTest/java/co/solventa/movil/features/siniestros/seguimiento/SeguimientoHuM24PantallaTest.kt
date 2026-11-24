package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
 * M10, M10b y M10c con lo que agrega HU-M2.4-01: el tiempo estimado de la
 * evaluación (criterio 2), el siniestro paramétrico resuelto automáticamente
 * (criterio 3) y la versión guardada en el teléfono sin red (criterio 5),
 * distinta del aviso de la caché del servicio (criterio 4).
 */
class SeguimientoHuM24PantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private fun montar(estado: DetalleSiniestroUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoDetalleSiniestro(
                    estado = estado,
                    idioma = Idioma.ESPANOL,
                    ahora = DatosDeConsulta.AHORA,
                    onAtras = {},
                    onReintentar = {},
                )
            }
        }
    }

    private fun fecha(instante: Long) = FormatoDeFecha.fecha(instante, Idioma.ESPANOL)

    private fun hora(instante: Long) = FormatoDeFecha.hora(instante, Idioma.ESPANOL)

    @Test
    fun en_evaluacion_muestra_desde_cuando_y_el_tiempo_estimado() {
        montar(DatosDeConsulta.detalle(desdeCache = false).copy(estimacion = EstimacionDeResolucion(nivel = 2, diasHabiles = 5)))

        val dias = contexto.resources.getQuantityString(R.plurals.siniestros_seguimiento_dias_habiles, 5, 5)
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_estimacion_con_nivel, 2, dias), substring = true)
            .performScrollTo().assertIsDisplayed()
        val inicio = DatosDeConsulta.cambios.last().en
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_etapa_desde, fecha(inicio), hora(inicio)), substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun el_siniestro_parametrico_se_identifica_como_resolucion_automatica_con_su_parametro_y_valor() {
        montar(DatosDeConsulta.detalleParametrico())

        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_resolucion_automatica)).assertIsDisplayed()
        val minutos = texto(R.string.siniestros_seguimiento_unidad_minutos)
        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_seguimiento_parametro_objetivo)}. " +
                texto(R.string.siniestros_seguimiento_parametro_retraso_vuelo, "AV-8421"),
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_seguimiento_umbral)}. ${texto(R.string.siniestros_seguimiento_umbral_valor, "180", minutos)}",
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_valor_observado), substring = true)
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_evento_observado, "215", minutos), substring = true)
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_etapa_validacion_automatica), substring = true)
            .performScrollTo().assertIsDisplayed()
        // La línea automática no tiene las etapas de un caso evaluado por un analista.
        assertEquals(
            0,
            composicion.onAllNodesWithContentDescription(texto(R.string.siniestros_seguimiento_etapa_revision), substring = true)
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun un_siniestro_evaluado_por_un_analista_no_lleva_el_rotulo_automatico() {
        montar(DatosDeConsulta.detalle(desdeCache = false))

        assertEquals(
            0,
            composicion.onAllNodesWithText(texto(R.string.siniestros_seguimiento_resolucion_automatica)).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun sin_conexion_muestra_la_version_guardada_con_su_hora_y_no_el_aviso_de_la_cache() {
        val guardadoEn = DatosDeConsulta.AHORA - 3_600_000L
        montar(DatosDeConsulta.detalle(desdeCache = false).copy(frescura = null, copiaLocalDe = guardadoEn))

        val cuando = texto(R.string.siniestros_seguimiento_hoy_completo, fecha(guardadoEn), hora(guardadoEn))
        // El aviso se anuncia entero: título y texto.
        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_seguimiento_sin_conexion_titulo)}. " +
                texto(R.string.siniestros_seguimiento_sin_conexion_texto, cuando),
        ).assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            texto(
                R.string.siniestros_seguimiento_pie_sin_conexion,
                texto(R.string.siniestros_seguimiento_fecha_y_hora, fecha(guardadoEn), hora(guardadoEn)),
            ),
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText("SIN-2026-01655").assertIsDisplayed()
        assertEquals(
            0,
            composicion.onAllNodesWithText(texto(R.string.siniestros_seguimiento_cache_titulo), substring = true)
                .fetchSemanticsNodes().size,
        )
    }
}
