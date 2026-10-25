package co.solventa.movil.features.auditoria.consentimientos

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.shared.ui.InsigniaEstado
import co.solventa.movil.shared.ui.TipoInsignia
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.ui.theme.Radio

/** El texto de un codigo del servicio, o el codigo tal cual si no se conoce. */
@Composable
internal fun textoDe(@StringRes clave: Int?, codigo: String): String =
    clave?.let { stringResource(it) } ?: codigo

/** La insignia del estado visible: verde para activo, gris para lo demas. */
@Composable
internal fun InsigniaDeConsentimiento(estado: String, modifier: Modifier = Modifier) {
    InsigniaEstado(
        texto = textoDe(EtiquetasDeConsentimiento.estado(estado), estado),
        tipo = when (estado) {
            EstadosDeConsentimiento.ACTIVO -> TipoInsignia.EXITO
            EstadosDeConsentimiento.REVOCACION_PENDIENTE -> TipoInsignia.ADVERTENCIA
            else -> TipoInsignia.NEUTRO
        },
        modifier = modifier,
    )
}

/** El icono de la entidad financiera en su recuadro (frames 37:1233 y 37:1418). */
@Composable
internal fun IconoDeEntidad(atenuado: Boolean, modifier: Modifier = Modifier) {
    val colores = LocalColoresSolventa.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .background(
                if (atenuado) colores.bordeBase.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(Radio.base),
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_banco),
            contentDescription = null,
            tint = if (atenuado) colores.textoSecundario else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Rotulo en mayusculas de una seccion o un campo («ACTIVOS (2)», «OTORGADO»). */
@Composable
internal fun Rotulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalColoresSolventa.current.textoSecundario,
        modifier = modifier,
    )
}
