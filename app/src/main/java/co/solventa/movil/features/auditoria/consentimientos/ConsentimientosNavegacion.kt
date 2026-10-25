package co.solventa.movil.features.auditoria.consentimientos

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * M8 y M8b (HU-M1.3-01). Se entra desde Perfil → Privacidad y consentimientos;
 * cada tarjeta abre su detalle. Revocar desde el detalle abre la pantalla de
 * revocado (HU-M1.3-02, 38:1414).
 */
fun NavGraphBuilder.grafoConsentimientos(navegacion: NavController) {
    composable<Ruta.Consentimientos> {
        ConsentimientosPantalla(
            onAtras = { navegacion.popBackStack() },
            onAbrir = { id -> navegacion.navigate(Ruta.DetalleConsentimiento(id)) },
        )
    }
    composable<Ruta.DetalleConsentimiento> {
        DetalleConsentimientoPantalla(
            onAtras = { navegacion.popBackStack() },
            // El revocado reemplaza al detalle: volver lleva a la lista.
            onRevocado = { id ->
                navegacion.navigate(Ruta.ConsentimientoRevocado(id)) {
                    popUpTo<Ruta.Consentimientos>()
                }
            },
        )
    }
    composable<Ruta.ConsentimientoRevocado> {
        ConsentimientoRevocadoPantalla(onVolver = { navegacion.popBackStack() })
    }
}
