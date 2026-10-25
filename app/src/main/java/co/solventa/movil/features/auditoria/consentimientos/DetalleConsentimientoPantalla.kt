package co.solventa.movil.features.auditoria.consentimientos

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaViewModel
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.CirculoConIcono
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
                Historial(c, estado.pasoFinal, idioma)
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

/**
 * El historial como linea de tiempo (frame 37:1418): un circulo relleno con
 * visto por cada hecho ocurrido, unidos por una linea vertical, y al final,
 * si el estado actual no es ya el ultimo hecho, un paso con circulo de
 * contorno y su numero ([PasoFinalDelHistorial]).
 */
@Composable
private fun Historial(c: Consentimiento, pasoFinal: PasoFinalDelHistorial?, idioma: Idioma) {
    Rotulo(stringResource(R.string.auditoria_detalle_historial))
    TarjetaSolventa {
        Column(modifier = Modifier.padding(vertical = Espacio.x16)) {
            val pasos = c.historial.size + if (pasoFinal != null) 1 else 0
            c.historial.forEachIndexed { indice, cambio ->
                EntradaDeHistorial(cambio, idioma, conLinea = indice < pasos - 1)
            }
            pasoFinal?.let { PasoFinal(it, numero = pasos, idioma = idioma) }
        }
    }
}

@Composable
private fun EntradaDeHistorial(cambio: CambioDeEstado, idioma: Idioma, conLinea: Boolean) {
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
    PasoDeLaLinea(titulo = estado, detalle = cuando, conLinea = conLinea) {
        CirculoConIcono(
            icono = R.drawable.ic_check,
            tinte = MaterialTheme.colorScheme.onPrimary,
            fondo = MaterialTheme.colorScheme.primary,
            descripcion = null,
            diametro = DIAMETRO_DEL_PASO,
            tamanoIcono = 16.dp,
        )
    }
}

@Composable
private fun PasoFinal(paso: PasoFinalDelHistorial, numero: Int, idioma: Idioma) {
    val (titulo, detalle) = when (paso) {
        is PasoFinalDelHistorial.Activo -> stringResource(R.string.auditoria_consentimientos_estado_activo) to
            stringResource(
                if (paso.sincronizado) R.string.auditoria_detalle_paso_vigente_sincronizado else R.string.auditoria_detalle_paso_vigente,
            )
        is PasoFinalDelHistorial.Vencido -> stringResource(R.string.auditoria_consentimientos_estado_vencido) to
            FormatoDeFecha.fecha(paso.vencioEn, idioma)
        is PasoFinalDelHistorial.RevocacionPendiente -> stringResource(R.string.auditoria_revocacion_paso_pendiente) to
            stringResource(
                R.string.auditoria_detalle_fecha_hora,
                FormatoDeFecha.fecha(paso.solicitadaEn, idioma),
                FormatoDeFecha.hora(paso.solicitadaEn, idioma),
            )
        is PasoFinalDelHistorial.RevocadoSinConfirmar ->
            stringResource(R.string.auditoria_revocacion_paso_sin_confirmar, paso.entidades.joinToString(", ")) to
                listOfNotNull(
                    FormatoDeFecha.fecha(paso.efectivaEn, idioma),
                    FormatoDeFecha.horaConSegundos(paso.efectivaEn, idioma),
                    paso.comprobante,
                ).joinToString(" · ")
    }
    PasoDeLaLinea(titulo = titulo, detalle = detalle, conLinea = false) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DIAMETRO_DEL_PASO)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Text(
                text = numero.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Una fila de la linea de tiempo: el circulo en una columna de 28 con la
 * linea de 2 que baja hasta el paso siguiente, y a 12 el estado con su
 * detalle. Se anuncia como una sola unidad para el lector de pantalla.
 */
@Composable
private fun PasoDeLaLinea(titulo: String, detalle: String, conLinea: Boolean, marca: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) { contentDescription = "$titulo. $detalle" },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(DIAMETRO_DEL_PASO)
                .fillMaxHeight(),
        ) {
            marca()
            if (conLinea) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (conLinea) Espacio.x12 else 0.dp),
        ) {
            Text(text = titulo, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(text = detalle, style = MaterialTheme.typography.bodyMedium, color = LocalColoresSolventa.current.textoSecundario)
        }
    }
}

/** El circulo de cada paso del historial mide 28 en el movil (componente «Paso de flujo»). */
private val DIAMETRO_DEL_PASO = 28.dp
