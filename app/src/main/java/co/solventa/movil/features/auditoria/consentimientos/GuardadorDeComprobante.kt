package co.solventa.movil.features.auditoria.consentimientos

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Escribe el comprobante en PDF donde el titular eligio con el selector del
 * sistema (`ActivityResultContracts.CreateDocument`): la aplicacion no
 * necesita permisos de almacenamiento ni un `FileProvider` (HU-M1.3-02, D-4 a).
 */
fun interface GuardadorDeComprobante {
    /** Devuelve si quedo guardado. [destino] es el `Uri` que entrego el selector. */
    suspend fun guardar(destino: String, bytes: ByteArray): Boolean
}

/** [GuardadorDeComprobante] con el `ContentResolver` del sistema. */
class GuardadorDeComprobanteAndroid @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : GuardadorDeComprobante {

    override suspend fun guardar(destino: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            contexto.contentResolver.openOutputStream(destino.toUri())?.use { it.write(bytes) } != null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
