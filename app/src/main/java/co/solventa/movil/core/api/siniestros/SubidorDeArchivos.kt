package co.solventa.movil.core.api.siniestros

import co.solventa.movil.core.di.SinSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sube el archivo de una evidencia a la URL firmada (HU-M2.1-01).
 *
 * **Va por un cliente sin sesión.** La firma viaja en la URL y cubre unos
 * encabezados concretos: cualquier otro —el `Authorization` de la aplicación,
 * el de correlación— hace que el almacenamiento rechace la subida. Por eso no
 * usa el cliente compartido ni Retrofit.
 */
@Singleton
class SubidorDeArchivos @Inject constructor(
    @SinSesion private val cliente: OkHttpClient,
) {

    /**
     * Devuelve `true` cuando el archivo quedó guardado. Un `403` es una URL
     * vencida o una firma que no corresponde: se resuelve pidiendo otra.
     */
    suspend fun subir(archivo: File, url: String, encabezados: Map<String, String>): Boolean =
        withContext(Dispatchers.IO) {
            val tipo = encabezados.entries
                .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                ?.value
                ?.toMediaTypeOrNull()
            val peticion = Request.Builder()
                .url(url)
                .put(archivo.asRequestBody(tipo))
                .apply {
                    encabezados.forEach { (nombre, valor) ->
                        // OkHttp calcula la longitud del cuerpo; repetirla
                        // produce un encabezado duplicado y una firma inválida.
                        if (!nombre.equals("Content-Length", ignoreCase = true)) {
                            header(nombre, valor)
                        }
                    }
                }
                .build()

            runCatching {
                cliente.newCall(peticion).execute().use { it.isSuccessful }
            }.getOrElse { error ->
                if (error is IOException) false else throw error
            }
        }
}
