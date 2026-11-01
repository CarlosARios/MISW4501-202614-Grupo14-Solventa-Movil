package co.solventa.movil.features.notificaciones.centro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaViewModel
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.InsigniaEstado
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.SeparadorTarjeta
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoInsignia
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M10b · Siniestro paramétrico resuelto automáticamente (HU-M3.1-01): lo que
 * abre la notificación, sin pasos intermedios (criterio 4). Muestra el monto
 * y la moneda, el evento, el parámetro con su umbral y el valor observado, el
 * medio de pago, la póliza, el siniestro y la clave de idempotencia del pago
 * (HA-D02).
 */
@Composable
fun DetallePagoPantalla(
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DetallePagoViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    ContenidoDetallePago(estado = estado, idioma = idioma, onAtras = onAtras, modifier = modifier)
}

/** La pantalla sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoDetallePago(
    estado: DetallePagoUiState,
    idioma: Idioma,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaRegistro(
        titulo = stringResource(R.string.notificaciones_pago_titulo),
        onAtras = onAtras,
        modifier = modifier,
        indicador = if (estado.sinConexion) {
            { IconoSinConexion() }
        } else {
            null
        },
    ) {
        if (estado.sinConexion && estado.notificacion != null) {
            AvisoSolventa(tipo = TipoAviso.ADVERTENCIA, texto = stringResource(R.string.notificaciones_pago_sin_conexion))
        }
        val pago = estado.notificacion?.pago
        when {
            estado.cargando -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(160.dp)) {
                CircularProgressIndicator()
            }
            pago == null -> Text(text = stringResource(R.string.notificaciones_pago_no_disponible), style = MaterialTheme.typography.bodyLarge)
            else -> Detalle(pago, idioma, estado.superoElUmbral)
        }
    }
}

@Composable
private fun Detalle(pago: PagoParametrico, idioma: Idioma, supero: Boolean) {
    val colores = LocalColoresSolventa.current
    val locale = idioma.aLocale()
    val d = pago.disparador

    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).background(colores.exitoSuave, RoundedCornerShape(12.dp)),
        ) {
            Icon(painter = painterResource(R.drawable.ic_zap), contentDescription = null, tint = colores.exitoFuerte)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
            Text(
                text = stringResource(R.string.notificaciones_pago_resuelto),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            InsigniaEstado(texto = stringResource(R.string.notificaciones_pago_insignia), tipo = TipoInsignia.EXITO)
        }
    }

    Campo(stringResource(R.string.notificaciones_pago_monto), TextosDeNotificacion.monto(pago.monto, pago.moneda, locale), destacado = true)

    TarjetaSolventa {
        Campo(stringResource(R.string.notificaciones_pago_evento), stringResource(TextosDeNotificacion.evento(d.parametro), d.evento))
        SeparadorTarjeta()
        Campo(
            stringResource(R.string.notificaciones_pago_parametro),
            TextosDeNotificacion.parametro(d.parametro)?.let { stringResource(it) } ?: d.parametro,
        )
        SeparadorTarjeta()
        Campo(stringResource(R.string.notificaciones_pago_umbral), conUnidad(d.umbral, d.unidad, idioma))
        SeparadorTarjeta()
        Campo(stringResource(R.string.notificaciones_pago_observado), conUnidad(d.observado, d.unidad, idioma))
    }
    if (supero) {
        AvisoSolventa(tipo = TipoAviso.EXITO, texto = stringResource(R.string.notificaciones_pago_supero))
    }

    TarjetaSolventa {
        Campo(stringResource(R.string.notificaciones_pago_medio), TextosDeNotificacion.medioDePago(pago.medioDePago, locale))
        SeparadorTarjeta()
        Campo(
            stringResource(R.string.notificaciones_pago_fecha),
            stringResource(
                R.string.notificaciones_pago_fecha_valor,
                FormatoDeFecha.fecha(pago.pagadoEn, idioma),
                FormatoDeFecha.hora(pago.pagadoEn, idioma),
            ),
        )
        SeparadorTarjeta()
        Campo(stringResource(R.string.notificaciones_pago_poliza), pago.poliza)
        SeparadorTarjeta()
        Campo(stringResource(R.string.notificaciones_pago_siniestro), pago.siniestro)
        SeparadorTarjeta()
        Campo(stringResource(R.string.notificaciones_pago_clave), pago.claveDeIdempotencia)
        Text(
            text = stringResource(R.string.notificaciones_pago_clave_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
            modifier = Modifier.padding(bottom = Espacio.x12),
        )
    }
}

/** «215 min»: el valor con su unidad, o la unidad tal cual si la aplicación no la conoce. */
@Composable
private fun conUnidad(valor: Double, unidad: String, idioma: Idioma): String {
    val numero = TextosDeNotificacion.numero(valor, idioma.aLocale())
    return TextosDeNotificacion.unidad(unidad)?.let { stringResource(it, numero) } ?: "$numero $unidad"
}

/** Un dato del pago: su rótulo y su valor, leídos juntos por el lector de pantalla. */
@Composable
private fun Campo(rotulo: String, valor: String, destacado: Boolean = false) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Espacio.x12)
            .clearAndSetSemantics { contentDescription = "$rotulo: $valor" },
    ) {
        Text(text = rotulo, style = MaterialTheme.typography.labelMedium, color = LocalColoresSolventa.current.textoSecundario)
        Text(
            text = valor,
            style = if (destacado) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
            color = if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
