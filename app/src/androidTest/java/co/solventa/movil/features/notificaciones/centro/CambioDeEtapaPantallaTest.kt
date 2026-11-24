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
 * M15 · Centro de notificaciones con el aviso de cada cambio de etapa del
 * siniestro (HU-M3.3-01): la etapa, el radicado, la fecha y hora y, cuando
 * aplica, el monto; tocarlo lleva a la línea de tiempo.
 */
class CambioDeEtapaPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private val cambio = 1_789_569_000_000L // 2026-09-16T14:30:00Z

    private fun etapa(etapa: String, monto: Double? = null) = Notificacion(
        id = "ntf-stage-$etapa", tipo = TiposDeNotificacion.CAMBIO_DE_ETAPA, ocurrioEn = cambio, leidaEn = null, pago = null,
        etapa = CambioDeEtapa(
            cambio = 42, siniestroId = "8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90", radicado = "SIN-2026-01655",
            poliza = "HG-2025-880231", etapa = etapa, cambioEn = cambio, monto = monto, moneda = monto?.let { "COP" },
            pagadoEn = if (etapa == EtapasDeSiniestro.PAGADO) cambio else null,
        ),
    )

    private var abierta: DestinoDeNotificacion? = null

    private fun montar(vararg notificaciones: Notificacion) {
        composicion.setContent {
            SolventaTheme {
                ContenidoCentro(
                    estado = CentroUiState(cargando = false, hoy = notificaciones.toList(), sinLeer = notificaciones.size),
                    idioma = Idioma.ESPANOL, ahora = cambio, onAtras = {}, onAbrir = { abierta = it },
                    onMarcarTodas = {}, onReintentar = {},
                )
            }
        }
    }

    private fun cuerpo(id: Int) = texto(id, "SIN-2026-01655", FormatoDeFecha.fecha(cambio, Idioma.ESPANOL), FormatoDeFecha.hora(cambio, Idioma.ESPANOL))

    @Test
    fun en_evaluacion_dice_la_etapa_el_radicado_y_cuando_criterios_1_y_5() {
        montar(etapa(EtapasDeSiniestro.EN_EVALUACION))

        composicion.onNodeWithText(texto(R.string.notificaciones_etapa_titulo_evaluacion)).assertIsDisplayed()
        composicion.onNodeWithText(cuerpo(R.string.notificaciones_etapa_cuerpo_evaluacion)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.notificaciones_etapa_ver_linea_de_tiempo)).assertIsDisplayed()
    }

    @Test
    fun aprobado_y_pagado_llevan_el_monto_criterios_2_y_3() {
        montar(etapa(EtapasDeSiniestro.PAGADO, 4_132_500.0), etapa(EtapasDeSiniestro.APROBADO, 4_132_500.0))

        composicion.onNodeWithText(texto(R.string.notificaciones_etapa_titulo_aprobado_monto, "$ 4.132.500 COP")).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.notificaciones_etapa_titulo_pagado_monto, "$ 4.132.500 COP")).assertIsDisplayed()
        composicion.onNodeWithText(cuerpo(R.string.notificaciones_etapa_cuerpo_pagado)).assertIsDisplayed()
    }

    @Test
    fun tocarla_lleva_a_la_linea_de_tiempo_del_siniestro_criterio_5() {
        montar(etapa(EtapasDeSiniestro.EN_EVALUACION))

        composicion.onNodeWithText(texto(R.string.notificaciones_etapa_titulo_evaluacion)).performClick()

        assertEquals(DestinoDeNotificacion.Siniestro("8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90", "ntf-stage-in_evaluation"), abierta)
    }
}
