package co.solventa.movil.features.acceso.bienvenida

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.LogotipoSolventa
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Azul900
import co.solventa.movil.ui.theme.Espacio

/**
 * M0 · Bienvenida. La primera pantalla de la aplicación sin sesión.
 *
 * Va sobre azul 900 a sangre completa, barra de estado incluida, con el
 * logotipo en su versión clara y los textos en blanco.
 */
@Composable
fun PantallaBienvenida(
    idioma: Idioma,
    onElegirIdioma: (Idioma) -> Unit,
    onCrearCuenta: () -> Unit,
    onYaTengoCuenta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var eligiendoIdioma by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Azul900),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x24, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x24, vertical = Espacio.x16),
        ) {
            LogotipoSolventa(sobreOscuro = true, grande = true)

            Icon(
                painter = painterResource(R.drawable.il_escudo_bienvenida),
                contentDescription = null,
                // La ilustracion trae sus colores: no se tinte.
                tint = Color.Unspecified,
                modifier = Modifier.size(200.dp),
            )

            Text(
                text = stringResource(R.string.bienvenida_lema),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.bienvenida_descripcion),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            SelectorDeIdioma(idioma = idioma, onAbrir = { eligiendoIdioma = true })
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x8),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = Espacio.x16,
                    end = Espacio.x16,
                    top = Espacio.x12,
                    bottom = Espacio.x24,
                ),
        ) {
            BotonSolventa(
                texto = stringResource(R.string.bienvenida_crear_cuenta),
                onClick = onCrearCuenta,
                tipo = TipoBoton.CLARO,
            )
            BotonSolventa(
                texto = stringResource(R.string.bienvenida_ya_tengo_cuenta),
                onClick = onYaTengoCuenta,
                tipo = TipoBoton.TEXTO_CLARO,
            )
        }
    }

    if (eligiendoIdioma) {
        HojaDeIdioma(
            actual = idioma,
            onElegir = {
                onElegirIdioma(it)
                eligiendoIdioma = false
            },
            onCerrar = { eligiendoIdioma = false },
        )
    }
}

/** El control de idioma del frame: globo, nombre del idioma y chevron. */
@Composable
private fun SelectorDeIdioma(idioma: Idioma, onAbrir: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
        modifier = Modifier
            .clip(RoundedCornerShape(Espacio.x8))
            .clickable(onClick = onAbrir)
            .padding(horizontal = Espacio.x12, vertical = Espacio.x8),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_globo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = idioma.etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_abajo),
            contentDescription = stringResource(R.string.bienvenida_cambiar_idioma),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}
