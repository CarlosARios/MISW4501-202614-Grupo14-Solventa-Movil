package co.solventa.movil.features.siniestros.seguimiento

/** Por qué no hay nada que mostrar. */
enum class FalloDeConsulta { SIN_CONEXION, ERROR, NO_ENCONTRADO }

/**
 * Estado de M9 · Mis siniestros (HU-M2.4-01) con el estado degradado de M9c
 * (HA-L02): con [frescura] desde la caché se muestra el aviso, la sección
 * marcada «versión en caché» y la hora en cada tarjeta.
 */
data class MisSiniestrosUiState(
    val cargando: Boolean = true,
    val enCurso: List<Siniestro> = emptyList(),
    val cerrados: List<Siniestro> = emptyList(),
    val frescura: Frescura? = null,
    /** Hay una consulta en curso después de la primera; no esconde lo que ya se ve. */
    val recargando: Boolean = false,
    /** La última consulta falló; si ya había siniestros, se siguen mostrando. */
    val fallo: FalloDeConsulta? = null,
) {
    val hayContenido: Boolean get() = enCurso.isNotEmpty() || cerrados.isNotEmpty()

    val desdeCache: Boolean get() = frescura?.desdeCache == true

    /** Sin siniestros y sin nada que explicar. */
    val vacio: Boolean get() = !cargando && !hayContenido && fallo == null
}

/** Estado de M10 · Detalle del siniestro con su línea de tiempo (HU-M2.4-01, HA-L02). */
data class DetalleSiniestroUiState(
    val cargando: Boolean = true,
    val siniestro: Siniestro? = null,
    val etapas: List<EtapaDeLinea> = emptyList(),
    val progreso: Float = 0f,
    val frescura: Frescura? = null,
    val recargando: Boolean = false,
    val fallo: FalloDeConsulta? = null,
) {
    val desdeCache: Boolean get() = frescura?.desdeCache == true
}
