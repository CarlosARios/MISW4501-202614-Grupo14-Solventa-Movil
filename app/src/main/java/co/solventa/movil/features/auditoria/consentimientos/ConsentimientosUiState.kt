package co.solventa.movil.features.auditoria.consentimientos

/**
 * Estado de M8 · Privacidad y consentimientos (HU-M1.3-01). Los estados del
 * prototipo —cargando, contenido, vacio, error, sin conexion y cuenta sin
 * vincular— viven en la misma pantalla.
 */
/** Un consentimiento cuya revocacion espera en la cola del telefono. */
data class ConsentimientoPendiente(val consentimiento: Consentimiento, val revocacion: RevocacionPendiente)

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
    /**
     * Consentimientos con una revocacion pendiente de envio (HU-M1.3-02,
     * `38:1503`), con su solicitud: se listan aparte y no entre los activos.
     */
    val pendientes: List<ConsentimientoPendiente> = emptyList(),
    /** Se esta reenviando la cola a pedido del titular. */
    val reenviando: Boolean = false,
    /** Revocaciones de la cola que el servicio ya no acepto, para avisarlas. */
    val descartes: List<AvisoDeRevocacion> = emptyList(),
) {
    val hayContenido: Boolean get() = activos.isNotEmpty() || historial.isNotEmpty() || pendientes.isNotEmpty()

    /** La ultima revocacion en cola, para el aviso «Sin conexion: revocacion en cola». */
    val ultimaPendiente: ConsentimientoPendiente? get() = pendientes.maxByOrNull { it.revocacion.solicitadaEn }

    /** «Reintentar envio ahora» solo con señal y si hay algo en la cola. */
    val reintentarHabilitado: Boolean get() = pendientes.isNotEmpty() && !sinConexion && !reenviando

    /** Sin consentimientos y sin nada que explicar: el estado vacio. */
    val vacio: Boolean get() = !cargando && !hayContenido && !error && !cuentaSinVincular && !sinConexion
}
