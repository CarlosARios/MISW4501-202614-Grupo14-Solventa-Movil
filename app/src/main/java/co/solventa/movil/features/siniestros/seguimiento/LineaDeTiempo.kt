package co.solventa.movil.features.siniestros.seguimiento

/**
 * Las etapas de la línea de tiempo de M10. Las cinco primeras son las de un
 * caso evaluado por un analista; las tres últimas, las de un siniestro
 * paramétrico resuelto automáticamente (HU-M2.4-01, criterio 3).
 */
enum class Etapa {
    RADICADO,
    REVISION,
    EVALUACION,
    DECISION,
    PAGO,
    EVENTO_DETECTADO,
    VALIDACION_AUTOMATICA,
    PAGO_EJECUTADO,
}

/** Si una etapa ya pasó, es la actual o todavía no llega. */
enum class Avance { HECHA, ACTUAL, PENDIENTE }

/**
 * Una etapa de la línea de tiempo. [desde] es cuándo empezó (la hora del
 * cambio de estado que la abrió), o `null` si todavía no llega o el historial
 * no la registra. [rechazada] marca la decisión cuando el caso se rechazó.
 */
data class EtapaDeLinea(
    val etapa: Etapa,
    val avance: Avance,
    val desde: Long?,
    val rechazada: Boolean = false,
)

/**
 * Arma la línea de tiempo de M10 (Radicado → Revisión del asesor → En
 * evaluación → Aprobado → Pagado) a partir del estado actual y del historial.
 *
 * - La etapa actual sale del estado; las anteriores quedan hechas y las
 *   siguientes, pendientes.
 * - La hora de cada etapa es la del primer cambio que llevó a ella.
 * - Un caso rechazado termina en la decisión (marcada como rechazo) y no
 *   tiene pago.
 */
object LineaDeTiempo {

    /** Las etapas de un caso evaluado por un analista, en orden. */
    val MANUALES = listOf(Etapa.RADICADO, Etapa.REVISION, Etapa.EVALUACION, Etapa.DECISION, Etapa.PAGO)

    /** Las de un siniestro paramétrico resuelto automáticamente, en orden. */
    val AUTOMATICAS = listOf(Etapa.EVENTO_DETECTADO, Etapa.VALIDACION_AUTOMATICA, Etapa.PAGO_EJECUTADO)

    /** La línea de tiempo del siniestro: la automática si un evento externo lo resolvió, la manual si no. */
    fun armar(siniestro: Siniestro, cambios: List<CambioDeEstado>): List<EtapaDeLinea> =
        siniestro.resolucionAutomatica?.let { armarAutomatica(siniestro.estado, it, cambios) } ?: armar(siniestro.estado, cambios)

    fun armar(estado: EstadoDeSiniestro, cambios: List<CambioDeEstado>): List<EtapaDeLinea> {
        val actual = etapaActual(estado)
        val etapas = if (estado == EstadoDeSiniestro.RECHAZADO) MANUALES.dropLast(1) else MANUALES
        return etapas.map { etapa ->
            val avance = when {
                estado == EstadoDeSiniestro.PAGADO || estado == EstadoDeSiniestro.RECHAZADO ->
                    if (etapa.ordinal <= actual.ordinal) Avance.HECHA else Avance.PENDIENTE
                etapa.ordinal < actual.ordinal -> Avance.HECHA
                etapa == actual -> Avance.ACTUAL
                else -> Avance.PENDIENTE
            }
            EtapaDeLinea(
                etapa = etapa,
                avance = avance,
                desde = if (avance == Avance.PENDIENTE) null else inicioDe(etapa, cambios),
                rechazada = etapa == Etapa.DECISION && estado == EstadoDeSiniestro.RECHAZADO,
            )
        }
    }

    /**
     * La línea de tiempo de un siniestro paramétrico (criterio 3): el evento
     * detectado (cuándo se observó), la validación automática de la cobertura
     * (cuándo el motor lo resolvió) y el pago, hecho cuando el caso quedó
     * pagado y actual mientras se paga.
     */
    fun armarAutomatica(
        estado: EstadoDeSiniestro,
        resolucion: ResolucionAutomatica,
        cambios: List<CambioDeEstado>,
    ): List<EtapaDeLinea> {
        val pago = when (estado) {
            EstadoDeSiniestro.PAGADO ->
                EtapaDeLinea(Etapa.PAGO_EJECUTADO, Avance.HECHA, cambios.firstOrNull { it.estado == EstadoDeSiniestro.PAGADO }?.en)
            EstadoDeSiniestro.APROBADO, EstadoDeSiniestro.PAGO_CON_NOVEDAD ->
                EtapaDeLinea(Etapa.PAGO_EJECUTADO, Avance.ACTUAL, cambios.firstOrNull { it.estado == EstadoDeSiniestro.APROBADO }?.en)
            else -> EtapaDeLinea(Etapa.PAGO_EJECUTADO, Avance.PENDIENTE, null)
        }
        return listOf(
            EtapaDeLinea(Etapa.EVENTO_DETECTADO, Avance.HECHA, resolucion.disparador.observadoEn),
            EtapaDeLinea(Etapa.VALIDACION_AUTOMATICA, Avance.HECHA, resolucion.resueltaEn),
            pago,
        )
    }

    /** Cuánto del recorrido lleva el caso, para la barra de M9 y M10: la etapa actual cuenta a medias. */
    fun progreso(estado: EstadoDeSiniestro): Float {
        if (estado.cerrado) return 1f
        return (MANUALES.indexOf(etapaActual(estado)) + 0.5f) / MANUALES.size
    }

    /** El progreso de cualquier siniestro: uno automático solo espera el pago. */
    fun progreso(siniestro: Siniestro): Float = when {
        !siniestro.automatico -> progreso(siniestro.estado)
        siniestro.estado.cerrado -> 1f
        else -> (AUTOMATICAS.size - 0.5f) / AUTOMATICAS.size
    }

    fun etapaActual(estado: EstadoDeSiniestro): Etapa = when (estado) {
        EstadoDeSiniestro.RADICADO, EstadoDeSiniestro.DEVUELTO, EstadoDeSiniestro.DESCONOCIDO -> Etapa.REVISION
        EstadoDeSiniestro.EN_EVALUACION -> Etapa.EVALUACION
        EstadoDeSiniestro.APROBADO, EstadoDeSiniestro.PAGO_CON_NOVEDAD -> Etapa.PAGO
        EstadoDeSiniestro.RECHAZADO -> Etapa.DECISION
        EstadoDeSiniestro.PAGADO -> Etapa.PAGO
    }

    private fun inicioDe(etapa: Etapa, cambios: List<CambioDeEstado>): Long? {
        val estados = when (etapa) {
            Etapa.RADICADO -> setOf(EstadoDeSiniestro.RADICADO)
            Etapa.REVISION -> setOf(EstadoDeSiniestro.EN_EVALUACION, EstadoDeSiniestro.DEVUELTO)
            Etapa.EVALUACION -> setOf(EstadoDeSiniestro.EN_EVALUACION)
            Etapa.DECISION -> setOf(EstadoDeSiniestro.APROBADO, EstadoDeSiniestro.RECHAZADO)
            Etapa.PAGO -> setOf(EstadoDeSiniestro.PAGADO, EstadoDeSiniestro.PAGO_CON_NOVEDAD)
            Etapa.EVENTO_DETECTADO, Etapa.VALIDACION_AUTOMATICA, Etapa.PAGO_EJECUTADO -> emptySet()
        }
        return cambios.firstOrNull { it.estado in estados }?.en
    }
}
