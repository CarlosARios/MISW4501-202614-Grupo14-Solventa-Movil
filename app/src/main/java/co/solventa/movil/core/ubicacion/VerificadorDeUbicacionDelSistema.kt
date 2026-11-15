package co.solventa.movil.core.ubicacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [VerificadorDeUbicacion] con el `LocationManager` del sistema, **sin Google
 * Play Services**, como [LocalizadorDelSistema].
 */
@Singleton
class VerificadorDeUbicacionDelSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : VerificadorDeUbicacion {

    override fun permiso(): PermisoDeUbicacion = when {
        concedido(Manifest.permission.ACCESS_FINE_LOCATION) -> PermisoDeUbicacion.PRECISO
        concedido(Manifest.permission.ACCESS_COARSE_LOCATION) -> PermisoDeUbicacion.APROXIMADO
        else -> PermisoDeUbicacion.NINGUNO
    }

    override fun servicioEncendido(): Boolean {
        val gestor = contexto.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(gestor)
    }

    private fun concedido(permiso: String): Boolean =
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
}
