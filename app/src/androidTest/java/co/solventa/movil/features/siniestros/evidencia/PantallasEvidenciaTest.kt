package co.solventa.movil.features.siniestros.evidencia

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R

import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * HU-M2.1-01 · captura de evidencia con geoetiquetado. Las pantallas se
 * dibujan con un estado fijo y una cámara de mentira: lo que se prueba es lo
 * que el asegurado ve y lo que puede tocar, no CameraX.
 */
class PantallasEvidenciaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg argumentos: Any) = contexto.getString(id, *argumentos)

    private val ubicacionConfiable = Ubicacion(
        latitud = 4.6782,
        longitud = -74.0482,
        altitud = 2600.0,
        precisionM = 12f,
        leidaEn = System.currentTimeMillis(),
    )

    private fun foto(i: Int) = EvidenciaDelReporte(
        id = "ev-$i",
        tipo = TipoDeEvidencia.FOTO,
        rutaArchivo = "/tmp/ev-$i.jpg",
        capturadaEn = 0L,
        ubicacion = ubicacionConfiable,
        duracionS = 0,
        estado = EstadoDeEvidencia.SUBIDA,
    )

    private fun camara(estado: EvidenciaUiState, onTomarFoto: () -> Unit = {}) {
        composicion.setContent {
            SolventaTheme {
                PantallaCamaraDeEvidencia(
                    estado = estado,
                    camara = camaraDeEvidenciaSimulada(EstadoDeLaCaptura.LISTA),
                    tipoDeSiniestro = "Colisión",
                    onCerrar = {},
                    onTomarFoto = onTomarFoto,
                    onGrabarVideo = {},
                    onEsperarSenal = {},
                    onCapturarIgual = {},
                )
            }
        }
    }

    // Criterio 1 — se piden cámara y ubicación explicando para qué.
    @Test
    fun los_permisos_se_piden_explicando_para_que_se_usan() {
        var permitio = false
        composicion.setContent {
            SolventaTheme {
                DialogoDePermisosDeCaptura(
                    estado = EstadoDeLaCaptura.SIN_PERMISOS,
                    onPermitir = { permitio = true },
                    onNoPermitir = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.evidencia_permisos_titulo)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.evidencia_permisos_detalle)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.evidencia_permisos_permitir)).performClick()
        assertTrue(permitio)
    }

    // Criterio 1 — sin permisos no hay evidencia verificable, y se dice.
    @Test
    fun sin_permisos_se_informa_que_no_hay_evidencia_verificable() {
        composicion.setContent {
            SolventaTheme {
                DialogoDePermisosDeCaptura(
                    estado = EstadoDeLaCaptura.PERMISOS_NEGADOS,
                    onPermitir = {},
                    onNoPermitir = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.evidencia_permisos_negados_titulo)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.evidencia_permisos_negados_detalle)).assertIsDisplayed()
    }

    // Criterio 2 — la galería no es una alternativa.
    @Test
    fun la_galeria_se_ve_pero_no_se_puede_usar() {
        camara(EvidenciaUiState(ubicacion = ubicacionConfiable))

        composicion.onNodeWithContentDescription(texto(R.string.evidencia_galeria_no_disponible))
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }

    // Criterio 3 — se avisa la precisión antes de capturar y se puede esperar o seguir.
    @Test
    fun una_ubicacion_imprecisa_se_avisa_con_su_precision_y_dos_salidas() {
        var espero = false
        var siguio = false
        composicion.setContent {
            SolventaTheme {
                HojaDePrecisionInsuficiente(
                    precisionM = 120f,
                    onEsperar = { espero = true },
                    onCapturarIgual = { siguio = true },
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.evidencia_precision_titulo)).assertIsDisplayed()
        composicion.onNodeWithText(
            texto(R.string.evidencia_precision_detalle, 120, Ubicacion.PRECISION_MAXIMA_M.toInt()),
        ).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.evidencia_precision_esperar)).performClick()
        composicion.onNodeWithText(texto(R.string.evidencia_precision_capturar_igual)).performClick()
        assertTrue(espero)
        assertTrue(siguio)
    }

    @Test
    fun sin_lectura_de_ubicacion_tambien_se_avisa() {
        composicion.setContent {
            SolventaTheme {
                HojaDePrecisionInsuficiente(precisionM = null, onEsperar = {}, onCapturarIgual = {})
            }
        }

        composicion.onNodeWithText(texto(R.string.evidencia_precision_sin_lectura)).assertIsDisplayed()
    }

    // Criterio 4 — el sello muestra la ubicación y su precisión al capturar.
    @Test
    fun el_sello_muestra_la_ubicacion_con_su_precision() {
        camara(EvidenciaUiState(ubicacion = ubicacionConfiable))

        // El sello se dibuja en mayúsculas, como en el frame M12.
        composicion.onNodeWithText("±12 m", substring = true, ignoreCase = true).assertIsDisplayed()
        composicion.onNodeWithText(
            "0482 " + texto(R.string.evidencia_oeste),
            substring = true,
            ignoreCase = true,
        ).assertIsDisplayed()
    }

    @Test
    fun sin_ubicacion_el_sello_lo_dice() {
        camara(EvidenciaUiState(ubicacion = null))

        composicion.onNodeWithText(texto(R.string.evidencia_sello_sin_ubicacion), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun el_disparador_toma_la_foto() {
        var tomo = false
        camara(EvidenciaUiState(ubicacion = ubicacionConfiable), onTomarFoto = { tomo = true })

        composicion.onNodeWithContentDescription(texto(R.string.evidencia_tomar_foto))
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
        assertTrue(tomo)
    }

    // Criterio 5 — tamaño acotado: con el máximo de fotos no se toma otra.
    @Test
    fun con_el_maximo_de_fotos_el_disparador_queda_inerte() {
        val llenas = List(LimitesDeEvidencia.MAXIMO_DE_FOTOS) { foto(it) }
        camara(EvidenciaUiState(evidencias = llenas, ubicacion = ubicacionConfiable))

        composicion.onNodeWithContentDescription(texto(R.string.evidencia_tomar_foto)).assertIsNotEnabled()
    }

    @Test
    fun mientras_graba_se_avisa_y_el_disparador_detiene() {
        camara(EvidenciaUiState(ubicacion = ubicacionConfiable, grabando = true))

        composicion.onNodeWithText(texto(R.string.evidencia_grabando)).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.evidencia_detener)).assertIsEnabled()
    }
}
