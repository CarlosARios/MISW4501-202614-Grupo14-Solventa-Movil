package co.solventa.movil.features.auditoria.consentimientos

/**
 * Codigos de estado de un consentimiento, tal como los entrega el servicio de
 * auditoria (HU-M1.3-01). `partially_revoked` es un revocado que alguna
 * entidad no confirmo (HU-M1.3-02, D-1 a); `revocation_pending` esta reservado
 * y el servidor no lo usa: «pendiente de envio» es un estado de la cola local.
 */
object EstadosDeConsentimiento {
    const val ACTIVO = "active"
    const val VENCIDO = "expired"
    const val REVOCADO = "revoked"
    const val REVOCACION_PENDIENTE = "revocation_pending"
    const val REVOCADO_PARCIAL = "partially_revoked"
}

/** Codigos de los cambios de estado del historial. */
object CambiosDeEstado {
    const val OTORGADO = "granted"
    const val VENCIDO = "expired"
    const val REVOCACION_SOLICITADA = "revocation_requested"
    const val REVOCADO = "revoked"

    /** De una entidad (HU-M1.3-02): confirmo, no confirmo o se le reintento el aviso. */
    const val REVOCACION_CONFIRMADA = "revocation_confirmed"
    const val REVOCACION_NO_CONFIRMADA = "revocation_not_confirmed"
    const val REVOCACION_REINTENTADA = "revocation_retry_requested"

    /** Los que son de una entidad y no del consentimiento entero. */
    val DE_ENTIDAD = setOf(REVOCACION_CONFIRMADA, REVOCACION_NO_CONFIRMADA, REVOCACION_REINTENTADA)
}

/** Estado del aviso de una revocacion a las entidades (HU-M1.3-02). */
object EstadosDelAviso {
    const val EN_CURSO = "in_progress"
    const val COMPLETADO = "completed"
    const val PARCIAL = "partial"
}

/** Estado del aviso a una entidad. */
object EstadosDeEntidad {
    const val PENDIENTE = "pending"
    const val CONFIRMADA = "confirmed"
    const val NO_CONFIRMADA = "not_confirmed"
}

/** El aviso de la revocacion a una entidad financiera. */
data class EntidadDeRevocacion(
    val codigo: String,
    val nombre: String,
    val estado: String,
    val confirmadaEn: Long?,
    val plazo: Long?,
)

/**
 * La revocacion de un consentimiento (HU-M1.3-02): el comprobante, la hora en
 * que el titular la confirmo y la hora en que termino la autorizacion, el
 * canal y el dispositivo, y el aviso a cada entidad.
 */
data class Revocacion(
    val comprobante: String,
    val estado: String,
    val solicitadaEn: Long,
    val efectivaEn: Long,
    val canal: String,
    val dispositivo: String?,
    val estadoDelAviso: String,
    val entidades: List<EntidadDeRevocacion>,
) {
    /**
     * Cuanto tardo en hacerse efectiva, en milisegundos, para «Efectivo en X
     * segundos»; null si sale negativo porque el reloj del telefono estaba
     * adelantado (entonces se muestra «Efectivo desde <hora>»).
     */
    val demora: Long? get() = (efectivaEn - solicitadaEn).takeIf { it >= 0 }

    /** Las entidades que todavia no confirman, para el aviso de revocacion parcial. */
    val sinConfirmar: List<EntidadDeRevocacion>
        get() = entidades.filter { it.estado != EstadosDeEntidad.CONFIRMADA }
}

/**
 * Una revocacion confirmada en el telefono que todavia no llega al servicio
 * (HU-M1.3-02, criterio 5): se muestra como «pendiente de envio».
 */
data class RevocacionPendiente(
    val consentimientoId: String,
    val entidad: String,
    val solicitadaEn: Long,
    val intentos: Int,
)

/** Entidad financiera autorizada, con su nombre visible. */
data class Entidad(val codigo: String, val nombre: String)

/** Una fuente del alcance: que datos lee y por cuanto tiempo. */
data class Fuente(val codigo: String, val categorias: List<String>, val vigencia: String)

/** Un cambio de estado del historial (criterio 2). */
data class CambioDeEstado(
    val estado: String,
    val ocurrioEn: Long,
    val comprobante: String?,
    val canal: String?,
    val entidad: String?,
)

/**
 * Un consentimiento de Open Finance del titular (criterios 1 a 3). Las fechas
 * van en milisegundos desde epoca.
 */
data class Consentimiento(
    val id: String,
    val estado: String,
    val vigente: Boolean,
    val entidades: List<Entidad>,
    val finalidad: String,
    val canal: String,
    val versionDeTerminos: String,
    val alcance: List<Fuente>,
    val otorgadoEn: Long,
    val venceEn: Long,
    val finalizadoEn: Long?,
    val comprobante: String?,
    val historial: List<CambioDeEstado>,
    /** Solo un activo se puede revocar (HU-M1.3-02, criterio 6). */
    val revocable: Boolean = estado == EstadosDeConsentimiento.ACTIVO,
    /** La revocacion que lo termino, si fue revocado por HU-M1.3-02. */
    val revocacion: Revocacion? = null,
) {
    /** El nombre de la entidad que se muestra en la tarjeta y el detalle. */
    val nombreDeEntidad: String
        get() = entidades.firstOrNull()?.nombre.orEmpty()
}

/**
 * El estado que se muestra a la hora dada: un consentimiento activo cuya
 * vigencia ya paso se muestra vencido aunque la copia local sea anterior al
 * vencimiento. Es la misma regla del servicio (decision D-4 a): activo hasta
 * justo antes de `venceEn`.
 */
fun estadoVisible(estado: String, venceEn: Long, ahora: Long): String =
    if (estado == EstadosDeConsentimiento.ACTIVO && ahora >= venceEn) EstadosDeConsentimiento.VENCIDO else estado

/**
 * El consentimiento como se ve a la hora dada: si vencio desde la ultima
 * sincronizacion, queda vencido, termina en `venceEn` y su historial suma el
 * vencimiento.
 */
fun Consentimiento.visibleEn(ahora: Long): Consentimiento {
    val visible = estadoVisible(estado, venceEn, ahora)
    if (visible == estado) return this
    val conVencimiento = if (historial.any { it.estado == CambiosDeEstado.VENCIDO }) {
        historial
    } else {
        historial + CambioDeEstado(CambiosDeEstado.VENCIDO, venceEn, comprobante = null, canal = null, entidad = null)
    }
    return copy(
        estado = visible,
        vigente = false,
        revocable = false,
        finalizadoEn = finalizadoEn ?: venceEn,
        historial = conVencimiento,
    )
}

/** Los consentimientos separados como los lee el titular (criterio 1). */
data class GruposDeConsentimientos(
    val activos: List<Consentimiento>,
    val historial: List<Consentimiento>,
)

/**
 * Activos primero, del otorgado mas reciente al mas antiguo; despues los
 * revocados o vencidos, del que termino mas recientemente.
 */
fun agrupar(consentimientos: List<Consentimiento>, ahora: Long): GruposDeConsentimientos {
    val visibles = consentimientos.map { it.visibleEn(ahora) }
    val (activos, historial) = visibles.partition { it.estado == EstadosDeConsentimiento.ACTIVO }
    return GruposDeConsentimientos(
        activos = activos.sortedWith(compareByDescending<Consentimiento> { it.otorgadoEn }.thenBy { it.id }),
        historial = historial.sortedWith(
            compareByDescending<Consentimiento> { it.finalizadoEn ?: it.otorgadoEn }.thenBy { it.id },
        ),
    )
}
