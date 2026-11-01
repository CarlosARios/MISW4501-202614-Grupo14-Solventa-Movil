package co.solventa.movil.features.originacion.polizas

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * M4 y M5 (HU-M1.2-01). Se entra por la pestaña Pólizas de la barra inferior;
 * cada tarjeta abre su detalle. Las dos pantallas leen la copia local, así que
 * se recorren igual sin conexión.
 */
fun NavGraphBuilder.grafoPolizas(navegacion: NavController) {
    composable<Ruta.Polizas> {
        PolizasPantalla(onAbrir = { id -> navegacion.navigate(Ruta.DetallePoliza(id)) })
    }
    composable<Ruta.DetallePoliza> {
        DetallePolizaPantalla(onAtras = { navegacion.popBackStack() })
    }
}
