package co.solventa.movil.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.Radio

/** Variantes del componente `Botón` del sistema de diseño. */
enum class TipoBoton {
    PRIMARIO,
    SECUNDARIO,
    TERCIARIO,

    /**
     * Sobre fondo oscuro: relleno claro con el texto en azul. Es el primario
     * de la bienvenida, donde el azul del tema desapareceria contra el fondo.
     */
    CLARO,

    /** Sobre fondo oscuro, sin relleno ni borde: solo el texto en blanco. */
    TEXTO_CLARO,
}

private val AlturaBoton = 48.dp

/**
 * Botón de ancho completo, que es como aparece en todas las pantallas
 * móviles del prototipo.
 */
@Composable
fun BotonSolventa(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tipo: TipoBoton = TipoBoton.PRIMARIO,
    icono: Painter? = null,
    habilitado: Boolean = true,
) {
    val contenido: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espacio.x8, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (icono != null) {
                Icon(painter = icono, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(text = texto, style = MaterialTheme.typography.labelLarge)
        }
    }

    val forma = MaterialTheme.shapes.small
    val alto = modifier.fillMaxWidth().height(AlturaBoton)

    when (tipo) {
        TipoBoton.PRIMARIO -> Button(
            onClick = onClick,
            enabled = habilitado,
            shape = forma,
            modifier = alto,
            contentPadding = ButtonDefaults.ContentPadding,
        ) { contenido() }

        TipoBoton.SECUNDARIO -> OutlinedButton(
            onClick = onClick,
            enabled = habilitado,
            shape = forma,
            modifier = alto,
        ) { contenido() }

        TipoBoton.TERCIARIO -> TextButton(
            onClick = onClick,
            enabled = habilitado,
            shape = forma,
            modifier = alto,
        ) { contenido() }

        TipoBoton.CLARO -> Button(
            onClick = onClick,
            enabled = habilitado,
            shape = forma,
            modifier = alto,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = ButtonDefaults.ContentPadding,
        ) { contenido() }

        TipoBoton.TEXTO_CLARO -> TextButton(
            onClick = onClick,
            enabled = habilitado,
            shape = forma,
            modifier = alto,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { contenido() }
    }
}

/** Radio compartido por los contenedores del sistema de diseño. */
internal val RadioTarjeta = Radio.tarjeta
