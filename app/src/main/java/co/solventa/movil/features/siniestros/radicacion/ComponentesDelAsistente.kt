package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.shared.ui.BarraProgreso
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * El indicador de pasos del asistente, con la barra de avance y el rótulo
 * «Paso N de 4». Es el mismo bloque en las cuatro pantallas.
 */
@Composable
fun PasosDelAsistente(
    etapa: Etapa,
    avance: Float,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x8),
        modifier = modifier.fillMaxWidth(),
    ) {
        BarraProgreso(avance = avance, color = MaterialTheme.colorScheme.primary)

        Row(modifier = Modifier.fillMaxWidth()) {
            Etapa.entries.forEachIndexed { indice, paso ->
                if (indice > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 13.dp)
                            .background(colores.bordeBase)
                            .size(width = 0.dp, height = 2.dp),
                    )
                }
                PasoDeFlujo(
                    numero = indice + 1,
                    etiqueta = etiquetaDe(paso),
                    alcanzado = indice <= etapa.ordinal,
                )
            }
        }

        Text(
            text = stringResource(
                R.string.radicacion_paso_de_cuatro,
                etapa.ordinal + 1,
                etiquetaDe(etapa),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )
    }
}

@Composable
private fun PasoDeFlujo(numero: Int, etiqueta: String, alcanzado: Boolean) {
    val colores = LocalColoresSolventa.current
    val tinte = if (alcanzado) MaterialTheme.colorScheme.primary else colores.textoSecundario
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = Modifier.size(width = 76.dp, height = 66.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = if (alcanzado) 2.dp else 1.dp,
                    color = if (alcanzado) MaterialTheme.colorScheme.primary else colores.bordeBase,
                    shape = CircleShape,
                ),
        ) {
            Text(
                text = numero.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = tinte,
            )
        }
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = if (alcanzado) MaterialTheme.colorScheme.onSurface else colores.textoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Una opción de una lista donde solo se elige una, como los tipos de
 * incidente de M11a. El grupo entero se anuncia como una sola unidad para el
 * lector de pantalla.
 */
@Composable
fun TarjetaDeOpcion(
    titulo: String,
    detalle: String?,
    icono: Painter?,
    elegida: Boolean,
    onElegir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Espacio.x8))
            .background(if (elegida) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = if (elegida) 2.dp else 1.dp,
                color = if (elegida) MaterialTheme.colorScheme.primary else colores.bordeBase,
                shape = RoundedCornerShape(Espacio.x8),
            )
            .selectable(selected = elegida, role = Role.RadioButton, onClick = onElegir)
            .padding(horizontal = Espacio.x16, vertical = Espacio.x12),
    ) {
        MarcaDeSeleccion(elegida = elegida)
        if (icono != null) {
            Icon(
                painter = icono,
                contentDescription = null,
                tint = if (elegida) MaterialTheme.colorScheme.primary else colores.textoSecundario,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyMedium)
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.textoSecundario,
                )
            }
        }
    }
}

/** El círculo de la izquierda: relleno cuando la opción está elegida. */
@Composable
private fun MarcaDeSeleccion(elegida: Boolean) {
    val colores = LocalColoresSolventa.current
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (elegida) 6.dp else 1.5.dp,
                color = if (elegida) MaterialTheme.colorScheme.primary else colores.bordeFuerte,
                shape = CircleShape,
            ),
    )
}

/**
 * Campo de selección del sistema de diseño: etiqueta arriba, caja con el
 * valor y el chevron, y texto de ayuda debajo.
 */
@Composable
fun CampoSelector(
    etiqueta: String,
    valor: String,
    onAbrir: () -> Unit,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    error: Boolean = false,
) {
    val colores = LocalColoresSolventa.current
    val borde = when {
        error -> MaterialTheme.colorScheme.error
        else -> colores.bordeBase
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(etiqueta, valor, ayuda).joinToString(". ")
            },
    ) {
        Text(
            text = etiqueta.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colores.textoSecundario,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Espacio.x8))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, borde, RoundedCornerShape(Espacio.x8))
                .selectable(selected = false, onClick = onAbrir)
                .padding(horizontal = Espacio.x16, vertical = Espacio.x12),
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.bodyLarge,
                color = if (valor.isBlank()) colores.textoSecundario else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_abajo),
                contentDescription = null,
                tint = colores.textoSecundario,
                modifier = Modifier.size(20.dp),
            )
        }
        if (ayuda != null) {
            Text(
                text = ayuda,
                style = MaterialTheme.typography.bodySmall,
                color = if (error) MaterialTheme.colorScheme.error else colores.textoSecundario,
            )
        }
    }
}

