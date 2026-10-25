package co.solventa.movil.features.auditoria.consentimientos

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
 * Consentimiento revocado y comprobante (HU-M1.3-02, frame `38:1414`,
 * criterios 3 y 4). Se abre solo cuando el servicio confirmo la revocacion en
 * linea; una revocacion enviada despues desde la cola se ve en la lista y el
 * detalle. Volver, con el boton o con atras, lleva a la lista.
 */
@Composable
fun ConsentimientoRevocadoPantalla(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    vm: RevocacionViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    // El titular elige donde guardar el PDF; la aplicacion solo escribe los
    // bytes (sin permisos de almacenamiento, plan §4.4).
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TIPO_PDF)) { destino ->
        vm.guardarComprobante(destino?.toString())
    }
    BackHandler(onBack = onVolver)
    ContenidoConsentimientoRevocado(
        estado = estado,
        idioma = idioma,
        onDescargar = { selector.launch(estado.nombreDelComprobante) },
        onVolver = onVolver,
        onCerrarAviso = vm::descartarAviso,
        modifier = modifier,
    )
}

/** La pantalla sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoConsentimientoRevocado(
    estado: RevocacionUiState,
    idioma: Idioma,
    onDescargar: () -> Unit,
    onVolver: () -> Unit,
    onCerrarAviso: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = estado.consentimiento
    val revocacion = c?.revocacion
    PantallaRegistro(
        titulo = stringResource(R.string.auditoria_revocacion_revocado_titulo),
        modifier = modifier,
        acciones = {
            BotonSolventa(
                texto = stringResource(R.string.auditoria_revocacion_descargar),
                onClick = onDescargar,
                icono = painterResource(R.drawable.ic_descargar),
                habilitado = estado.comprobanteDisponible && !estado.descargando,
            )
            BotonSolventa(
                texto = stringResource(R.string.auditoria_revocacion_volver),
                onClick = onVolver,
                tipo = TipoBoton.SECUNDARIO,
            )
        },
    ) {
        if (c == null || revocacion == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
            }
            return@PantallaRegistro
        }
        Cabecera(c, revocacion, idioma)
        Datos(c, revocacion, idioma)
        AvisoSolventa(
            tipo = TipoAviso.INFORMACION,
            titulo = stringResource(R.string.auditoria_revocacion_que_cambia_titulo),
            texto = stringResource(R.string.auditoria_revocacion_que_cambia),
        )
        if (estado.descargando) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
            }
        }
        estado.aviso?.let { AvisoDeLaRevocacionVisible(it, onCerrarAviso) }
    }
}

/** El escudo, el titulo y cuanto tardo en hacerse efectiva. */
@Composable
private fun Cabecera(c: Consentimiento, revocacion: Revocacion, idioma: Idioma) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.x16),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Espacio.x8),
    ) {
        CirculoConIcono(
            icono = R.drawable.ic_escudo_check,
            tinte = MaterialTheme.colorScheme.primary,
            fondo = MaterialTheme.colorScheme.primaryContainer,
            descripcion = stringResource(R.string.auditoria_revocacion_revocado_icono),
            diametro = 64.dp,
            tamanoIcono = 28.dp,
        )
        Text(
            text = stringResource(R.string.auditoria_revocacion_revocado_titulo),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = efectividad(revocacion, c.nombreDeEntidad, idioma),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalColoresSolventa.current.textoSecundario,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * «Efectivo en 2,1 segundos» (`effective_at − requested_at`, con el separador
 * decimal del idioma); si sale negativo porque el reloj del telefono estaba
 * adelantado, «Efectivo desde las <hora>» (plan §4.4).
 */
@Composable
private fun efectividad(revocacion: Revocacion, entidad: String, idioma: Idioma): String {
    val demora = revocacion.demora
    return if (demora != null) {
        stringResource(R.string.auditoria_revocacion_revocado_efectivo_en, segundos(demora, idioma), entidad)
    } else {
        stringResource(
            R.string.auditoria_revocacion_revocado_efectivo_desde,
            FormatoDeFecha.horaConSegundos(revocacion.efectivaEn, idioma),
            entidad,
        )
    }
}

/** Milisegundos como segundos con un decimal, en el formato del idioma. */
internal fun segundos(milisegundos: Long, idioma: Idioma): String =
    String.format(idioma.aLocale(), "%.1f", milisegundos / MILISEGUNDOS_POR_SEGUNDO)

/** La tarjeta del criterio 4: consentimiento y entidad, estado, horas y comprobante. */
@Composable
private fun Datos(c: Consentimiento, revocacion: Revocacion, idioma: Idioma) {
    TarjetaSolventa {
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier.padding(vertical = Espacio.x16),
        ) {
            Dato(stringResource(R.string.auditoria_revocacion_campo_consentimiento)) {
                Valor(stringResource(R.string.auditoria_revocacion_consentimiento_y_entidad, c.id, c.nombreDeEntidad))
            }
            Dato(stringResource(R.string.auditoria_revocacion_campo_estado)) {
                InsigniaDeConsentimiento(c.estado)
            }
            Dato(stringResource(R.string.auditoria_revocacion_campo_solicitado)) {
                Valor(conSegundos(revocacion.solicitadaEn, idioma))
            }
            Dato(stringResource(R.string.auditoria_revocacion_campo_efectivo_desde)) {
                Valor(conSegundos(revocacion.efectivaEn, idioma))
            }
            Dato(stringResource(R.string.auditoria_revocacion_campo_comprobante)) {
                Valor(stringResource(R.string.auditoria_revocacion_comprobante_detalle, revocacion.comprobante))
            }
        }
    }
}

@Composable
private fun conSegundos(instante: Long, idioma: Idioma): String = stringResource(
    R.string.auditoria_revocacion_fecha_hora_segundos,
    FormatoDeFecha.fecha(instante, idioma),
    FormatoDeFecha.horaConSegundos(instante, idioma),
)

@Composable
private fun Dato(rotulo: String, valor: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
        Rotulo(rotulo)
        valor()
    }
}

@Composable
private fun Valor(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.bodyLarge)
}

/** El tipo del comprobante para el selector del sistema. */
internal const val TIPO_PDF = "application/pdf"

private const val MILISEGUNDOS_POR_SEGUNDO = 1000.0
