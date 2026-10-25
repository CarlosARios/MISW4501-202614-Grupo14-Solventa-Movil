package co.solventa.movil.features.siniestros.evidencia

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Tamaño y resumen de integridad de un archivo capturado. */
data class Huella(val sha256: String, val tamanoBytes: Long)

/**
 * Calcula el resumen de integridad de la evidencia (HU-M2.1-01, criterio 4).
 *
 * Es lo que permite comprobar después que el archivo no fue alterado: viaja
 * en la firma de la subida, el almacenamiento lo verifica al recibirlo y el
 * servicio lo guarda con el caso.
 */
interface HuellaDeArchivo {
    /** `null` cuando el archivo no existe o no se puede leer. */
    suspend fun de(archivo: File): Huella?
}

@Singleton
class HuellaSha256 @Inject constructor() : HuellaDeArchivo {

    override suspend fun de(archivo: File): Huella? = withContext(Dispatchers.IO) {
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            archivo.inputStream().use { entrada ->
                val bloque = ByteArray(TAMANO_DEL_BLOQUE)
                while (true) {
                    val leidos = entrada.read(bloque)
                    if (leidos <= 0) break
                    digest.update(bloque, 0, leidos)
                }
            }
            Huella(
                sha256 = digest.digest().joinToString("") { "%02x".format(it) },
                tamanoBytes = archivo.length(),
            )
        }.getOrNull()
    }

    private companion object {
        /** Un video no cabe en memoria: se lee por bloques. */
        const val TAMANO_DEL_BLOQUE = 64 * 1024
    }
}
