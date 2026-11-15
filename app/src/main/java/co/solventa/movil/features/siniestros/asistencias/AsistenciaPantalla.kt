package co.solventa.movil.features.siniestros.asistencias

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.FilaEstado
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.SeparadorTarjeta
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.ui.theme.Radio
import kotlinx.coroutines.launch
import java.util.Locale

/** Los dos permisos: con solo la aproximada la ubicación se envía igual (D5). */
private val PERMISOS_DE_UBICACION = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/**
 * M14 · Solicitar asistencia en sitio, con M14b y M14c como estados
 * (HU-M2.3-01). Dibuja y ejecuta lo que decide [AsistenciaViewModel]: el
 * diálogo de permisos, los ajustes y el marcador. Al volver a la vista
 * (`ON_RESUME`) el `ViewModel` revisa otra vez (S2).
 */
@Composable
fun AsistenciaPantalla(
    onAtras: () -> Unit,
    vm: AsistenciaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val portapapeles = LocalClipboard.current
    val alcance = rememberCoroutineScope()

    val lanzadorDePermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { respuesta ->
        val concedido = respuesta.values.any { it }
        // Negado y sin «justificación» que mostrar: el sistema ya no volverá a
        // abrir su diálogo, así que la próxima vez se va a los ajustes.
        val actividad = contexto.actividad()
        val bloqueado = !concedido && actividad != null &&
            PERMISOS_DE_UBICACION.none { ActivityCompat.shouldShowRequestPermissionRationale(actividad, it) }
        vm.permisoRespondido(concedido, bloqueado)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.alVolver() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.alSalir() }

    LaunchedEffect(estado.evento) {
        when (val evento = estado.evento) {
            null -> Unit
            EventoDeAsistencia.PedirPermiso -> {
                vm.eventoAtendido()
                lanzadorDePermiso.launch(PERMISOS_DE_UBICACION)
            }
            EventoDeAsistencia.AbrirAjustesDeUbicacion -> {
                vm.eventoAtendido()
                contexto.abrir(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            EventoDeAsistencia.AbrirAjustesDeLaApp -> {
                vm.eventoAtendido()
                contexto.abrir(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null)),
                )
            }
            is EventoDeAsistencia.AbrirMarcador ->
                if (contexto.abrir(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${evento.numero}")))) {
                    vm.marcadorAbierto()
                } else {
                    vm.marcadorNoDisponible()
                }
        }
    }

    ContenidoAsistencia(
        estado = estado,
        onAtras = onAtras,
        onElegirPoliza = vm::elegirPoliza,
        onSolicitar = vm::solicitar,
        onLlamarSinSolicitud = vm::llamarSinSolicitud,
        onActivarUbicacion = vm::activarUbicacion,
        onLlamarSinUbicacion = vm::llamarSinUbicacion,
        onCancelar = vm::cancelar,
        onVolverALlamar = vm::volverALlamar,
        onVerEstado = vm::verEstado,
        onSolicitarOtra = vm::solicitarOtra,
        onCopiarNumero = {
            alcance.launch { portapapeles.setClipEntry(ClipEntry(ClipData.newPlainText(null, LineaDeAtencion.VISIBLE))) }
        },
    )
}

/** Lo que se ve, con el estado inyectado: lo usan las vistas previas y las pruebas de interfaz. */
@Composable
internal fun ContenidoAsistencia(
    estado: AsistenciaUiState,
    onAtras: () -> Unit,
    onElegirPoliza: (String) -> Unit,
    onSolicitar: () -> Unit,
    onLlamarSinSolicitud: () -> Unit,
    onActivarUbicacion: () -> Unit,
    onLlamarSinUbicacion: () -> Unit,
    onCancelar: () -> Unit,
    onVolverALlamar: () -> Unit,
    onVerEstado: () -> Unit,
    onSolicitarOtra: () -> Unit,
    onCopiarNumero: () -> Unit,
) {
    // El idioma activo, observable: cambiarlo recompone con los formatos nuevos.
    val idioma: Locale = LocalConfiguration.current.locales[0]
    val solicitada = estado.paso == PasoDeAsistencia.Solicitada

    PantallaRegistro(
        titulo = stringResource(R.string.siniestros_asistencia_titulo),
        onAtras = onAtras,
        acciones = {
            when {
                solicitada -> {
                    BotonSolventa(
                        texto = stringResource(R.string.siniestros_asistencia_volver_a_llamar),
                        onClick = onVolverALlamar,
                        icono = painterResource(R.drawable.ic_telefono),
                    )
                    if (estado.puedeVerEstado) {
                        BotonSolventa(
                            texto = stringResource(R.string.siniestros_asistencia_ver_estado),
                            onClick = onVerEstado,
                            tipo = TipoBoton.SECUNDARIO,
                            habilitado = !estado.consultandoEstado,
                        )
                    }
                    BotonSolventa(
                        texto = stringResource(R.string.siniestros_asistencia_otra),
                        onClick = onSolicitarOtra,
                        tipo = TipoBoton.TERCIARIO,
                    )
                }
                estado.polizas == PolizasDeAsistencia.SinPolizas -> BotonSolventa(
                    texto = stringResource(R.string.siniestros_asistencia_llamar_linea),
                    onClick = onLlamarSinSolicitud,
                    icono = painterResource(R.drawable.ic_telefono),
                )
                estado.polizas is PolizasDeAsistencia.Lista -> BotonSolventa(
                    texto = stringResource(R.string.siniestros_asistencia_solicitar),
                    onClick = onSolicitar,
                    icono = painterResource(R.drawable.ic_telefono),
                    habilitado = estado.puedeSolicitar,
                )
                else -> Unit
            }
        },
    ) {
        when {
            solicitada -> Solicitada(estado, idioma)
            estado.paso == PasoDeAsistencia.ObteniendoUbicacion -> Esperando(stringResource(R.string.siniestros_asistencia_obteniendo))
            else -> Inicial(estado, onElegirPoliza)
        }
        if (estado.sinMarcador) SinMarcador(onCopiarNumero)
    }

    (estado.paso as? PasoDeAsistencia.Dialogo)?.let { dialogo ->
        DialogoDeUbicacion(dialogo.tipo, onActivarUbicacion, onLlamarSinUbicacion, onCancelar)
    }
}

@Composable
private fun Inicial(estado: AsistenciaUiState, onElegirPoliza: (String) -> Unit) {
    when (val polizas = estado.polizas) {
        PolizasDeAsistencia.Cargando -> Esperando(stringResource(R.string.siniestros_asistencia_cargando))
        PolizasDeAsistencia.SinPolizas -> AvisoSolventa(
            tipo = TipoAviso.INFORMACION,
            texto = stringResource(R.string.siniestros_asistencia_sin_polizas),
        )
        is PolizasDeAsistencia.Lista -> SelectorDePoliza(polizas, onElegirPoliza)
    }
    TarjetaSolventa(relleno = PaddingValues(Espacio.x16)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_telefono),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.siniestros_asistencia_linea_titulo),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            text = stringResource(R.string.siniestros_asistencia_linea_texto, LineaDeAtencion.VISIBLE),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Espacio.x8),
        )
    }
}

