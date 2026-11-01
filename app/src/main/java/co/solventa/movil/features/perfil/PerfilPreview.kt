package co.solventa.movil.features.perfil

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/** Vista previa de M19 · Perfil con los datos del prototipo. */
@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun PerfilPreview() {
    SolventaTheme {
        ContenidoPerfil(
            estado = PerfilUiState(
                nombre = "Carolina Pérez Ruiz",
                correo = "carolina.perez@correo.com",
                iniciales = "CP",
                biometriaHabilitada = true,
                biometriaDisponible = true,
                idioma = Idioma.ESPANOL,
                activos = 2,
                version = "1.0",
                minutosDeCierre = 5,
            ),
            onAlternarBiometria = {},
            onElegirIdioma = {},
            onConsentimientos = {},
            onCerrarSesion = {},
        )
    }
}
