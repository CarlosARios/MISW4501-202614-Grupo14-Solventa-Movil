package co.solventa.movil.core.i18n

import java.util.Locale

/**
 * Los idiomas que la aplicación ofrece, los mismos que tienen traducción en
 * `values/` y `values-en/`.
 *
 * **La etiqueta no se traduce.** Un selector de idioma muestra cada opción en
 * su propio idioma: quien tiene la aplicación en inglés y busca español tiene
 * que leer «Español», no «Spanish». Por eso el texto está aquí y no en los
 * recursos.
 */
enum class Idioma(val etiqueta: String, private val etiquetaDeLocale: String) {
    ESPANOL("Español (Colombia)", "es-CO"),
    INGLES("English", "en"),
    ;

    fun aLocale(): Locale = Locale.forLanguageTag(etiquetaDeLocale)

    companion object {
        /**
         * El que mejor corresponde al idioma del dispositivo. Cualquier
         * variante del español es español; lo demás cae en inglés, que es el
         * otro idioma que la aplicación tiene traducido.
         */
        fun delSistema(locale: Locale): Idioma =
            if (locale.language.equals("es", ignoreCase = true)) ESPANOL else INGLES

        /** El guardado, o `null` si lo que hay no corresponde a ninguno. */
        fun deNombre(nombre: String?): Idioma? =
            entries.firstOrNull { it.name == nombre }
    }
}