@Composable
private fun SelectorDePoliza(polizas: PolizasDeAsistencia.Lista, onElegirPoliza: (String) -> Unit) {
    Text(
        text = stringResource(
            if (polizas.elegibles.size > 1) R.string.siniestros_asistencia_poliza_elegir else R.string.siniestros_asistencia_poliza_titulo,
        ),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    TarjetaSolventa(modifier = Modifier.selectableGroup()) {
        polizas.elegibles.forEachIndexed { indice, poliza ->
            if (indice > 0) SeparadorTarjeta()
            val elegida = poliza.id == polizas.elegida?.id
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = elegida, onClick = { onElegirPoliza(poliza.id) }, role = Role.RadioButton)
                    .padding(vertical = Espacio.x8),
            ) {
                RadioButton(selected = elegida, onClick = null)
                Text(text = poliza.etiqueta, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Esperando(texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Espacio.x24)
            .clearAndSetSemantics { contentDescription = texto },
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Solicitada(estado: AsistenciaUiState, idioma: Locale) {
    val solicitud = estado.solicitud
    val registradaEn = solicitud?.registradaEn
    val colores = LocalColoresSolventa.current

    when (estado.encabezado) {
        EncabezadoDeAsistencia.ENVIADA -> AvisoSolventa(
            tipo = TipoAviso.EXITO,
            texto = stringResource(
                R.string.siniestros_asistencia_enviada,
                registradaEn?.let { PresentacionDeAsistencia.horaConSegundos(it, idioma) }.orEmpty(),
            ),
        )
        EncabezadoDeAsistencia.REGISTRADA -> AvisoSolventa(
            tipo = TipoAviso.EXITO,
            texto = stringResource(
                R.string.siniestros_asistencia_registrada,
                registradaEn?.let { PresentacionDeAsistencia.horaConSegundos(it, idioma) }.orEmpty(),
            ),
        )
        EncabezadoDeAsistencia.ENVIANDO -> Esperando(stringResource(R.string.siniestros_asistencia_enviando))
        EncabezadoDeAsistencia.SIN_CONEXION -> AvisoSolventa(
            tipo = TipoAviso.INFORMACION,
            texto = stringResource(R.string.siniestros_asistencia_sin_conexion),
        )
        EncabezadoDeAsistencia.NO_REGISTRADA -> AvisoSolventa(
            tipo = TipoAviso.ERROR,
            titulo = stringResource(R.string.siniestros_asistencia_no_registrada_titulo),
            texto = stringResource(TextosDeAsistencia.descarte(estado.motivoDeDescarte ?: MotivoDeDescarte.RECHAZADA)),
        )
        null -> Unit
    }

    // M14c: el aviso amarillo, con la llamada ya en curso.
    estado.motivoSinUbicacion?.let { motivo ->
        AvisoSolventa(
            tipo = TipoAviso.ADVERTENCIA,
            titulo = stringResource(R.string.siniestros_asistencia_sin_ubicacion_titulo),
            texto = stringResource(
                TextosDeAsistencia.sinUbicacion(motivo),
            ),
        )
    }

    if (solicitud != null) DatosEnviados(solicitud, idioma)

    if (estado.pasosDelEstado.isNotEmpty()) {
        Text(
            text = stringResource(R.string.siniestros_asistencia_estado_titulo),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        TarjetaSolventa {
            estado.pasosDelEstado.forEachIndexed { indice, paso ->
                if (indice > 0) SeparadorTarjeta()
                val (icono, tinte) = when (paso) {
                    PasoDelEstado.UBICACION_RECIBIDA, PasoDelEstado.SOLICITUD_RECIBIDA, PasoDelEstado.DESPACHADA,
                    PasoDelEstado.CERRADA,
                    -> R.drawable.ic_circulo_check to colores.exitoFuerte
                    PasoDelEstado.LLAMADA_EN_CURSO, PasoDelEstado.EN_ATENCION -> R.drawable.ic_telefono to colores.informacionFuerte
                    PasoDelEstado.ENVIANDO, PasoDelEstado.POR_DESPACHAR, PasoDelEstado.DESCONOCIDO ->
                        R.drawable.ic_reloj to colores.textoSecundario
                    PasoDelEstado.SIN_CONEXION -> R.drawable.ic_sin_conexion to colores.advertenciaFuerte
                    PasoDelEstado.NO_REGISTRADA -> R.drawable.ic_circulo_x to MaterialTheme.colorScheme.error
                }
                FilaEstado(icono = painterResource(icono), tinte = tinte, texto = stringResource(TextosDeAsistencia.estado(paso)))
            }
        }
    }
    if (estado.consultandoEstado) Esperando(stringResource(R.string.siniestros_asistencia_consultando))
    if (estado.errorAlConsultar) {
        AvisoSolventa(tipo = TipoAviso.ADVERTENCIA, texto = stringResource(R.string.siniestros_asistencia_estado_error))
    }
}

@Composable
private fun DatosEnviados(solicitud: SolicitudDeAsistencia, idioma: Locale) {
    Text(
        text = stringResource(R.string.siniestros_asistencia_datos_titulo),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    TarjetaSolventa {
        Dato(stringResource(R.string.siniestros_asistencia_dato_poliza), solicitud.etiquetaDePoliza)
        SeparadorTarjeta()
        when (val resultado = solicitud.resultado) {
            is ResultadoDeUbicacion.Compartida -> {
                val coordenadas = PresentacionDeAsistencia.coordenadas(resultado.ubicacion, idioma)
                Dato(
                    stringResource(R.string.siniestros_asistencia_dato_ubicacion),
                    stringResource(
                        R.string.siniestros_asistencia_dato_coordenadas,
                        coordenadas.latitud,
                        stringResource(TextosDeAsistencia.hemisferio(coordenadas.hemisferioDeLatitud)),
                        coordenadas.longitud,
                        stringResource(TextosDeAsistencia.hemisferio(coordenadas.hemisferioDeLongitud)),
                    ),
                )
                SeparadorTarjeta()
                Dato(
                    stringResource(R.string.siniestros_asistencia_dato_precision),
                    stringResource(
                        R.string.siniestros_asistencia_dato_precision_valor,
                        PresentacionDeAsistencia.precision(resultado.ubicacion, idioma),
                    ),
                )
            }
            is ResultadoDeUbicacion.NoDisponible -> Dato(
                stringResource(R.string.siniestros_asistencia_dato_ubicacion),
                stringResource(R.string.siniestros_asistencia_dato_no_disponible),
            )
        }
        solicitud.lugar?.let {
            SeparadorTarjeta()
            Dato(stringResource(R.string.siniestros_asistencia_dato_lugar), it)
        }
        SeparadorTarjeta()
        Dato(
            stringResource(R.string.siniestros_asistencia_dato_hora),
            solicitud.registradaEn?.let { instante ->
                val (local, utc) = PresentacionDeAsistencia.horaLocalYUtc(instante, idioma)
                stringResource(R.string.siniestros_asistencia_dato_hora_valor, local, utc)
            } ?: stringResource(R.string.siniestros_asistencia_dato_hora_pendiente),
        )
        solicitud.numero?.let {
            SeparadorTarjeta()
            Dato(stringResource(R.string.siniestros_asistencia_dato_numero), it)
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Espacio.x12)
            .clearAndSetSemantics { contentDescription = "$etiqueta: $valor" },
    ) {
        Text(text = etiqueta, style = MaterialTheme.typography.bodySmall, color = LocalColoresSolventa.current.textoSecundario)
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}

/** S4: sin marcador, el número se puede leer, seleccionar y copiar. */
@Composable
private fun SinMarcador(onCopiarNumero: () -> Unit) {
    AvisoSolventa(tipo = TipoAviso.INFORMACION, texto = stringResource(R.string.siniestros_asistencia_sin_marcador))
    SelectionContainer {
        Text(text = LineaDeAtencion.VISIBLE, style = MaterialTheme.typography.headlineSmall)
    }
    BotonSolventa(
        texto = stringResource(R.string.siniestros_asistencia_copiar),
        onClick = onCopiarNumero,
        tipo = TipoBoton.SECUNDARIO,
    )
}

/** M14b: por qué se necesita el GPS y las tres salidas (criterios 1 y 4). */
@Composable
private fun DialogoDeUbicacion(
    tipo: TipoDeDialogo,
    onActivarUbicacion: () -> Unit,
    onLlamarSinUbicacion: () -> Unit,
    onCancelar: () -> Unit,
) {
    Dialog(onDismissRequest = onCancelar) {
        Surface(shape = RoundedCornerShape(Radio.tarjeta), color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.x12), modifier = Modifier.padding(Espacio.x24)) {
                Icon(
                    painter = painterResource(R.drawable.ic_pin_mapa),
                    contentDescription = stringResource(R.string.siniestros_asistencia_icono_alerta),
                    tint = LocalColoresSolventa.current.advertenciaFuerte,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(
                        TextosDeAsistencia.dialogo(tipo),
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(text = stringResource(R.string.siniestros_asistencia_dialogo_texto), style = MaterialTheme.typography.bodyMedium)
                BotonSolventa(
                    texto = stringResource(R.string.siniestros_asistencia_dialogo_activar),
                    onClick = onActivarUbicacion,
                    modifier = Modifier.padding(top = Espacio.x8),
                )
                BotonSolventa(
                    texto = stringResource(R.string.siniestros_asistencia_dialogo_sin_ubicacion),
                    onClick = onLlamarSinUbicacion,
                    tipo = TipoBoton.SECUNDARIO,
                    icono = painterResource(R.drawable.ic_telefono),
                )
                BotonSolventa(
                    texto = stringResource(R.string.siniestros_asistencia_dialogo_cancelar),
                    onClick = onCancelar,
                    tipo = TipoBoton.TERCIARIO,
                )
            }
        }
    }
}

/** Abre otra pantalla del sistema; `false` si no hay quien la atienda (S4). */
private fun Context.abrir(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}

private fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}
