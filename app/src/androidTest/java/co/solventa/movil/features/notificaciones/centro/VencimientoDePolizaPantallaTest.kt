package co.solventa.movil.features.notificaciones.centro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * M15 · Centro de notificaciones con la alerta de vencimiento de la póliza
 * (HU-M3.2-01): cuántos días faltan, la póliza y la fecha exacta del
 * vencimiento; tocarla lleva al detalle de la póliza (M5).
 */
class VencimientoDePolizaPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private val enviada = 1_789_569_000_000L // 2026-09-16T14:30:00Z
    private val vence = enviada + 15 * 86_400_000L

    private fun alerta(dias: Int, hito: Int = 15) = Notificacion(
        id = "ntf-expiry-AU-2025-118204-$hito", tipo = TiposDeNotificacion.VENCIMIENTO_DE_POLIZA, ocurrioEn = enviada,
        leidaEn = null, pago = null,
        vencimiento = AlertaDeVencimiento(
            poliza = "AU-2025-118204", linea = LineasDeVencimiento.AUTOS, producto = "Autos", objetoAsegurado = "KJT-482",
            venceEn = vence, hito = hito, diasRestantes = dias,
        ),
    )

    private var abierta: DestinoDeNotificacion? = null

    private fun montar(vararg notificaciones: Notificacion) {
        composicion.setContent {
            SolventaTheme {
                ContenidoCentro(
                    estado = CentroUiState(cargando = false, hoy = notificaciones.toList(), sinLeer = notificaciones.size),
                    idioma = Idioma.ESPANOL, ahora = enviada, onAtras = {}, onAbrir = { abierta = it },
                    onMarcarTodas = {}, onReintentar = {},
                )
            }
        }
    }

    @Test
    fun dice_los_dias_la_poliza_y_la_fecha_exacta_del_vencimiento_criterio_1() {
        montar(alerta(dias = 15))

        composicion.onNodeWithText(contexto.resources.getQuantityString(R.plurals.notificaciones_vencimiento_titulo, 15, 15))
            .assertIsDisplayed()
        composicion.onNodeWithText(
            texto(R.string.notificaciones_vencimiento_cuerpo, "AU-2025-118204", "Autos · KJT-482", FormatoDeFecha.fecha(vence, Idioma.ESPANOL)),
        ).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.notificaciones_vencimiento_ver_poliza)).assertIsDisplayed()
    }

    @Test
    fun el_dia_del_vencimiento_dice_que_vence_hoy() {
        montar(alerta(dias = 0, hito = 5))

        composicion.onNodeWithText(texto(R.string.notificaciones_vencimiento_titulo_hoy)).assertIsDisplayed()
    }

    @Test
    fun tocarla_lleva_al_detalle_de_la_poliza() {
        montar(alerta(dias = 15))

        composicion.onNodeWithText(texto(R.string.notificaciones_vencimiento_ver_poliza)).performClick()

        assertEquals(DestinoDeNotificacion.Poliza("AU-2025-118204", "ntf-expiry-AU-2025-118204-15"), abierta)
    }
}
