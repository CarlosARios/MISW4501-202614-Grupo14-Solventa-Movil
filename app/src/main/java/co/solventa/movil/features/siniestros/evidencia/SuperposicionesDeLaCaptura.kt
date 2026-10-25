package co.solventa.movil.features.siniestros.evidencia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M12b · Permisos de cámara y ubicación (HU-M2.1-01, criterio 1).
 *
 * **Las tres opciones del frame las pone Android**, que es lo que ese frame
 * dibuja: el diálogo del sistema. Lo que corresponde a la aplicación es
 * explicar para qué se usan antes de pedirlos, y eso es esta superposición.
 *
 * Cuando el usuario ya los negó, Android no deja volver a preguntar desde la
 * aplicación: el texto lo dice y la única salida es cerrar. No se ofrece la
 * galería, porque el criterio 2 prohíbe aceptar imágenes que no se tomaron en
 * el lugar.
 */
@Composable
fun DialogoDePermisosDeCaptura(
    estado: EstadoDeLaCaptura,
    onPermitir: () -> Unit,
    onNoPermitir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val negados = estado == EstadoDeLaCaptura.PERMISOS_NEGADOS

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(Espacio.x24),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier
                .width(310.dp)
                .clip(RoundedCornerShape(Espacio.x16))
                .background(MaterialTheme.colorScheme.surface)
                .padding(Espacio.x24),
        ) {
            Text(
                text = stringResource(
                    if (negados) {
                        R.string.evidencia_permisos_negados_titulo
                    } else {
                        R.string.evidencia_permisos_titulo
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(
                    if (negados) {
                        R.string.evidencia_permisos_negados_detalle
                    } else {
                        R.string.evidencia_permisos_detalle
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.textoSecundario,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Espacio.x16, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(R.drawable.ic_camara, R.drawable.ic_pin_mapa).forEach { icono ->
                    Icon(
                        painter = painterResource(icono),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            if (!negados) {
                BotonSolventa(
                    texto = stringResource(R.string.evidencia_permisos_permitir),
                    onClick = onPermitir,
                )
            }
            BotonSolventa(
                texto = stringResource(
                    if (negados) R.string.comun_cerrar else R.string.evidencia_permisos_no_permitir,
                ),
                onClick = onNoPermitir,
                tipo = if (negados) TipoBoton.SECUNDARIO else TipoBoton.TERCIARIO,
            )
        }
    }
}

/**
 * M12c · Precisión del GPS insuficiente (HU-M2.1-01, criterio 3).
 *
 * Avisa antes de capturar, con la precisión actual y el límite, y deja las
 * dos salidas del frame. Si captura de todas formas, la evidencia se guarda
 * marcada como de ubicación no confiable.
 */
@Composable
fun HojaDePrecisionInsuficiente(
    precisionM: Float?,
    onEsperar: () -> Unit,
    onCapturarIgual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x16),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = Espacio.x12, topEnd = Espacio.x12))
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(start = Espacio.x16, end = Espacio.x16, top = Espacio.x8, bottom = Espacio.x32),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colores.bordeFuerte),
            )
            Text(
                text = stringResource(R.string.evidencia_precision_titulo),
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_navegacion),
                    contentDescription = null,
                    tint = colores.advertenciaFuerte,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = precisionM?.let {
                        stringResource(
                            R.string.evidencia_precision_detalle,
                            it.toInt(),
                            co.solventa.movil.core.ubicacion.Ubicacion.PRECISION_MAXIMA_M.toInt(),
                        )
                    } ?: stringResource(R.string.evidencia_precision_sin_lectura),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colores.textoSecundario,
                )
            }
            BotonSolventa(
                texto = stringResource(R.string.evidencia_precision_esperar),
                onClick = onEsperar,
                icono = painterResource(R.drawable.ic_recargar),
            )
            BotonSolventa(
                texto = stringResource(R.string.evidencia_precision_capturar_igual),
                onClick = onCapturarIgual,
                tipo = TipoBoton.SECUNDARIO,
            )
        }
    }
}
