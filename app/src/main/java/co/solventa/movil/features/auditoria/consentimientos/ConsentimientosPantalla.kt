package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
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
 * M8 · Privacidad y consentimientos (HU-M1.3-01, frame 37:1233; sin conexion,
 * frame 38:1503). Solo lectura: ninguna tarjeta modifica nada (criterio 4).
 *
 * HU-M1.3-02 (`38:1503`): las revocaciones en cola van en su propia seccion,
 * «Pendientes de envio», con el aviso «Sin conexion: revocacion en cola» y el
 * boton «Reintentar envio ahora», deshabilitado sin señal (criterio 5).
 */
@Composable
fun ConsentimientosPantalla(
    onAtras: () -> Unit,
    onAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: ConsentimientosViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    ContenidoConsentimientos(
        estado = estado,
        idioma = idioma,
        onAtras = onAtras,
        onAbrir = onAbrir,
        onReintentar = vm::reintentar,
        modifier = modifier,
        onReintentarEnvio = vm::reintentarEnvio,
        onCerrarAviso = vm::descartarAviso,
    )
}

/** La pantalla sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoConsentimientos(
    estado: ConsentimientosUiState,
    idioma: Idioma,
    onAtras: () -> Unit,
    onAbrir: (String) -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    onReintentarEnvio: () -> Unit = {},
    onCerrarAviso: (String) -> Unit = {},
) {
    PantallaRegistro(
        titulo = stringResource(R.string.auditoria_consentimientos_titulo),
        onAtras = onAtras,
        modifier = modifier,
        indicador = if (estado.sinConexion) {
            { IconoSinConexion() }
        } else {
            null
        },
        acciones = {
            if (estado.pendientes.isNotEmpty()) {
                BotonSolventa(
                    texto = stringResource(R.string.auditoria_revocacion_reintentar),
                    onClick = onReintentarEnvio,
                    tipo = TipoBoton.SECUNDARIO,
                    icono = painterResource(R.drawable.ic_sincronizar),
                    habilitado = estado.reintentarHabilitado,
                )
            }
        },
    ) {
        // Sin conexion, el aviso va justo bajo el encabezado, antes del texto
        // introductorio (frame 38:1503). Con una revocacion en cola, el aviso
        // es el de la cola, con la entidad y la hora de la ultima solicitud.
        val ultimaPendiente = estado.ultimaPendiente
        if (ultimaPendiente != null) {
            AvisoDeRevocacionEnCola(ultimaPendiente.revocacion, idioma)
        } else if (estado.sinConexion) {
            val sincronizado = estado.sincronizadoEn
            AvisoSolventa(
                tipo = TipoAviso.ADVERTENCIA,
                texto = if (sincronizado != null) {
                    stringResource(
                        R.string.auditoria_consentimientos_sin_conexion,
                        FormatoDeFecha.fecha(sincronizado, idioma),
                        FormatoDeFecha.hora(sincronizado, idioma),
                    )
                } else {
                    stringResource(R.string.auditoria_consentimientos_sin_conexion_sin_copia)
                },
            )
        }

        estado.descartes.forEach { descarte ->
            AvisoDeDescarte(descarte, onCerrar = { onCerrarAviso(descarte.consentimientoId) })
        }

        Text(
            text = stringResource(R.string.auditoria_consentimientos_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalColoresSolventa.current.textoSecundario,
        )

        when {
            estado.cargando -> Cargando()
            estado.cuentaSinVincular -> MensajeConReintento(
                texto = stringResource(R.string.auditoria_consentimientos_sin_vincular),
                onReintentar = onReintentar,
            )
            estado.error && !estado.hayContenido -> MensajeConReintento(
                texto = stringResource(R.string.auditoria_consentimientos_error),
                onReintentar = onReintentar,
            )
            estado.vacio -> Text(
                text = stringResource(R.string.auditoria_consentimientos_vacio),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> {
                if (estado.error) {
                    AvisoSolventa(tipo = TipoAviso.ERROR, texto = stringResource(R.string.auditoria_consentimientos_error))
                }
                if (estado.pendientes.isNotEmpty()) {
                    SeccionDePendientes(estado.pendientes, idioma, onAbrir)
                }
                Seccion(
                    titulo = stringResource(R.string.auditoria_consentimientos_seccion_activos, estado.activos.size),
                    consentimientos = estado.activos,
                    idioma = idioma,
                    onAbrir = onAbrir,
                )
                Seccion(
                    titulo = stringResource(R.string.auditoria_consentimientos_seccion_historial, estado.historial.size),
                    consentimientos = estado.historial,
                    idioma = idioma,
                    onAbrir = onAbrir,
                )
            }
        }
    }
}

@Composable
private fun Cargando() {
    val descripcion = stringResource(R.string.auditoria_consentimientos_cargando)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.x48)) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
    }
}

@Composable
private fun MensajeConReintento(texto: String, onReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x16)) {
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
        BotonSolventa(
            texto = stringResource(R.string.auditoria_consentimientos_reintentar),
            onClick = onReintentar,
            tipo = TipoBoton.SECUNDARIO,
        )
    }
}

@Composable
private fun Seccion(
    titulo: String,
    consentimientos: List<Consentimiento>,
    idioma: Idioma,
    onAbrir: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x8)) {
        Rotulo(titulo)
        consentimientos.forEach { TarjetaDeConsentimiento(it, idioma, onAbrir) }
    }
}

@Composable
private fun TarjetaDeConsentimiento(c: Consentimiento, idioma: Idioma, onAbrir: (String) -> Unit) {
    val colores = LocalColoresSolventa.current
    val historial = c.estado != EstadosDeConsentimiento.ACTIVO
    val otorgado = FormatoDeFecha.fecha(c.otorgadoEn, idioma)
    val fin = c.finalizadoEn?.let { FormatoDeFecha.fecha(it, idioma) }
    val fechas = when {
        c.estado == EstadosDeConsentimiento.ACTIVO ->
            stringResource(R.string.auditoria_consentimientos_otorgado_vence, otorgado, FormatoDeFecha.fecha(c.venceEn, idioma))
        c.estado == EstadosDeConsentimiento.REVOCADO && fin != null ->
            stringResource(R.string.auditoria_consentimientos_otorgado_revocado, otorgado, fin)
        c.estado == EstadosDeConsentimiento.VENCIDO && fin != null ->
            stringResource(R.string.auditoria_consentimientos_otorgado_vencio, otorgado, fin)
        else -> stringResource(R.string.auditoria_consentimientos_otorgado, otorgado)
    }
    TarjetaSolventa(
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.auditoria_consentimientos_ver_detalle)) {
            onAbrir(c.id)
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier.padding(vertical = Espacio.x16),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.Top) {
                IconoDeEntidad(atenuado = historial)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                    Text(
                        text = c.nombreDeEntidad,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (historial) colores.textoSecundario else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(text = fechas, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_derecha),
                    contentDescription = null,
                    tint = colores.textoSecundario,
                    modifier = Modifier.size(20.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                InsigniaDeConsentimiento(c.estado)
                c.comprobante?.let { ComprobanteDeLaTarjeta(it) }
            }
        }
    }
}

/** «Pendientes de envio (n)» (`38:1503`): las revocaciones que esperan en la cola. */
@Composable
private fun SeccionDePendientes(pendientes: List<ConsentimientoPendiente>, idioma: Idioma, onAbrir: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x8)) {
        Rotulo(stringResource(R.string.auditoria_revocacion_seccion_pendientes, pendientes.size))
        pendientes.forEach { TarjetaDePendiente(it, idioma, onAbrir) }
    }
}

