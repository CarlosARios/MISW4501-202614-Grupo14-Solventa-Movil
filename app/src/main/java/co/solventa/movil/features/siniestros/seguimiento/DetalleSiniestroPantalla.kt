package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaViewModel
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BarraProgreso
import co.solventa.movil.shared.ui.InsigniaEstado
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoInsignia
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.ui.theme.Radio

/**
 * M10 · Detalle del siniestro con su línea de tiempo (HU-M2.4-01): el
 * encabezado del caso, las cinco etapas con la hora en que empezó cada una,
 * «Qué sigue» y el pie «Actualizado hoy a las 14:30». Con la respuesta desde
 * la caché del servicio (HA-L02) muestra el aviso de M9c y la hora de esa
 * versión.
 */
@Composable
fun DetalleSiniestroPantalla(
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DetalleSiniestroViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    ContenidoDetalleSiniestro(
        estado = estado,
        idioma = idioma,
        ahora = System.currentTimeMillis(),
        onAtras = onAtras,
        onReintentar = vm::reintentar,
        modifier = modifier,
    )
}

/** La pantalla sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoDetalleSiniestro(
    estado: DetalleSiniestroUiState,
    idioma: Idioma,
    ahora: Long,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaRegistro(titulo = stringResource(R.string.siniestros_seguimiento_detalle_titulo), modifier = modifier, onAtras = onAtras) {
        val frescura = estado.frescura
        if (frescura != null && frescura.desdeCache) {
            AvisoDeVersionEnCache(frescura.leidoEn, ahora, idioma, estado.recargando, onReintentar)
        }
        val siniestro = estado.siniestro
        val fallo = estado.fallo
        when {
            estado.cargando -> CargandoSiniestros()
            siniestro == null -> FalloConReintento(fallo ?: FalloDeConsulta.ERROR, onReintentar)
            else -> {
                if (fallo != null) {
                    AvisoSolventa(tipo = TipoAviso.ERROR, texto = stringResource(R.string.siniestros_seguimiento_error_con_lista))
                }
                Encabezado(siniestro, estado.progreso, idioma)
                RotuloDeSeccion(stringResource(R.string.siniestros_seguimiento_linea_de_tiempo))
                TarjetaSolventa {
                    Column(modifier = Modifier.padding(vertical = Espacio.x16)) {
                        estado.etapas.forEachIndexed { i, etapa ->
                            FilaDeEtapa(etapa, numero = i + 1, ultima = i == estado.etapas.lastIndex, idioma = idioma)
                        }
                    }
                }
                QueSigue(siniestro.estado)
                if (frescura != null) PieDeFrescura(frescura, ahora, idioma)
            }
        }
    }
}

@Composable
private fun Encabezado(s: Siniestro, progreso: Float, idioma: Idioma) {
    val colores = LocalColoresSolventa.current
    val detalle = listOfNotNull(
        s.poliza,
        stringResource(
            R.string.siniestros_seguimiento_hecho_el,
            FormatoDeFecha.fecha(s.ocurrioEn, idioma),
            FormatoDeFecha.hora(s.ocurrioEn, idioma),
        ),
        s.lugar,
    ).joinToString(" · ")
    TarjetaSolventa {
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.x8), modifier = Modifier.padding(vertical = Espacio.x16)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = s.radicado,
                    style = MaterialTheme.typography.labelMedium,
                    color = colores.textoSecundario,
                    modifier = Modifier.weight(1f),
                )
                InsigniaDeSiniestro(s)
            }
            Text(text = tituloDe(s), style = MaterialTheme.typography.titleLarge)
            Text(text = detalle, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
            BarraProgreso(avance = progreso, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Una etapa: hecha (con su hora), la actual («Etapa actual», desde cuándo) o pendiente (lo que pasará). */
