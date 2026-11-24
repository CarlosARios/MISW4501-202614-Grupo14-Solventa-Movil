package co.solventa.movil.features.notificaciones.centro

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * M15 y M10b (HU-M3.1-01). Al centro se entra desde Inicio; cada tarjeta, y
 * cada notificación del sistema, abre el detalle del pago o, la de un cambio
 * de etapa, la línea de tiempo del siniestro (HU-M3.3-01), y la de un
 * vencimiento, el detalle de la póliza (HU-M3.2-01).
 */
fun NavGraphBuilder.grafoNotificaciones(navegacion: NavController) {
    composable<Ruta.Notificaciones> {
        CentroPantalla(
            onAtras = { navegacion.popBackStack() },
            onAbrir = { destino -> navegacion.navigate(rutaDe(destino)) },
        )
    }
    composable<Ruta.DetallePagoParametrico> {
        DetallePagoPantalla(onAtras = { navegacion.popBackStack() })
    }
}

/**
 * La pantalla de cada destino: el detalle del pago (M10b), la línea de tiempo
 * del siniestro (M10) o el detalle de la póliza por vencer (M5, HU-M3.2-01).
 */
fun rutaDe(destino: DestinoDeNotificacion): Ruta = when (destino) {
    is DestinoDeNotificacion.Pago -> Ruta.DetallePagoParametrico(destino.notificacionId)
    is DestinoDeNotificacion.Siniestro -> Ruta.DetalleSiniestro(destino.siniestroId)
    is DestinoDeNotificacion.Poliza -> Ruta.DetallePoliza(destino.polizaId)
}
