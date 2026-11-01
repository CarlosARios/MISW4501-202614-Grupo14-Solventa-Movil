package co.solventa.movil.features.notificaciones.centro

/**
 * Estado de M15 · Centro de notificaciones (HU-M3.1-01). Los estados —
 * cargando, contenido, vacío, error y sin conexión— viven en la misma
 * pantalla.
 */
data class CentroUiState(
    val cargando: Boolean = true,
    /** Las de hoy, la más reciente primero. */
    val hoy: List<Notificacion> = emptyList(),
    /** Las de días anteriores, la más reciente primero. */
    val anteriores: List<Notificacion> = emptyList(),
    val sinLeer: Int = 0,
    val sinConexion: Boolean = false,
    val sincronizadoEn: Long? = null,
    /** La última sincronización falló por otra razón que la red. */
    val error: Boolean = false,
) {
    val hayContenido: Boolean get() = hoy.isNotEmpty() || anteriores.isNotEmpty()

    /** «Marcar todas como leídas» solo si hay alguna sin leer. */
    val puedeMarcarTodas: Boolean get() = sinLeer > 0

    val vacio: Boolean get() = !cargando && !hayContenido && !error && !sinConexion
}

/** Estado de M10b · Siniestro paramétrico resuelto automáticamente (HU-M3.1-01). */
data class DetallePagoUiState(
    val cargando: Boolean = true,
    val notificacion: Notificacion? = null,
    /** No está en la copia ni la trajo el servicio (otra cuenta, o ya no existe). */
    val noDisponible: Boolean = false,
    val sinConexion: Boolean = false,
) {
    /** El valor observado superó el umbral: por eso se pagó. */
    val superoElUmbral: Boolean
        get() = notificacion?.pago?.disparador?.let { it.observado >= it.umbral } ?: false
}
