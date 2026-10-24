package co.solventa.movil.core.imagen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Reduce la captura con las herramientas del dispositivo.
 *
 * Son dos pasos: primero se decodifica saltando píxeles —lo que evita cargar
 * la foto completa en memoria— y después se escala al lado largo exacto. La
 * calidad baja por escalones solo si el resultado no cabe en el tope del
 * contrato, porque cada escalón se nota en la legibilidad del documento y es
 * justo lo que la verificación necesita leer.
 */
@Singleton
class PreparadorDeCapturaJpeg @Inject constructor() : PreparadorDeCaptura {

    override suspend fun aBase64(rutaArchivo: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val reducido = reducir(rutaArchivo) ?: return@runCatching null
            try {
                val bytes = comprimir(reducido) ?: return@runCatching null
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } finally {
                reducido.recycle()
            }
        }.getOrNull()
    }

    override suspend fun descartar(rutaArchivo: String) {
        withContext(Dispatchers.IO) {
            runCatching { File(rutaArchivo).delete() }
        }
    }

    /** Decodifica la foto con el lado largo en [LADO_LARGO] píxeles. */
    private fun reducir(rutaArchivo: String): Bitmap? {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(rutaArchivo, medidas)
        val ladoLargo = maxOf(medidas.outWidth, medidas.outHeight)
        if (ladoLargo <= 0) return null

        val opciones = BitmapFactory.Options().apply { inSampleSize = salto(ladoLargo) }
        val decodificado = BitmapFactory.decodeFile(rutaArchivo, opciones) ?: return null
        val largoDecodificado = maxOf(decodificado.width, decodificado.height)
        if (largoDecodificado <= LADO_LARGO) return decodificado

        val factor = LADO_LARGO.toFloat() / largoDecodificado
        val escalado = Bitmap.createScaledBitmap(
            decodificado,
            (decodificado.width * factor).roundToInt().coerceAtLeast(1),
            (decodificado.height * factor).roundToInt().coerceAtLeast(1),
            true,
        )
        if (escalado !== decodificado) decodificado.recycle()
        return escalado
    }

    /** Comprime a JPEG y baja la calidad solo si no cabe en el tope. */
    private fun comprimir(imagen: Bitmap): ByteArray? {
        var ultimo: ByteArray? = null
        for (calidad in CALIDADES) {
            val salida = ByteArrayOutputStream()
            if (!imagen.compress(Bitmap.CompressFormat.JPEG, calidad, salida)) return null
            ultimo = salida.toByteArray()
            if (ultimo.size <= TOPE_POR_CARA) return ultimo
        }
        return ultimo
    }

    /**
     * Potencia de dos con la que el decodificador salta píxeles sin bajar del
     * lado largo que se quiere.
     */
    private fun salto(ladoLargo: Int): Int {
        var salto = 1
        while (ladoLargo / (salto * 2) >= LADO_LARGO) {
            salto *= 2
        }
        return salto
    }

    private companion object {
        /**
         * Lado largo de la imagen enviada. Con 1600 píxeles el número del
         * documento se sigue leyendo y cada cara queda muy por debajo del tope.
         */
        const val LADO_LARGO = 1600

        /** Escalones de calidad del JPEG, del mejor al peor aceptable. */
        val CALIDADES = listOf(80, 60, 45)

        /** El tope por cara del contrato: 1,5 MB una vez decodificada. */
        const val TOPE_POR_CARA = 1_500_000
    }
}
