package co.solventa.movil.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import co.solventa.movil.R
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.ui.theme.Radio

/** Variantes del componente `Aviso` del sistema de diseño. */
enum class TipoAviso { EXITO, ADVERTENCIA, ERROR, INFORMACION }

/**
 * Bloque de aviso con barra lateral de color, como en M2c y M17b.
 *
 * El color nunca es la única señal: el ícono y el texto dicen lo mismo
 * (OBJ 13). Se anuncia como una sola unidad para el lector de pantalla.
 */
@Composable
fun AvisoSolventa(
    tipo: TipoAviso,
    texto: String,
    modifier: Modifier = Modifier,
    titulo: String? = null,
) {
    val colores = LocalColoresSolventa.current
    val (tinte, fondo, icono) = when (tipo) {
        TipoAviso.EXITO ->
            Triple(colores.exitoFuerte, colores.exitoSuave, R.drawable.ic_circulo_check)

        TipoAviso.ADVERTENCIA ->
            Triple(colores.advertenciaFuerte, colores.advertenciaSuave, R.drawable.ic_triangulo_alerta)

        TipoAviso.ERROR ->
            Triple(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer, R.drawable.ic_circulo_x)

        TipoAviso.INFORMACION ->
            Triple(colores.informacionFuerte, colores.informacionSuave, R.drawable.ic_circulo_info)
    }

    val descripcion = listOfNotNull(titulo, texto).joinToString(". ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(fondo, RoundedCornerShape(Radio.base))
            .clearAndSetSemantics { contentDescription = descripcion },
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(tinte, RoundedCornerShape(topStart = Radio.base, bottomStart = Radio.base)),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier.padding(Espacio.x12),
        ) {
            Icon(
                painter = painterResource(icono),
                contentDescription = null,
                tint = tinte,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                if (titulo != null) {
                    Text(text = titulo, style = MaterialTheme.typography.titleSmall)
                }
                Text(text = texto, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Medallón circular con un ícono, en el color que indique el estado.
 * [tamanoIcono] permite un ícono menor que 24 dp en un círculo pequeño, como
 * el paso con visto del historial de M8b (círculo de 28, visto de 16, frame
 * 37:1418); por omisión conserva la medida de siempre.
 */
@Composable
fun CirculoConIcono(
    icono: Int,
    tinte: Color,
    fondo: Color,
    descripcion: String?,
    modifier: Modifier = Modifier,
    diametro: androidx.compose.ui.unit.Dp = 96.dp,
    tamanoIcono: androidx.compose.ui.unit.Dp = max(diametro / 2, 24.dp),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(diametro)
            .background(fondo, androidx.compose.foundation.shape.CircleShape),
    ) {
        Icon(
            painter = painterResource(icono),
            contentDescription = descripcion,
            tint = tinte,
            modifier = Modifier.size(tamanoIcono),
        )
    }
}

/** Separador horizontal del ancho completo. */
@Composable
fun SeparadorHorizontal(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalColoresSolventa.current.bordeBase),
    )
}
