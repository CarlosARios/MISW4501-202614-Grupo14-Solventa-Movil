package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.features.siniestros.evidencia.EstadoDeEvidencia
import co.solventa.movil.features.siniestros.evidencia.EvidenciaDelReporte
import co.solventa.movil.features.siniestros.evidencia.LimitesDeEvidencia
import co.solventa.movil.features.siniestros.evidencia.MiniaturaDeEvidencia
import co.solventa.movil.features.siniestros.evidencia.TipoDeEvidencia
import co.solventa.movil.features.siniestros.evidencia.VistaDeEvidencia
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M11c · Radicación · Paso 3 Evidencias.
 *
 * La cuadrícula del frame con lo capturado: seis casillas, la siguiente
 * resaltada, y cada evidencia con su estado de envío (HU-M2.1-01, criterio
 * 5). Mientras falte el mínimo, *Continuar* queda deshabilitado y el texto
 * dice qué falta, que es el criterio 3 de HU-M2.2-01.
 */
@Composable
fun PantallaEvidencias(
    estado: RadicacionUiState,
    onCapturar: () -> Unit,
    onDescartar: (String) -> Unit,
    onContinuar: () -> Unit,
    onGuardarYSalir: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val capturadas = estado.fotosCapturadas
    // Qué evidencia se está viendo a pantalla completa. Es estado de la
    // pantalla y no del asistente: no sobrevive ni tiene que guardarse.
    var enVista by remember { mutableStateOf<String?>(null) }
    val evidenciaEnVista = estado.evidencias.firstOrNull { it.id == enVista }

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
                    LimitesDeEvidencia.MINIMO_DE_FOTOS,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (estado.fotosEnviadas >= LimitesDeEvidencia.MINIMO_DE_FOTOS) {
                MarcaDeCompleto()
            }
        }

        // La cuadrícula del frame: seis casillas en orden de captura, con la
        // siguiente resaltada.
        val fotos = estado.evidencias.filter { it.tipo == TipoDeEvidencia.FOTO }
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
                        val indice = fila * 3 + columna
                        CasillaDeCaptura(
                            evidencia = fotos.getOrNull(indice),
                            siguiente = indice == fotos.size,
                            onCapturar = onCapturar,
                            onVer = { enVista = it },
                            onDescartar = onDescartar,
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
            val video = estado.evidencias.firstOrNull { it.tipo == TipoDeEvidencia.VIDEO }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.radicacion_video_titulo),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    // Con un video grabado, la fila dice cuánto dura y en qué
                    // va su envío: en la cuadrícula solo caben las fotos.
                    text = video?.let {
                        stringResource(
                            R.string.radicacion_video_capturado,
                            it.duracionS,
                            stringResource(estadoDeEnvio(it)),
                        )
                    } ?: stringResource(R.string.radicacion_video_detalle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.textoSecundario,
                )
            }
            if (video == null) {
                BotonSolventa(
                    texto = stringResource(R.string.radicacion_video_grabar),
                    onClick = onCapturar,
                    tipo = TipoBoton.SECUNDARIO,
                    // Con ancho fijo: el botón ocupa todo el que le den, y sin
                    // tope aplasta el título de la fila hasta partirlo letra
                    // por letra.
                    modifier = Modifier.width(ANCHO_DEL_BOTON_DE_VIDEO),
                )
            } else {
                // Con un video grabado, el botón pasa a reproducirlo: la
                // cuadrícula es solo de fotos y si no, no habría cómo verlo.
                BotonSolventa(
                    texto = stringResource(R.string.radicacion_video_ver),
                    onClick = { enVista = video.id },
                    tipo = TipoBoton.SECUNDARIO,
                    modifier = Modifier.width(ANCHO_DEL_BOTON_DE_VIDEO),
                )
            }
        }

        AvisoSolventa(
            tipo = TipoAviso.INFORMACION,
            titulo = stringResource(R.string.radicacion_evidencias_aviso_titulo),
            texto = stringResource(R.string.radicacion_evidencias_aviso),
        )
    }

    if (evidenciaEnVista != null) {
        VistaDeEvidencia(
            evidencia = evidenciaEnVista,
            onDescartar = {
                onDescartar(evidenciaEnVista.id)
                enVista = null
            },
            onCerrar = { enVista = null },
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
 * Una casilla de la cuadrícula: la evidencia capturada con su estado de
 * envío, o el hueco de la siguiente, resaltado como en el frame.
 *
 * **Tocar una evidencia la muestra; la caneca la descarta.** Antes el toque
 * borraba, de modo que mirar una foto era perderla.
 */
@Composable
private fun CasillaDeCaptura(
    evidencia: EvidenciaDelReporte?,
    siguiente: Boolean,
    onCapturar: () -> Unit,
    onVer: (String) -> Unit,
    onDescartar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val borde = if (siguiente) MaterialTheme.colorScheme.primary else colores.bordeBase
    val descripcion = stringResource(
        if (evidencia == null) R.string.evidencia_tomar else R.string.evidencia_ver,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1.17f)
            .clip(RoundedCornerShape(Espacio.x8))
            .background(
                if (siguiente) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .border(1.dp, borde, RoundedCornerShape(Espacio.x8))
            .clickable(
                enabled = evidencia != null || siguiente,
                role = Role.Button,
                onClickLabel = descripcion,
                onClick = { if (evidencia == null) onCapturar() else onVer(evidencia.id) },
            ),
    ) {
        if (evidencia == null) {
            Icon(
                painter = painterResource(R.drawable.ic_camara),
                contentDescription = null,
                tint = if (siguiente) MaterialTheme.colorScheme.primary else colores.textoSecundario,
            )
        } else {
            MiniaturaDeEvidencia(
                rutaArchivo = evidencia.rutaArchivo,
                esVideo = evidencia.tipo == TipoDeEvidencia.VIDEO,
                modifier = Modifier.fillMaxSize(),
            )
            CanecaDeLaCasilla(
                onDescartar = { onDescartar(evidencia.id) },
                modifier = Modifier.align(Alignment.TopEnd),
            )
            // La franja va con el estado en corto: en una casilla de este
            // tamaño, la frase completa se amontona y no se lee. La frase
            // entera está en la vista a pantalla completa.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.x4),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = Espacio.x4, vertical = Espacio.x4),
            ) {
                Text(
                    text = stringResource(estadoCortoDeEnvio(evidencia)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!evidencia.ubicacionConfiable) {
                    Icon(
                        painter = painterResource(R.drawable.ic_triangulo_alerta),
                        contentDescription = stringResource(R.string.evidencia_sin_ubicacion_confiable),
                        tint = colores.advertenciaSuave,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/** La caneca de la casilla: descarta esa evidencia para volver a tomarla. */
@Composable
private fun CanecaDeLaCasilla(onDescartar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(Espacio.x4)
            .size(28.dp)
            .clip(RoundedCornerShape(Espacio.x8))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(role = Role.Button, onClick = onDescartar),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_caneca),
            contentDescription = stringResource(R.string.evidencia_descartar),
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Lo que necesitan «Grabar» y «Ver video» sin partirse ni aplastar la fila. */
private val ANCHO_DEL_BOTON_DE_VIDEO = 132.dp

/** El estado del envío en una palabra, para la franja de la casilla. */
private fun estadoCortoDeEnvio(evidencia: EvidenciaDelReporte): Int = when (evidencia.estado) {
    EstadoDeEvidencia.PENDIENTE -> R.string.evidencia_estado_corto_pendiente
    EstadoDeEvidencia.SUBIENDO -> R.string.evidencia_estado_corto_subiendo
    EstadoDeEvidencia.SUBIDA -> R.string.evidencia_estado_corto_subida
    EstadoDeEvidencia.FALLIDA -> R.string.evidencia_estado_corto_fallida
}

/** Criterio 5: cada evidencia muestra en qué va su envío. */
private fun estadoDeEnvio(evidencia: EvidenciaDelReporte): Int = when (evidencia.estado) {
    EstadoDeEvidencia.PENDIENTE -> R.string.evidencia_estado_pendiente
    EstadoDeEvidencia.SUBIENDO -> R.string.evidencia_estado_subiendo
    EstadoDeEvidencia.SUBIDA -> R.string.evidencia_estado_subida
    EstadoDeEvidencia.FALLIDA -> R.string.evidencia_estado_fallida
}
