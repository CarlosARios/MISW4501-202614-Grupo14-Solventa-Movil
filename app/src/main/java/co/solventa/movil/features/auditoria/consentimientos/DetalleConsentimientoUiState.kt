package co.solventa.movil.features.auditoria.consentimientos

/** Estado de M8b · Detalle del consentimiento (HU-M1.3-01). */
data class DetalleConsentimientoUiState(
    val cargando: Boolean = true,
    /** El consentimiento como se ve ahora (estado visible), si esta en la copia. */
    val consentimiento: Consentimiento? = null,
    /** El identificador ya no esta en la copia local. */
    val noDisponible: Boolean = false,
    /** No hay red: lo que se ve es la copia local, sin confirmar con el servicio. */
    val sinConexion: Boolean = false,
) {
    /**
     * El boton «Revocar consentimiento» solo aparece en un activo y siempre
     * deshabilitado: revocar es de HU-M1.3-02 (D-8 b). En revocados y
     * vencidos no aparece.
     */
    val revocarVisible: Boolean get() = consentimiento?.estado == EstadosDeConsentimiento.ACTIVO

    /** El paso con que termina el historial, si el estado actual no es ya su ultimo hecho. */
    val pasoFinal: PasoFinalDelHistorial? get() = consentimiento?.let { pasoFinalDelHistorial(it, sinConexion) }
}

/**
 * El ultimo paso de la linea de tiempo del historial de M8b (frame 37:1418):
 * el estado en que esta el consentimiento ahora, cuando ningun hecho del
 * historial lo dice todavia. Se dibuja con circulo de contorno y su numero.
 */
sealed interface PasoFinalDelHistorial {

    /**
     * Activo: «Vigente · sincronizado con Solventa», o solo «Vigente» sin
     * conexion, porque entonces lo que se ve es la copia local.
     */
    data class Activo(val sincronizado: Boolean) : PasoFinalDelHistorial

    /** Vencido, con la fecha en que vencio. */
    data class Vencido(val vencioEn: Long) : PasoFinalDelHistorial
}

/**
 * El paso final sale del estado visible que ya deriva la aplicacion, sin
 * inventar nada: un activo siempre lo tiene (no hay un hecho «activo» en el
 * historial); un vencido, solo si su ultimo hecho no es ya el vencimiento (por
 * ejemplo, una revocacion registrada despues de vencer, que no cambia el
 * estado); un revocado termina en su revocacion y no lleva paso.
 */
fun pasoFinalDelHistorial(c: Consentimiento, sinConexion: Boolean): PasoFinalDelHistorial? = when (c.estado) {
    EstadosDeConsentimiento.ACTIVO -> PasoFinalDelHistorial.Activo(sincronizado = !sinConexion)
    EstadosDeConsentimiento.VENCIDO ->
        if (c.historial.lastOrNull()?.estado == CambiosDeEstado.VENCIDO) {
            null
        } else {
            PasoFinalDelHistorial.Vencido(vencioEn = c.finalizadoEn ?: c.venceEn)
        }
    else -> null
}
