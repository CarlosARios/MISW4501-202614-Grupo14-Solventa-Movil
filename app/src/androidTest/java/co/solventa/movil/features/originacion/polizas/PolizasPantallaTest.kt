package co.solventa.movil.features.originacion.polizas

import androidx.compose.ui.test.assertIsDisplayed
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
 * M4 · Mis pólizas (HU-M1.2-01) con los datos del prototipo: las tarjetas y
 * su insignia, el indicador de la última sincronización, el aviso sin
 * conexión con el ícono de sin red (criterio 1), el aviso de cambios
 * (criterio 4) y la advertencia de la copia desactualizada (criterio 5).
 */
class PolizasPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private var abierta: String? = null
    private var reintentos = 0
    private var cierres = 0

    private fun montar(estado: PolizasUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoPolizas(
                    estado = estado,
                    idioma = Idioma.ESPANOL,
                    onAbrir = { abierta = it },
                    onReintentar = { reintentos++ },
                    onCerrarCambios = { cierres++ },
                )
            }
        }
    }

    private fun conContenido(sinConexion: Boolean = false) = PolizasUiState(
        cargando = false,
        polizas = listOf(DatosDePantalla.vida, DatosDePantalla.autosPorVencer, DatosDePantalla.cancelada),
        sinConexion = sinConexion,
        sincronizadoEn = DatosDePantalla.SINCRONIZADO,
    )

    private fun fecha() = FormatoDeFecha.fecha(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL)

    private fun hora() = FormatoDeFecha.hora(DatosDePantalla.SINCRONIZADO, Idioma.ESPANOL)

    @Test
    fun con_conexion_indica_la_ultima_sincronizacion_y_lista_las_polizas() {
        montar(conContenido())

        composicion.onNodeWithContentDescription(texto(R.string.originacion_polizas_ultima_sincronizacion, fecha(), hora()))
            .assertIsDisplayed()
        composicion.onNodeWithText("Vida hipotecario").assertIsDisplayed()
        composicion.onNodeWithText("$ 250.000.000").assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.originacion_polizas_situacion_por_vencer)).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.originacion_polizas_sin_cobertura)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sin_conexion_avisa_la_fecha_y_hora_de_la_copia_y_sigue_mostrando_las_polizas() {
        montar(conContenido(sinConexion = true))

        composicion.onNodeWithContentDescription(texto(R.string.originacion_polizas_sin_conexion, fecha(), hora()), substring = true)
            .assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.originacion_polizas_sin_conexion_icono)).assertIsDisplayed()
        composicion.onNodeWithText("Vida hipotecario").assertIsDisplayed()
    }

    @Test
    fun tocar_una_tarjeta_abre_su_detalle() {
        montar(conContenido())

        composicion.onNodeWithText("Vida hipotecario").performClick()

        assertEquals("VH-2026-004512", abierta)
    }

    @Test
    fun avisa_los_cambios_detectados_y_se_puede_cerrar() {
        val renovada = DatosDePantalla.autosPorVencer.copy(renovada = true)
        montar(
            conContenido().copy(
                polizas = listOf(renovada),
                cambios = listOf(CambioDePoliza(renovada.poliza, TipoDeCambio.RENOVADA), CambioDePoliza(DatosDePantalla.cancelada.poliza, TipoDeCambio.CANCELADA)),
            ),
        )

        val titulo = contexto.resources.getQuantityString(R.plurals.originacion_polizas_cambios_titulo, 2, 2)
        composicion.onNodeWithContentDescription(titulo, substring = true).assertIsDisplayed()
        composicion.onNodeWithText(
            texto(R.string.originacion_polizas_situacion_renovada, texto(R.string.originacion_polizas_situacion_por_vencer)),
        ).performScrollTo().assertIsDisplayed()

        composicion.onNodeWithContentDescription(texto(R.string.originacion_polizas_cambios_cerrar)).performClick()
        assertEquals(1, cierres)
    }

    @Test
    fun advierte_la_copia_desactualizada_sin_esconder_las_polizas() {
        montar(conContenido().copy(desactualizada = true))

        composicion.onNodeWithContentDescription(texto(R.string.originacion_polizas_desactualizada_titulo), substring = true)
            .assertIsDisplayed()
        composicion.onNodeWithText("Vida hipotecario").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun un_error_sin_copia_ofrece_reintentar() {
        montar(PolizasUiState(cargando = false, error = true))

        composicion.onNodeWithText(texto(R.string.originacion_polizas_reintentar)).performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun sin_polizas_muestra_el_estado_vacio() {
        montar(PolizasUiState(cargando = false, sincronizadoEn = DatosDePantalla.SINCRONIZADO))

        composicion.onNodeWithText(texto(R.string.originacion_polizas_vacio)).assertIsDisplayed()
    }
}