@Composable
internal fun etiquetaDe(etapa: Etapa): String = stringResource(
    when (etapa) {
        Etapa.IDENTIFICACION -> R.string.radicacion_etapa_identificacion
        Etapa.HECHOS -> R.string.radicacion_etapa_hechos
        Etapa.EVIDENCIAS -> R.string.radicacion_etapa_evidencias
        Etapa.DECLARACION -> R.string.radicacion_etapa_declaracion
    },
)

/**
 * Campo de texto del sistema de diseño: etiqueta arriba, caja y ayuda
 * debajo. La ayuda solo muestra el formato, como pide el componente.
 */
@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    error: Boolean = false,
    lineas: Int = 1,
) {
    val colores = LocalColoresSolventa.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = etiqueta.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colores.textoSecundario,
        )
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = lineas == 1,
            minLines = lineas,
            isError = error,
            supportingText = ayuda?.let { { Text(it) } },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Una pregunta cuya respuesta se elige de una lista, como las de M11b. El
 * enunciado va fuera de las opciones, con el estilo de etiqueta.
 */
@Composable
fun PreguntaDeLista(
    enunciado: String,
    pregunta: PreguntaAdaptativa,
    respuesta: String?,
    onResponder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x8),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = enunciado,
            style = MaterialTheme.typography.labelSmall,
            color = colores.textoSecundario,
        )
        pregunta.opciones.forEach { opcion ->
            TarjetaDeOpcion(
                titulo = stringResource(opcion.etiqueta),
                detalle = opcion.detalle?.let { stringResource(it) },
                icono = null,
                elegida = respuesta == opcion.valor,
                onElegir = { onResponder(opcion.valor) },
            )
        }
    }
}

/**
 * Lo que falta para avanzar, bajo el botón (criterio 3).
 *
 * El botón deshabilitado dice que algo falta; esta línea dice **qué**. Los
 * campos no se marcan en rojo por estar vacíos: en un asistente eso sería
 * reprochar lo que todavía no se ha tenido ocasión de escribir. El rojo
 * queda para lo que el servicio rechazó.
 */
@Composable
fun TextoDeFaltantes(estado: RadicacionUiState, modifier: Modifier = Modifier) {
    val faltantes = estado.faltantes
    if (faltantes.isEmpty()) return

    val colores = LocalColoresSolventa.current
    Text(
        text = if (estado.etapa == Etapa.EVIDENCIAS) {
            // La etapa de evidencias tiene su propio texto en el prototipo.
            stringResource(R.string.radicacion_evidencias_faltantes)
        } else {
            stringResource(R.string.radicacion_faltan, enumerar(faltantes))
        },
        style = MaterialTheme.typography.bodySmall,
        color = colores.textoSecundario,
        modifier = modifier,
    )
}

/** «la póliza y el tipo de incidente», en el orden en que aparecen. */
@Composable
private fun enumerar(faltantes: Set<CampoDeRadicacion>): String {
    val nombres = CampoDeRadicacion.entries
        .filter { it in faltantes }
        .map { stringResource(nombreDe(it)) }
    if (nombres.size <= 1) return nombres.firstOrNull().orEmpty()
    val y = stringResource(R.string.comun_y)
    return nombres.dropLast(1).joinToString(", ") + " $y " + nombres.last()
}

private fun nombreDe(campo: CampoDeRadicacion) = when (campo) {
    CampoDeRadicacion.POLIZA -> R.string.radicacion_falta_poliza
    CampoDeRadicacion.TIPO -> R.string.radicacion_falta_tipo
    CampoDeRadicacion.FECHA -> R.string.radicacion_falta_fecha
    CampoDeRadicacion.LUGAR -> R.string.radicacion_falta_lugar
    CampoDeRadicacion.DESCRIPCION -> R.string.radicacion_falta_descripcion
    CampoDeRadicacion.RESPUESTAS -> R.string.radicacion_falta_respuestas
    CampoDeRadicacion.DECLARACION -> R.string.radicacion_falta_declaracion
}
