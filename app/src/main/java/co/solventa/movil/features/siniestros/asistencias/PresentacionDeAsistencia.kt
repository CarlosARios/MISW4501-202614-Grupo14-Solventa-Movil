package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.core.ubicacion.Ubicacion
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** El hemisferio de cada coordenada: el texto (N, S, E, O/W) lo pone el idioma. */
enum class Hemisferio { NORTE, SUR, ESTE, OESTE }

/** «4,7712 N · 74,0452 O», en partes para que el idioma ponga las letras. */
data class CoordenadasVisibles(
    val latitud: String,
    val hemisferioDeLatitud: Hemisferio,
    val longitud: String,
    val hemisferioDeLongitud: Hemisferio,
)

/**
 * Cómo se leen en M14 los datos de la solicitud (HU-M2.3-01, «Datos que
 * enviamos»): coordenadas con cuatro decimales y su hemisferio, precisión en
 * metros y la hora del registro en hora local y en UTC (D10). Con el `Locale`
 * activo: en español la coma decimal y 24 horas; en inglés el punto y AM/PM.
 */
object PresentacionDeAsistencia {

    fun coordenadas(ubicacion: Ubicacion, idioma: Locale): CoordenadasVisibles {
        val formato = NumberFormat.getNumberInstance(idioma).apply {
            minimumFractionDigits = DECIMALES
            maximumFractionDigits = DECIMALES
            roundingMode = RoundingMode.HALF_UP
            isGroupingUsed = false
        }
        return CoordenadasVisibles(
            latitud = formato.format(abs(ubicacion.latitud)),
            hemisferioDeLatitud = if (ubicacion.latitud < 0) Hemisferio.SUR else Hemisferio.NORTE,
            longitud = formato.format(abs(ubicacion.longitud)),
            hemisferioDeLongitud = if (ubicacion.longitud < 0) Hemisferio.OESTE else Hemisferio.ESTE,
        )
    }

    /** La precisión en metros enteros; nunca «0 m», que diría que es exacta. */
    fun precision(ubicacion: Ubicacion, idioma: Locale): String =
        NumberFormat.getIntegerInstance(idioma).format(ubicacion.precisionM.toDouble().roundToLong().coerceAtLeast(1))

    /** «16:31:08»: la hora del encabezado «Ubicación enviada a Solventa». */
    fun horaConSegundos(instante: Long, idioma: Locale, zona: ZoneId = ZoneId.systemDefault()): String =
        formatear(instante, if (enIngles(idioma)) "h:mm:ss a" else "HH:mm:ss", idioma, zona)

    /** «16:31» y «21:31»: la hora local y la UTC de la fila «Hora de registro». */
    fun horaLocalYUtc(instante: Long, idioma: Locale, zona: ZoneId = ZoneId.systemDefault()): Pair<String, String> {
        val patron = if (enIngles(idioma)) "h:mm a" else "HH:mm"
        return formatear(instante, patron, idioma, zona) to formatear(instante, patron, idioma, ZoneOffset.UTC)
    }

    private fun enIngles(idioma: Locale) = idioma.language == Locale.ENGLISH.language

    private fun formatear(instante: Long, patron: String, idioma: Locale, zona: ZoneId): String =
        DateTimeFormatter.ofPattern(patron, idioma).withZone(zona).format(Instant.ofEpochMilli(instante))

    private const val DECIMALES = 4
}
