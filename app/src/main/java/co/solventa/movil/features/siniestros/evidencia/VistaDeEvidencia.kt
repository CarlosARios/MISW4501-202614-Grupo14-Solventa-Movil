package co.solventa.movil.features.siniestros.evidencia

import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import co.solventa.movil.R
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lo que se capturó, a pantalla completa (HU-M2.1-01).
 *
 * El prototipo no dibuja esta vista: la pidió el equipo al probar en el
 * dispositivo, porque sin ella no hay forma de saber qué quedó en cada
 * casilla. Desde aquí también se descarta una evidencia para volver a
 * tomarla.
 */
@Composable
fun VistaDeEvidencia(
    evidencia: EvidenciaDelReporte,
    onDescartar: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(role = Role.Button, onClick = onCerrar),
    ) {
        if (evidencia.tipo == TipoDeEvidencia.VIDEO) {
            ReproductorDeVideo(
                rutaArchivo = evidencia.rutaArchivo,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
            )
        } else {
            ImagenDeEvidencia(
                rutaArchivo = evidencia.rutaArchivo,
                ladoMaximo = LADO_DE_LA_VISTA,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(vertical = Espacio.x64),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(Espacio.x16),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = stringResource(R.string.comun_cerrar),
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(role = Role.Button, onClick = onCerrar),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x8),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(Espacio.x16),
        ) {
            Text(
                text = stringResource(estadoDeEnvioDe(evidencia)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
            BotonSolventa(
                texto = stringResource(R.string.evidencia_descartar_y_repetir),
                onClick = onDescartar,
                tipo = TipoBoton.SECUNDARIO,
                icono = painterResource(R.drawable.ic_caneca),
            )
        }
    }
}

/** Criterio 5: en qué va el envío de esta evidencia. */
fun estadoDeEnvioDe(evidencia: EvidenciaDelReporte): Int = when (evidencia.estado) {
    EstadoDeEvidencia.PENDIENTE -> R.string.evidencia_estado_pendiente
    EstadoDeEvidencia.SUBIENDO -> R.string.evidencia_estado_subiendo
    EstadoDeEvidencia.SUBIDA -> R.string.evidencia_estado_subida
    EstadoDeEvidencia.FALLIDA -> R.string.evidencia_estado_fallida
}

/**
 * Reproduce el video capturado con el reproductor del sistema. No se suma
 * ninguna biblioteca: es un archivo local de treinta segundos.
 */
@Composable
private fun ReproductorDeVideo(rutaArchivo: String, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }
    AndroidView(
        factory = { contexto ->
            VideoView(contexto).apply {
                setVideoPath(rutaArchivo)
                setOnPreparedListener { reproductor ->
                    reproductor.isLooping = true
                    start()
                }
            }
        },
        modifier = modifier,
    )
}

/** Lado largo con el que se decodifica la imagen a pantalla completa. */
private const val LADO_DE_LA_VISTA = 1280
