package co.solventa.movil.features.auditoria.consentimientos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.CirculoConIcono
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M8b · Detalle del consentimiento (HU-M1.3-01, frame 37:1418). Muestra todo lo
 * que pide el criterio 2 y no permite editar nada (criterio 4).
 *
 * HU-M1.3-02: «Revocar consentimiento» abre la hoja de confirmacion
 * (`37:1519`, con la huella o la contraseña); confirmada y registrada, se
 * abre la pantalla de revocado (`38:1414`). Con la revocacion en cola, el
 * detalle la avisa y ofrece «Reintentar envio ahora»; en una
 * revocacion parcial, el aviso de la entidad que falta y el comprobante.
 */
@Composable
fun DetalleConsentimientoPantalla(
    onAtras: () -> Unit,
    onRevocado: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: DetalleConsentimientoViewModel = hiltViewModel(),
    revocacionVm: RevocacionViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val revocacion by revocacionVm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TIPO_PDF)) { destino ->
        revocacionVm.guardarComprobante(destino?.toString())
    }

    // Registrada en linea: se abre la pantalla de revocado una sola vez.
    val paso = revocacion.paso
    LaunchedEffect(paso) {
        if (paso is PasoDeRevocacion.Revocado) {
            estado.consentimiento?.id?.let(onRevocado)
            revocacionVm.alMostrarRevocado()
        }
    }

    ContenidoDetalleConsentimiento(
        estado = estado,
        idioma = idioma,
        onAtras = onAtras,
        modifier = modifier,
        revocacion = revocacion,
        onRevocar = revocacionVm::abrirHoja,
        onReintentar = revocacionVm::reintentarAhora,
        onDescargarComprobante = { selector.launch(revocacion.nombreDelComprobante) },
        onCerrarAviso = revocacionVm::descartarAviso,
    )

    val hoja = paso as? PasoDeRevocacion.Hoja
    val consentimiento = estado.consentimiento
    if (hoja != null && consentimiento != null) {
        HojaDeRevocacion(
            entidad = consentimiento.nombreDeEntidad,
            hoja = hoja,
            onConfirmarConBiometria = revocacionVm::confirmarConBiometria,
            onConfirmarConContrasena = revocacionVm::confirmarConContrasena,
            onCancelar = revocacionVm::cancelar,
        )
    }
}

/** El detalle sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoDetalleConsentimiento(
    estado: DetalleConsentimientoUiState,
    idioma: Idioma,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    revocacion: RevocacionUiState = RevocacionUiState(),
    onRevocar: () -> Unit = {},
    onReintentar: () -> Unit = {},
    onDescargarComprobante: () -> Unit = {},
    onCerrarAviso: () -> Unit = {},
) {
    PantallaRegistro(
        titulo = stringResource(R.string.auditoria_detalle_titulo),
        onAtras = onAtras,
        modifier = modifier,
        acciones = {
            AccionesDelDetalle(estado, revocacion, onRevocar, onReintentar)
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
                AvisosDeLaRevocacion(estado, revocacion, c, idioma, onCerrarAviso)
                Cabecera(c, pendiente = estado.pendiente != null)
                Campos(c, idioma)
                Historial(estado.historialVisible, estado.pasoFinal, idioma)
                if (c.revocacion != null) {
                    EnlaceDeComprobante(
                        texto = stringResource(R.string.auditoria_revocacion_descargar_detalle),
                        descargando = revocacion.descargando,
                        onClick = onDescargarComprobante,
                    )
                }
            }
        }
    }
}

/**
 * Los botones de abajo (criterio 6): «Revocar consentimiento» en rojo solo en
 * un activo revocable sin revocacion en cola; con la revocacion en cola,
 * «Reintentar envio ahora», deshabilitado sin señal; en
 * revocados, vencidos y parciales, ninguno.
 */
