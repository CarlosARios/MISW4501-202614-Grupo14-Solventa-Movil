package co.solventa.movil.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.core.i18n.ConIdioma
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * Elegir el idioma de la aplicación.
 *
 * **El prototipo no dibuja esta hoja**, solo el selector cerrado de M0 con su
 * chevron. Se resuelve con una hoja inferior, que es el patrón que ya usan la
 * sesión expirada y el borrador del reporte. Queda anotado en
 * `docs/pantallas.md`.
 *
 * Cada idioma se lee en su propio idioma: quien tenga la aplicación en inglés
 * y busque español tiene que reconocer «Español».
 *
 * Vive en `shared/ui` desde HU-M1.3-01 porque la usan dos dominios: la
 * bienvenida (M0) y el perfil (M19). No cambió su comportamiento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDeIdioma(
    actual: Idioma,
    onElegir: (Idioma) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current

    ModalBottomSheet(onDismissRequest = onCerrar, modifier = modifier) {
        // Una hoja se dibuja en su propia ventana, y esa ventana vuelve a
        // proveer el contexto y la configuracion del dispositivo. Sin esto,
        // la hoja saldria en el idioma del telefono y no en el de la
        // aplicacion.
        ConIdioma(actual) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x8),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Espacio.x16, vertical = Espacio.x8),
        ) {
            Text(
                text = stringResource(R.string.bienvenida_idioma_titulo),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Espacio.x8),
            )

            Idioma.entries.forEach { idioma ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = idioma == actual,
                            role = Role.RadioButton,
                            onClick = { onElegir(idioma) },
                        )
                        .padding(vertical = Espacio.x12),
                ) {
                    Text(
                        text = idioma.etiqueta,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(0.8f),
                    )
                    if (idioma == actual) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = colores.exitoFuerte,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        }
    }
}
