package co.solventa.movil.features.siniestros.evidencia

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * La cámara trasera vista desde la captura de evidencia (HU-M2.1-01).
 *
 * Hace dos cosas que la captura del documento no necesita: graba video de
 * hasta 30 segundos y usa una sola sesión para foto y video, de modo que
 * cambiar de modo no reinicia la cámara.
 *
 * **El video se graba sin audio.** La evidencia que los criterios piden es el
 * lugar, la hora y la imagen; grabar sonido obligaría a pedir el permiso de
 * micrófono, que el diálogo del prototipo no menciona.
 */
class ControladorDeEvidencia internal constructor(
    private val contexto: Context,
) {
    private var proveedor: ProcessCameraProvider? = null
    private var captura: ImageCapture? = null
    private var video: VideoCapture<Recorder>? = null
    private var grabacion: Recording? = null

    /** Enciende la cámara y la ata al ciclo de vida de la pantalla. */
    internal suspend fun iniciar(dueño: LifecycleOwner, vista: PreviewView): Boolean {
        val proveedor = ProcessCameraProvider.awaitInstance(contexto).also { this.proveedor = it }
        if (!proveedor.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) return false

        val previa = Preview.Builder().build().apply { surfaceProvider = vista.surfaceProvider }
        val captura = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        // 720p es lo que el criterio fija para el video; si el dispositivo no
        // lo tiene, CameraX elige la calidad más cercana.
        val grabador = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        val video = VideoCapture.withOutput(grabador)

        proveedor.unbindAll()
        proveedor.bindToLifecycle(dueño, CameraSelector.DEFAULT_BACK_CAMERA, previa, captura, video)
        this.captura = captura
        this.video = video
        return true
    }

    internal fun detener() {
        grabacion?.stop()
        grabacion = null
        proveedor?.unbindAll()
        proveedor = null
        captura = null
        video = null
    }

    /** Toma la fotografía y devuelve el archivo. */
    suspend fun capturarFoto(destino: File): File {
        val captura = checkNotNull(this.captura) { "La cámara no está lista" }
        val opciones = ImageCapture.OutputFileOptions.Builder(destino).build()

        return suspendCancellableCoroutine { continuacion ->
            captura.takePicture(
                opciones,
                ContextCompat.getMainExecutor(contexto),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                        continuacion.resume(destino)
                    }

                    override fun onError(excepcion: ImageCaptureException) {
                        continuacion.resumeWithException(excepcion)
                    }
                },
            )
        }
    }

    /** Si hay una grabación en curso. */
    val grabando: Boolean
        get() = grabacion != null

    /**
     * Empieza a grabar. [alTerminar] recibe el archivo cuando la grabación
     * cierra, por parada manual o por llegar al tope de segundos.
     */
    @Suppress("MissingPermission")
    fun grabar(destino: File, alTerminar: (File?) -> Unit) {
        val video = this.video ?: return alTerminar(null)
        if (grabacion != null) return

        val salida = FileOutputOptions.Builder(destino)
            .setDurationLimitMillis(LimitesDeEvidencia.MAXIMO_DE_SEGUNDOS * 1_000L)
            .build()
        grabacion = video.output
            .prepareRecording(contexto, salida)
            .start(ContextCompat.getMainExecutor(contexto)) { evento ->
                if (evento is VideoRecordEvent.Finalize) {
                    grabacion = null
                    alTerminar(if (evento.hasError()) null else destino)
                }
            }
    }

    /** Cierra la grabación en curso. */
    fun detenerGrabacion() {
        grabacion?.stop()
    }
}
