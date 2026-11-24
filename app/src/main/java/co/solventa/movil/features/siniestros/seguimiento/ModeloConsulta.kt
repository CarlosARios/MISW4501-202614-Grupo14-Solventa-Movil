package co.solventa.movil.features.siniestros.seguimiento

import kotlinx.serialization.Serializable

/**
 * El estado de un siniestro, como lo nombra el servicio de siniestros. Un
 * código que la aplicación todavía no conoce queda como [DESCONOCIDO] y se
 * muestra tal cual, en lugar de esconderlo.
 */
enum class EstadoDeSiniestro(val codigo: String) {
    RADICADO("active"),
    DEVUELTO("returned"),
    EN_EVALUACION("in_evaluation"),
    APROBADO("approved"),
    PAGADO("paid"),
    RECHAZADO("rejected"),
    PAGO_CON_NOVEDAD("payment_exception"),
    DESCONOCIDO(""),
    ;

    /** Un caso cerrado ya no cambia: va en «Cerrados» de M9. */
    val cerrado: Boolean get() = this == PAGADO || this == RECHAZADO

    companion object {
        fun de(codigo: String): EstadoDeSiniestro = entries.firstOrNull { it.codigo == codigo && it != DESCONOCIDO } ?: DESCONOCIDO
    }
}

/**
 * Un siniestro del asegurado (HU-M2.4-01): lo que muestran la tarjeta de M9 y
 * el encabezado de M10. Es serializable para guardarlo en la copia local sin
 * conexión (criterio 5).
 */
@Serializable
data class Siniestro(
    val id: String,
    /** `SIN-<año>-<cinco dígitos>`. */
    val radicado: String,
    val estado: EstadoDeSiniestro,
    /** El código que dio el servicio, para mostrarlo si no se conoce. */
    val codigoDeEstado: String,
    val poliza: String,
    /** `auto`, `home`, `travel` o `life`; en un caso del móvil se deduce del número de la póliza. */
    val linea: String?,
    val producto: String?,
    val objetoAsegurado: String?,
    /** `collision`, `theft`, `water_damage`…, tal cual lo da el servicio. */
    val tipoDeIncidente: String,
    val ocurrioEn: Long,
    val radicadoEn: Long,
    /** Solo en el detalle. */
    val lugar: String? = null,
    /** Lo que resolvió el siniestro paramétrico sin evaluación manual (criterio 3); `null` en los demás. */
    val resolucionAutomatica: ResolucionAutomatica? = null,
) {
    val automatico: Boolean get() = resolucionAutomatica != null
}

/**
 * El evento externo que cruzó el umbral de la póliza y generó el pago
 * (HU-M2.4-01, criterio 3): el parámetro objetivo, su umbral y el valor
 * observado.
 */
@Serializable
data class Disparador(
    /** `flight_delay`… */
    val parametro: String,
    /** Lo observado, por ejemplo el vuelo `AV-8421`. */
    val referencia: String,
    val umbral: Double,
    val observado: Double,
    /** `minutes`… */
    val unidad: String,
    val observadoEn: Long,
)

/** La resolución automática de un siniestro paramétrico. */
@Serializable
data class ResolucionAutomatica(val resueltaEn: Long, val version: String, val disparador: Disparador)

/**
 * El tiempo estimado de resolución de un caso en evaluación (criterio 2),
 * del triage del servicio: el nivel y los días hábiles.
 */
@Serializable
data class EstimacionDeResolucion(val nivel: Int?, val diasHabiles: Int)

/** Un cambio de estado de la línea de tiempo. */
@Serializable
data class CambioDeEstado(val estado: EstadoDeSiniestro, val en: Long)

/**
 * De dónde salió una consulta (HA-L02). [desdeCache] es `true` cuando el
 * servicio no alcanzó a leer su base y respondió la última versión guardada;
 * [leidoEn] es cuándo se leyeron esos datos: la hora del pie «Actualizado hoy
 * a las 14:30» de M10, o la de «Caché 14:30» de M9c.
 */
data class Frescura(val desdeCache: Boolean, val leidoEn: Long) {
    companion object {
        /**
         * La frescura de una consulta que juntó varias respuestas: viene de la
         * caché si alguna vino, y vale la hora más antigua.
         */
        fun combinar(una: Frescura, otra: Frescura): Frescura =
            Frescura(una.desdeCache || otra.desdeCache, minOf(una.leidoEn, otra.leidoEn))
    }
}

/**
 * El detalle de M10: el siniestro, cada cambio de estado (el más antiguo
 * primero) y, en evaluación, la estimación del triage si ya la hay.
 */
@Serializable
data class DetalleDeSiniestro(
    val siniestro: Siniestro,
    val cambios: List<CambioDeEstado>,
    val estimacion: EstimacionDeResolucion? = null,
)
