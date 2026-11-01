package co.solventa.movil.features.notificaciones.centro

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * M15 y M10b (HU-M3.1-01). Al centro se entra desde Inicio; cada tarjeta, y
 * cada notificación del sistema, abre el detalle del pago.
 */
fun NavGraphBuilder.grafoNotificaciones(navegacion: NavController) {
    composable<Ruta.Notificaciones> {
        CentroPantalla(
            onAtras = { navegacion.popBackStack() },
            onAbrir = { id -> navegacion.navigate(Ruta.DetallePagoParametrico(id)) },
        )
    }
    composable<Ruta.DetallePagoParametrico> {
        DetallePagoPantalla(onAtras = { navegacion.popBackStack() })
    }
}
