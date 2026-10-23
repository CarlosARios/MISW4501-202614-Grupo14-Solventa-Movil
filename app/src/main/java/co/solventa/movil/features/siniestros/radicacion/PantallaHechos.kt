package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import co.solventa.movil.R
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TipoBoton

/**
 * M11b · Radicación · Paso 2 Hechos.
 *
 * Cuándo, dónde y qué pasó, más las preguntas que dependen del tipo de
 * incidente elegido en el paso anterior (criterio 2).
 */
@Composable
fun PantallaHechos(
    estado: RadicacionUiState,
    onFechaCambia: (Long) -> Unit,
    onLugarCambia: (String) -> Unit,
    onDescripcionCambia: (String) -> Unit,
    onResponder: (String, String) -> Unit,
    onContinuar: () -> Unit,
    onGuardarYSalir: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var eligiendoFecha by remember { mutableStateOf(false) }

    if (eligiendoFecha) {
        SelectorDeFechaYHora(
            inicial = estado.radicacion.ocurrioEn,
            onElegida = {
                onFechaCambia(it)
                eligiendoFecha = false
            },
            onCerrar = { eligiendoFecha = false },
        )
    }

    PantallaRegistro(
        titulo = stringResource(R.string.radicacion_titulo),
        onAtras = onAtras,
        onCerrar = onGuardarYSalir,
        modifier = modifier,
        acciones = {
            BotonSolventa(
                texto = stringResource(R.string.comun_continuar),
                onClick = onContinuar,
                habilitado = estado.puedeAvanzar,
                icono = painterResource(R.drawable.ic_flecha_derecha),
            )
            TextoDeFaltantes(estado)
            BotonSolventa(
                texto = stringResource(R.string.radicacion_guardar_y_salir),
                onClick = onGuardarYSalir,
                tipo = TipoBoton.SECUNDARIO,
            )
        },
    ) {
        PasosDelAsistente(etapa = estado.etapa, avance = estado.avance)

        Text(
            text = stringResource(R.string.radicacion_hechos_pregunta),
            style = MaterialTheme.typography.titleMedium,
        )

        CampoSelector(
            etiqueta = stringResource(R.string.radicacion_fecha_y_hora),
            valor = FechaDelHecho.aTexto(estado.radicacion.ocurrioEn),
            onAbrir = { eligiendoFecha = true },
            ayuda = stringResource(R.string.radicacion_fecha_ayuda),
            error = estado.camposRechazados.contains(CampoDeRadicacion.FECHA),
        )

        CampoTexto(
            etiqueta = stringResource(R.string.radicacion_lugar),
            valor = estado.radicacion.lugar,
            onCambio = onLugarCambia,
            ayuda = stringResource(R.string.radicacion_lugar_ayuda),
            error = estado.camposRechazados.contains(CampoDeRadicacion.LUGAR),
        )

        preguntasDe(estado.radicacion.tipo).forEach { pregunta ->
            if (pregunta.esAbierta) {
                CampoTexto(
                    etiqueta = stringResource(pregunta.etiqueta),
                    valor = estado.radicacion.respuestas[pregunta.id].orEmpty(),
                    onCambio = { onResponder(pregunta.id, it) },
                    error = estado.camposRechazados.contains(CampoDeRadicacion.RESPUESTAS),
                    lineas = 3,
                )
            } else {
                PreguntaDeLista(
                    enunciado = stringResource(pregunta.etiqueta),
                    pregunta = pregunta,
                    respuesta = estado.radicacion.respuestas[pregunta.id],
                    onResponder = { onResponder(pregunta.id, it) },
                )
            }
        }

        CampoTexto(
            etiqueta = stringResource(R.string.radicacion_descripcion),
            valor = estado.radicacion.descripcion,
            onCambio = onDescripcionCambia,
            ayuda = stringResource(R.string.radicacion_descripcion_ayuda),
            error = estado.camposRechazados.contains(CampoDeRadicacion.DESCRIPCION),
            lineas = 3,
        )
    }
}
