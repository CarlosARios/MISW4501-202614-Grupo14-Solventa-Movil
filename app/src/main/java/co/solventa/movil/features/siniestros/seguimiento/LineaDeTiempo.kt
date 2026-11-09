package co.solventa.movil.features.siniestros.seguimiento

/** Las etapas de la línea de tiempo de M10, en orden. */
enum class Etapa { RADICADO, REVISION, EVALUACION, DECISION, PAGO }

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

    fun armar(estado: EstadoDeSiniestro, cambios: List<CambioDeEstado>): List<EtapaDeLinea> {
        val actual = etapaActual(estado)
        val etapas = if (estado == EstadoDeSiniestro.RECHAZADO) Etapa.entries.dropLast(1) else Etapa.entries
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

    /** Cuánto del recorrido lleva el caso, para la barra de M9 y M10: la etapa actual cuenta a medias. */
    fun progreso(estado: EstadoDeSiniestro): Float {
        if (estado.cerrado) return 1f
        val total = Etapa.entries.size
        return (etapaActual(estado).ordinal + 0.5f) / total
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
        }
        return cambios.firstOrNull { it.estado in estados }?.en
    }
}
