package co.solventa.movil.features.notificaciones.centro

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de M15 y M10b con el pago del prototipo (HU-M3.1-01) y los
 * cambios de etapa del siniestro (HU-M3.3-01). Son
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

/** El avance de SIN-2026-01655 (HU-M3.3-01): en evaluación y aprobado con el monto. */
private fun etapaPrevia(etapa: String, monto: Double?, leida: Boolean) = Notificacion(
    id = "ntf-stage-$etapa", tipo = TiposDeNotificacion.CAMBIO_DE_ETAPA, ocurrioEn = PAGADO_PREVIA,
    leidaEn = if (leida) PAGADO_PREVIA else null, pago = null,
    etapa = CambioDeEtapa(
        cambio = 42, siniestroId = "8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90", radicado = "SIN-2026-01655",
        poliza = "HG-2025-880231", etapa = etapa, cambioEn = PAGADO_PREVIA, monto = monto, moneda = monto?.let { "COP" },
        pagadoEn = null,
    ),
)

@Preview(name = "M15 · Cambios de etapa del siniestro", showBackground = true, heightDp = 900)
@Composable
private fun CentroConEtapasPreview() {
    SolventaTheme {
        ContenidoCentro(
            estado = CentroUiState(
                cargando = false,
                hoy = listOf(
                    etapaPrevia(EtapasDeSiniestro.APROBADO, 4_132_500.0, leida = false),
                    etapaPrevia(EtapasDeSiniestro.EN_EVALUACION, null, leida = true),
                    pagoPrevia,
                ),
                sinLeer = 2,
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
