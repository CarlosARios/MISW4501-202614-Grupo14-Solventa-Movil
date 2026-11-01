package co.solventa.movil.features.perfil

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * M19 · Perfil (HU-M1.3-01). Perfil no conoce a la auditoria: abrir
 * Privacidad y consentimientos y salir tras cerrar la sesion son *callbacks*
 * que resuelve la raiz de la navegacion.
 */
fun NavGraphBuilder.destinoPerfil(onConsentimientos: () -> Unit, onSesionCerrada: () -> Unit) {
    composable<Ruta.Perfil> {
        PerfilPantalla(onConsentimientos = onConsentimientos, onSesionCerrada = onSesionCerrada)
    }
}
