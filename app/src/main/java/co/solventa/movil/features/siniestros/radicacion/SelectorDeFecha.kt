package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import co.solventa.movil.R
import co.solventa.movil.core.i18n.recordarIdiomaDeLaPantalla

/**
 * El selector de fecha y hora del hecho (M11b).
 *
 * Son dos diálogos encadenados, día y hora, porque el campo del prototipo
 * muestra las dos cosas y el sistema de diseño lo dibuja como selector.
 *
 * **No deja elegir un día futuro.** Es la misma regla que valida el servicio,
 * aplicada antes de escribir: más vale no ofrecer lo que va a ser rechazado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDeFechaYHora(
    inicial: Long?,
    onElegida: (Long) -> Unit,
    onCerrar: () -> Unit,
) {
    var dia by remember { mutableStateOf<Long?>(null) }
    // Los diálogos tienen su propia ventana: sin esto los botones, el
    // calendario y el reloj salen en el idioma del teléfono.
    val idioma = recordarIdiomaDeLaPantalla()

    if (dia == null) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = inicial,
            selectableDates = HastaHoy,
        )
        DatePickerDialog(
            onDismissRequest = onCerrar,
            confirmButton = {
                idioma.Aplicar {
                    TextButton(
                        onClick = { dia = estado.selectedDateMillis },
                        enabled = estado.selectedDateMillis != null,
                    ) { Text(stringResource(R.string.comun_continuar)) }
                }
            },
            dismissButton = {
                idioma.Aplicar {
                    TextButton(onClick = onCerrar) { Text(stringResource(R.string.comun_cancelar)) }
                }
            },
        ) {
            idioma.Aplicar { DatePicker(state = estado) }
        }
        return
    }

    val hora = rememberTimePickerState(is24Hour = true)
    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            idioma.Aplicar {
                TextButton(
                    onClick = { onElegida(FechaDelHecho.combinar(dia!!, hora.hour, hora.minute)) },
                ) { Text(stringResource(R.string.comun_aceptar)) }
            }
        },
        dismissButton = {
            idioma.Aplicar {
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.comun_cancelar)) }
            }
        },
        title = { idioma.Aplicar { Text(stringResource(R.string.radicacion_hora_del_hecho)) } },
        text = { idioma.Aplicar { TimePicker(state = hora) } },
    )
}

/** Un siniestro no puede haber ocurrido mañana. */
@OptIn(ExperimentalMaterial3Api::class)
private object HastaHoy : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= System.currentTimeMillis()

    override fun isSelectableYear(year: Int): Boolean =
        year <= java.time.LocalDate.now().year
}
