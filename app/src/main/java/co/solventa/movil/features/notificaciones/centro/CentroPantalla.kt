package co.solventa.movil.features.notificaciones.centro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M15 · Centro de notificaciones (HU-M3.1-01): las de hoy y las anteriores,
 * con las no leídas marcadas, «Marcar todas como leídas» y «Ver detalle», que
 * abre el pago (M10b). Se lee de la copia local, así que también sirve sin
 * conexión. Solo muestra los pagos paramétricos: los demás tipos del prototipo
 * (renovación, soporte pendiente) son de HU-M3.2-01 y HU-M3.3-01.
 */
@Composable
fun CentroPantalla(
    onAtras: () -> Unit,
    onAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: CentroViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    ContenidoCentro(
        estado = estado,
        idioma = idioma,
        ahora = System.currentTimeMillis(),
        onAtras = onAtras,
        onAbrir = onAbrir,
        onMarcarTodas = vm::marcarTodasLeidas,
        onReintentar = vm::reintentar,
        modifier = modifier,
    )
}

/** La pantalla sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoCentro(
    estado: CentroUiState,
    idioma: Idioma,
    ahora: Long,
    onAtras: () -> Unit,
    onAbrir: (String) -> Unit,
    onMarcarTodas: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaRegistro(
        titulo = stringResource(R.string.notificaciones_centro_titulo),
        onAtras = onAtras,
        modifier = modifier,
        indicador = if (estado.sinConexion) {
            { IconoSinConexion() }
        } else {
            null
        },
    ) {
        if (estado.sinConexion) {
            val sincronizado = estado.sincronizadoEn
            AvisoSolventa(
                tipo = TipoAviso.ADVERTENCIA,
                texto = if (sincronizado != null) {
                    stringResource(
                        R.string.notificaciones_centro_sin_conexion,
                        FormatoDeFecha.fecha(sincronizado, idioma),
                        FormatoDeFecha.hora(sincronizado, idioma),
                    )
                } else {
                    stringResource(R.string.notificaciones_centro_sin_conexion_sin_copia)
                },
            )
        }
        when {
            estado.cargando -> Cargando()
            estado.error && !estado.hayContenido -> Column(verticalArrangement = Arrangement.spacedBy(Espacio.x16)) {
                Text(text = stringResource(R.string.notificaciones_centro_error_sin_copia), style = MaterialTheme.typography.bodyLarge)
                BotonSolventa(
                    texto = stringResource(R.string.notificaciones_centro_reintentar),
                    onClick = onReintentar,
                    tipo = TipoBoton.SECUNDARIO,
                )
            }
            estado.vacio -> Text(text = stringResource(R.string.notificaciones_centro_vacio), style = MaterialTheme.typography.bodyLarge)
            else -> {
                if (estado.error) {
                    AvisoSolventa(tipo = TipoAviso.ERROR, texto = stringResource(R.string.notificaciones_centro_error))
                }
                Seccion(
                    titulo = stringResource(R.string.notificaciones_centro_hoy),
                    notificaciones = estado.hoy,
                    idioma = idioma,
                    deHoy = true,
                    onAbrir = onAbrir,
                    accion = if (estado.puedeMarcarTodas) {
                        { EnlaceMarcarTodas(onMarcarTodas) }
                    } else {
                        null
                    },
                )
                Seccion(
                    titulo = stringResource(R.string.notificaciones_centro_anteriores),
                    notificaciones = estado.anteriores,
                    idioma = idioma,
                    deHoy = false,
                    onAbrir = onAbrir,
                    accion = if (estado.puedeMarcarTodas && estado.hoy.isEmpty()) {
                        { EnlaceMarcarTodas(onMarcarTodas) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun Seccion(
    titulo: String,
    notificaciones: List<Notificacion>,
    idioma: Idioma,
    deHoy: Boolean,
    onAbrir: (String) -> Unit,
    accion: (@Composable () -> Unit)?,
) {
    if (notificaciones.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x12)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = titulo.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LocalColoresSolventa.current.textoSecundario,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            accion?.invoke()
        }
        notificaciones.forEach { TarjetaDePago(it, idioma, deHoy, onAbrir) }
    }
}

@Composable
private fun EnlaceMarcarTodas(onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.notificaciones_centro_marcar_todas),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Espacio.x8),
    )
}

/** Una tarjeta de pago como la del prototipo: sin leer, con la barra lateral y el punto. */
@Composable
private fun TarjetaDePago(n: Notificacion, idioma: Idioma, deHoy: Boolean, onAbrir: (String) -> Unit) {
    val pago = n.pago ?: return
    val colores = LocalColoresSolventa.current
    val locale = idioma.aLocale()
    val medio = TextosDeNotificacion.medioDePago(pago.medioDePago, locale)
    val hora = FormatoDeFecha.hora(n.ocurrioEn, idioma)
    val cuando = if (deHoy) {
        stringResource(R.string.notificaciones_centro_hora_hoy, hora)
    } else {
        stringResource(R.string.notificaciones_centro_hora_fecha, FormatoDeFecha.fecha(n.ocurrioEn, idioma), hora)
    }
    val sinLeer = stringResource(R.string.notificaciones_centro_sin_leer)
    TarjetaSolventa(
        relleno = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (n.leida) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .clickable(onClickLabel = stringResource(R.string.notificaciones_centro_ver_detalle)) { onAbrir(n.id) },
    ) {
        Row {
            if (!n.leida) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.x8), modifier = Modifier.padding(Espacio.x16)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.Top) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp).background(colores.exitoSuave, RoundedCornerShape(8.dp)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_zap),
                            contentDescription = null,
                            tint = colores.exitoFuerte,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                        Text(
                            text = stringResource(R.string.notificaciones_aviso_titulo, TextosDeNotificacion.monto(pago.monto, pago.moneda, locale)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                TextosDeNotificacion.cuerpoDelCentro(pago.disparador.parametro),
                                pago.disparador.evento,
                                TextosDeNotificacion.numero(pago.disparador.observado, locale),
                                medio,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.textoSecundario,
                        )
                    }
                    if (!n.leida) {
                        Box(
                            modifier = Modifier
                                .padding(top = Espacio.x8)
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .semantics { contentDescription = sinLeer },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = cuando.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colores.textoSecundario,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.notificaciones_centro_ver_detalle),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Cargando() {
    val descripcion = stringResource(R.string.notificaciones_centro_cargando)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(160.dp)) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
    }
}

/** El ícono de sin red de la barra superior, como el de M8 sin conexión. */
@Composable
internal fun IconoSinConexion() {
    Icon(
        painter = painterResource(R.drawable.ic_sin_conexion),
        contentDescription = stringResource(R.string.notificaciones_centro_sin_conexion_icono),
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .size(40.dp)
            .padding(Espacio.x8),
    )
}
