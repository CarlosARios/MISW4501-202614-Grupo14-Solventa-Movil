package co.solventa.movil.features.siniestros.asistencias

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * La dirección legible de unas coordenadas (`place`, opcional en el
 * contrato). El servicio no geocodifica: lo hace el teléfono (D7).
 */
fun interface TraductorDeCoordenadas {
    /** Una línea legible de 3 a 160 caracteres, o `null` si no se pudo. */
    suspend fun traducir(latitud: Double, longitud: Double): String?
}

/**
 * [TraductorDeCoordenadas] con el `Geocoder` del sistema, que envía las
 * coordenadas al proveedor de geocodificación del teléfono. Espera a lo sumo
 * 3 s; sin `Geocoder`, sin resultado o con un error la solicitud sale sin
 * `place` (D7). Lo usa el trabajador, así que el marcador nunca lo espera.
 */
@Singleton
class TraductorConGeocoder internal constructor(
    private val crear: () -> Geocoder,
    private val presente: () -> Boolean,
) : TraductorDeCoordenadas {

    @Inject
    constructor(@ApplicationContext contexto: Context) : this(
        crear = { Geocoder(contexto, Locale.getDefault()) },
        presente = Geocoder::isPresent,
    )

    override suspend fun traducir(latitud: Double, longitud: Double): String? {
        if (!presente()) return null
        val direcciones = withTimeoutOrNull(ESPERA_MAXIMA_MS) { leer(crear(), latitud, longitud) }
        return direcciones?.firstOrNull()?.let(::lineaDe)?.let(ReglasDeAsistencia::lugarValido)
    }

    private suspend fun leer(geocoder: Geocoder, latitud: Double, longitud: Double): List<Address>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuacion ->
                try {
                    geocoder.getFromLocation(
                        latitud,
                        longitud,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(direcciones: MutableList<Address>) {
                                if (continuacion.isActive) continuacion.resume(direcciones)
                            }

                            override fun onError(mensaje: String?) {
                                if (continuacion.isActive) continuacion.resume(null)
                            }
                        },
                    )
                } catch (_: IllegalArgumentException) {
                    continuacion.resume(null)
                }
            }
        } else {
            runInterruptible(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitud, longitud, 1)
                } catch (_: IOException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }

    /** La primera línea de la dirección o, sin ella, calle y ciudad. */
    private fun lineaDe(direccion: Address): String? =
        direccion.getAddressLine(0)
            ?: listOfNotNull(direccion.thoroughfare, direccion.locality).joinToString(", ").ifBlank { null }

    private companion object {
        const val ESPERA_MAXIMA_MS = 3_000L
    }
}
