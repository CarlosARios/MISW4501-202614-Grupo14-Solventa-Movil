package co.solventa.movil.features.siniestros.evidencia

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Qué le falta a la captura de evidencia para poder empezar. */
enum class EstadoDeLaCaptura {
    /** Falta conceder cámara o ubicación precisa, y todavía se puede pedir. */
    SIN_PERMISOS,

    /**
     * El usuario negó los permisos. Android no deja volver a preguntar desde
     * la aplicación: la salida es la configuración del sistema. Criterio 1:
     * sin ellos no hay evidencia verificable y no se ofrece la galería.
     */
    PERMISOS_NEGADOS,

    /** El dispositivo no tiene cámara trasera utilizable. */
    SIN_CAMARA,

    /** Se puede capturar. */
    LISTA,
}

/** Todo lo que la pantalla de captura necesita saber de la cámara. */
class CamaraDeEvidencia internal constructor(
    val estado: EstadoDeLaCaptura,
    val controlador: ControladorDeEvidencia?,
    val pedirPermisos: () -> Unit,
)

/** Una cámara de mentira para las vistas previas y las pruebas de interfaz. */
fun camaraDeEvidenciaSimulada(
    estado: EstadoDeLaCaptura = EstadoDeLaCaptura.SIN_CAMARA,
): CamaraDeEvidencia = CamaraDeEvidencia(estado, controlador = null) {}

/**
 * Prepara la cámara y los permisos de la captura de evidencia.
 *
 * **Cámara y ubicación precisa se piden juntas** (criterio 1): la evidencia
 * sin lugar no respalda nada, así que no tiene sentido conceder una y negar
 * la otra en pasos distintos.
 */
@Composable
fun recordarCamaraDeEvidencia(): CamaraDeEvidencia {
    val contexto = LocalContext.current
    if (LocalInspectionMode.current) {
        return CamaraDeEvidencia(EstadoDeLaCaptura.SIN_CAMARA, null) {}
    }

    var concedidos by remember { mutableStateOf(contexto.tienePermisosDeCaptura()) }
    var yaSePregunto by remember { mutableStateOf(false) }
    var hayCamara by remember { mutableStateOf<Boolean?>(null) }

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { respuesta ->
        concedidos = respuesta[Manifest.permission.CAMERA] == true && respuesta.hayUbicacion()
        yaSePregunto = true
    }

    val controlador = remember { ControladorDeEvidencia(contexto) }

    LaunchedEffect(concedidos) {
        if (concedidos && hayCamara == null) hayCamara = true
    }

    DisposableEffect(Unit) {
        onDispose { controlador.detener() }
    }

    val estado = when {
        !concedidos && yaSePregunto -> EstadoDeLaCaptura.PERMISOS_NEGADOS
        !concedidos -> EstadoDeLaCaptura.SIN_PERMISOS
        hayCamara == false -> EstadoDeLaCaptura.SIN_CAMARA
        else -> EstadoDeLaCaptura.LISTA
    }

    return CamaraDeEvidencia(
        estado = estado,
        controlador = controlador.takeIf { estado == EstadoDeLaCaptura.LISTA },
        pedirPermisos = {
            lanzador.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
    )
}

/** El visor en vivo. Solo se dibuja con la cámara lista. */
@Composable
fun VisorDeEvidencia(camara: CamaraDeEvidencia, modifier: Modifier = Modifier) {
    val controlador = camara.controlador ?: return
    val dueño = LocalLifecycleOwner.current
    val contexto = LocalContext.current
    val vista = remember {
        PreviewView(contexto).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(controlador, vista, dueño) { controlador.iniciar(dueño, vista) }

    AndroidView(factory = { vista }, modifier = modifier)
}

/**
 * **La ubicación aproximada alcanza para capturar.** Desde Android 12 el
 * usuario puede conceder solo esa, y negarle la captura por eso sería peor
 * que registrarla: con poca precisión la evidencia se guarda marcada como de
 * ubicación no confiable, que es lo que pide el criterio 3.
 */
internal fun Context.tienePermisosDeCaptura(): Boolean =
    concedido(Manifest.permission.CAMERA) &&
        (concedido(Manifest.permission.ACCESS_FINE_LOCATION) ||
            concedido(Manifest.permission.ACCESS_COARSE_LOCATION))

private fun Context.concedido(permiso: String): Boolean =
    ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED

private fun Map<String, Boolean>.hayUbicacion(): Boolean =
    this[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
        this[Manifest.permission.ACCESS_COARSE_LOCATION] == true
