package co.solventa.movil.features.siniestros.evidencia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.ui.theme.Azul900
import co.solventa.movil.ui.theme.Espacio
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Fondo del visor, el del frame M12. */
private val FondoDeLaCamara = Color(0xFF0D1424)
private val FondoDelVisor = Color(0xFF1C2638)

/**
 * M12 · Cámara · captura de evidencia con GPS y hora (HU-M2.1-01).
 *
 * Es la pantalla del criterio 4: cada captura queda con su ubicación, su hora
 * en UTC y su resumen de integridad, y el chip inferior lo muestra mientras
 * se apunta.
 *
 * **La galería se dibuja deshabilitada.** El frame la tiene y el criterio 2
 * prohíbe ofrecerla cuando el tipo de siniestro exige evidencia tomada en el
 * lugar, así que el control queda inerte en lugar de desaparecer.
 */
@Composable
fun PantallaCamaraDeEvidencia(
    estado: EvidenciaUiState,
    camara: CamaraDeEvidencia,
    tipoDeSiniestro: String,
    onCerrar: () -> Unit,
    onTomarFoto: () -> Unit,
    onGrabarVideo: () -> Unit,
    onEsperarSenal: () -> Unit,
    onCapturarIgual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(FondoDeLaCamara)) {
        Column(modifier = Modifier.fillMaxSize()) {
            EncabezadoDeLaCaptura(
                numero = estado.numeroDeLaSiguiente,
                tipoDeSiniestro = tipoDeSiniestro,
                onCerrar = onCerrar,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(FondoDelVisor),
            ) {
                VisorDeEvidencia(camara = camara, modifier = Modifier.fillMaxSize())
                GuiasDeEncuadre(modifier = Modifier.fillMaxSize())
                if (estado.grabando) {
                    IndicadorDeGrabacion(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(Espacio.x16),
                    )
                }
                SelloDeLaCaptura(
                    estado = estado,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Espacio.x16),
                )
            }

            ControlesDeLaCaptura(
                estado = estado,
                onTomarFoto = onTomarFoto,
                onGrabarVideo = onGrabarVideo,
            )
        }

        // Criterio 1: la explicación de por qué se piden la cámara y la
        // ubicación va antes del diálogo del sistema (M12b).
        if (camara.estado != EstadoDeLaCaptura.LISTA) {
            DialogoDePermisosDeCaptura(
                estado = camara.estado,
                onPermitir = camara.pedirPermisos,
                onNoPermitir = onCerrar,
            )
        }

        // Criterio 3: la ubicación poco precisa se advierte antes de capturar.
        if (estado.confirmandoPrecision != null) {
            HojaDePrecisionInsuficiente(
                precisionM = estado.ubicacion?.precisionM,
                onEsperar = onEsperarSenal,
                onCapturarIgual = onCapturarIgual,
            )
        }
    }
}

/** El frame pone el encabezado sobre azul 900, con la barra de estado dentro. */
@Composable
private fun EncabezadoDeLaCaptura(
    numero: Int,
    tipoDeSiniestro: String,
    onCerrar: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        modifier = Modifier
            .fillMaxWidth()
            .background(Azul900)
            .statusBarsPadding()
            .padding(horizontal = Espacio.x16, vertical = Espacio.x8),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_x),
            contentDescription = stringResource(R.string.evidencia_cerrar),
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .clickable(role = Role.Button, onClick = onCerrar),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(
                    R.string.evidencia_contador_visor,
                    numero,
                    LimitesDeEvidencia.MAXIMO_DE_FOTOS,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.evidencia_subtitulo_visor, tipoDeSiniestro)
                    .uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
        // El flash queda como lo dibuja el frame: visible y sin acción, que es
        // lo que la historia pide capturar, no iluminar.
        Icon(
            painter = painterResource(R.drawable.ic_zap),
            contentDescription = null,
            tint = Color.White.copy(alpha = INERTE),
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Las guías del encuadre: las esquinas y la retícula de tercios, en blanco
 * translúcido sobre el visor.
 *
 * **Sin silueta de vehículo.** El frame dibuja una para la colisión, pero la
 * evidencia se captura para cuatro tipos de siniestro y una silueta de carro
 * estorba encuadrando un vidrio roto o un documento.
 */
@Composable
private fun GuiasDeEncuadre(modifier: Modifier = Modifier) {
    val color = Color.White.copy(alpha = 0.35f)
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val margen = size.width * 0.08f
            val alto = size.height * 0.68f
            val arriba = (size.height - alto) / 2
            val abajo = arriba + alto
            val derecha = size.width - margen
            val brazo = size.width * 0.12f
            val grosor = 2.dp.toPx()

            // Las cuatro esquinas del encuadre.
            listOf(
                Offset(margen, arriba) to listOf(Offset(brazo, 0f), Offset(0f, brazo)),
                Offset(derecha, arriba) to listOf(Offset(-brazo, 0f), Offset(0f, brazo)),
                Offset(margen, abajo) to listOf(Offset(brazo, 0f), Offset(0f, -brazo)),
                Offset(derecha, abajo) to listOf(Offset(-brazo, 0f), Offset(0f, -brazo)),
            ).forEach { (esquina, brazos) ->
                brazos.forEach { brazo ->
                    drawLine(
                        color = color,
                        start = esquina,
                        end = Offset(esquina.x + brazo.x, esquina.y + brazo.y),
                        strokeWidth = grosor,
                    )
                }
            }

            // La retícula de tercios, más tenue.
            val tenue = color.copy(alpha = 0.18f)
            for (i in 1..2) {
                val x = margen + (derecha - margen) * i / 3f
                drawLine(tenue, Offset(x, arriba), Offset(x, abajo), grosor / 2)
                val y = arriba + alto * i / 3f
                drawLine(tenue, Offset(margen, y), Offset(derecha, y), grosor / 2)
            }
        }
    }
}

