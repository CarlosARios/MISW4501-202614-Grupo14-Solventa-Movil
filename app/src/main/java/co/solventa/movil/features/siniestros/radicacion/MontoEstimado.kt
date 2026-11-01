package co.solventa.movil.features.siniestros.radicacion

import java.text.NumberFormat
import java.util.Locale

/**
 * El monto estimado del siniestro (HU-M2.2-01), con las mismas reglas que la
 * radicación web (HU-W2.2-01) para que los dos canales lo traten igual:
 * pesos colombianos enteros, opcional, entre 1 y 100.000.000.000, con o sin
 * separadores de miles (`.`, `,` o espacio) en grupos de tres. Al servicio
 * viaja en centavos (`estimated_amount_minor`).
 */
object MontoEstimado {

    const val MINIMO_PESOS = 1L
    const val MAXIMO_PESOS = 100_000_000_000L

    /** Uno a tres dígitos y después grupos de tres, con o sin separador. */
    private val FORMATO = Regex("""^\d{1,3}([.,\s]?\d{3})*$""")

    /** Lo que el asegurado escribió, leído. */
    sealed interface Lectura {
        /** Sin monto: el campo es opcional. */
        data object Vacio : Lectura

        data class Valido(val pesos: Long) : Lectura {
            val centavos: Long get() = pesos * CENTAVOS_POR_PESO
        }

        data class Invalido(val problema: Problema) : Lectura
    }

    enum class Problema { FORMATO, RANGO }

    fun leer(texto: String): Lectura {
        val valor = texto.trim()
        if (valor.isEmpty()) return Lectura.Vacio
        if (!FORMATO.matches(valor)) return Lectura.Invalido(Problema.FORMATO)
        val pesos = valor.filter(Char::isDigit).toLongOrNull()
            ?: return Lectura.Invalido(Problema.RANGO)
        if (pesos < MINIMO_PESOS || pesos > MAXIMO_PESOS) return Lectura.Invalido(Problema.RANGO)
        return Lectura.Valido(pesos)
    }

    /** Los centavos que viajan al servicio, o `null` si no hay un monto válido. */
    fun centavos(texto: String): Long? = (leer(texto) as? Lectura.Valido)?.centavos

    /** «$ 4.800.000 COP» en el idioma de la aplicación. */
    fun aTexto(pesos: Long, locale: Locale): String {
        val formato = NumberFormat.getIntegerInstance(locale)
        return "$ ${formato.format(pesos)} COP"
    }

    private const val CENTAVOS_POR_PESO = 100L
}
