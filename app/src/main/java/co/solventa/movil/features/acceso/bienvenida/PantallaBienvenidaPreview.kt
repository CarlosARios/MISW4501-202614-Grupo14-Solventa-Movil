package co.solventa.movil.features.acceso.bienvenida

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.SolventaTheme

/** M0 contra su frame: 390 × 844. */
@Preview(name = "M0 · Bienvenida", widthDp = 390, heightDp = 844)
@Composable
private fun VistaPreviaBienvenida() {
    SolventaTheme {
        PantallaBienvenida(
            idioma = Idioma.ESPANOL,
            onElegirIdioma = {},
            onCrearCuenta = {},
            onYaTengoCuenta = {},
        )
    }
}
