package co.solventa.movil.features.acceso.registro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.solventa.movil.R
import co.solventa.movil.core.i18n.recordarIdiomaDeLaPantalla
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BarraProgreso
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.ListaConsejos
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.shared.ui.camara.CamaraDeDocumento
import co.solventa.movil.shared.ui.camara.EstadoDeLaCamara
import co.solventa.movil.shared.ui.camara.VistaPreviaDeCamara
import co.solventa.movil.shared.ui.camara.recordarCamaraDeDocumento
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import kotlinx.coroutines.launch

/**
 * M1a y M1b · captura del frente y del reverso del documento.
 *
 * Son la misma pantalla con distinto texto, igual que en el prototipo. El
 * estado M1f —calidad insuficiente— se resuelve aquí dentro con una hoja
 * inferior, porque es un estado de esta pantalla y no un destino aparte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturaDocumentoPantalla(
    cara: CaraDocumento,
    estado: RegistroUiState,
    onCapturar: (rutaArchivo: String) -> Unit,
    onLuzInsuficiente: () -> Unit,
    onDescartarAviso: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    camara: CamaraDeDocumento = recordarCamaraDeDocumento(),
) {
    val colores = LocalColoresSolventa.current
    val esFrente = cara == CaraDocumento.FRENTE
    val alcance = rememberCoroutineScope()
    var tomandoFoto by remember { mutableStateOf(false) }
    var falloLaFoto by remember { mutableStateOf(false) }

    PantallaRegistro(
        titulo = stringResource(R.string.registro_captura_titulo),
        onAtras = onAtras,
        modifier = modifier,
        acciones = {
            BotonSolventa(
                texto = stringResource(R.string.registro_captura_accion),
                onClick = {
                    // Criterio 4: con poca luz no se toma la foto, se avisa.
                    // La medida viene del propio sensor, no de un guion.
                    if (camara.luzInsuficiente) {
                        onLuzInsuficiente()
                        return@BotonSolventa
                    }
                    val controlador = camara.controlador ?: return@BotonSolventa
                    if (tomandoFoto) return@BotonSolventa
                    tomandoFoto = true
                    falloLaFoto = false
                    alcance.launch {
                        runCatching { controlador.capturar(cara.name.lowercase()) }
                            .onSuccess(onCapturar)
                            .onFailure { falloLaFoto = true }
                        tomandoFoto = false
                    }
                },
                icono = painterResource(R.drawable.ic_camara),
                habilitado = !estado.analizando &&
                    !tomandoFoto &&
                    camara.estado == EstadoDeLaCamara.LISTA,
            )
            BotonSolventa(
                texto = stringResource(R.string.registro_captura_problemas),
                onClick = onAtras,
                tipo = TipoBoton.TERCIARIO,
            )
        },
    ) {
        PasoDelRegistro(
            avance = 1f / 3f,
            etiqueta = stringResource(R.string.registro_captura_paso),
        )

        Text(
            text = stringResource(
                if (esFrente) {
                    R.string.registro_captura_frente_instruccion
                } else {
                    R.string.registro_captura_reverso_instruccion
                },
            ),
            style = MaterialTheme.typography.titleLarge,
        )

        MarcoDocumento(
            etiqueta = stringResource(
                if (esFrente) {
                    R.string.registro_captura_frente_etiqueta
                } else {
                    R.string.registro_captura_reverso_etiqueta
                },
            ),
            camara = camara,
        )

        if (camara.estado == EstadoDeLaCamara.LISTA && camara.luzInsuficiente) {
            AvisoSolventa(
                texto = stringResource(R.string.registro_camara_poca_luz),
                tipo = TipoAviso.ADVERTENCIA,
            )
        }

        if (falloLaFoto) {
            AvisoSolventa(
                texto = stringResource(R.string.registro_camara_error),
                tipo = TipoAviso.ERROR,
            )
        }

        if (estado.lecturaFallida && cara == CaraDocumento.REVERSO) {
            AvisoSolventa(
                texto = stringResource(R.string.registro_lectura_fallida),
                tipo = TipoAviso.ERROR,
            )
        }

        EstadoDeLaCamaraAviso(camara = camara)

        Text(
            text = stringResource(
                if (esFrente) {
                    R.string.registro_captura_frente_ayuda
                } else {
                    R.string.registro_captura_reverso_ayuda
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.textoSecundario,
        )

        ListaConsejos(
            consejos = listOf(
                stringResource(R.string.registro_captura_consejo_luz),
                stringResource(R.string.registro_captura_consejo_reflejos),
                stringResource(R.string.registro_captura_consejo_nitidez),
            ),
            icono = painterResource(R.drawable.ic_check),
            tinte = colores.exitoFuerte,
        )

        Text(
            text = stringResource(R.string.registro_captura_privacidad),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )
    }

    val problema = estado.problemaCalidad
    // La hoja tiene su propia ventana: sin esto, M1f sale en el idioma del
    // teléfono y no en el que el usuario eligió en la aplicación.
    val idioma = recordarIdiomaDeLaPantalla()
    if (problema != null) {
        ModalBottomSheet(onDismissRequest = onDescartarAviso) {
            idioma.Aplicar {
                HojaCalidadInsuficiente(problema = problema, onReintentar = onDescartarAviso)
            }
        }
    }
}

/** M1f · la hoja que explica por qué la imagen no sirve y cómo corregirlo. */
@Composable
private fun HojaCalidadInsuficiente(
    problema: ProblemaCalidad,
    onReintentar: () -> Unit,
) {
    val colores = LocalColoresSolventa.current
    val (tituloRes, detalleRes) = when (problema) {
        ProblemaCalidad.ILUMINACION_INSUFICIENTE ->
            R.string.registro_calidad_oscura_titulo to R.string.registro_calidad_oscura_detalle

        ProblemaCalidad.IMAGEN_BORROSA ->
            R.string.registro_calidad_borrosa_titulo to R.string.registro_calidad_borrosa_detalle

        ProblemaCalidad.DOCUMENTO_INCOMPLETO ->
            R.string.registro_calidad_incompleta_titulo to R.string.registro_calidad_incompleta_detalle
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x12),
        modifier = Modifier.padding(
            start = Espacio.x16,
            end = Espacio.x16,
            bottom = Espacio.x32,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_triangulo_alerta),
                contentDescription = null,
                tint = colores.advertenciaFuerte,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(tituloRes),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = stringResource(detalleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.textoSecundario,
        )
        BotonSolventa(
            texto = stringResource(R.string.registro_calidad_reintentar),
            onClick = onReintentar,
        )
    }
}

