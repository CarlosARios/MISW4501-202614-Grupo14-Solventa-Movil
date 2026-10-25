package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Las pantallas del asistente contra sus frames. El tamaño es el del
 * prototipo: 390 × 844.
 */
private const val ANCHO = 390
private const val ALTO = 844

private val POLIZA = PolizaElegible(
    id = "AU-2025-118204",
    etiqueta = "Autos · AU-2025-118204 · KJT-482",
)

private fun estado(
    etapa: Etapa,
    radicacion: Radicacion = Radicacion(),
    envio: EstadoDelEnvio = EstadoDelEnvio.NoIniciado,
) = RadicacionUiState(
    borradorId = "borrador-1",
    etapa = etapa,
    radicacion = radicacion,
    polizas = listOf(POLIZA),
    envio = envio,
    cargando = false,
)

private val DILIGENCIADA = Radicacion(
    poliza = "AU-2025-118204",
    tipo = TipoDeIncidente.COLISION,
    // 14/09/2026 18:40 en Bogotá.
    ocurrioEn = 1_789_515_600_000L,
    lugar = "Autopista Norte km 12, Bogotá",
    descripcion = "Choque por alcance en el semáforo.",
    respuestas = mapOf("personas_lesionadas" to "no", "informe_policia" to "si"),
    declaracionAceptada = true,
)

@Preview(name = "M11a · Paso 1 Identificación", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaIdentificacion() {
    SolventaTheme {
        PantallaIdentificacion(
            estado = estado(Etapa.IDENTIFICACION, DILIGENCIADA.copy(tipo = TipoDeIncidente.COLISION)),
            onElegirPoliza = {},
            onElegirTipo = {},
            onContinuar = {},
            onGuardarYSalir = {},
            onAtras = {},
        )
    }
}

@Preview(name = "M11a · con faltantes", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaFaltantes() {
    SolventaTheme {
        PantallaIdentificacion(
            estado = estado(Etapa.IDENTIFICACION),
            onElegirPoliza = {},
            onElegirTipo = {},
            onContinuar = {},
            onGuardarYSalir = {},
            onAtras = {},
        )
    }
}

@Preview(name = "M11b · Paso 2 Hechos", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaHechos() {
    SolventaTheme {
        PantallaHechos(
            estado = estado(Etapa.HECHOS, DILIGENCIADA),
            onFechaCambia = {},
            onLugarCambia = {},
            onDescripcionCambia = {},
            onResponder = { _, _ -> },
            onContinuar = {},
            onGuardarYSalir = {},
            onAtras = {},
        )
    }
}

@Preview(name = "M11c · Paso 3 Evidencias", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaEvidencias() {
    SolventaTheme {
        PantallaEvidencias(
            estado = estado(Etapa.EVIDENCIAS, DILIGENCIADA),
            onCapturar = {},
            onDescartar = {},
            onContinuar = {},
            onGuardarYSalir = {},
            onAtras = {},
        )
    }
}

@Preview(name = "M11d · Paso 4 Declaración", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaDeclaracion() {
    SolventaTheme {
        PantallaDeclaracion(
            estado = estado(Etapa.DECLARACION, DILIGENCIADA),
            onDeclarar = {},
            onRadicar = {},
            onGuardarYSalir = {},
            onAtras = {},
            onDescartarAviso = {},
        )
    }
}

@Preview(name = "M11d · envío fallido", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaEnvioFallido() {
    SolventaTheme {
        PantallaDeclaracion(
            estado = estado(
                Etapa.DECLARACION,
                DILIGENCIADA,
                EstadoDelEnvio.NoDisponible("a1b2c3d4e5f6"),
            ),
            onDeclarar = {},
            onRadicar = {},
            onGuardarYSalir = {},
            onAtras = {},
            onDescartarAviso = {},
        )
    }
}

@Preview(name = "M11e · Radicado generado", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaRadicado() {
    SolventaTheme {
        PantallaRadicado(
            estado = estado(Etapa.DECLARACION, DILIGENCIADA),
            caso = CasoRadicado(
                casoId = "cl-1",
                radicado = "SIN-2026-01877",
                radicadoEn = "2026-09-14T23:55:00Z",
            ),
            onIrAMisSiniestros = {},
            onIrAlInicio = {},
        )
    }
}
