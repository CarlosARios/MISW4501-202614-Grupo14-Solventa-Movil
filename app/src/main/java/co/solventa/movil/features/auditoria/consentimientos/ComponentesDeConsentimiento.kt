package co.solventa.movil.features.auditoria.consentimientos

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.InsigniaEstado
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoInsignia
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.ui.theme.Radio

/** El texto de un codigo del servicio, o el codigo tal cual si no se conoce. */
@Composable
internal fun textoDe(@StringRes clave: Int?, codigo: String): String =
    clave?.let { stringResource(it) } ?: codigo

/**
 * La insignia del estado visible: verde para activo, ambar para una
 * revocacion que no termina (parcial, HU-M1.3-02) y gris para lo
 * demas.
 */
@Composable
internal fun InsigniaDeConsentimiento(estado: String, modifier: Modifier = Modifier) {
    InsigniaEstado(
        texto = textoDe(EtiquetasDeConsentimiento.estado(estado), estado),
        tipo = when (estado) {
            EstadosDeConsentimiento.ACTIVO -> TipoInsignia.EXITO
            EstadosDeConsentimiento.REVOCACION_PENDIENTE, EstadosDeConsentimiento.REVOCADO_PARCIAL -> TipoInsignia.ADVERTENCIA
            else -> TipoInsignia.NEUTRO
        },
        modifier = modifier,
    )
}

/**
 * «Revocacion pendiente de envio» (HU-M1.3-02, `38:1503` y el detalle): la
 * revocacion confirmada que espera en la cola del telefono, con punto y texto.
 */
@Composable
internal fun InsigniaDePendienteDeEnvio(modifier: Modifier = Modifier) {
    InsigniaEstado(
        texto = stringResource(R.string.auditoria_revocacion_pendiente_de_envio),
        tipo = TipoInsignia.ADVERTENCIA,
        modifier = modifier,
    )
}

/**
 * El boton de revocar, en el color de error del tema como en los frames
 * `37:1418` y `37:1519`: revocar es una accion que no se deshace. Mide 48 y
 * usa la forma de los botones del sistema de diseño.
 */
@Composable
internal fun BotonDeRevocar(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    conHuella: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x8), verticalAlignment = Alignment.CenterVertically) {
            if (conHuella) {
                Icon(painter = painterResource(R.drawable.ic_huella), contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(text = texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Un aviso que el titular puede cerrar: el componente «Aviso» y, a su
 * derecha, una «x» para cerrarlo. Lo usan los mensajes de la revocacion
 * (HU-M1.3-02).
 */
@Composable
internal fun AvisoQueSeCierra(
    tipo: TipoAviso,
    texto: String,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    titulo: String? = null,
) {
    Row(verticalAlignment = Alignment.Top, modifier = modifier.fillMaxWidth()) {
        AvisoSolventa(tipo = tipo, texto = texto, titulo = titulo, modifier = Modifier.weight(1f))
        IconButton(onClick = onCerrar, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = stringResource(R.string.auditoria_revocacion_cerrar_aviso),
                tint = LocalColoresSolventa.current.textoSecundario,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** El mensaje de la revocacion como aviso: su tono y su texto. */
@Composable
internal fun AvisoDeLaRevocacionVisible(aviso: AvisoDeLaRevocacion, onCerrar: () -> Unit) {
    val (tipo, texto) = when (aviso) {
        AvisoDeLaRevocacion.YA_REVOCADO -> TipoAviso.INFORMACION to R.string.auditoria_revocacion_aviso_ya_revocado
        AvisoDeLaRevocacion.YA_NO_ACTIVO -> TipoAviso.ADVERTENCIA to R.string.auditoria_revocacion_aviso_ya_no_activo
        AvisoDeLaRevocacion.NO_SE_PUDO -> TipoAviso.ERROR to R.string.auditoria_revocacion_aviso_no_se_pudo
        AvisoDeLaRevocacion.HUELLA_NO_RECONOCIDA -> TipoAviso.ERROR to R.string.auditoria_revocacion_aviso_huella_no_reconocida
        AvisoDeLaRevocacion.CONTRASENA_INCORRECTA -> TipoAviso.ERROR to R.string.auditoria_revocacion_aviso_contrasena_incorrecta
        AvisoDeLaRevocacion.COMPROBANTE_GUARDADO -> TipoAviso.EXITO to R.string.auditoria_revocacion_aviso_comprobante_guardado
        AvisoDeLaRevocacion.COMPROBANTE_SIN_CONEXION ->
            TipoAviso.ADVERTENCIA to R.string.auditoria_revocacion_aviso_comprobante_sin_conexion
        AvisoDeLaRevocacion.COMPROBANTE_ERROR -> TipoAviso.ERROR to R.string.auditoria_revocacion_aviso_comprobante_error
    }
    AvisoQueSeCierra(tipo = tipo, texto = stringResource(texto), onCerrar = onCerrar)
}

/**
 * Una revocacion de la cola que el servicio ya no acepto (sin frame: se arma
 * con el componente «Aviso» de `38:1503`): vencio mientras esperaba o ya
 * estaba registrada.
 */
@Composable
internal fun AvisoDeDescarte(descarte: AvisoDeRevocacion, onCerrar: () -> Unit) {
    when (descarte.motivo) {
        MotivoDeDescarte.VENCIO -> AvisoQueSeCierra(
            tipo = TipoAviso.ADVERTENCIA,
            texto = stringResource(R.string.auditoria_revocacion_descarte_vencio, descarte.entidad),
            onCerrar = onCerrar,
        )
        MotivoDeDescarte.YA_REVOCADO -> AvisoQueSeCierra(
            tipo = TipoAviso.INFORMACION,
            texto = stringResource(R.string.auditoria_revocacion_descarte_ya_revocado, descarte.entidad),
            onCerrar = onCerrar,
        )
    }
}

/**
 * El enlace «Descargar comprobante de revocacion (PDF)» del detalle
 * en una revocacion, como el del comprobante de otorgamiento de `37:1418`: icono de
 * comprobante y texto en azul. Mientras descarga muestra el progreso.
 */
@Composable
internal fun EnlaceDeComprobante(texto: String, descargando: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !descargando, role = Role.Button, onClick = onClick)
            .padding(vertical = Espacio.x8),
    ) {
        if (descargando) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_comprobante),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(text = texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
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
