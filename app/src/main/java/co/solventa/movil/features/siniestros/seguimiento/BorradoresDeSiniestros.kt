package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.recordarIdiomaDeLaPantalla
import co.solventa.movil.features.siniestros.radicacion.BorradorResumen
import co.solventa.movil.features.siniestros.radicacion.EnvioDelBorrador
import co.solventa.movil.features.siniestros.radicacion.Etapa
import co.solventa.movil.features.siniestros.radicacion.etiquetaDe
import co.solventa.movil.features.siniestros.radicacion.tituloDe
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BarraProgreso
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.InsigniaEstado
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.shared.ui.TipoInsignia
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M13c: el reporte guardado sin conexión se envió solo y ya tiene radicado.
 * M13d: se agotaron los reintentos automáticos; el borrador sigue en el
 * teléfono y *Reintentar ahora* no duplica el radicado (HU-M2.2-02).
 */
@Composable
internal fun AvisosDeBorradores(
    estado: BorradoresUiState,
    idioma: Idioma,
    onReintentar: () -> Unit,
    onCerrarRadicado: () -> Unit,
) {
    val radicado = estado.radicado
    if (radicado?.radicado != null) {
        val en = radicado.radicadoEn ?: radicado.actualizadoEn
        AvisoSolventa(
            tipo = TipoAviso.EXITO,
            titulo = stringResource(R.string.siniestros_borradores_registrado_titulo),
            texto = stringResource(
                R.string.siniestros_borradores_registrado,
                "${FormatoDeFecha.fecha(en, idioma)} ${FormatoDeFecha.hora(en, idioma)}",
                radicado.radicado,
            ),
        )
        BotonSolventa(
            texto = stringResource(R.string.comun_entendido),
            onClick = onCerrarRadicado,
            tipo = TipoBoton.TERCIARIO,
        )
    }
    val fallido = estado.fallidos.firstOrNull()
    if (fallido != null) {
        AvisoSolventa(
            tipo = TipoAviso.ERROR,
            titulo = stringResource(R.string.siniestros_borradores_fallido_titulo),
            texto = pluralStringResource(R.plurals.siniestros_borradores_fallido, fallido.intentos, fallido.intentos, fallido.fotos),
        )
        BotonSolventa(
            texto = stringResource(R.string.siniestros_seguimiento_reintentar_ahora),
            onClick = onReintentar,
            tipo = TipoBoton.SECUNDARIO,
            icono = painterResource(R.drawable.ic_recargar),
        )
    }
}

/** «BORRADORES (n)» con una tarjeta por borrador (M13b, M13d). */
@Composable
internal fun SeccionDeBorradores(
    borradores: List<BorradorResumen>,
    idioma: Idioma,
    onAbrir: (String) -> Unit,
) {
    if (borradores.isEmpty()) return
    RotuloDeSeccion(stringResource(R.string.siniestros_borradores_seccion, borradores.size))
    borradores.forEach { TarjetaDeBorrador(it, idioma, onAbrir) }
}