@Composable
private fun AccionesDelDetalle(
    estado: DetalleConsentimientoUiState,
    revocacion: RevocacionUiState,
    onRevocar: () -> Unit,
    onReintentar: () -> Unit,
) {
    val enviando = revocacion.paso == PasoDeRevocacion.Enviando
    when {
        estado.pendiente != null -> BotonSolventa(
            texto = stringResource(R.string.auditoria_revocacion_reintentar),
            onClick = onReintentar,
            tipo = TipoBoton.SECUNDARIO,
            icono = painterResource(R.drawable.ic_sincronizar),
            habilitado = revocacion.reintentarHabilitado,
        )
        estado.revocarVisible -> {
            if (enviando) {
                val texto = stringResource(R.string.auditoria_revocacion_enviando)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Espacio.x8, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) { contentDescription = texto },
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(text = texto, style = MaterialTheme.typography.bodyMedium)
                }
            }
            BotonDeRevocar(
                texto = stringResource(R.string.auditoria_detalle_revocar),
                onClick = onRevocar,
                habilitado = !enviando,
            )
        }
    }
}

/**
 * Los avisos de arriba: la revocacion en cola, la revocacion parcial, la
 * que el servicio ya no acepto y el resultado de
 * revocar o de descargar el comprobante.
 */
@Composable
private fun AvisosDeLaRevocacion(
    estado: DetalleConsentimientoUiState,
    revocacion: RevocacionUiState,
    c: Consentimiento,
    idioma: Idioma,
    onCerrarAviso: () -> Unit,
) {
    estado.pendiente?.let { AvisoDeRevocacionEnCola(it, idioma) }
    if (estado.parcial) {
        val efectiva = c.revocacion?.efectivaEn ?: c.finalizadoEn ?: c.otorgadoEn
        val faltan = c.revocacion?.sinConfirmar?.map { it.nombre }.orEmpty().ifEmpty { listOf(c.nombreDeEntidad) }
        AvisoSolventa(
            tipo = TipoAviso.ADVERTENCIA,
            titulo = stringResource(R.string.auditoria_revocacion_parcial_titulo, faltan.joinToString(", ")),
            texto = stringResource(
                R.string.auditoria_revocacion_parcial,
                FormatoDeFecha.fecha(efectiva, idioma),
                FormatoDeFecha.horaConSegundos(efectiva, idioma),
            ),
        )
    }
    revocacion.descarte?.let { AvisoDeDescarte(it, onCerrarAviso) }
    revocacion.aviso?.let { AvisoDeLaRevocacionVisible(it, onCerrarAviso) }
}

/** «Sin conexion: revocacion en cola», con la entidad y la hora de la solicitud (`38:1503` y el detalle). */
@Composable
internal fun AvisoDeRevocacionEnCola(pendiente: RevocacionPendiente, idioma: Idioma, modifier: Modifier = Modifier) {
    AvisoSolventa(
        tipo = TipoAviso.ADVERTENCIA,
        titulo = stringResource(R.string.auditoria_revocacion_en_cola_titulo),
        texto = stringResource(
            R.string.auditoria_revocacion_en_cola,
            pendiente.entidad,
            FormatoDeFecha.fecha(pendiente.solicitadaEn, idioma),
            FormatoDeFecha.hora(pendiente.solicitadaEn, idioma),
        ),
        modifier = modifier,
    )
}

/**
 * La cabecera con la entidad y su estado. En un activo, vencido o revocado la
 * insignia va a la derecha (37:1418); con la revocacion en cola o parcial va
 * bajo el identificador, porque su texto es largo.
 */
@Composable
private fun Cabecera(c: Consentimiento, pendiente: Boolean) {
    val bajoElNombre = pendiente || c.estado == EstadosDeConsentimiento.REVOCADO_PARCIAL
    TarjetaSolventa {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Espacio.x16),
        ) {
            IconoDeEntidad(atenuado = !entidadResaltada(c.estado))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = c.nombreDeEntidad, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = c.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalColoresSolventa.current.textoSecundario,
                )
                if (bajoElNombre) {
                    if (pendiente) InsigniaDePendienteDeEnvio() else InsigniaDeConsentimiento(c.estado)
                }
            }
            if (!bajoElNombre) {
                InsigniaDeConsentimiento(c.estado)
            }
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
private fun Historial(historial: List<CambioDeEstado>, pasoFinal: PasoFinalDelHistorial?, idioma: Idioma) {
    Rotulo(stringResource(R.string.auditoria_detalle_historial))
    TarjetaSolventa {
        Column(modifier = Modifier.padding(vertical = Espacio.x16)) {
            val pasos = historial.size + if (pasoFinal != null) 1 else 0
            historial.forEachIndexed { indice, cambio ->
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
