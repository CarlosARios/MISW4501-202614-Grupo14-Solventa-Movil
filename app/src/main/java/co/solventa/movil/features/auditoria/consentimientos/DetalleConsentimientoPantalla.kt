package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M8b · Detalle del consentimiento (HU-M1.3-01, frame 37:1418). Muestra todo lo
 * que pide el criterio 2 y no permite editar nada (criterio 4): el unico boton,
 * «Revocar consentimiento», aparece solo en un activo y deshabilitado hasta
 * HU-M1.3-02 (D-8 b).
 */
@Composable
fun DetalleConsentimientoPantalla(
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DetalleConsentimientoViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    ContenidoDetalleConsentimiento(estado = estado, idioma = idioma, onAtras = onAtras, modifier = modifier)
}

/** El detalle sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoDetalleConsentimiento(
    estado: DetalleConsentimientoUiState,
    idioma: Idioma,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaRegistro(
        titulo = stringResource(R.string.auditoria_detalle_titulo),
        onAtras = onAtras,
        modifier = modifier,
        acciones = {
            if (estado.revocarVisible) {
                BotonSolventa(
                    texto = stringResource(R.string.auditoria_detalle_revocar),
                    onClick = {},
                    habilitado = false,
                )
            }
        },
    ) {
        val c = estado.consentimiento
        when {
            estado.cargando -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
            }
            c == null -> {
                Text(text = stringResource(R.string.auditoria_detalle_no_disponible), style = MaterialTheme.typography.bodyLarge)
                BotonSolventa(texto = stringResource(R.string.comun_atras), onClick = onAtras, tipo = TipoBoton.SECUNDARIO)
            }
            else -> {
                Cabecera(c)
                Campos(c, idioma)
                Historial(c, idioma)
            }
        }
    }
}

@Composable
private fun Cabecera(c: Consentimiento) {
    TarjetaSolventa {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Espacio.x16),
        ) {
            IconoDeEntidad(atenuado = c.estado != EstadosDeConsentimiento.ACTIVO)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = c.nombreDeEntidad, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = c.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalColoresSolventa.current.textoSecundario,
                )
            }
            InsigniaDeConsentimiento(c.estado)
        }
    }
}

@Composable
private fun Campos(c: Consentimiento, idioma: Idioma) {
    TarjetaSolventa {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier.padding(vertical = Espacio.x16),
        ) {
            Campo(
                rotulo = stringResource(R.string.auditoria_detalle_otorgado),
                valor = stringResource(
                    R.string.auditoria_detalle_fecha_hora_canal,
                    FormatoDeFecha.fecha(c.otorgadoEn, idioma),
                    FormatoDeFecha.hora(c.otorgadoEn, idioma),
                    textoDe(EtiquetasDeConsentimiento.canal(c.canal), c.canal),
                ),
            )
            val fin = c.finalizadoEn
            when {
                c.estado == EstadosDeConsentimiento.REVOCADO && fin != null -> {
                    Campo(
                        rotulo = stringResource(R.string.auditoria_detalle_revocado_el),
                        valor = stringResource(
                            R.string.auditoria_detalle_fecha_hora,
                            FormatoDeFecha.fecha(fin, idioma),
                            FormatoDeFecha.hora(fin, idioma),
                        ),
                    )
                    c.comprobante?.let {
                        Campo(rotulo = stringResource(R.string.auditoria_detalle_comprobante), valor = it)
                    }
                }
                c.estado == EstadosDeConsentimiento.VENCIDO && fin != null -> Campo(
                    rotulo = stringResource(R.string.auditoria_detalle_vencio_el),
                    valor = FormatoDeFecha.fecha(fin, idioma),
                )
                else -> Campo(
                    rotulo = stringResource(R.string.auditoria_detalle_vence),
                    valor = FormatoDeFecha.fecha(c.venceEn, idioma),
                )
            }
            Campo(
                rotulo = stringResource(R.string.auditoria_detalle_alcance),
                valor = c.alcance.map { fuente ->
                    stringResource(
                        R.string.auditoria_detalle_fuente,
                        textoDe(EtiquetasDeConsentimiento.fuente(fuente.codigo), fuente.codigo),
                        fuente.categorias.map { textoDe(EtiquetasDeConsentimiento.categoria(it), it) }.joinToString(" · "),
                        textoDe(EtiquetasDeConsentimiento.vigencia(fuente.vigencia), fuente.vigencia),
                    )
                }.joinToString("\n"),
            )
            Campo(
                rotulo = stringResource(R.string.auditoria_detalle_finalidad),
                valor = textoDe(EtiquetasDeConsentimiento.finalidad(c.finalidad), c.finalidad),
            )
        }
    }
}

@Composable
private fun Campo(rotulo: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
        Rotulo(rotulo)
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Historial(c: Consentimiento, idioma: Idioma) {
    Rotulo(stringResource(R.string.auditoria_detalle_historial))
    TarjetaSolventa {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier.padding(vertical = Espacio.x16),
        ) {
            c.historial.forEach { cambio -> EntradaDeHistorial(cambio, idioma) }
        }
    }
}

@Composable
private fun EntradaDeHistorial(cambio: CambioDeEstado, idioma: Idioma) {
    val colores = LocalColoresSolventa.current
    val estado = textoDe(EtiquetasDeConsentimiento.cambio(cambio.estado), cambio.estado)
    val cuando = listOfNotNull(
        stringResource(
            R.string.auditoria_detalle_fecha_hora,
            FormatoDeFecha.fecha(cambio.ocurrioEn, idioma),
            FormatoDeFecha.hora(cambio.ocurrioEn, idioma),
        ),
        cambio.canal?.let { textoDe(EtiquetasDeConsentimiento.canal(it), it) },
        cambio.comprobante?.let { stringResource(R.string.auditoria_consentimientos_comprobante, it) },
    ).joinToString(" · ")
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$estado. $cuando" },
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Column {
            Text(text = estado, style = MaterialTheme.typography.titleSmall)
            Text(text = cuando, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
        }
    }
}
