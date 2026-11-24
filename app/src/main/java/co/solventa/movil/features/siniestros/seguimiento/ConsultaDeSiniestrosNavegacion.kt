package co.solventa.movil.features.siniestros.seguimiento

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * M9 y M10 (HU-M2.4-01, HA-L02). Se entra por la pestaña Siniestros de la
 * barra inferior, o desde el radicado recién generado; cada tarjeta abre su
 * detalle con la línea de tiempo.
 */
fun NavGraphBuilder.grafoConsultaDeSiniestros(navegacion: NavController) {
    composable<Ruta.Siniestros> {
        MisSiniestrosPantalla(
            onAbrir = { id -> navegacion.navigate(Ruta.DetalleSiniestro(id)) },
            onReportar = { navegacion.navigate(Ruta.Radicacion()) },
            // M13b: el borrador elegido ya quedó de primero; el asistente lo retoma.
            onContinuarBorrador = { navegacion.navigate(Ruta.Radicacion(retomar = true)) },
        )
    }
    composable<Ruta.DetalleSiniestro> {
        DetalleSiniestroPantalla(onAtras = { navegacion.popBackStack() })
    }
}
