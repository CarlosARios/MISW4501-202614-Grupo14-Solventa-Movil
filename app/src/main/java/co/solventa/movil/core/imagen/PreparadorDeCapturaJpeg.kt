package co.solventa.movil.core.imagen

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reduce la captura del documento con las herramientas del dispositivo, sobre
 * los pasos compartidos de `ReduccionJpeg`.
 *
 * La calidad baja por escalones solo si el resultado no cabe en el tope del
 * contrato, porque cada escalón se nota en la legibilidad del documento y es
 * justo lo que la verificación necesita leer.
 */
@Singleton
class PreparadorDeCapturaJpeg @Inject constructor() : PreparadorDeCaptura {

    override suspend fun aBase64(rutaArchivo: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val reducido = reducirAJpeg(rutaArchivo, LADO_LARGO) ?: return@runCatching null
            try {
                val bytes = comprimirJpeg(reducido, CALIDADES, TOPE_POR_CARA)
                    ?: return@runCatching null
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
