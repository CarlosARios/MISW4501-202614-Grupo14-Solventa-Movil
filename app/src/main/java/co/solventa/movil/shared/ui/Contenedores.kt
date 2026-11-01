package co.solventa.movil.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * Armazón común de las pantallas del registro, con la misma estructura de los
 * frames: encabezado, contenido desplazable y bloque de acciones abajo.
 *
 */
@Composable
fun PantallaRegistro(
    titulo: String,
    modifier: Modifier = Modifier,
    onAtras: (() -> Unit)? = null,
    onCerrar: (() -> Unit)? = null,
    acciones: @Composable ColumnScope.() -> Unit = {},
    indicador: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EncabezadoPantalla(titulo = titulo, onAtras = onAtras, onCerrar = onCerrar, indicador = indicador)
        },
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Espacio.x8),
                modifier = Modifier
                    .fillMaxWidth()
                    // La aplicacion dibuja de borde a borde, asi que el
                    // teclado no encoge la ventana: lo hace esta barra, que
                    // se aparta lo que ocupe el teclado o la barra de
                    // navegacion, lo que sea mayor. Al crecer, el contenido
                    // de arriba se encoge y el campo con el foco puede
                    // desplazarse hasta quedar a la vista.
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(
                        start = Espacio.x16,
                        end = Espacio.x16,
                        top = Espacio.x12,
                        bottom = Espacio.x16,
                    ),
                content = acciones,
            )
        },
    ) { relleno ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x16),
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x16, vertical = Espacio.x24),
            content = contenido,
        )
    }
}

@Composable
private fun EncabezadoPantalla(
    titulo: String,
    onAtras: (() -> Unit)?,
    onCerrar: (() -> Unit)? = null,
    indicador: (@Composable () -> Unit)? = null,
) {
    // La aplicacion dibuja de borde a borde, asi que el encabezado tiene que
    // dejarle su espacio a la barra de estado: sin esto el titulo queda
    // encima del reloj y los iconos del sistema.
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Espacio.x16, vertical = Espacio.x8),
            ) {
                if (onAtras != null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_flecha_izquierda),
                        contentDescription = stringResource(R.string.comun_atras),
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(onClick = onAtras)
                            .padding(Espacio.x8),
                    )
                }
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                // Un estado de la pantalla a la derecha del titulo, como el
                // icono de sin red de M8 (frame 38:1503). No se toca.
                indicador?.invoke()
                // Salir del flujo sin terminarlo, como en M11a. Lo que ya se
                // diligencio queda guardado como borrador.
                if (onCerrar != null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_x),
                        contentDescription = stringResource(R.string.comun_cerrar),
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(onClick = onCerrar)
                            .padding(Espacio.x8),
                    )
                }
            }
            HorizontalDivider(color = LocalColoresSolventa.current.bordeBase)
        }
    }
}

/** Tarjeta del sistema de diseño: superficie blanca, borde base, radio 12. */
@Composable
fun TarjetaSolventa(
    modifier: Modifier = Modifier,
    relleno: PaddingValues = PaddingValues(horizontal = Espacio.x16),
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val colores = LocalColoresSolventa.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(RadioTarjeta))
            .border(1.dp, colores.bordeBase, RoundedCornerShape(RadioTarjeta))
            .padding(relleno),
        content = contenido,
    )
}

/** Separador entre filas dentro de una tarjeta. */
@Composable
fun SeparadorTarjeta() {
    HorizontalDivider(color = LocalColoresSolventa.current.bordeBase)
}

/** Barra de avance de los tres pasos del registro. */
@Composable
fun BarraProgreso(
    avance: Float,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(colores.bordeBase, RoundedCornerShape(Espacio.x4))
            // Sin esto el lector de pantalla no anuncia el avance: la barra
            // sería solo dos rectángulos de color.
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(avance.coerceIn(0f, 1f), 0f..1f) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(avance.coerceIn(0f, 1f))
                .height(6.dp)
                .background(color, RoundedCornerShape(Espacio.x4)),
        )
    }
}
