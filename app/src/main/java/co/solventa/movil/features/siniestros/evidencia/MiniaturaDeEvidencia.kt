package co.solventa.movil.features.siniestros.evidencia

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * La imagen de una evidencia: la miniatura de la cuadrícula o la vista a
 * pantalla completa, según el lado que se pida.
 *
 * Decodifica saltando píxeles, de modo que en memoria entra una imagen del
 * tamaño en que se va a ver y no la captura completa: seis evidencias sin
 * reducir llenarían la memoria de la pantalla.
 *
 * **Sin biblioteca de carga de imágenes.** Para unos archivos locales, sumar
 * Coil o Glide pesaría más que estas líneas.
 */
@Composable
fun ImagenDeEvidencia(
    rutaArchivo: String,
    modifier: Modifier = Modifier,
    ladoMaximo: Int = LADO_DE_LA_MINIATURA,
    esVideo: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var imagen by remember(rutaArchivo, ladoMaximo) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(rutaArchivo, ladoMaximo) {
        imagen = withContext(Dispatchers.IO) {
            if (esVideo) cuadroDelVideo(rutaArchivo, ladoMaximo) else decodificar(rutaArchivo, ladoMaximo)
        }
    }

    val bitmap = imagen
    if (bitmap == null) {
        Box(modifier = modifier)
        return
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}

/** La miniatura de la cuadrícula del paso 3. */
@Composable
fun MiniaturaDeEvidencia(rutaArchivo: String, modifier: Modifier = Modifier, esVideo: Boolean = false) {
    ImagenDeEvidencia(rutaArchivo = rutaArchivo, modifier = modifier, esVideo = esVideo)
}

private fun decodificar(rutaArchivo: String, ladoMaximo: Int): Bitmap? = runCatching {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(rutaArchivo, medidas)
    val ladoLargo = maxOf(medidas.outWidth, medidas.outHeight)
    if (ladoLargo <= 0) return null

    var salto = 1
    while (ladoLargo / (salto * 2) >= ladoMaximo) {
        salto *= 2
    }
    BitmapFactory.decodeFile(rutaArchivo, BitmapFactory.Options().apply { inSampleSize = salto })
}.getOrNull()

/**
 * Un cuadro del video, para que su casilla no quede en negro.
 *
 * Con `MediaMetadataRetriever` y no con `ThumbnailUtils`, que pide API 29 y
 * el mínimo del proyecto es 26 —el Moto G20 de las pruebas corre Android 11,
 * pero la aplicación se instala desde Android 8—.
 */
private fun cuadroDelVideo(rutaArchivo: String, @Suppress("UNUSED_PARAMETER") ladoMaximo: Int): Bitmap? {
    val lector = MediaMetadataRetriever()
    return try {
        lector.setDataSource(rutaArchivo)
        lector.frameAtTime
    } catch (sinCuadro: RuntimeException) {
        null
    } finally {
        lector.release()
    }
}

/** Lado largo de la miniatura, en píxeles: la casilla no da para más. */
private const val LADO_DE_LA_MINIATURA = 256
