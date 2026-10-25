package co.solventa.movil.features.auditoria.consentimientos

/**
 * Codigos de estado de un consentimiento, tal como los entrega el servicio de
 * auditoria (HU-M1.3-01). Los dos ultimos estan reservados para la revocacion
 * (HU-M1.3-02) y la revocacion por entidad (HU-W4.2-02).
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
}

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
    return copy(estado = visible, vigente = false, finalizadoEn = finalizadoEn ?: venceEn, historial = conVencimiento)
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
