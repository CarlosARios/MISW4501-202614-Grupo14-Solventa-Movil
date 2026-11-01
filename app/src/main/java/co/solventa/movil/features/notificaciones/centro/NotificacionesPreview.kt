package co.solventa.movil.features.notificaciones.centro

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de M15 y M10b con el pago del prototipo (HU-M3.1-01). Son
 * herramienta de diseño: no corren en la aplicación.
 */
private const val PAGADO_PREVIA = 1_789_162_800_000L // 11/09/2026 21:40 UTC

private val pagoPrevia = Notificacion(
    id = "ntf-ppo-1",
    tipo = TiposDeNotificacion.PAGO_PARAMETRICO,
    ocurrioEn = PAGADO_PREVIA,
    leidaEn = null,
    pago = PagoParametrico(
        id = "ppo-1", siniestro = "SIN-2026-01702", poliza = "VJ-2026-000931", monto = 180_000.0, moneda = "COP",
        disparador = Disparador("flight_delay", "AV-8421", 180.0, 215.0, "minutes", PAGADO_PREVIA - 60_000),
        medioDePago = MedioDePago("visa", "1111"), claveDeIdempotencia = "pay-ppo-1", pagadoEn = PAGADO_PREVIA,
    ),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CentroPreview() {
    SolventaTheme {
        ContenidoCentro(
            estado = CentroUiState(
                cargando = false,
                hoy = listOf(pagoPrevia),
                anteriores = listOf(pagoPrevia.copy(id = "ntf-ppo-0", ocurrioEn = PAGADO_PREVIA - 518_400_000, leidaEn = PAGADO_PREVIA)),
                sinLeer = 1,
            ),
            idioma = Idioma.ESPANOL,
            ahora = PAGADO_PREVIA,
            onAtras = {},
            onAbrir = {},
            onMarcarTodas = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun DetallePagoPreview() {
    SolventaTheme {
        ContenidoDetallePago(estado = DetallePagoUiState(cargando = false, notificacion = pagoPrevia), idioma = Idioma.ESPANOL, onAtras = {})
    }
}
