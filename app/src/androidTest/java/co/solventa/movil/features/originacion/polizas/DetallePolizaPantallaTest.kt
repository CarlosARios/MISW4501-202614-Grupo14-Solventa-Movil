package co.solventa.movil.features.originacion.polizas

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Rule
import org.junit.Test

/**
 * M5 · Detalle de la póliza (HU-M1.2-01, criterio 2): número, tipo de
 * seguro, vigencia, coberturas con sus montos y asegurado principal, también
 * sin conexión.
 */
class DetallePolizaPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private fun montar(estado: DetallePolizaUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoDetallePoliza(estado = estado, idioma = Idioma.ESPANOL, onAtras = {})
            }
        }
    }

    @Test
    fun sin_conexion_muestra_todo_lo_que_pide_el_criterio_2() {
        val p = DatosDePantalla.autosPorVencer
        montar(DetallePolizaUiState(cargando = false, poliza = p, sinConexion = true, sincronizadoEn = DatosDePantalla.SINCRONIZADO))

        val fecha = { instante: Long -> FormatoDeFecha.fecha(instante, Idioma.ESPANOL) }
        composicion.onNodeWithContentDescription(
            texto(R.string.originacion_poliza_sin_conexion, fecha(DatosDePantalla.SINCRONIZADO), FormatoDeFecha.hora(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL)),
        ).assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.originacion_poliza_numero)}: AU-2025-118204").assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.originacion_poliza_tipo)}: ${texto(R.string.originacion_polizas_linea_autos)}")
            .assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            "${texto(R.string.originacion_poliza_vigencia)}: " +
                texto(R.string.originacion_poliza_vigencia_valor, fecha(p.poliza.inicio), fecha(p.poliza.fin)),
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.originacion_polizas_cobertura_choque)}: $ 72.500.000")
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.originacion_polizas_cobertura_hurto)}: $ 72.500.000")
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription("Carolina Pérez Ruiz, CC 1020304050").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun una_poliza_con_prima_la_muestra() {
        montar(DetallePolizaUiState(cargando = false, poliza = DatosDePantalla.vida, sincronizadoEn = DatosDePantalla.SINCRONIZADO))

        composicion.onNodeWithContentDescription("${texto(R.string.originacion_poliza_prima)}: $ 68.500").performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.originacion_polizas_cobertura_muerte)}: $ 250.000.000")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun una_poliza_que_ya_no_esta_lo_dice() {
        montar(DetallePolizaUiState(cargando = false, noDisponible = true))

        composicion.onNodeWithText(texto(R.string.originacion_poliza_no_disponible)).assertIsDisplayed()
    }
}
