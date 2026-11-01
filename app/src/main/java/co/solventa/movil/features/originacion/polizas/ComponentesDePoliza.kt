package co.solventa.movil.features.originacion.polizas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.InsigniaEstado
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoInsignia
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.ui.theme.Radio

/** El nombre de la póliza: el producto si el núcleo lo da, si no el tipo de seguro. */
@Composable
internal fun nombreDe(poliza: Poliza): String =
    poliza.producto ?: textoDeLinea(poliza.linea)

/** El tipo de seguro en el idioma elegido, o el código si la aplicación no lo conoce. */
@Composable
internal fun textoDeLinea(linea: String): String =
    EtiquetasDePoliza.linea(linea)?.let { stringResource(it) } ?: linea

/** «Activa», «Por vencer»…, con «· renovada» cuando es una renovación (criterio 4). */
@Composable
internal fun textoDeSituacion(p: PolizaEnLista): String {
    val base = stringResource(EtiquetasDePoliza.situacion(p.situacion))
    return if (PresentacionDePolizas.insigniaRenovada(p)) {
        stringResource(R.string.originacion_polizas_situacion_renovada, base)
    } else {
        base
    }
}

@Composable
internal fun InsigniaDeSituacion(p: PolizaEnLista) {
    InsigniaEstado(
        texto = textoDeSituacion(p),
        tipo = when (p.situacion) {
            Situacion.ACTIVA -> TipoInsignia.EXITO
            Situacion.POR_VENCER -> TipoInsignia.ADVERTENCIA
            Situacion.VENCIDA, Situacion.CANCELADA -> TipoInsignia.NEUTRO
        },
    )
}

/** El ícono del tipo de seguro en su cuadro, atenuado si la póliza ya no cubre. */
@Composable
internal fun IconoDeLinea(linea: String, atenuado: Boolean, modifier: Modifier = Modifier) {
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
            painter = painterResource(EtiquetasDePoliza.icono(linea)),
            contentDescription = null,
            tint = if (atenuado) colores.textoSecundario else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** La línea bajo el nombre: número y vigencia, o lo que la situación dice de ella. */
@Composable
internal fun subtituloDe(p: PolizaEnLista, idioma: Idioma): String {
    val poliza = p.poliza
    val fin = FormatoDeFecha.fecha(poliza.fin, idioma)
    return when (PresentacionDePolizas.subtitulo(p)) {
        PresentacionDePolizas.Subtitulo.CANCELADA -> stringResource(R.string.originacion_polizas_subtitulo_cancelada, poliza.id)
        PresentacionDePolizas.Subtitulo.VENCIO -> stringResource(R.string.originacion_polizas_subtitulo_vencio, poliza.id, fin)
        PresentacionDePolizas.Subtitulo.RENOVADA -> stringResource(R.string.originacion_polizas_subtitulo_renovada, poliza.id, fin)
        PresentacionDePolizas.Subtitulo.VENCE -> stringResource(R.string.originacion_polizas_subtitulo_vence, poliza.id, fin)
        PresentacionDePolizas.Subtitulo.VIGENCIA -> stringResource(
            R.string.originacion_polizas_subtitulo_vigencia,
            poliza.id,
            FormatoDeFecha.fecha(poliza.inicio, idioma),
            fin,
        )
    }
}

/** Una tarjeta de M4: tipo, número y vigencia, situación, valor asegurado y prima u objeto. */
@Composable
internal fun TarjetaDePoliza(p: PolizaEnLista, idioma: Idioma, onAbrir: (String) -> Unit) {
    val colores = LocalColoresSolventa.current
    val poliza = p.poliza
    val sinCobertura = PresentacionDePolizas.sinCobertura(p)
    val locale = idioma.aLocale()
    val pie = listOfNotNull(
        stringResource(R.string.originacion_polizas_valor_asegurado),
        poliza.prima?.let { stringResource(R.string.originacion_polizas_prima_mensual, EtiquetasDePoliza.monto(it, poliza.moneda, locale)) },
        poliza.objetoAsegurado,
    ).joinToString(" · ")
    TarjetaSolventa(
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.originacion_polizas_ver_detalle, poliza.id)) {
            onAbrir(poliza.id)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.x16), modifier = Modifier.padding(vertical = Espacio.x16)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.Top) {
                IconoDeLinea(poliza.linea, atenuado = sinCobertura)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                    Text(
                        text = nombreDe(poliza),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (sinCobertura) colores.textoSecundario else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(text = subtituloDe(p, idioma), style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_derecha),
                    contentDescription = null,
                    tint = colores.textoSecundario,
                    modifier = Modifier.size(20.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { InsigniaDeSituacion(p) }
                Text(
                    text = if (p.situacion == Situacion.CANCELADA) {
                        stringResource(R.string.originacion_polizas_sin_cobertura)
                    } else {
                        EtiquetasDePoliza.monto(poliza.valorAsegurado, poliza.moneda, locale)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (sinCobertura) colores.textoSecundario else MaterialTheme.colorScheme.primary,
                )
            }
            if (p.situacion != Situacion.CANCELADA) {
                Text(text = pie, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
            }
        }
    }
}

/** El texto de un cambio: «Tu póliza AU-2025-118204 fue renovada hasta el 02/10/2027.» */
@Composable
internal fun textoDeCambio(cambio: CambioDePoliza, idioma: Idioma): String =
    stringResource(EtiquetasDePoliza.cambio(cambio.tipo), cambio.poliza.id, FormatoDeFecha.fecha(cambio.poliza.fin, idioma))

/** Un aviso con su botón para cerrarlo, como el de los cambios detectados (criterio 4). */
@Composable
internal fun AvisoQueSeCierra(
    tipo: TipoAviso,
    titulo: String,
    texto: String,
    descripcionCerrar: String,
    onCerrar: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        AvisoSolventa(tipo = tipo, titulo = titulo, texto = texto, modifier = Modifier.weight(1f))
        IconButton(onClick = onCerrar, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = descripcionCerrar,
                tint = LocalColoresSolventa.current.textoSecundario,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** El ícono de sin red de la barra superior, como el de M8 sin conexión. */
@Composable
internal fun IconoSinConexion() {
    Icon(
        painter = painterResource(R.drawable.ic_sin_conexion),
        contentDescription = stringResource(R.string.originacion_polizas_sin_conexion_icono),
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .size(40.dp)
            .padding(Espacio.x8),
    )
}
