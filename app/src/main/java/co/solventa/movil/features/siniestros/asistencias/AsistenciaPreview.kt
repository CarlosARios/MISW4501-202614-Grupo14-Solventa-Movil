package co.solventa.movil.features.siniestros.asistencias

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.features.siniestros.radicacion.PolizaElegible
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de M14, M14b y M14c con el dato del prototipo (el mismo de
 * `PolizasDeDemostracion`): Autos · AU-2025-118204 · KJT-482, en la Autopista
 * Norte (4,7712 N · 74,0452 O), con precisión de 8 m.
 */
private val poliza = PolizaElegible(id = "AU-2025-118204", etiqueta = "Autos · AU-2025-118204 · KJT-482")

private val enviada = SolicitudDeAsistencia(
    id = 1,
    poliza = poliza.id,
    etiquetaDePoliza = poliza.etiqueta,
    resultado = ResultadoDeUbicacion.Compartida(
        Ubicacion(latitud = 4.7712, longitud = -74.0452, altitud = null, precisionM = 8f, leidaEn = 1_794_710_331_000),
    ),
    envio = EstadoDeEnvio.Confirmada,
    creadaEn = 1_794_710_331_000,
    lugar = "Autopista Norte km 12, Bogotá",
    requestId = "5004f477-d43b-4b8a-80b0-b045207c5d11",
    numero = "AST-2026-0001",
    registradaEn = 1_794_710_337_043,
    estado = EstadoDeLaSolicitud.NUEVA,
)

@Composable
private fun Vista(estado: AsistenciaUiState) {
    SolventaTheme {
        ContenidoAsistencia(
            estado = estado,
            onAtras = {},
            onElegirPoliza = {},
            onSolicitar = {},
            onLlamarSinSolicitud = {},
            onActivarUbicacion = {},
            onLlamarSinUbicacion = {},
            onCancelar = {},
            onVolverALlamar = {},
            onVerEstado = {},
            onSolicitarOtra = {},
            onCopiarNumero = {},
        )
    }
}

@Preview(showBackground = true, name = "M14 · inicial")
@Composable
private fun VistaPreviaInicial() {
    Vista(AsistenciaUiState(polizas = PolizasDeAsistencia.Lista(listOf(poliza), poliza)))
}

@Preview(showBackground = true, name = "M14b · servicio apagado")
@Composable
private fun VistaPreviaDialogo() {
    Vista(
        AsistenciaUiState(
            polizas = PolizasDeAsistencia.Lista(listOf(poliza), poliza),
            paso = PasoDeAsistencia.Dialogo(TipoDeDialogo.SERVICIO_APAGADO),
        ),
    )
}

@Preview(showBackground = true, name = "M14 · ubicación enviada")
@Composable
private fun VistaPreviaEnviada() {
    Vista(
        AsistenciaUiState(
            polizas = PolizasDeAsistencia.Lista(listOf(poliza), poliza),
            paso = PasoDeAsistencia.Solicitada,
            solicitud = enviada,
            marcadorAbierto = true,
        ),
    )
}

@Preview(showBackground = true, name = "M14c · sin señal de satélite")
@Composable
private fun VistaPreviaSinUbicacion() {
    Vista(
        AsistenciaUiState(
            polizas = PolizasDeAsistencia.Lista(listOf(poliza), poliza),
            paso = PasoDeAsistencia.Solicitada,
            solicitud = enviada.copy(
                resultado = ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA),
                envio = EstadoDeEnvio.EnCola,
                lugar = null,
                requestId = null,
                numero = null,
                registradaEn = null,
                estado = null,
            ),
            conectado = false,
            marcadorAbierto = true,
        ),
    )
}
