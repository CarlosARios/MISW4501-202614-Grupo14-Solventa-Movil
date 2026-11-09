package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de M9, M9c y M10 con los datos del prototipo (HU-M2.4-01,
 * HA-L02). Son herramienta de diseño: no corren en la aplicación.
 */
private const val AHORA_PREVIA = 1_789_569_000_000L // 16/09/2026 14:30 UTC
private const val HORA_PREVIA = 3_600_000L

private val hogarPrevia = Siniestro(
    id = "8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90", radicado = "SIN-2026-01655",
    estado = EstadoDeSiniestro.EN_EVALUACION, codigoDeEstado = "in_evaluation",
    poliza = "HG-2025-880231", linea = "home", producto = null, objetoAsegurado = null,
    tipoDeIncidente = "water_damage", ocurrioEn = AHORA_PREVIA - 300 * HORA_PREVIA,
    radicadoEn = AHORA_PREVIA - 280 * HORA_PREVIA, lugar = "Carrera 15 # 93-40, apto 402",
)

private val autosPrevia = hogarPrevia.copy(
    id = "a", radicado = "SIN-2026-01877", estado = EstadoDeSiniestro.DEVUELTO, codigoDeEstado = "returned",
    poliza = "AU-2025-118204", linea = "auto", tipoDeIncidente = "collision",
)

private val pagadoPrevia = autosPrevia.copy(
    id = "b", radicado = "SIN-2026-01702", estado = EstadoDeSiniestro.PAGADO, codigoDeEstado = "paid",
    tipoDeIncidente = "theft",
)

private fun listaPrevia(desdeCache: Boolean) = MisSiniestrosUiState(
    cargando = false,
    enCurso = listOf(autosPrevia, hogarPrevia),
    cerrados = listOf(pagadoPrevia),
    frescura = Frescura(desdeCache, AHORA_PREVIA),
)

private fun detallePrevia(desdeCache: Boolean) = DetalleSiniestroUiState(
    cargando = false,
    siniestro = hogarPrevia,
    etapas = LineaDeTiempo.armar(
        hogarPrevia.estado,
        listOf(
            CambioDeEstado(EstadoDeSiniestro.RADICADO, AHORA_PREVIA - 280 * HORA_PREVIA),
            CambioDeEstado(EstadoDeSiniestro.EN_EVALUACION, AHORA_PREVIA - 260 * HORA_PREVIA),
        ),
    ),
    progreso = LineaDeTiempo.progreso(hogarPrevia.estado),
    frescura = Frescura(desdeCache, AHORA_PREVIA),
)

@Preview(name = "M9 · Mis siniestros", showBackground = true, heightDp = 900)
@Composable
private fun MisSiniestrosPrevia() = SolventaTheme {
    ContenidoMisSiniestros(listaPrevia(desdeCache = false), Idioma.ESPANOL, AHORA_PREVIA, onAbrir = {}, onReintentar = {})
}

@Preview(name = "M9c · Servicio degradado (caché)", showBackground = true, heightDp = 1000)
@Composable
private fun MisSiniestrosDesdeCachePrevia() = SolventaTheme {
    ContenidoMisSiniestros(listaPrevia(desdeCache = true), Idioma.ESPANOL, AHORA_PREVIA, onAbrir = {}, onReintentar = {})
}

@Preview(name = "M10 · Detalle con línea de tiempo", showBackground = true, heightDp = 1100)
@Composable
private fun DetalleSiniestroPrevia() = SolventaTheme {
    ContenidoDetalleSiniestro(detallePrevia(desdeCache = false), Idioma.ESPANOL, AHORA_PREVIA, onAtras = {}, onReintentar = {})
}

@Preview(name = "M10 · Detalle desde la caché", showBackground = true, heightDp = 1200)
@Composable
private fun DetalleSiniestroDesdeCachePrevia() = SolventaTheme {
    ContenidoDetalleSiniestro(detallePrevia(desdeCache = true), Idioma.ESPANOL, AHORA_PREVIA, onAtras = {}, onReintentar = {})
}
