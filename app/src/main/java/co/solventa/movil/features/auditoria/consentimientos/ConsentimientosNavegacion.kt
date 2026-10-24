package co.solventa.movil.features.auditoria.consentimientos

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * M8 y M8b (HU-M1.3-01). Se entra desde Perfil → Privacidad y consentimientos;
 * cada tarjeta abre su detalle.
 */
fun NavGraphBuilder.grafoConsentimientos(navegacion: NavController) {
    composable<Ruta.Consentimientos> {
        ConsentimientosPantalla(
            onAtras = { navegacion.popBackStack() },
            onAbrir = { id -> navegacion.navigate(Ruta.DetalleConsentimiento(id)) },
        )
    }
    composable<Ruta.DetalleConsentimiento> {
        DetalleConsentimientoPantalla(onAtras = { navegacion.popBackStack() })
    }
}
