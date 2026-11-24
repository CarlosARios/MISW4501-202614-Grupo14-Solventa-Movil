package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de M9, M9c, M10, M10b y M10c con los datos del prototipo (HU-M2.4-01,
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

/** El pago paramétrico del prototipo (M10b): AV-8421, 215 minutos de retraso, umbral 180. */
private val parametricoPrevia = Siniestro(
    id = "6f1c2a7e-3b4d-4e5f-8a9b-0c1d2e3f4a5b", radicado = "SIN-2026-01702",
    estado = EstadoDeSiniestro.PAGADO, codigoDeEstado = "paid",
    poliza = "VJ-2026-000931", linea = "travel", producto = "Viaje", objetoAsegurado = null,
    tipoDeIncidente = "flight_delay", ocurrioEn = AHORA_PREVIA - 113 * HORA_PREVIA,
    radicadoEn = AHORA_PREVIA - 113 * HORA_PREVIA, lugar = "AV-8421",
    resolucionAutomatica = ResolucionAutomatica(
        resueltaEn = AHORA_PREVIA - 113 * HORA_PREVIA + 180_000L,
        version = "parametric-rules-v1",
        disparador = Disparador("flight_delay", "AV-8421", 180.0, 215.0, "minutes", AHORA_PREVIA - 113 * HORA_PREVIA),
    ),
)

@Preview(name = "M10b · Siniestro paramétrico resuelto automáticamente", showBackground = true, heightDp = 1200)
@Composable
private fun DetalleParametricoPrevia() = SolventaTheme {
    val pagado = parametricoPrevia.radicadoEn + 480_000L
    val estado = DetalleSiniestroUiState(
        cargando = false,
        siniestro = parametricoPrevia,
        etapas = LineaDeTiempo.armar(
            parametricoPrevia,
            listOf(
                CambioDeEstado(EstadoDeSiniestro.EN_EVALUACION, parametricoPrevia.radicadoEn),
                CambioDeEstado(EstadoDeSiniestro.APROBADO, parametricoPrevia.radicadoEn),
                CambioDeEstado(EstadoDeSiniestro.PAGADO, pagado),
            ),
        ),
        progreso = LineaDeTiempo.progreso(parametricoPrevia),
        frescura = Frescura(false, AHORA_PREVIA),
    )
    ContenidoDetalleSiniestro(estado, Idioma.ESPANOL, AHORA_PREVIA, onAtras = {}, onReintentar = {})
}

@Preview(name = "M10c · Detalle sin conexión", showBackground = true, heightDp = 1200)
@Composable
private fun DetalleSinConexionPrevia() = SolventaTheme {
    val estado = detallePrevia(desdeCache = false).copy(
        frescura = null,
        copiaLocalDe = AHORA_PREVIA,
        estimacion = EstimacionDeResolucion(nivel = 2, diasHabiles = 5),
    )
    ContenidoDetalleSiniestro(estado, Idioma.ESPANOL, AHORA_PREVIA, onAtras = {}, onReintentar = {})
}
