package co.solventa.movil.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.solventa.movil.R
import androidx.compose.ui.res.stringResource
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * Logotipo de la marca: el nombre en azul con el punto lima.
 *
 * Se lee como una sola unidad para el lector de pantalla; el punto es
 * decorativo y no se anuncia aparte.
 */
@Composable
fun LogotipoSolventa(
    modifier: Modifier = Modifier,
    sobreOscuro: Boolean = false,
    grande: Boolean = false,
) {
    val nombre = stringResource(R.string.bienvenida_titulo)
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.clearAndSetSemantics { contentDescription = nombre },
    ) {
        Text(
            text = nombre,
            // Alto 24 en las barras y 40 en la bienvenida, como dice el
            // componente del prototipo.
            style = if (grande) {
                MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (sobreOscuro) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Box(
            modifier = Modifier
                .padding(start = Espacio.x4, bottom = if (grande) Espacio.x8 else Espacio.x4)
                .size(if (grande) 8.dp else 6.dp)
                .background(LocalColoresSolventa.current.lima, CircleShape),
        )
    }
}
