package co.solventa.movil.core.imagen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Lo que comparten las dos reducciones de captura del teléfono: la del
 * documento de identidad (HU-M1.1-01), que viaja en base64 dentro del cuerpo,
 * y la de la evidencia del siniestro (HU-M2.1-01), que se sube como archivo.
 *
 * Las dos tienen el mismo problema: la foto que produce CameraX pesa varios
 * megabytes, muy por encima del tope que acepta cada contrato, así que
 * reducirla no es una optimización sino parte de poder enviarla. Lo que
 * cambia es el lado largo y el tope, que cada una fija.
 */

/** Decodifica la foto con el lado largo en [ladoLargo] píxeles. */
internal fun reducirAJpeg(rutaArchivo: String, ladoLargo: Int): Bitmap? {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(rutaArchivo, medidas)
    val ladoDelArchivo = maxOf(medidas.outWidth, medidas.outHeight)
    if (ladoDelArchivo <= 0) return null

    val opciones = BitmapFactory.Options().apply { inSampleSize = salto(ladoDelArchivo, ladoLargo) }
    val decodificado = BitmapFactory.decodeFile(rutaArchivo, opciones) ?: return null
    val largoDecodificado = maxOf(decodificado.width, decodificado.height)
    if (largoDecodificado <= ladoLargo) return decodificado

    val factor = ladoLargo.toFloat() / largoDecodificado
    val escalado = Bitmap.createScaledBitmap(
        decodificado,
        (decodificado.width * factor).roundToInt().coerceAtLeast(1),
        (decodificado.height * factor).roundToInt().coerceAtLeast(1),
        true,
    )
    if (escalado !== decodificado) decodificado.recycle()
    return escalado
}

/** Comprime a JPEG y baja la calidad solo si no cabe en [tope]. */
internal fun comprimirJpeg(imagen: Bitmap, calidades: List<Int>, tope: Int): ByteArray? {
    var ultimo: ByteArray? = null
    for (calidad in calidades) {
        val salida = ByteArrayOutputStream()
        if (!imagen.compress(Bitmap.CompressFormat.JPEG, calidad, salida)) return null
        ultimo = salida.toByteArray()
        if (ultimo.size <= tope) return ultimo
    }
    return ultimo
}

/**
 * Potencia de dos con la que el decodificador salta píxeles sin bajar del
 * lado largo que se quiere.
 */
private fun salto(ladoDelArchivo: Int, ladoLargo: Int): Int {
    var salto = 1
    while (ladoDelArchivo / (salto * 2) >= ladoLargo) {
        salto *= 2
    }
    return salto
}
