package co.solventa.movil.features.siniestros.radicacion

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * La fecha y la hora del hecho, entre lo que se escribe y lo que se guarda.
 *
 * En pantalla es `DD/MM/AAAA HH:MM`, como lo pide el sistema de diseño: el
 * texto de ayuda del campo muestra el formato. Dentro es un instante, que es
 * lo que el contrato recibe en UTC.
 *
 * La conversión usa la zona horaria del dispositivo: quien reporta escribe la
 * hora de su reloj, no la del servidor.
 */
object FechaDelHecho {

    /**
     * `uuuu` y no `yyyy`, y resolucion estricta: asi una fecha que no existe
     * —el 31 de febrero— se rechaza en lugar de correrse al ultimo dia del
     * mes, que es lo que hace la resolucion por omision.
     */
    private val formato: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM/uuuu · HH:mm")
        .withResolverStyle(ResolverStyle.STRICT)

    /** Devuelve `null` cuando lo escrito todavía no es una fecha completa. */
    fun aInstante(texto: String, zona: ZoneId = ZoneId.systemDefault()): Long? = try {
        LocalDateTime.parse(texto.trim(), formato).atZone(zona).toInstant().toEpochMilli()
    } catch (incompleta: DateTimeParseException) {
        null
    }

    /**
     * Arma el instante con el dia que devuelve el selector de fecha —que es
     * medianoche en UTC— y la hora que devuelve el de hora, ya en la zona del
     * dispositivo.
     */
    /** `14/09/2026` del instante que el servicio devolvio en RFC 3339. */
    fun diaDeRfc3339(texto: String, zona: ZoneId = ZoneId.systemDefault()): String =
        enZona(texto, zona)?.format(DateTimeFormatter.ofPattern("dd/MM/uuuu")).orEmpty()

    /** `18:55` del mismo instante. */
    fun horaDeRfc3339(texto: String, zona: ZoneId = ZoneId.systemDefault()): String =
        enZona(texto, zona)?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()

    private fun enZona(texto: String, zona: ZoneId): LocalDateTime? =
        runCatching {
            LocalDateTime.ofInstant(java.time.Instant.parse(texto.trim()), zona)
        }.getOrNull()

    fun combinar(
        diaEnUtc: Long,
        hora: Int,
        minuto: Int,
        zona: ZoneId = ZoneId.systemDefault(),
    ): Long = java.time.Instant.ofEpochMilli(diaEnUtc)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
        .atTime(hora, minuto)
        .atZone(zona)
        .toInstant()
        .toEpochMilli()

    fun aTexto(instante: Long?, zona: ZoneId = ZoneId.systemDefault()): String =
        instante?.let {
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), zona).format(formato)
        }.orEmpty()
}
