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
    /** Su revocacion pendiente de envio, si la hay (HU-M1.3-02, `1379:3583`). */
    val pendiente: RevocacionPendiente? = null,
) {
    /**
     * «Revocar consentimiento» solo en un activo que el servicio deja revocar
     * y sin revocacion en cola; en revocados, vencidos y pendientes no aparece
     * (HU-M1.3-02, criterio 6). HU-M1.3-01 lo dejaba deshabilitado (D-8 b).
     */
    val revocarVisible: Boolean
        get() = consentimiento?.let { it.estado == EstadosDeConsentimiento.ACTIVO && it.revocable } == true && pendiente == null

    /** Revocado, pero alguna entidad no confirmo (HU-M1.3-02, `1379:3687`). */
    val parcial: Boolean get() = consentimiento?.estado == EstadosDeConsentimiento.REVOCADO_PARCIAL

    /**
     * Los hechos que dibuja la linea de tiempo: los del consentimiento entero
     * (los avisos a cada entidad se resumen en el aviso de revocacion parcial).
     * En un parcial, la revocacion pasa a ser el paso final, «sin confirmar».
     */
    val historialVisible: List<CambioDeEstado>
        get() = consentimiento?.historial.orEmpty()
            .filter { it.estado !in CambiosDeEstado.DE_ENTIDAD }
            .filterNot { parcial && it.estado == CambiosDeEstado.REVOCADO && it.entidad == null }

    /** El paso con que termina el historial, si el estado actual no es ya su ultimo hecho. */
    val pasoFinal: PasoFinalDelHistorial? get() = consentimiento?.let { pasoFinalDelHistorial(it, sinConexion, pendiente) }
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

    /** Revocacion solicitada en el telefono y pendiente de envio (HU-M1.3-02, `1379:3583`). */
    data class RevocacionPendiente(val solicitadaEn: Long) : PasoFinalDelHistorial

    /**
     * Revocado, con entidades sin confirmar (HU-M1.3-02, `1379:3687`): la
     * revocacion con su hora y su comprobante, que todavia no termina.
     */
    data class RevocadoSinConfirmar(val efectivaEn: Long, val comprobante: String?, val entidades: List<String>) :
        PasoFinalDelHistorial
}

/**
 * El paso final sale del estado visible que ya deriva la aplicacion, sin
 * inventar nada: un activo siempre lo tiene (no hay un hecho «activo» en el
 * historial); un vencido, solo si su historial no trae ya el vencimiento en
 * ningun punto, porque repetirlo despues de un hecho posterior (por ejemplo,
 * una revocacion registrada despues de vencer, que no cambia el estado) lo
 * dejaria duplicado y fuera de orden; un revocado termina en su revocacion y
 * no lleva paso.
 *
 * HU-M1.3-02: un activo con su revocacion en cola termina en «Revocacion
 * solicitada · pendiente de envio»; un revocado parcial, en su revocacion
 * «sin confirmar» con las entidades que faltan.
 */
fun pasoFinalDelHistorial(
    c: Consentimiento,
    sinConexion: Boolean,
    pendiente: co.solventa.movil.features.auditoria.consentimientos.RevocacionPendiente? = null,
): PasoFinalDelHistorial? = when (c.estado) {
    EstadosDeConsentimiento.ACTIVO ->
        if (pendiente != null) {
            PasoFinalDelHistorial.RevocacionPendiente(pendiente.solicitadaEn)
        } else {
            PasoFinalDelHistorial.Activo(sincronizado = !sinConexion)
        }
    EstadosDeConsentimiento.REVOCADO_PARCIAL -> PasoFinalDelHistorial.RevocadoSinConfirmar(
        efectivaEn = c.revocacion?.efectivaEn ?: c.finalizadoEn ?: c.otorgadoEn,
        comprobante = c.revocacion?.comprobante ?: c.comprobante,
        entidades = c.revocacion?.sinConfirmar?.map { it.nombre }.orEmpty().ifEmpty { listOf(c.nombreDeEntidad) },
    )
    EstadosDeConsentimiento.VENCIDO ->
        if (c.historial.any { it.estado == CambiosDeEstado.VENCIDO }) {
            null
        } else {
            PasoFinalDelHistorial.Vencido(vencioEn = c.finalizadoEn ?: c.venceEn)
        }
    else -> null
}
