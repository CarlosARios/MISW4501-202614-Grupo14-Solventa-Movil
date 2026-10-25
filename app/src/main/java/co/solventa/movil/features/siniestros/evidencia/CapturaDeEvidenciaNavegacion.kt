package co.solventa.movil.features.siniestros.evidencia

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import co.solventa.movil.core.navegacion.Ruta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * La captura de evidencia del siniestro (HU-M2.1-01).
 *
 * El archivo se crea aquí, en la caché de la aplicación, y el `ViewModel` lo
 * registra con la ubicación del momento. La pantalla vuelve al asistente en
 * cuanto la captura queda: el envío sigue solo, en segundo plano.
 */
fun NavGraphBuilder.grafoCapturaDeEvidencia(navegacion: NavController) {

    composable<Ruta.CapturaDeEvidencia> { entrada ->
        val ruta = entrada.toRoute<Ruta.CapturaDeEvidencia>()
        val viewModel: EvidenciaViewModel = hiltViewModel()
        val estado by viewModel.estado.collectAsStateWithLifecycle()
        val camara = recordarCamaraDeEvidencia()
        val alcance = rememberCoroutineScope()
        val cache = LocalContext.current.cacheDir

        LaunchedEffect(ruta.borradorId) { viewModel.abrir(ruta.borradorId) }

        // La ubicación se vuelve a leer cuando la cámara queda lista: al
        // abrir la pantalla puede no haber permiso todavía, y la primera
        // lectura habría salido vacía.
        LaunchedEffect(camara.estado) {
            if (camara.estado == EstadoDeLaCaptura.LISTA) viewModel.refrescarUbicacion()
        }

        PantallaCamaraDeEvidencia(
            estado = estado,
            camara = camara,
            tipoDeSiniestro = ruta.tipoDeSiniestro,
            onCerrar = {
                navegacion.popBackStack()
                Unit
            },
            onTomarFoto = {
                when {
                    // El mismo botón cierra la grabación en curso, como en el frame.
                    estado.grabando -> camara.controlador?.detenerGrabacion()
                    viewModel.intentarCapturar(TipoDeEvidencia.FOTO) ->
                        tomarFoto(camara.controlador, cache, alcance, viewModel)
                }
            },
            onGrabarVideo = {
                if (viewModel.intentarCapturar(TipoDeEvidencia.VIDEO)) {
                    grabarVideo(camara.controlador, cache, viewModel)
                }
            },
            onEsperarSenal = viewModel::esperarMejorSenal,
            // Aquí **no** se vuelve a preguntar por la precisión: el usuario
            // acaba de decidir capturar igual, y volver a la compuerta dejaría
            // el aviso dando vueltas sin tomar nunca la evidencia.
            onCapturarIgual = {
                when (viewModel.capturarDeTodasFormas()) {
                    TipoDeEvidencia.FOTO -> tomarFoto(camara.controlador, cache, alcance, viewModel)
                    TipoDeEvidencia.VIDEO -> grabarVideo(camara.controlador, cache, viewModel)
                    null -> Unit
                }
            },
        )
    }
}

/** Toma la fotografía y la registra con la ubicación del momento. */
private fun tomarFoto(
    controlador: ControladorDeEvidencia?,
    cache: File,
    alcance: CoroutineScope,
    viewModel: EvidenciaViewModel,
) {
    if (controlador == null) return
    viewModel.marcarCapturando(true)
    alcance.launch {
        val archivo = File(cache, "evidencia-${System.currentTimeMillis()}.jpg")
        runCatching { controlador.capturarFoto(archivo) }
            .onSuccess { viewModel.registrar(it, TipoDeEvidencia.FOTO) }
            .onFailure { viewModel.marcarCapturando(false) }
    }
}

/** Empieza a grabar y registra el archivo cuando la grabación cierra. */
private fun grabarVideo(
    controlador: ControladorDeEvidencia?,
    cache: File,
    viewModel: EvidenciaViewModel,
) {
    if (controlador == null) return
    val inicio = System.currentTimeMillis()
    val archivo = File(cache, "evidencia-$inicio.mp4")
    viewModel.marcarGrabando(true)
    controlador.grabar(archivo) { grabado ->
        if (grabado == null) {
            viewModel.marcarGrabando(false)
            return@grabar
        }
        val duracion = ((System.currentTimeMillis() - inicio) / 1_000L).toInt()
        viewModel.registrar(grabado, TipoDeEvidencia.VIDEO, duracion.coerceAtLeast(1))
    }
}