/**
 * Aviso de grabación en curso. El frame no lo dibuja —solo tiene el botón de
 * video—, pero sin él no hay forma de saber si la cámara está grabando.
 */
@Composable
private fun IndicadorDeGrabacion(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
        modifier = modifier
            .clip(RoundedCornerShape(Espacio.x16))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = Espacio.x12, vertical = Espacio.x4),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
        )
        Text(
            text = stringResource(R.string.evidencia_grabando),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

/** El chip del frame: punto, precisión, hora en UTC y sello de integridad. */
@Composable
private fun SelloDeLaCaptura(estado: EvidenciaUiState, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = modifier
            .clip(RoundedCornerShape(Espacio.x8))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = Espacio.x12, vertical = Espacio.x8),
    ) {
        LineaDelSello(
            icono = R.drawable.ic_pin_mapa,
            texto = estado.ubicacion?.let {
                stringResource(
                    R.string.evidencia_sello_ubicacion,
                    enGrados(it.latitud, "N", "S"),
                    enGrados(it.longitud, "E", "O"),
                    it.precisionM.toInt(),
                )
            } ?: stringResource(R.string.evidencia_sello_sin_ubicacion),
        )
        LineaDelSello(
            icono = R.drawable.ic_reloj,
            texto = stringResource(R.string.evidencia_sello_hora, horaUtc()),
        )
    }
}

@Composable
private fun LineaDelSello(icono: Int, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x4),
    ) {
        Icon(
            painter = painterResource(icono),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = texto.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

/** Galería deshabilitada, disparador y video, como en el frame. */
@Composable
private fun ControlesDeLaCaptura(
    estado: EvidenciaUiState,
    onTomarFoto: () -> Unit,
    onGrabarVideo: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = Espacio.x32, end = Espacio.x32, top = Espacio.x16, bottom = Espacio.x24),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(Espacio.x8))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .alpha(INERTE),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_imagen),
                contentDescription = stringResource(R.string.evidencia_galeria_no_disponible),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White)
                .alpha(if (estado.puedeTomarFoto || estado.grabando) 1f else INERTE)
                .clickable(
                    enabled = estado.puedeTomarFoto || estado.grabando,
                    role = Role.Button,
                    onClick = onTomarFoto,
                ),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (estado.grabando) MaterialTheme.colorScheme.error else Color.White)
                    .border(3.dp, FondoDeLaCamara, CircleShape),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.x4),
            modifier = Modifier
                .alpha(if (estado.puedeGrabarVideo) 1f else INERTE)
                .clickable(
                    enabled = estado.puedeGrabarVideo,
                    role = Role.Button,
                    onClick = onGrabarVideo,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_video),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(
                    if (estado.grabando) R.string.evidencia_detener else R.string.evidencia_video,
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

/** Opacidad de los controles inertes, la misma del sistema de diseño. */
private const val INERTE = 0.4f

/**
 * El punto como lo escribe el frame: cuatro decimales y la letra del
 * hemisferio, con el separador decimal del idioma en curso.
 */
private fun enGrados(valor: Double, positivo: String, negativo: String): String =
    String.format(Locale.getDefault(), "%.4f %s", abs(valor), if (valor >= 0) positivo else negativo)

/** La hora de la captura en UTC, como la sella la evidencia. */
private fun horaUtc(): String =
    DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        .withZone(ZoneOffset.UTC)
        .format(Instant.now())
