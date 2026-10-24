package co.solventa.movil.features.auditoria.consentimientos

/** Estado de M8b · Detalle del consentimiento (HU-M1.3-01). */
data class DetalleConsentimientoUiState(
    val cargando: Boolean = true,
    /** El consentimiento como se ve ahora (estado visible), si esta en la copia. */
    val consentimiento: Consentimiento? = null,
    /** El identificador ya no esta en la copia local. */
    val noDisponible: Boolean = false,
) {
    /**
     * El boton «Revocar consentimiento» solo aparece en un activo y siempre
     * deshabilitado: revocar es de HU-M1.3-02 (D-8 b). En revocados y
     * vencidos no aparece.
     */
    val revocarVisible: Boolean get() = consentimiento?.estado == EstadosDeConsentimiento.ACTIVO
}
