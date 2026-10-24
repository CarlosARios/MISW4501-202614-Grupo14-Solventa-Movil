package co.solventa.movil.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.ui.theme.Radio

/** Tono de una insignia de estado. */
enum class TipoInsignia { EXITO, NEUTRO, ADVERTENCIA }

/**
 * Insignia de estado con punto **y** texto, como «● Activo» del prototipo
 * (frame 37:1233). El estado nunca se comunica solo con color (OBJ 13): el
 * texto dice lo mismo que el tono. La usan la consulta de consentimientos
 * (HU-M1.3-01), el perfil y, despues, la revocacion (HU-M1.3-02).
 */
@Composable
fun InsigniaEstado(
    texto: String,
    tipo: TipoInsignia,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val (fondo, tinte) = when (tipo) {
        TipoInsignia.EXITO -> colores.exitoSuave to colores.exitoFuerte
        TipoInsignia.ADVERTENCIA -> colores.advertenciaSuave to colores.advertenciaFuerte
        TipoInsignia.NEUTRO -> colores.bordeBase.copy(alpha = 0.5f) to colores.textoSecundario
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = modifier
            .background(fondo, RoundedCornerShape(Radio.pequeno))
            .padding(horizontal = Espacio.x8, vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(tinte, CircleShape),
        )
        Text(text = texto, style = MaterialTheme.typography.labelMedium, color = tinte)
    }
}
