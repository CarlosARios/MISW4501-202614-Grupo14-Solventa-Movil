package co.solventa.movil.features.originacion.polizas

/** Cómo se presenta una póliza en la billetera: la insignia de su tarjeta. */
enum class Situacion { ACTIVA, POR_VENCER, VENCIDA, CANCELADA }

/**
 * Una tarjeta de M4 · Mis pólizas: la póliza, su situación a la hora de
 * componer y si es una renovación (criterio 4, «Activa · renovada»).
 */
data class PolizaEnLista(
    val poliza: Poliza,
    val situacion: Situacion,
    val renovada: Boolean = false,
)

/**
 * Estado de M4 · Mis pólizas (HU-M1.2-01). Los estados del prototipo —con
 * conexión, sin conexión, cambios detectados— y los que la historia exige
 * además —cargando, vacío, error, copia desactualizada— viven en la misma
 * pantalla.
 */
data class PolizasUiState(
    val cargando: Boolean = true,
    /** Vigentes primero, luego las vencidas y canceladas, en el orden del servicio. */
    val polizas: List<PolizaEnLista> = emptyList(),
    /** Se muestra la copia local porque no hay red (criterio 1). */
    val sinConexion: Boolean = false,
    /** Cuándo se sincronizó la copia, para el indicador (criterios 1 y 3). */
    val sincronizadoEn: Long? = null,
    /** Hay una sincronización en curso; no bloquea la pantalla (criterio 3). */
    val sincronizando: Boolean = false,
    /** La última sincronización falló por otra razón que la red. */
    val error: Boolean = false,
    /** La copia tiene más de [PolizasViewModel.DIAS_SIN_SINCRONIZAR] días (criterio 5). */
    val desactualizada: Boolean = false,
    /** Lo que cambió en la última sincronización, hasta que se descarta (criterio 4). */
    val cambios: List<CambioDePoliza> = emptyList(),
) {
    val hayContenido: Boolean get() = polizas.isNotEmpty()

    /** Sin pólizas y sin nada que explicar: el estado vacío. */
    val vacio: Boolean get() = !cargando && !hayContenido && !error && !sinConexion
}

/** Estado de M5 · Detalle de la póliza (HU-M1.2-01, criterio 2). */
data class DetallePolizaUiState(
    val cargando: Boolean = true,
    val poliza: PolizaEnLista? = null,
    /** La póliza ya no está en la copia (otra cuenta, o la quitó una sincronización). */
    val noDisponible: Boolean = false,
    val sinConexion: Boolean = false,
    val sincronizadoEn: Long? = null,
    /** El cambio que la última sincronización detectó sobre esta póliza, si lo hay. */
    val cambio: CambioDePoliza? = null,
)