/**
 * Marco guía donde se ubica el documento. La proporción es la de una cédula
 * colombiana.
 *
 * Dentro va la vista previa de la cámara cuando se puede. Cuando no —sin
 * permiso, sin cámara o todavía preguntando— queda el marcador con la
 * etiqueta, que es lo que había antes de CameraX: la pantalla sigue siendo
 * navegable y las pruebas de interfaz siguen pudiendo leerla.
 */
@Composable
private fun MarcoDocumento(
    etiqueta: String,
    camara: CamaraDeDocumento,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val forma = RoundedCornerShape(co.solventa.movil.ui.theme.Radio.tarjeta)
    val enVivo = camara.estado == EstadoDeLaCamara.LISTA
    val descripcion = stringResource(
        if (enVivo) {
            R.string.registro_camara_vista_descripcion
        } else {
            R.string.registro_captura_marco_descripcion
        },
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(forma)
            .background(MaterialTheme.colorScheme.secondaryContainer, forma)
            .border(2.dp, MaterialTheme.colorScheme.secondary, forma)
            .clearAndSetSemantics { contentDescription = descripcion },
    ) {
        if (enVivo) {
            VistaPreviaDeCamara(camara = camara, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = colores.textoSecundario,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Lo que se le dice al usuario cuando la cámara no está disponible, con la
 * salida que corresponda a cada caso.
 *
 * Negar el permiso no es lo mismo que no haberlo pedido: Android no deja
 * volver a preguntar desde la aplicación una vez negado, así que ahí la única
 * salida honesta es mandarlo a los ajustes en lugar de un botón que no haría
 * nada.
 */
@Composable
private fun EstadoDeLaCamaraAviso(camara: CamaraDeDocumento) {
    val contexto = LocalContext.current
    when (camara.estado) {
        EstadoDeLaCamara.LISTA -> Unit

        EstadoDeLaCamara.CONSULTANDO -> AvisoSolventa(
            tipo = TipoAviso.INFORMACION,
            texto = stringResource(R.string.registro_camara_preparando),
        )

        EstadoDeLaCamara.SIN_CAMARA -> AvisoSolventa(
            tipo = TipoAviso.ADVERTENCIA,
            texto = stringResource(R.string.registro_camara_sin_camara),
        )

        EstadoDeLaCamara.SIN_PERMISO -> {
            AvisoSolventa(
                tipo = TipoAviso.INFORMACION,
                titulo = stringResource(R.string.registro_camara_permiso_titulo),
                texto = stringResource(R.string.registro_camara_permiso_detalle),
            )
            BotonSolventa(
                texto = stringResource(R.string.registro_camara_permiso_accion),
                onClick = camara.pedirPermiso,
                tipo = TipoBoton.SECUNDARIO,
            )
        }

        EstadoDeLaCamara.PERMISO_NEGADO -> {
            AvisoSolventa(
                tipo = TipoAviso.ADVERTENCIA,
                titulo = stringResource(R.string.registro_camara_negado_titulo),
                texto = stringResource(R.string.registro_camara_negado_detalle),
            )
            BotonSolventa(
                texto = stringResource(R.string.registro_camara_negado_accion),
                onClick = { contexto.abrirAjustesDeLaAplicacion() },
                tipo = TipoBoton.SECUNDARIO,
            )
        }
    }
}

/** Lleva a la ficha de la aplicación en los ajustes del sistema. */
private fun Context.abrirAjustesDeLaAplicacion() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Barra de avance con la etiqueta del paso, común a las ocho pantallas. */
@Composable
internal fun PasoDelRegistro(
    avance: Float,
    etiqueta: String,
    color: Color = MaterialTheme.colorScheme.secondary,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x8),
        modifier = modifier.fillMaxWidth(),
    ) {
        BarraProgreso(avance = avance, color = color)
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )
    }
}

/**
 * M1c · confirmación de los datos personales.
 *
 * **La contraseña no está en el frame.** El contrato del registro la exige
 * para crear la cuenta en el *user pool*, y en el prototipo se pasó por
 * alto: el campo se agregó aquí el 22/10/2026 y queda anotado en
 * `docs/pantallas.md` como diferencia que hay que llevar a Figma.
 */
@Composable
fun ConfirmacionDatosPantalla(
    estado: RegistroUiState,
    onDatosCambian: (DatosDocumento) -> Unit,
    onContrasenaCambia: (String) -> Unit,
    onTerminos: (Boolean) -> Unit,
    onVerificar: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val datos = estado.datos ?: return

    PantallaRegistro(
        titulo = stringResource(R.string.registro_datos_titulo),
        onAtras = onAtras,
        modifier = modifier,
        acciones = {
            BotonSolventa(
                texto = stringResource(R.string.registro_datos_accion),
                onClick = onVerificar,
                habilitado = estado.puedeVerificar,
            )
        },
    ) {
        PasoDelRegistro(
            avance = 2f / 3f,
            etiqueta = stringResource(R.string.registro_datos_paso),
        )

        Text(
            text = stringResource(R.string.registro_datos_introduccion),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.textoSecundario,
        )

        if (estado.yaRegistrado) {
            AvisoSolventa(
                tipo = TipoAviso.ERROR,
                titulo = stringResource(R.string.registro_datos_duplicado_titulo),
                texto = stringResource(R.string.registro_datos_duplicado_detalle),
            )
        }

        CampoDato(
            etiqueta = stringResource(R.string.registro_datos_nombres),
            valor = datos.nombres,
            onCambio = { onDatosCambian(datos.copy(nombres = it)) },
            error = estado.camposInvalidos.contains(CampoDeRegistro.NOMBRE),
        )
        CampoDato(
            etiqueta = stringResource(R.string.registro_datos_apellidos),
            valor = datos.apellidos,
            onCambio = { onDatosCambian(datos.copy(apellidos = it)) },
            error = estado.camposInvalidos.contains(CampoDeRegistro.NOMBRE),
        )
        CampoDato(
            etiqueta = stringResource(R.string.registro_datos_documento),
            valor = "${datos.tipoDocumento} ${datos.numeroDocumento}",
            onCambio = {},
            soloLectura = true,
            ayuda = stringResource(R.string.registro_datos_documento_ayuda),
            error = estado.camposInvalidos.contains(CampoDeRegistro.DOCUMENTO),
        )
        CampoDato(
            etiqueta = stringResource(R.string.registro_datos_nacimiento),
            valor = datos.fechaNacimiento,
            onCambio = { onDatosCambian(datos.copy(fechaNacimiento = it)) },
            ayuda = stringResource(R.string.registro_datos_nacimiento_formato),
            error = estado.camposInvalidos.contains(CampoDeRegistro.FECHA_NACIMIENTO),
        )
        CampoDato(
            etiqueta = stringResource(R.string.registro_datos_correo),
            valor = datos.correo,
            onCambio = { onDatosCambian(datos.copy(correo = it)) },
            error = estado.camposInvalidos.contains(CampoDeRegistro.CORREO),
        )
        CampoDato(
            etiqueta = stringResource(R.string.registro_datos_celular),
            valor = datos.celular,
            onCambio = { onDatosCambian(datos.copy(celular = it)) },
            ayuda = stringResource(R.string.registro_datos_celular_ayuda),
        )

        // Visible y no enmascarada: es una credencial de prueba que hay que
        // poder leer para volver a entrar por M2a.
        CampoDato(
            etiqueta = stringResource(R.string.registro_datos_contrasena),
            valor = estado.contrasena,
            onCambio = onContrasenaCambia,
            ayuda = stringResource(R.string.registro_datos_contrasena_ayuda),
            error = estado.camposInvalidos.contains(CampoDeRegistro.CONTRASENA),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(checked = estado.terminosAceptados, onCheckedChange = onTerminos)
            Text(
                text = stringResource(R.string.registro_datos_terminos),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CampoDato(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    soloLectura: Boolean = false,
    ayuda: String? = null,
    error: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        readOnly = soloLectura,
        singleLine = true,
        isError = error,
        supportingText = (if (error) stringResource(R.string.registro_datos_campo_rechazado) else ayuda)
            ?.let { { Text(it) } },
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    )
}
