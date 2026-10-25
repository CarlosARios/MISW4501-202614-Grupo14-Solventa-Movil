package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import co.solventa.movil.R
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio

/**
 * «Tienes un reporte sin terminar» (M13b).
 *
 * Criterio 7: al volver a entrar, el asistente ofrece retomar el borrador en
 * la etapa donde quedó. En el prototipo la hoja aparece sobre *Mis
 * siniestros*; aquí aparece al abrir el asistente, porque esa lista es de
 * HU-M2.4-01 y todavía no existe. El contenido es el mismo.
 *
 * No se puede cerrar tocando fuera: la decisión es continuar o descartar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDeBorrador(
    estado: RadicacionUiState,
    onContinuar: () -> Unit,
    onDescartar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onContinuar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x16),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Espacio.x16, vertical = Espacio.x8),
        ) {
            Text(
                text = stringResource(R.string.radicacion_borrador_titulo),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.radicacion_borrador_detalle,
                    listOfNotNull(
                        estado.radicacion.tipo?.let { stringResource(tituloDe(it)) },
                        estado.polizaElegida?.etiqueta,
                    ).joinToString(" · "),
                    estado.etapa.ordinal + 1,
                    etiquetaDe(estado.etapa).lowercase(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            BotonSolventa(
                texto = stringResource(R.string.radicacion_borrador_continuar),
                onClick = onContinuar,
            )
            BotonSolventa(
                texto = stringResource(R.string.radicacion_borrador_descartar),
                onClick = onDescartar,
                tipo = TipoBoton.SECUNDARIO,
            )
        }
    }
}
