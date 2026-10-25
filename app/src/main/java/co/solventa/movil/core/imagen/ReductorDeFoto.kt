package co.solventa.movil.core.imagen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deja la fotografía de una evidencia dentro del tamaño que el contrato
 * acepta (HU-M2.1-01, criterio 5): lado largo de 2048 píxeles y hasta 2 MB.
 *
 * No es cosmético. La foto se sube con una URL firmada que fija el tamaño, y
 * el servicio rechaza la que pase del tope: sin reducirla, en un teléfono de
 * sensor grande la casilla no se podría llenar.
 *
 * Es una interfaz porque lo que hay debajo es `Bitmap`, que solo existe en el
 * dispositivo: así el repositorio de evidencia se prueba en la JVM.
 */
interface ReductorDeFoto {
    /**
     * Reescribe el archivo reducido. Devuelve `false` si no se pudo leer o
     * comprimir, y entonces el archivo queda como estaba.
     */
    suspend fun reducir(archivo: File): Boolean
}

@Singleton
class ReductorDeFotoJpeg @Inject constructor() : ReductorDeFoto {

    override suspend fun reducir(archivo: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val reducido = reducirAJpeg(archivo.absolutePath, LADO_LARGO) ?: return@runCatching false
            val bytes = try {
                comprimirJpeg(reducido, CALIDADES, TOPE)
            } finally {
                reducido.recycle()
            } ?: return@runCatching false
            archivo.writeBytes(bytes)
            true
        }.getOrDefault(false)
    }

    private companion object {
        /** El lado largo que fija el criterio 5. */
        const val LADO_LARGO = 2048

        /** Escalones de calidad del JPEG, del mejor al peor aceptable. */
        val CALIDADES = listOf(85, 70, 55)

        /** El tope por fotografía del contrato: 2 MB. */
        const val TOPE = 2_000_000
    }
}
