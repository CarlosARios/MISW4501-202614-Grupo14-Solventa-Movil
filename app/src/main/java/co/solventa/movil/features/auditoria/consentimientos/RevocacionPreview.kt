package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de la revocacion (HU-M1.3-02) con los datos del prototipo:
 * la hoja (37:1519, con huella y con contraseña), el revocado (38:1414), la
 * lista con la cola (38:1503) y el detalle pendiente y parcial. Son
 * herramienta de diseño: no corren en la aplicacion.
 */
private const val OTORGADO = 1_789_573_920_000L // 16/09/2026 15:52 UTC
private const val VENCE = 1_821_109_920_000L
private const val SOLICITADA = 1_789_593_004_000L // 16/09/2026 21:10:04 UTC
private const val EFECTIVA = SOLICITADA + 2_100L

private val activo = Consentimiento(
    id = "CONS-2026-55AA31",
    estado = EstadosDeConsentimiento.ACTIVO,
    vigente = true,
    entidades = listOf(Entidad("aliado-banco-1", "Banco Aliado")),
    finalidad = "risk_profiling_mortgage_life_offer",
    canal = "mobile",
    versionDeTerminos = "v1",
    alcance = listOf(Fuente("open_finance", listOf("credit_products", "balances", "payment_behavior"), "24_months")),
    otorgadoEn = OTORGADO,
    venceEn = VENCE,
    finalizadoEn = null,
    comprobante = null,
    historial = listOf(CambioDeEstado(CambiosDeEstado.OTORGADO, OTORGADO, null, "mobile", null)),
)

private fun revocacion(estadoDelAviso: String, entidad: String) = Revocacion(
    comprobante = "REV-2026-0088",
    estado = if (estadoDelAviso == EstadosDelAviso.PARCIAL) EstadosDeConsentimiento.REVOCADO_PARCIAL else EstadosDeConsentimiento.REVOCADO,
    solicitadaEn = SOLICITADA,
    efectivaEn = EFECTIVA,
    canal = "mobile",
    dispositivo = "Pixel 8",
    estadoDelAviso = estadoDelAviso,
    entidades = listOf(EntidadDeRevocacion("aliado-banco-1", "Banco Aliado", entidad, null, EFECTIVA + 300_000L)),
)

private val revocado = activo.copy(
    estado = EstadosDeConsentimiento.REVOCADO,
    vigente = false,
    revocable = false,
    finalizadoEn = EFECTIVA,
    comprobante = "REV-2026-0088",
    revocacion = revocacion(EstadosDelAviso.COMPLETADO, EstadosDeEntidad.CONFIRMADA),
)

private val parcial = revocado.copy(
    estado = EstadosDeConsentimiento.REVOCADO_PARCIAL,
    revocacion = revocacion(EstadosDelAviso.PARCIAL, EstadosDeEntidad.NO_CONFIRMADA),
    historial = activo.historial +
        CambioDeEstado(CambiosDeEstado.REVOCADO, EFECTIVA, "REV-2026-0088", "mobile", null),
)

private val enCola = RevocacionPendiente("CONS-2026-55AA31", "Banco Aliado", SOLICITADA, 1)

@Preview(showBackground = true)
@Composable
private fun HojaConBiometriaPreview() {
    SolventaTheme {
        ContenidoHojaDeRevocacion(
            entidad = "Banco Aliado",
            hoja = PasoDeRevocacion.Hoja(MetodoDeConfirmacion.BIOMETRIA),
            onConfirmarConBiometria = {},
            onConfirmarConContrasena = {},
            onCancelar = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HojaConContrasenaPreview() {
    SolventaTheme {
        ContenidoHojaDeRevocacion(
            entidad = "Banco Aliado",
            hoja = PasoDeRevocacion.Hoja(MetodoDeConfirmacion.CONTRASENA),
            onConfirmarConBiometria = {},
            onConfirmarConContrasena = {},
            onCancelar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ConsentimientoRevocadoPreview() {
    SolventaTheme {
        ContenidoConsentimientoRevocado(
            estado = RevocacionUiState(consentimiento = revocado),
            idioma = Idioma.ESPANOL,
            onDescargar = {},
            onVolver = {},
            onCerrarAviso = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ListaConRevocacionEnColaPreview() {
    SolventaTheme {
        ContenidoConsentimientos(
            estado = ConsentimientosUiState(
                cargando = false,
                sinConexion = true,
                sincronizadoEn = SOLICITADA,
                pendientes = listOf(ConsentimientoPendiente(activo, enCola)),
            ),
            idioma = Idioma.ESPANOL,
            onAtras = {},
            onAbrir = {},
            onReintentar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun DetallePendienteDeEnvioPreview() {
    SolventaTheme {
        ContenidoDetalleConsentimiento(
            estado = DetalleConsentimientoUiState(cargando = false, consentimiento = activo, sinConexion = true, pendiente = enCola),
            idioma = Idioma.ESPANOL,
            onAtras = {},
            revocacion = RevocacionUiState(consentimiento = activo, pendiente = enCola, conectado = false),
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun DetalleRevocacionParcialPreview() {
    SolventaTheme {
        ContenidoDetalleConsentimiento(
            estado = DetalleConsentimientoUiState(cargando = false, consentimiento = parcial),
            idioma = Idioma.ESPANOL,
            onAtras = {},
            revocacion = RevocacionUiState(consentimiento = parcial),
        )
    }
}
