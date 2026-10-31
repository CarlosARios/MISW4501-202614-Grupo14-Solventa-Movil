package co.solventa.movil.core.imagen

import javax.inject.Inject

/** Lee la luminancia de una foto guardada en el dispositivo. */
fun interface LectorDeLuminancia {
    /** `null` cuando el archivo no se puede decodificar. */
    fun leer(rutaArchivo: String): Luminancia?
}

/**
 * Decodifica el JPEG con el lado largo en [LADO_LARGO] píxeles y extrae su
 * luminancia.
 *
 * La foto de CameraX tiene varios megapíxeles; medir la nitidez a 640 px es
 * lo bastante fino para ver el texto de un documento y se calcula en unas
 * decenas de milisegundos en el Moto G20. Medir siempre al mismo tamaño es
 * además lo que hace comparable el umbral entre teléfonos con cámaras de
 * resolución distinta.
 */
class LectorDeLuminanciaJpeg @Inject constructor() : LectorDeLuminancia {

    override fun leer(rutaArchivo: String): Luminancia? {
        val imagen = reducirAJpeg(rutaArchivo, LADO_LARGO) ?: return null
        return try {
            val pixeles = IntArray(imagen.width * imagen.height)
            imagen.getPixels(pixeles, 0, imagen.width, 0, 0, imagen.width, imagen.height)
            Luminancia(imagen.width, imagen.height, IntArray(pixeles.size) { luminanciaDe(pixeles[it]) })
        } finally {
            imagen.recycle()
        }
    }

    internal companion object {
        const val LADO_LARGO = 640
    }
}
