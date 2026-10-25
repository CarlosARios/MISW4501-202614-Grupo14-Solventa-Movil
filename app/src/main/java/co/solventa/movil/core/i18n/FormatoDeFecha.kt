package co.solventa.movil.core.i18n

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Fechas y horas en el formato del idioma elegido (HU-M1.3-01), en la zona del
 * dispositivo: en español `dd/MM/aaaa` y `HH:mm`, como el prototipo y la fecha
 * del hecho de la radicacion; en ingles `MM/dd/yyyy` y `h:mm a`.
 */
object FormatoDeFecha {

    fun fecha(instante: Long, idioma: Idioma, zona: ZoneId = ZoneId.systemDefault()): String =
        formatear(instante, if (idioma == Idioma.ESPANOL) "dd/MM/uuuu" else "MM/dd/uuuu", idioma, zona)

    fun hora(instante: Long, idioma: Idioma, zona: ZoneId = ZoneId.systemDefault()): String =
        formatear(instante, if (idioma == Idioma.ESPANOL) "HH:mm" else "h:mm a", idioma, zona)

    /**
     * La hora con segundos, para las horas exactas de una revocacion
     * (HU-M1.3-02, frames `38:1414` y `1379:3687`: «16:10:06»).
     */
    fun horaConSegundos(instante: Long, idioma: Idioma, zona: ZoneId = ZoneId.systemDefault()): String =
        formatear(instante, if (idioma == Idioma.ESPANOL) "HH:mm:ss" else "h:mm:ss a", idioma, zona)

    private fun formatear(instante: Long, patron: String, idioma: Idioma, zona: ZoneId): String =
        DateTimeFormatter.ofPattern(patron, idioma.aLocale()).withZone(zona).format(Instant.ofEpochMilli(instante))
}
