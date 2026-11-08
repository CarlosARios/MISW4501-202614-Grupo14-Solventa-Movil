package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.InsigniaEstado
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.ui.theme.Radio

/** «Hogar · Daño por agua»: la línea de la póliza (o el producto) y el tipo de incidente. */
@Composable
internal fun tituloDe(s: Siniestro): String {
    val linea = s.producto ?: EtiquetasDeSiniestro.linea(s.linea)?.let { stringResource(it) }
    val incidente = EtiquetasDeSiniestro.incidente(s.tipoDeIncidente)?.let { stringResource(it) } ?: s.tipoDeIncidente
    return if (linea != null) stringResource(R.string.siniestros_seguimiento_titulo_caso, linea, incidente) else incidente
}

@Composable
internal fun InsigniaDeSiniestro(s: Siniestro, modifier: Modifier = Modifier) {
    InsigniaEstado(
        texto = EtiquetasDeSiniestro.estado(s.estado)?.let { stringResource(it) } ?: s.codigoDeEstado,
        tipo = EtiquetasDeSiniestro.tono(s.estado),
        modifier = modifier,
    )
}

/** El ícono del tipo de seguro en su cuadro. */
@Composable
internal fun IconoDeSiniestro(linea: String?, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(Radio.base)),
    ) {
        Icon(
            painter = painterResource(EtiquetasDeSiniestro.icono(linea)),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** «hoy 14:30» si es de hoy; si no, «02/09/2026 14:30». */
@Composable
internal fun cuando(instante: Long, ahora: Long, idioma: Idioma): String {
    val hora = FormatoDeFecha.hora(instante, idioma)
    return if (PresentacionDeSiniestros.esDeHoy(instante, ahora)) {
        stringResource(R.string.siniestros_seguimiento_hoy, hora)
    } else {
        stringResource(R.string.siniestros_seguimiento_fecha_y_hora, FormatoDeFecha.fecha(instante, idioma), hora)
    }
}

/**
 * El aviso de M9c (HA-L02): el servicio tardó más de lo esperado y lo que se
 * ve es la última versión guardada, con su hora; se reintenta solo y con el
 * botón *Reintentar ahora*.
 */
@Composable
internal fun AvisoDeVersionEnCache(
    leidoEn: Long,
    ahora: Long,
    idioma: Idioma,
    recargando: Boolean,
    onReintentar: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x12)) {
        AvisoSolventa(
            tipo = TipoAviso.ADVERTENCIA,
            titulo = stringResource(R.string.siniestros_seguimiento_cache_titulo),
            texto = stringResource(R.string.siniestros_seguimiento_cache_texto, cuando(leidoEn, ahora, idioma)),
        )
        BotonSolventa(
            texto = stringResource(
                if (recargando) R.string.siniestros_seguimiento_reintentando else R.string.siniestros_seguimiento_reintentar_ahora,
            ),
            onClick = onReintentar,
            tipo = TipoBoton.SECUNDARIO,
        )
    }
}

@Composable
internal fun CargandoSiniestros() {
    val descripcion = stringResource(R.string.siniestros_seguimiento_cargando)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.x48)) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
    }
}

/** Sin nada que mostrar: el mensaje según el fallo y el botón para reintentar. */
@Composable
internal fun FalloConReintento(fallo: FalloDeConsulta, onReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x16)) {
        Text(
            text = stringResource(
                when (fallo) {
                    FalloDeConsulta.SIN_CONEXION -> R.string.siniestros_seguimiento_sin_conexion
                    FalloDeConsulta.NO_ENCONTRADO -> R.string.siniestros_seguimiento_no_encontrado
                    FalloDeConsulta.ERROR -> R.string.siniestros_seguimiento_error
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (fallo != FalloDeConsulta.NO_ENCONTRADO) {
            BotonSolventa(
                texto = stringResource(R.string.siniestros_seguimiento_reintentar),
                onClick = onReintentar,
                tipo = TipoBoton.SECUNDARIO,
            )
        }
    }
}

/** El rótulo de una sección: «EN CURSO (2) · VERSIÓN EN CACHÉ». */
@Composable
internal fun RotuloDeSeccion(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = LocalColoresSolventa.current.textoSecundario,
        modifier = Modifier.padding(top = Espacio.x8),
    )
}
