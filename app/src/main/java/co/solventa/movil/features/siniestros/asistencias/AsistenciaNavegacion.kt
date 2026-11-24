package co.solventa.movil.features.siniestros.asistencias

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * M14 · Solicitar asistencia (HU-M2.3-01). Se entra desde el Inicio
 * provisional (D12); M14b y M14c son estados de la misma pantalla.
 */
fun NavGraphBuilder.destinoAsistencia(navegacion: NavController) {
    composable<Ruta.Asistencia> {
        AsistenciaPantalla(onAtras = { navegacion.popBackStack() })
    }
}
