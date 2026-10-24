package co.solventa.movil.features.perfil

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/** Etiqueta de prueba de la flecha de una fila que abre otra pantalla. */
const val ETIQUETA_FLECHA_DE_FILA = "perfil_flecha_de_fila"

/**
 * Una fila del perfil (M19). Con [alTocar] nulo queda **en espera**:
 * atenuada, sin flecha, sin responder al toque y con «Próximamente» como
 * valor, porque todavia no tiene servicio detras. Con
 * [conFlecha] en falso una fila activa responde sin flecha, como «Cerrar
 * sesión», que no abre otra pantalla.
 */
@Composable
fun FilaDePerfil(
    @DrawableRes icono: Int,
    titulo: String,
    valor: String?,
    alTocar: (() -> Unit)?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    adorno: (@Composable () -> Unit)? = null,
    conFlecha: Boolean = true,
) {
    val colores = LocalColoresSolventa.current
    val enEspera = alTocar == null
    val tinte = if (enEspera) colores.textoSecundario.copy(alpha = 0.7f) else color
    val valorVisible = if (enEspera) stringResource(R.string.comun_proximamente) else valor
    val descripcionEnEspera = stringResource(R.string.perfil_en_espera_descripcion, titulo)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (alTocar != null) {
                    Modifier.clickable(onClick = alTocar)
                } else {
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = descripcionEnEspera
                        disabled()
                    }
                },
            )
            .padding(vertical = Espacio.x12),
    ) {
        Icon(painter = painterResource(icono), contentDescription = null, tint = tinte, modifier = Modifier.size(20.dp))
        Text(text = titulo, style = MaterialTheme.typography.bodyLarge, color = tinte, modifier = Modifier.weight(1f))
        adorno?.invoke()
        valorVisible?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
        }
        if (!enEspera && conFlecha) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_derecha),
                contentDescription = null,
                tint = colores.textoSecundario,
                modifier = Modifier
                    .size(20.dp)
                    .testTag(ETIQUETA_FLECHA_DE_FILA),
            )
        }
    }
}

/** Una fila que solo informa (Cierre automatico · 5 min): color normal, sin flecha ni toque. */
@Composable
fun FilaInformativaDePerfil(@DrawableRes icono: Int, titulo: String, valor: String, modifier: Modifier = Modifier) {
    val colores = LocalColoresSolventa.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = Espacio.x12),
    ) {
        Icon(painter = painterResource(icono), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text = titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = valor, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
    }
}

/**
 * La fila de la biometria con su interruptor, que va entre el icono y el
 * texto. Sin huella o rostro configurado queda deshabilitada y lo dice.
 */
@Composable
fun FilaConInterruptor(
    @DrawableRes icono: Int,
    titulo: String,
    activado: Boolean,
    habilitado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    detalle: String? = null,
) {
    val colores = LocalColoresSolventa.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Espacio.x8),
    ) {
        Icon(painter = painterResource(icono), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Switch(
            checked = activado,
            onCheckedChange = onCambio,
            enabled = habilitado,
            modifier = Modifier.semantics { contentDescription = titulo },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyLarge)
            detalle?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario) }
        }
    }
}
