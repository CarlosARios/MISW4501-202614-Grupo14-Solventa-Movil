package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de M8 y M8b con los datos del prototipo (frames 37:1233,
 * 37:1418 y 38:1503). Son herramienta de diseño: no corren en la aplicacion.
 */
private const val OTORGADO_PREVIA = 1_789_573_920_000L // 16/09/2026 15:52 UTC
private const val VENCE_PREVIA = 1_821_109_920_000L

private val activoPrevia = Consentimiento(
    id = "cns-4f1c2a9be0d34c6f8a7e5d1b2c3a4f5e",
    estado = EstadosDeConsentimiento.ACTIVO,
    vigente = true,
    entidades = listOf(Entidad("aliado-banco-1", "Banco Aliado")),
    finalidad = "risk_profiling_mortgage_life_offer",
    canal = "embedded",
    versionDeTerminos = "v1",
    alcance = listOf(
        Fuente("open_finance", listOf("credit_products", "balances", "payment_behavior"), "12_months"),
        Fuente("open_data", listOf("public_records"), "12_months"),
        Fuente("kyc", listOf("identity_verification"), "single_use"),
    ),
    otorgadoEn = OTORGADO_PREVIA,
    venceEn = VENCE_PREVIA,
    finalizadoEn = null,
    comprobante = null,
    historial = listOf(CambioDeEstado(CambiosDeEstado.OTORGADO, OTORGADO_PREVIA, null, "embedded", null)),
)

private val revocadoPrevia = activoPrevia.copy(
    id = "cns-9a8b7c6d5e4f30211234567890abcdef",
    estado = EstadosDeConsentimiento.REVOCADO,
    vigente = false,
    finalizadoEn = OTORGADO_PREVIA + 3_000_000_000L,
    comprobante = "REV-2026-0012",
    historial = activoPrevia.historial + CambioDeEstado(
        CambiosDeEstado.REVOCADO, OTORGADO_PREVIA + 3_000_000_000L, "REV-2026-0012", "mobile", null,
    ),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ConsentimientosConContenidoPreview() {
    SolventaTheme {
        ContenidoConsentimientos(
            estado = ConsentimientosUiState(cargando = false, activos = listOf(activoPrevia), historial = listOf(revocadoPrevia)),
            idioma = Idioma.ESPANOL,
            onAtras = {},
            onAbrir = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ConsentimientosSinConexionPreview() {
    SolventaTheme {
        ContenidoConsentimientos(
            estado = ConsentimientosUiState(
                cargando = false,
                activos = listOf(activoPrevia),
                sinConexion = true,
                sincronizadoEn = OTORGADO_PREVIA,
            ),
            idioma = Idioma.ESPANOL,
            onAtras = {},
            onAbrir = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsentimientosSinVincularPreview() {
    SolventaTheme {
        ContenidoConsentimientos(
            estado = ConsentimientosUiState(cargando = false, cuentaSinVincular = true),
            idioma = Idioma.ESPANOL,
            onAtras = {},
            onAbrir = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun DetalleActivoPreview() {
    SolventaTheme {
        ContenidoDetalleConsentimiento(
            estado = DetalleConsentimientoUiState(cargando = false, consentimiento = activoPrevia),
            idioma = Idioma.ESPANOL,
            onAtras = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun DetalleRevocadoPreview() {
    SolventaTheme {
        ContenidoDetalleConsentimiento(
            estado = DetalleConsentimientoUiState(cargando = false, consentimiento = revocadoPrevia),
            idioma = Idioma.INGLES,
            onAtras = {},
        )
    }
}
