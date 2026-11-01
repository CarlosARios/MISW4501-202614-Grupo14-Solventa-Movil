package co.solventa.movil.features.originacion.polizas

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de M4 y M5 con los datos del prototipo (mockups de
 * HU-M1.2-01: con conexión, sin conexión y cambios detectados). Son
 * herramienta de diseño: no corren en la aplicación.
 */
private const val SINCRONIZADO_PREVIA = 1_789_569_000_000L // 16/09/2026 14:30 UTC
private const val DIA_PREVIA = 86_400_000L

private val aseguradoPrevia = Asegurado("Carolina Pérez Ruiz", "CC", "1020304050")

private val vidaPrevia = Poliza(
    id = "VH-2026-004512", origen = "policy_core", linea = LineasDePoliza.VIDA, producto = "Vida hipotecario",
    objetoAsegurado = null, estado = EstadosDePoliza.ACTIVA, asegurado = aseguradoPrevia, prima = 68_500.0, moneda = "COP",
    valorAsegurado = 250_000_000.0, coberturas = listOf(Cobertura("death", 250_000_000.0)),
    inicio = SINCRONIZADO_PREVIA, fin = SINCRONIZADO_PREVIA + 7_305 * DIA_PREVIA, renovacionDe = null,
)

private val autosPrevia = Poliza(
    id = "AU-2025-118204", origen = "policy_core", linea = LineasDePoliza.AUTOS, producto = "Autos",
    objetoAsegurado = "KJT-482", estado = EstadosDePoliza.ACTIVA, asegurado = aseguradoPrevia, prima = null, moneda = "COP",
    valorAsegurado = 72_500_000.0,
    coberturas = listOf(
        Cobertura("collision", 72_500_000.0), Cobertura("theft", 72_500_000.0), Cobertura("third_party_damage", 72_500_000.0),
    ),
    inicio = SINCRONIZADO_PREVIA - 349 * DIA_PREVIA, fin = SINCRONIZADO_PREVIA + 16 * DIA_PREVIA, renovacionDe = null,
)

private val viajePrevia = Poliza(
    id = "VJ-2026-000931", origen = "policy_core", linea = LineasDePoliza.VIAJE, producto = "Viaje",
    objetoAsegurado = "Retraso de vuelo", estado = EstadosDePoliza.ACTIVA, asegurado = aseguradoPrevia, prima = null,
    moneda = "COP", valorAsegurado = 180_000.0, coberturas = listOf(Cobertura("flight_delay", 180_000.0)),
    inicio = SINCRONIZADO_PREVIA - 6 * DIA_PREVIA, fin = SINCRONIZADO_PREVIA + 4 * DIA_PREVIA, renovacionDe = null,
)

private val hogarPrevia = Poliza(
    id = "HG-2025-880231", origen = "policy_core", linea = LineasDePoliza.HOGAR, producto = "Hogar",
    objetoAsegurado = null, estado = EstadosDePoliza.ACTIVA, asegurado = aseguradoPrevia, prima = null, moneda = "COP",
    valorAsegurado = 320_000_000.0, coberturas = listOf(Cobertura("water_damage", 320_000_000.0)),
    inicio = SINCRONIZADO_PREVIA - 199 * DIA_PREVIA, fin = SINCRONIZADO_PREVIA + 166 * DIA_PREVIA, renovacionDe = null,
)

private val canceladaPrevia = autosPrevia.copy(id = "AU-2024-093377", estado = EstadosDePoliza.CANCELADA)

private fun estadoPrevia(sinConexion: Boolean = false, cambios: List<CambioDePoliza> = emptyList(), lista: List<Poliza>) =
    PolizasUiState(
        cargando = false,
        polizas = PresentacionDePolizas.tarjetas(lista, cambios, SINCRONIZADO_PREVIA),
        sinConexion = sinConexion,
        sincronizadoEn = SINCRONIZADO_PREVIA,
        cambios = cambios,
    )

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun PolizasConConexionPreview() {
    SolventaTheme {
        ContenidoPolizas(
            estado = estadoPrevia(lista = listOf(vidaPrevia, autosPrevia, viajePrevia, hogarPrevia, canceladaPrevia)),
            idioma = Idioma.ESPANOL,
            onAbrir = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun PolizasSinConexionPreview() {
    SolventaTheme {
        ContenidoPolizas(
            estado = estadoPrevia(sinConexion = true, lista = listOf(vidaPrevia, autosPrevia, viajePrevia, hogarPrevia)),
            idioma = Idioma.ESPANOL,
            onAbrir = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun PolizasConCambiosPreview() {
    val renovada = autosPrevia.copy(fin = autosPrevia.fin + 365 * DIA_PREVIA)
    val cancelada = hogarPrevia.copy(estado = EstadosDePoliza.CANCELADA)
    SolventaTheme {
        ContenidoPolizas(
            estado = estadoPrevia(
                cambios = listOf(CambioDePoliza(renovada, TipoDeCambio.RENOVADA), CambioDePoliza(cancelada, TipoDeCambio.CANCELADA)),
                lista = listOf(vidaPrevia, renovada, viajePrevia, cancelada),
            ),
            idioma = Idioma.ESPANOL,
            onAbrir = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun DetallePolizaSinConexionPreview() {
    SolventaTheme {
        ContenidoDetallePoliza(
            estado = DetallePolizaUiState(
                cargando = false,
                poliza = PolizaEnLista(autosPrevia, Situacion.POR_VENCER),
                sinConexion = true,
                sincronizadoEn = SINCRONIZADO_PREVIA,
            ),
            idioma = Idioma.ESPANOL,
            onAtras = {},
        )
    }
}