@Composable
private fun FilaDeEtapa(etapa: EtapaDeLinea, numero: Int, ultima: Boolean, idioma: Idioma) {
    val colores = LocalColoresSolventa.current
    val titulo = stringResource(EtiquetasDeSiniestro.etapa(etapa))
    val desde = etapa.desde
    val detalle = when (etapa.avance) {
        Avance.HECHA -> desde?.let { stringResource(R.string.siniestros_seguimiento_fecha_y_hora, FormatoDeFecha.fecha(it, idioma), FormatoDeFecha.hora(it, idioma)) }
        Avance.ACTUAL -> desde?.let { stringResource(R.string.siniestros_seguimiento_etapa_desde, FormatoDeFecha.fecha(it, idioma), FormatoDeFecha.hora(it, idioma)) }
        Avance.PENDIENTE -> EtiquetasDeSiniestro.etapaPendiente(etapa.etapa)?.let { stringResource(it) }
    }
    val estadoLeido = stringResource(
        when (etapa.avance) {
            Avance.HECHA -> R.string.siniestros_seguimiento_etapa_hecha
            Avance.ACTUAL -> R.string.siniestros_seguimiento_etapa_actual
            Avance.PENDIENTE -> R.string.siniestros_seguimiento_etapa_pendiente
        },
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics { contentDescription = listOfNotNull(titulo, estadoLeido, detalle).joinToString(". ") },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
            MarcaDeEtapa(etapa.avance, numero)
            if (!ultima) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (etapa.avance == Avance.HECHA) MaterialTheme.colorScheme.primary else colores.bordeBase),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x4),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (ultima) 0.dp else Espacio.x16),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.x8)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (etapa.avance == Avance.PENDIENTE) colores.textoSecundario else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (etapa.avance == Avance.ACTUAL) {
                    InsigniaEstado(texto = estadoLeido, tipo = TipoInsignia.INFORMACION)
                }
            }
            if (detalle != null) {
                Text(text = detalle, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
            }
        }
    }
}

/** El círculo de la etapa: con visto si pasó, con su número si es la actual o está pendiente. */
@Composable
private fun MarcaDeEtapa(avance: Avance, numero: Int) {
    val primario = MaterialTheme.colorScheme.primary
    val borde = LocalColoresSolventa.current.bordeBase
    val forma = Modifier.size(24.dp)
    when (avance) {
        Avance.HECHA -> Box(contentAlignment = Alignment.Center, modifier = forma.background(primario, CircleShape)) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
        Avance.ACTUAL, Avance.PENDIENTE -> Box(
            contentAlignment = Alignment.Center,
            modifier = forma
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(2.dp, if (avance == Avance.ACTUAL) primario else borde, CircleShape),
        ) {
            Text(
                text = numero.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (avance == Avance.ACTUAL) primario else LocalColoresSolventa.current.textoSecundario,
            )
        }
    }
}

/** «Qué sigue»: lo que pasará después, según el estado. */
@Composable
private fun QueSigue(estado: EstadoDeSiniestro) {
    val colores = LocalColoresSolventa.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = Modifier
            .fillMaxWidth()
            .background(colores.informacionSuave, RoundedCornerShape(Radio.base))
            .padding(Espacio.x16),
    ) {
        Text(text = stringResource(R.string.siniestros_seguimiento_que_sigue), style = MaterialTheme.typography.titleSmall)
        Text(text = stringResource(EtiquetasDeSiniestro.queSigue(estado)), style = MaterialTheme.typography.bodyMedium)
    }
}

/** «Actualizado hoy a las 14:30», o la hora de la versión en caché (HA-L02). */
@Composable
private fun PieDeFrescura(frescura: Frescura, ahora: Long, idioma: Idioma) {
    val colores = LocalColoresSolventa.current
    val texto = stringResource(
        if (frescura.desdeCache) R.string.siniestros_seguimiento_pie_cache else R.string.siniestros_seguimiento_pie_actualizado,
        cuando(frescura.leidoEn, ahora, idioma),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = texto },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sincronizar),
            contentDescription = null,
            tint = colores.textoSecundario,
            modifier = Modifier.size(16.dp),
        )
        Text(text = texto, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
    }
}