/**
 * La tarjeta de una revocacion en cola (`38:1503`): la entidad, «Revocacion
 * solicitada <fecha> <hora>», la insignia «Revocacion pendiente de envio» y
 * «Se reintenta al recuperar señal» con el reloj. Abre el detalle.
 */
@Composable
private fun TarjetaDePendiente(p: ConsentimientoPendiente, idioma: Idioma, onAbrir: (String) -> Unit) {
    val colores = LocalColoresSolventa.current
    val solicitada = p.revocacion.solicitadaEn
    TarjetaSolventa(
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.auditoria_consentimientos_ver_detalle)) {
            onAbrir(p.consentimiento.id)
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier.padding(vertical = Espacio.x16),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.Top) {
                IconoDeEntidad(atenuado = false)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                    Text(text = p.consentimiento.nombreDeEntidad, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            R.string.auditoria_revocacion_solicitada_el,
                            FormatoDeFecha.fecha(solicitada, idioma),
                            FormatoDeFecha.hora(solicitada, idioma),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.textoSecundario,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_derecha),
                    contentDescription = null,
                    tint = colores.textoSecundario,
                    modifier = Modifier.size(20.dp),
                )
            }
            InsigniaDePendienteDeEnvio()
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x8), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_reloj),
                    contentDescription = null,
                    tint = colores.advertenciaFuerte,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.auditoria_revocacion_se_reintenta),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.textoSecundario,
                )
            }
        }
    }
}

/**
 * El comprobante de un revocado (frame 37:1233): icono de comprobante de 16
 * y, a 8 de el, «Comprobante REV-…». El icono se describe para el lector de
 * pantalla como el comprobante de la revocacion.
 */
@Composable
private fun ComprobanteDeLaTarjeta(comprobante: String) {
    val colores = LocalColoresSolventa.current
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x8), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_comprobante),
            contentDescription = stringResource(R.string.auditoria_detalle_comprobante),
            tint = colores.textoSecundario,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.auditoria_consentimientos_comprobante, comprobante),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )
    }
}

/** Icono de sin red de la barra superior (frame 38:1503), en color de error. */
@Composable
private fun IconoSinConexion() {
    Icon(
        painter = painterResource(R.drawable.ic_sin_conexion),
        contentDescription = stringResource(R.string.auditoria_consentimientos_sin_conexion_icono),
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .size(40.dp)
            .padding(Espacio.x8),
    )
}
