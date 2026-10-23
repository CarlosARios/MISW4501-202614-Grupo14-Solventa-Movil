package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/** El mínimo que exige una colisión, según M11c. */
const val MINIMO_DE_FOTOS = 4

/**
 * M11c · Radicación · Paso 3 Evidencias.
 *
 * **La captura está deshabilitada, no quitada.** La cámara con
 * geoetiquetado, el mínimo por tipo de siniestro y el envío en segundo plano
 * son HU-M2.1-01. La etapa existe desde ahora porque el indicador de pasos la
 * anuncia y el resumen del paso 4 la menciona.
 *
 * Con esa historia entregada, [RadicacionUiState.faltantesDe] empieza a
 * exigir el mínimo y la pantalla muestra el estado de M11f: botón
 * deshabilitado y el texto de lo que falta.
 */
@Composable
fun PantallaEvidencias(
    estado: RadicacionUiState,
    onContinuar: () -> Unit,
    onGuardarYSalir: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val capturadas = 0

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
            text = stringResource(R.string.radicacion_evidencias_titulo),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.radicacion_evidencias_detalle),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    R.string.radicacion_evidencias_contador,
                    capturadas,
                    MINIMO_DE_FOTOS,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (capturadas >= MINIMO_DE_FOTOS) {
                MarcaDeCompleto()
            }
        }

        // La cuadrícula del frame: seis casillas, la primera lista para la
        // captura siguiente. Sin acción hasta que exista la cámara.
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x8),
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(2) { fila ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    repeat(3) { columna ->
                        CasillaDeCaptura(
                            siguiente = fila == 0 && columna == 0,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.radicacion_video_titulo),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.radicacion_video_detalle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.textoSecundario,
                )
            }
            BotonSolventa(
                texto = stringResource(R.string.radicacion_video_grabar),
                onClick = {},
                tipo = TipoBoton.SECUNDARIO,
                habilitado = false,
                modifier = Modifier.width(120.dp),
            )
        }

        AvisoSolventa(
            tipo = TipoAviso.INFORMACION,
            titulo = stringResource(R.string.radicacion_evidencias_aviso_titulo),
            texto = stringResource(R.string.radicacion_evidencias_aviso),
        )
    }
}

/** El distintivo verde «Completo» junto al contador de fotos. */
@Composable
private fun MarcaDeCompleto() {
    val colores = LocalColoresSolventa.current
    Text(
        text = stringResource(R.string.radicacion_evidencias_completo),
        style = MaterialTheme.typography.labelSmall,
        color = colores.exitoFuerte,
        modifier = Modifier
            .clip(RoundedCornerShape(Espacio.x16))
            .background(colores.exitoSuave)
            .padding(horizontal = Espacio.x12, vertical = Espacio.x4),
    )
}

/**
 * Una casilla de la cuadrícula. La siguiente a capturar va con borde
 * resaltado, como en el frame; las demás quedan en gris.
 */
@Composable
private fun CasillaDeCaptura(siguiente: Boolean, modifier: Modifier = Modifier) {
    val colores = LocalColoresSolventa.current
    val borde = if (siguiente) MaterialTheme.colorScheme.primary else colores.bordeBase
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1.17f)
            .clip(RoundedCornerShape(Espacio.x8))
            .background(
                if (siguiente) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
            )
            .border(1.dp, borde, RoundedCornerShape(Espacio.x8)),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camara),
            contentDescription = null,
            tint = if (siguiente) MaterialTheme.colorScheme.primary else colores.textoSecundario,
        )
    }
}
