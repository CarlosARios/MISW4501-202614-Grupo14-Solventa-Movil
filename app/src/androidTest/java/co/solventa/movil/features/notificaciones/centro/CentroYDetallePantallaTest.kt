package co.solventa.movil.features.notificaciones.centro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * M15 · Centro de notificaciones y M10b · Pago paramétrico (HU-M3.1-01) con el
 * pago del prototipo: $ 180.000 COP por el vuelo AV-8421, 215 min de retraso.
 */
class CentroYDetallePantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private val pagado = 1_789_162_800_000L // 2026-09-11T21:40:00Z

    private fun pago(id: String = "ntf-ppo-1", leida: Boolean = false, ocurrio: Long = pagado) = Notificacion(
        id = id, tipo = TiposDeNotificacion.PAGO_PARAMETRICO, ocurrioEn = ocurrio, leidaEn = if (leida) ocurrio else null,
        pago = PagoParametrico(
            id = "ppo-1", siniestro = "SIN-2026-01702", poliza = "VJ-2026-000931", monto = 180_000.0, moneda = "COP",
            disparador = Disparador("flight_delay", "AV-8421", 180.0, 215.0, "minutes", ocurrio),
            medioDePago = MedioDePago("visa", "1111"), claveDeIdempotencia = "pay-ppo-1", pagadoEn = ocurrio,
        ),
    )

    private var abierta: String? = null
    private var marcadas = 0

    private fun montarCentro(estado: CentroUiState) {
        composicion.setContent {
            SolventaTheme {
                ContenidoCentro(
                    estado = estado, idioma = Idioma.ESPANOL, ahora = pagado, onAtras = {}, onAbrir = { abierta = it },
                    onMarcarTodas = { marcadas++ }, onReintentar = {},
                )
            }
        }
    }

    @Test
    fun el_centro_muestra_el_pago_con_monto_moneda_y_evento_y_abre_su_detalle() {
        montarCentro(CentroUiState(cargando = false, hoy = listOf(pago()), sinLeer = 1))

        composicion.onNodeWithText(texto(R.string.notificaciones_centro_hoy).uppercase()).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.notificaciones_aviso_titulo, "$ 180.000 COP")).assertIsDisplayed()
        composicion.onNodeWithText(
            texto(R.string.notificaciones_centro_cuerpo_retraso_de_vuelo, "AV-8421", "215", "Visa •••• 1111"),
        ).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.notificaciones_centro_sin_leer)).assertIsDisplayed()

        composicion.onNodeWithText(texto(R.string.notificaciones_aviso_titulo, "$ 180.000 COP")).performClick()
        assertEquals("ntf-ppo-1", abierta)
    }

    @Test
    fun marcar_todas_como_leidas() {
        montarCentro(CentroUiState(cargando = false, hoy = listOf(pago()), sinLeer = 1))

        composicion.onNodeWithText(texto(R.string.notificaciones_centro_marcar_todas)).performClick()

        assertEquals(1, marcadas)
    }

    @Test
    fun varios_pagos_se_ven_todos_entre_hoy_y_anteriores() {
        montarCentro(
            CentroUiState(
                cargando = false,
                hoy = listOf(pago("ntf-1"), pago("ntf-2")),
                anteriores = listOf(pago("ntf-3", leida = true, ocurrio = pagado - 2 * 86_400_000L)),
                sinLeer = 2,
            ),
        )

        composicion.onNodeWithText(texto(R.string.notificaciones_centro_anteriores).uppercase()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sin_notificaciones_muestra_el_estado_vacio() {
        montarCentro(CentroUiState(cargando = false))

        composicion.onNodeWithText(texto(R.string.notificaciones_centro_vacio)).assertIsDisplayed()
    }

    @Test
    fun el_detalle_muestra_umbral_valor_observado_y_clave_de_idempotencia() {
        composicion.setContent {
            SolventaTheme {
                ContenidoDetallePago(
                    estado = DetallePagoUiState(cargando = false, notificacion = pago()),
                    idioma = Idioma.ESPANOL,
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithContentDescription("${texto(R.string.notificaciones_pago_monto)}: $ 180.000 COP").assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            "${texto(R.string.notificaciones_pago_evento)}: ${texto(R.string.notificaciones_evento_retraso_de_vuelo, "AV-8421")}",
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            "${texto(R.string.notificaciones_pago_umbral)}: ${texto(R.string.notificaciones_unidad_minutos, "180")}",
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            "${texto(R.string.notificaciones_pago_observado)}: ${texto(R.string.notificaciones_unidad_minutos, "215")}",
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.notificaciones_pago_clave)}: pay-ppo-1")
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.notificaciones_pago_medio)}: Visa •••• 1111")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun un_detalle_que_no_esta_lo_dice() {
        composicion.setContent {
            SolventaTheme {
                ContenidoDetallePago(estado = DetallePagoUiState(cargando = false, noDisponible = true), idioma = Idioma.ESPANOL, onAtras = {})
            }
        }

        composicion.onNodeWithText(texto(R.string.notificaciones_pago_no_disponible)).assertIsDisplayed()
    }
}
