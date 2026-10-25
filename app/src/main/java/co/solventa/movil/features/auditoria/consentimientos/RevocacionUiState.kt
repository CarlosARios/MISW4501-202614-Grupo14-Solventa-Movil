package co.solventa.movil.features.auditoria.consentimientos

/**
 * En que va la revocacion de un consentimiento (HU-M1.3-02). Los estados del
 * prototipo —hoja con el efecto, verificando, pidiendo la contraseña,
 * enviando, revocado y pendiente de envio— viven en la misma pantalla.
 */
sealed interface PasoDeRevocacion {
    /** El detalle, sin hoja: el estado de partida y al que se vuelve al cancelar (criterio 2). */
    data object Oculto : PasoDeRevocacion

    /**
     * La hoja inferior con el efecto de la revocacion (`37:1519`, con la huella o
     * con la contraseña). [verificando] mientras el sistema pide la huella o
     * se verifica la contraseña; [sinConexion] si la contraseña no se pudo
     * verificar por falta de red.
     */
    data class Hoja(
        val metodo: MetodoDeConfirmacion,
        val verificando: Boolean = false,
        val sinConexion: Boolean = false,
    ) : PasoDeRevocacion

    /** Confirmada: se esta registrando en el servicio (como mucho 4 s). */
    data object Enviando : PasoDeRevocacion

    /** El servicio la registro: se abre la pantalla de revocado (`38:1414`). */
    data class Revocado(val revocacion: Revocacion) : PasoDeRevocacion
}

/** Mensajes de la revocacion que se muestran con el componente «Aviso». */
enum class AvisoDeLaRevocacion {
    /** Otra solicitud ya lo habia revocado. */
    YA_REVOCADO,

    /** El consentimiento vencio y ya no se puede revocar. */
    YA_NO_ACTIVO,

    /** El servicio rechazo la revocacion por otra causa. */
    NO_SE_PUDO,

    /** El comprobante quedo guardado donde el titular eligio. */
    COMPROBANTE_GUARDADO,

    /** El comprobante necesita conexion. */
    COMPROBANTE_SIN_CONEXION,

    /** El comprobante no se pudo descargar o guardar. */
    COMPROBANTE_ERROR,
}

/** Estado de la revocacion de un consentimiento (HU-M1.3-02). */
data class RevocacionUiState(
    /** El consentimiento como se ve ahora, de la copia local. */
    val consentimiento: Consentimiento? = null,
    /** Su revocacion pendiente de envio, si la hay (criterio 5). */
    val pendiente: RevocacionPendiente? = null,
    val conectado: Boolean = true,
    val paso: PasoDeRevocacion = PasoDeRevocacion.Oculto,
    /** Se esta reenviando la cola a pedido del titular. */
    val reenviando: Boolean = false,
    val descargando: Boolean = false,
    val aviso: AvisoDeLaRevocacion? = null,
    /** La revocacion de la cola que el servicio ya no acepto (vencido o ya revocado). */
    val descarte: AvisoDeRevocacion? = null,
) {
    /**
     * «Revocar consentimiento» solo en un activo que el servicio deja revocar
     * y sin revocacion en cola; en revocados, vencidos y pendientes no aparece
     * (criterio 6).
     */
    val revocarDisponible: Boolean
        get() = consentimiento?.let { it.estado == EstadosDeConsentimiento.ACTIVO && it.revocable } == true && pendiente == null

    /** «Reintentar envio ahora» reemplaza a Revocar mientras hay una pendiente. */
    val reintentarVisible: Boolean get() = pendiente != null

    /** Sin señal el reintento no puede salir: el boton queda deshabilitado. */
    val reintentarHabilitado: Boolean get() = pendiente != null && conectado && !reenviando

    /** El comprobante se puede descargar de una revocacion registrada por el servicio. */
    val comprobanteDisponible: Boolean get() = consentimiento?.revocacion != null

    /** Nombre sugerido del archivo: el numero del comprobante. */
    val nombreDelComprobante: String
        get() = (consentimiento?.revocacion?.comprobante ?: consentimiento?.comprobante ?: consentimiento?.id ?: "comprobante") + ".pdf"
}
