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
 * M9 · Mis siniestros (HU-M2.4-01) y su estado degradado M9c (HA-L02,
 * criterio 5: respuesta normal y respuesta desde la caché), con los datos del
 * prototipo.
 */
class MisSiniestrosPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    /** El título de la tarjeta en el idioma del dispositivo: «Hogar · Daño por agua». */
    private fun titulo(linea: Int, incidente: Int) =
        texto(R.string.siniestros_seguimiento_titulo_caso, texto(linea), texto(incidente))

    private val hogar get() = titulo(R.string.siniestros_seguimiento_linea_hogar, R.string.siniestros_seguimiento_incidente_agua)
    private val autos get() = titulo(R.string.siniestros_seguimiento_linea_autos, R.string.siniestros_seguimiento_incidente_colision)

    private var abierto: String? = null
    private var reintentos = 0

    private fun montar(estado: MisSiniestrosUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoMisSiniestros(
                    estado = estado,
                    idioma = Idioma.ESPANOL,
                    ahora = DatosDeConsulta.AHORA,
                    onAbrir = { abierto = it },
                    onReintentar = { reintentos++ },
                )
            }
        }
    }

    private fun lista(desdeCache: Boolean) = MisSiniestrosUiState(
        cargando = false,
        enCurso = listOf(DatosDeConsulta.autos, DatosDeConsulta.hogar),
        cerrados = listOf(DatosDeConsulta.pagado),
        frescura = Frescura(desdeCache, DatosDeConsulta.AHORA),
    )

    private fun hora() = FormatoDeFecha.hora(DatosDeConsulta.AHORA, Idioma.ESPANOL)

    @Test
    fun respuesta_normal_lista_en_curso_y_cerrados_sin_aviso_de_cache() {
        montar(lista(desdeCache = false))

        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_en_curso, 2).uppercase()).assertIsDisplayed()
        composicion.onNodeWithText(autos).assertIsDisplayed()
        composicion.onNodeWithText(hogar).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_estado_devuelto)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_cerrados, 1).uppercase()).performScrollTo().assertIsDisplayed()
        assertEquals(
            0,
            composicion.onAllNodesWithContentDescription(texto(R.string.siniestros_seguimiento_cache_titulo), substring = true)
                .fetchSemanticsNodes().size,
        )
        assertEquals(
            0,
            composicion.onAllNodesWithText(texto(R.string.siniestros_seguimiento_cache_hora, hora())).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun respuesta_desde_cache_avisa_con_la_hora_y_marca_cada_tarjeta() {
        montar(lista(desdeCache = true))

        composicion.onNodeWithContentDescription(texto(R.string.siniestros_seguimiento_cache_titulo), substring = true)
            .assertIsDisplayed()
        val seccion = texto(R.string.siniestros_seguimiento_seccion_en_cache, texto(R.string.siniestros_seguimiento_en_curso, 2))
        composicion.onNodeWithText(seccion.uppercase()).assertIsDisplayed()
        assertEquals(
            3,
            composicion.onAllNodesWithText(texto(R.string.siniestros_seguimiento_cache_hora, hora())).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun reintentar_ahora_pide_otra_consulta() {
        montar(lista(desdeCache = true))

        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_reintentar_ahora)).performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun una_tarjeta_abre_su_detalle() {
        montar(lista(desdeCache = false))

        composicion.onNodeWithText(hogar).performClick()

        assertEquals(DatosDeConsulta.hogar.id, abierto)
    }

    @Test
    fun sin_siniestros_y_sin_red() {
        montar(MisSiniestrosUiState(cargando = false))
        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_vacio)).assertIsDisplayed()
    }

    @Test
    fun sin_red_y_sin_lista_ofrece_reintentar() {
        montar(MisSiniestrosUiState(cargando = false, fallo = FalloDeConsulta.SIN_CONEXION))

        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_sin_conexion)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_reintentar)).performClick()
        assertEquals(1, reintentos)
    }
}
