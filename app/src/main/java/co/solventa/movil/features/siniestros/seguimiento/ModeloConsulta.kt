package co.solventa.movil.features.siniestros.seguimiento

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

/** Un siniestro del asegurado (HU-M2.4-01): lo que muestran la tarjeta de M9 y el encabezado de M10. */
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
)

/** Un cambio de estado de la línea de tiempo. */
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

/** El detalle de M10: el siniestro y cada cambio de estado, el más antiguo primero. */
data class DetalleDeSiniestro(val siniestro: Siniestro, val cambios: List<CambioDeEstado>)
