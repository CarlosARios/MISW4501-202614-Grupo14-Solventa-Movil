package co.solventa.movil.core.ubicacion

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * El permiso y el servicio de ubicación del sistema (Robolectric,
 * HU-M2.3-01, criterio 1): precisa, aproximada o ninguna; servicio encendido
 * o apagado.
 */
@RunWith(AndroidJUnit4::class)
class VerificadorDeUbicacionTest {

    private val aplicacion = ApplicationProvider.getApplicationContext<Application>()
    private val verificador = VerificadorDeUbicacionDelSistema(aplicacion)

    @Test
    fun `sin permisos no hay ninguno`() {
        shadowOf(aplicacion).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        assertEquals(PermisoDeUbicacion.NINGUNO, verificador.permiso())
    }

    @Test
    fun `con solo la aproximada es aproximado`() {
        shadowOf(aplicacion).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(aplicacion).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

        assertEquals(PermisoDeUbicacion.APROXIMADO, verificador.permiso())
    }

    @Test
    fun `con la precisa es preciso`() {
        shadowOf(aplicacion).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        assertEquals(PermisoDeUbicacion.PRECISO, verificador.permiso())
    }

    @Test
    fun `el servicio encendido y apagado`() {
        val gestor = aplicacion.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        shadowOf(gestor).setLocationEnabled(true)
        assertTrue(verificador.servicioEncendido())

        shadowOf(gestor).setLocationEnabled(false)
        assertFalse(verificador.servicioEncendido())
    }
}
