package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.core.api.siniestros.EvidenciaRadicada

/**
 * Las cuatro etapas del asistente (HU-M2.2-01). El orden es el del proceso y
 * no una decisión de interfaz: la declaración bajo gravedad de juramento solo
 * tiene sentido sobre lo que ya se diligenció.
 */
enum class Etapa { IDENTIFICACION, HECHOS, EVIDENCIAS, DECLARACION }

/**
 * Tipo de incidente. Los cuatro del frame M11a; el valor que viaja al
 * servicio es el del contrato, en inglés como el resto de su vocabulario.
 */
enum class TipoDeIncidente(val valorDelContrato: String) {
    COLISION("collision"),
    HURTO("theft"),
    DANOS_POR_TERCEROS("third_party_damage"),
    OTRO("other"),
}

/**
 * Lo que el asistente reúne. Es también lo que se guarda como borrador, así
 * que no contiene nada que no pueda escribirse en el dispositivo.
 *
 * `ocurrioEn` son milisegundos desde el epoch, no una fecha con formato: el
 * formato es cosa de la pantalla y del contrato, cada uno con el suyo.
 */
data class Radicacion(
    val poliza: String = "",
    val tipo: TipoDeIncidente? = null,
    val ocurrioEn: Long? = null,
    val lugar: String = "",
    val descripcion: String = "",
    /** Respuestas de las preguntas que dependen del tipo de incidente. */
    val respuestas: Map<String, String> = emptyMap(),
    val declaracionAceptada: Boolean = false,
    /**
     * El monto estimado tal como se escribió (HU-M2.2-01): opcional, en pesos.
     * Se guarda el texto y no el número para que el borrador conserve lo que
     * el asegurado escribió aunque todavía no sea válido.
     */
    val montoEstimado: String = "",
)

/** Campo que el servicio puede rechazar, para marcarlo en su etapa. */
enum class CampoDeRadicacion(val etapa: Etapa) {
    POLIZA(Etapa.IDENTIFICACION),
    TIPO(Etapa.IDENTIFICACION),
    FECHA(Etapa.HECHOS),
    LUGAR(Etapa.HECHOS),
    DESCRIPCION(Etapa.HECHOS),
    MONTO(Etapa.HECHOS),
    RESPUESTAS(Etapa.HECHOS),
    EVIDENCIAS(Etapa.EVIDENCIAS),
    DECLARACION(Etapa.DECLARACION),
}

/** El caso creado, con el número que se le muestra al asegurado. */
data class CasoRadicado(
    val casoId: String,
    val radicado: String,
    val radicadoEn: String,
    /** Lo que el servicio asoció al caso. */
    val fotos: Int = 0,
    val videos: Int = 0,
)

sealed interface ResultadoRadicacion {
    data class Exitoso(val caso: CasoRadicado) : ResultadoRadicacion

    /** `422`: el usuario corrige en la etapa del campo y vuelve a enviar. */
    data class DatosInvalidos(val campos: Set<CampoDeRadicacion>) : ResultadoRadicacion

    /**
     * `503`, red o tiempo agotado. La referencia es el identificador de
     * correlación de la petición, con el que soporte la encuentra.
     */
    data class NoDisponible(val referencia: String) : ResultadoRadicacion
}

interface RadicacionRepositorio {
    /**
     * Radica el siniestro. `borrador` es la clave de idempotencia: reintentar
     * con la misma devuelve el caso que ya se creó.
     */
    /**
     * Radica el siniestro. Las evidencias ya están en el repositorio: aquí
     * viajan sus llaves y sus metadatos (HU-M2.1-01).
     */
    suspend fun radicar(
        borrador: String,
        radicacion: Radicacion,
        evidencias: List<EvidenciaRadicada> = emptyList(),
    ): ResultadoRadicacion
}
