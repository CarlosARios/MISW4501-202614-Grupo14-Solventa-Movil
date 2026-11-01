package co.solventa.movil.core.ubicacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * La ubicación con el `LocationManager` del sistema.
 *
 * **No usa Google Play Services.** El dato que la evidencia necesita —punto,
 * precisión y hora— lo da el propio sistema, y agregar una dependencia de
 * Google para obtener lo mismo le añadiría peso a la aplicación y una
 * dependencia de servicios que no todos los dispositivos traen.
 *
 * Primero mira la última posición conocida, y si no hay ninguna reciente
 * **pide una lectura nueva**: dentro de un edificio, o en un teléfono recién
 * encendido, el sistema puede no tener nada guardado, y sin pedirla la
 * captura se quedaría siempre sin ubicación.
 */
@Singleton
class LocalizadorDelSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : Localizador {

    private val gestor by lazy {
        contexto.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    override suspend fun ubicacionActual(): Ubicacion? = withContext(Dispatchers.IO) {
        if (!tienePermiso()) return@withContext null
        val gestor = gestor ?: return@withContext null
        laUltimaConocida(gestor) ?: unaLecturaNueva(gestor)
    }

    /** La mejor de las que el sistema ya tiene, si todavía dice dónde está. */
    private fun laUltimaConocida(gestor: LocationManager): Ubicacion? {
        val ahora = System.currentTimeMillis()
        return gestor.proveedores()
            .mapNotNull { proveedor -> ultimaDe(gestor, proveedor) }
            .filter { ahora - it.time <= Ubicacion.VIGENCIA_MS }
            .minByOrNull { it.accuracy.takeIf { precision -> precision > 0 } ?: Float.MAX_VALUE }
            ?.aUbicacion()
    }

    /**
     * Pide una posición al sistema y espera a lo sumo [ESPERA_MAXIMA_MS].
     * Quien está capturando no puede quedarse mirando la pantalla: si no
     * llega a tiempo, la evidencia se guarda sin ubicación confiable.
     */
    private suspend fun unaLecturaNueva(gestor: LocationManager): Ubicacion? =
        withTimeoutOrNull(ESPERA_MAXIMA_MS) {
            suspendCancellableCoroutine { continuacion ->
                // A todos los proveedores a la vez, y manda el primero que
                // conteste: bajo techo el GPS no fija y el de la red sí, y al
                // revés en campo abierto.
                val proveedores = gestor.proveedores()
                if (proveedores.isEmpty()) {
                    continuacion.resume(null)
                    return@suspendCancellableCoroutine
                }
                val escucha = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        gestor.removeUpdates(this)
                        if (continuacion.isActive) continuacion.resume(location.aUbicacion())
                    }

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) = Unit
                }
                try {
                    proveedores.forEach { proveedor ->
                        gestor.requestLocationUpdates(proveedor, 0L, 0f, escucha, Looper.getMainLooper())
                    }
                } catch (revocado: SecurityException) {
                    gestor.removeUpdates(escucha)
                    continuacion.resume(null)
                    return@suspendCancellableCoroutine
                }
                continuacion.invokeOnCancellation { gestor.removeUpdates(escucha) }
            }
        }

    /**
     * El permiso se comprueba antes, pero el sistema puede revocarlo entre
     * una cosa y la otra: sin ubicación la captura sigue, marcada como de
     * ubicación no confiable.
     */
    private fun ultimaDe(gestor: LocationManager, proveedor: String): Location? =
        try {
            gestor.getLastKnownLocation(proveedor)
        } catch (revocado: SecurityException) {
            null
        }

    /** La aproximada sirve: lo que cambia es la precisión que se registra. */
    private fun tienePermiso(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any {
                ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
            }

    /**
     * Los proveedores encendidos, el más preciso primero. El pasivo queda
     * fuera: solo entrega lo que otra aplicación pida, así que esperar por él
     * es esperar a nadie.
     */
    private fun LocationManager.proveedores(): List<String> =
        runCatching { getProviders(true) }.getOrDefault(emptyList())
            .filterNot { it == LocationManager.PASSIVE_PROVIDER }
            .sortedBy { if (it == LocationManager.GPS_PROVIDER) 0 else 1 }

    private fun Location.aUbicacion() = Ubicacion(
        latitud = latitude,
        longitud = longitude,
        altitud = if (hasAltitude()) altitude else null,
        precisionM = accuracy,
        leidaEn = time,
    )

    private companion object {
        /** Lo que la captura espera por una posición nueva. */
        const val ESPERA_MAXIMA_MS = 8_000L
    }
}
