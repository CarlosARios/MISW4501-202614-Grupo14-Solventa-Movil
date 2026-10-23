package co.solventa.movil.features.siniestros.radicacion

/** Estado del envío de la radicación. */
sealed interface EstadoDelEnvio {
    data object NoIniciado : EstadoDelEnvio
    data object Enviando : EstadoDelEnvio
    data class Radicado(val caso: CasoRadicado) : EstadoDelEnvio

    /**
     * El servicio no respondió. El borrador se conserva, así que el reintento
     * usa la misma clave y no crea un segundo caso.
     */
    data class NoDisponible(val referencia: String) : EstadoDelEnvio
}

data class RadicacionUiState(
    /** Identificador del borrador; es la clave con la que se radica. */
    val borradorId: String = "",
    val etapa: Etapa = Etapa.IDENTIFICACION,
    val radicacion: Radicacion = Radicacion(),
    /** Las polizas vigentes entre las que se elige en el paso 1. */
    val polizas: List<PolizaElegible> = emptyList(),
    /** Campos que el servicio rechazó con un `422`. */
    val camposRechazados: Set<CampoDeRadicacion> = emptySet(),
    val envio: EstadoDelEnvio = EstadoDelEnvio.NoIniciado,
    /** Cierto mientras se lee el borrador guardado. */
    val cargando: Boolean = true,
    /**
     * Cierto cuando al abrir el asistente habia un reporte sin terminar y
     * todavia no se decide si se retoma o se descarta (criterio 7).
     */
    val retomandoBorrador: Boolean = false,
) {
    /** Lo que falta en la etapa actual. Vacío significa que se puede avanzar. */
    val faltantes: Set<CampoDeRadicacion>
        get() = faltantesDe(etapa)

    /** Criterio 3: el botón de avanzar depende de lo que falte. */
    val puedeAvanzar: Boolean
        get() = faltantes.isEmpty() && envio != EstadoDelEnvio.Enviando

    /** Criterio 5: cuánto se lleva recorrido, de 0 a 1. */
    val avance: Float
        get() = (etapa.ordinal + 1f) / Etapa.entries.size

    /** La poliza elegida, para mostrarla en el selector. */
    val polizaElegida: PolizaElegible?
        get() = polizas.firstOrNull { it.id == radicacion.poliza }

    val esPrimeraEtapa: Boolean
        get() = etapa == Etapa.IDENTIFICACION

    val esUltimaEtapa: Boolean
        get() = etapa == Etapa.DECLARACION

    /**
     * Lo que falta en una etapa cualquiera. La de evidencias no exige nada
     * todavía: el mínimo por tipo de siniestro llega con la captura de
     * HU-M2.1-01.
     */
    fun faltantesDe(etapa: Etapa): Set<CampoDeRadicacion> = when (etapa) {
        Etapa.IDENTIFICACION -> buildSet {
            if (radicacion.poliza.isBlank()) add(CampoDeRadicacion.POLIZA)
            if (radicacion.tipo == null) add(CampoDeRadicacion.TIPO)
        }

        Etapa.HECHOS -> buildSet {
            if (radicacion.ocurrioEn == null) add(CampoDeRadicacion.FECHA)
            if (radicacion.lugar.trim().length < MINIMO_LUGAR) add(CampoDeRadicacion.LUGAR)
            if (radicacion.descripcion.trim().length < MINIMO_DESCRIPCION) {
                add(CampoDeRadicacion.DESCRIPCION)
            }
            if (preguntasSinResponder().isNotEmpty()) add(CampoDeRadicacion.RESPUESTAS)
        }

        Etapa.EVIDENCIAS -> emptySet()

        Etapa.DECLARACION -> buildSet {
            if (!radicacion.declaracionAceptada) add(CampoDeRadicacion.DECLARACION)
        }
    }

    /** Las preguntas del tipo elegido que todavía no tienen respuesta. */
    fun preguntasSinResponder(): List<PreguntaAdaptativa> =
        preguntasDe(radicacion.tipo).filter {
            it.obligatoria && radicacion.respuestas[it.id].isNullOrBlank()
        }

    companion object {
        /** Los mismos mínimos que valida el servicio. */
        const val MINIMO_LUGAR = 3
        const val MINIMO_DESCRIPCION = 10
    }
}