@Composable
private fun TarjetaDeBorrador(b: BorradorResumen, idioma: Idioma, onAbrir: (String) -> Unit) {
    val colores = LocalColoresSolventa.current
    val titulo = b.tipo?.let { stringResource(tituloDe(it)) } ?: stringResource(R.string.siniestros_borradores_sin_tipo)
    TarjetaSolventa(
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.siniestros_borradores_abrir)) { onAbrir(b.id) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.x12), modifier = Modifier.padding(vertical = Espacio.x16)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.Top) {
                IconoDeSiniestro(null)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                    Text(
                        text = stringResource(R.string.siniestros_borradores_titulo, titulo),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = subtituloDe(b, idioma),
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { InsigniaDeBorrador(b.envio) }
                Text(
                    text = capturadoDe(b),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.textoSecundario,
                )
            }
            BarraProgreso(
                avance = (b.etapa.ordinal + 1f) / Etapa.entries.size,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun subtituloDe(b: BorradorResumen, idioma: Idioma): String {
    val ultimo = b.ultimoIntentoEn
    return if (b.envio == EnvioDelBorrador.FALLIDO && ultimo != null) {
        pluralStringResource(
            R.plurals.siniestros_borradores_intentos_fallidos,
            b.intentos,
            momento(b.actualizadoEn, idioma),
            b.intentos,
            "${FormatoDeFecha.fecha(ultimo, idioma)} ${FormatoDeFecha.hora(ultimo, idioma)}",
        )
    } else {
        stringResource(
            R.string.siniestros_borradores_guardado,
            momento(b.actualizadoEn, idioma),
            b.etapa.ordinal + 1,
            Etapa.entries.size,
        )
    }
}

private fun momento(instante: Long, idioma: Idioma) =
    "${FormatoDeFecha.fecha(instante, idioma)} ${FormatoDeFecha.hora(instante, idioma)}"

@Composable
private fun capturadoDe(b: BorradorResumen): String {
    val fotos = pluralStringResource(R.plurals.siniestros_borradores_fotos, b.fotos, b.fotos)
    return if (b.videos > 0) {
        fotos + " · " + pluralStringResource(R.plurals.siniestros_borradores_videos, b.videos, b.videos)
    } else {
        fotos
    }
}

@Composable
private fun InsigniaDeBorrador(envio: EnvioDelBorrador) {
    val (texto, tipo) = when (envio) {
        EnvioDelBorrador.EDICION, EnvioDelBorrador.EN_COLA ->
            R.string.siniestros_borradores_sin_enviar to TipoInsignia.ADVERTENCIA
        EnvioDelBorrador.ENVIANDO -> R.string.siniestros_borradores_enviando to TipoInsignia.INFORMACION
        EnvioDelBorrador.FALLIDO -> R.string.siniestros_borradores_envio_fallido to TipoInsignia.ERROR
        EnvioDelBorrador.RADICADO -> R.string.siniestros_borradores_radicado to TipoInsignia.EXITO
    }
    InsigniaEstado(texto = stringResource(texto), tipo = tipo)
}

/** «Sincronizado en segundo plano · N intentos con espera creciente · sin datos perdidos» (M13c). */
@Composable
internal fun PieDeSincronizacion(radicado: BorradorResumen?) {
    if (radicado?.radicado == null) return
    val intentos = radicado.intentos + 1
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x8), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_recargar),
            contentDescription = null,
            tint = LocalColoresSolventa.current.exitoFuerte,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = pluralStringResource(R.plurals.siniestros_borradores_sincronizado, intentos, intentos),
            style = MaterialTheme.typography.bodySmall,
            color = LocalColoresSolventa.current.textoSecundario,
        )
    }
}

/**
 * «Tienes un reporte sin terminar» (M13b): continuar exactamente donde quedó
 * o descartarlo. No se cierra tocando fuera sin decidir: cerrar es dejarlo
 * para después.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HojaDeBorradorGuardado(
    borrador: BorradorResumen,
    idioma: Idioma,
    onContinuar: () -> Unit,
    onDescartar: () -> Unit,
    onCerrar: () -> Unit,
) {
    val pantalla = recordarIdiomaDeLaPantalla()
    val tipo = borrador.tipo?.let { stringResource(tituloDe(it)) }
    val etapa = etiquetaDe(borrador.etapa).lowercase()
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        pantalla.Aplicar {
            Column(
                verticalArrangement = Arrangement.spacedBy(Espacio.x16),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Espacio.x16, vertical = Espacio.x8),
            ) {
                Text(
                    text = stringResource(R.string.radicacion_borrador_titulo),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.siniestros_borradores_hoja_detalle,
                        listOfNotNull(tipo, borrador.poliza.ifBlank { null }).joinToString(" · "),
                        FormatoDeFecha.fecha(borrador.actualizadoEn, idioma),
                        FormatoDeFecha.hora(borrador.actualizadoEn, idioma),
                        borrador.etapa.ordinal + 1,
                        Etapa.entries.size,
                        etapa,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                BotonSolventa(
                    texto = stringResource(R.string.radicacion_borrador_continuar),
                    onClick = onContinuar,
                    icono = painterResource(R.drawable.ic_flecha_derecha),
                )
                BotonSolventa(
                    texto = stringResource(R.string.radicacion_borrador_descartar),
                    onClick = onDescartar,
                    tipo = TipoBoton.SECUNDARIO,
                )
            }
        }
    }
}
