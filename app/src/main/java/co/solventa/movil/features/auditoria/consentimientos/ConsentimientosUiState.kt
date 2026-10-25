package co.solventa.movil.features.auditoria.consentimientos

/**
 * Estado de M8 · Privacidad y consentimientos (HU-M1.3-01). Los estados del
 * prototipo —cargando, contenido, vacio, error, sin conexion y cuenta sin
 * vincular— viven en la misma pantalla.
 */
data class ConsentimientosUiState(
    val cargando: Boolean = true,
    /** Vigentes, del otorgado mas reciente al mas antiguo. */
    val activos: List<Consentimiento> = emptyList(),
    /** Revocados o vencidos, del que termino mas recientemente. */
    val historial: List<Consentimiento> = emptyList(),
    /** Se muestra la copia local porque no hay red (criterio 5). */
    val sinConexion: Boolean = false,
    /** Cuando se sincronizo la copia, para el aviso sin conexion. */
    val sincronizadoEn: Long? = null,
    /** La ultima actualizacion fallo por otra razon que la red. */
    val error: Boolean = false,
    /** 403 `holder_not_linked`: la cuenta todavia no esta vinculada. */
    val cuentaSinVincular: Boolean = false,
) {
    val hayContenido: Boolean get() = activos.isNotEmpty() || historial.isNotEmpty()

    /** Sin consentimientos y sin nada que explicar: el estado vacio. */
    val vacio: Boolean get() = !cargando && !hayContenido && !error && !cuentaSinVincular && !sinConexion
}
