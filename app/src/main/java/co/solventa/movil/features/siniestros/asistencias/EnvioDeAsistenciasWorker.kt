package co.solventa.movil.features.siniestros.asistencias

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import co.solventa.movil.R
import dagger.hilt.android.EntryPointAccessors

/**
 * Envía la cola de solicitudes de asistencia cuando hay red, aunque Android
 * haya cerrado la aplicación durante la llamada (HU-M2.3-01, D2). Es delgado:
 * delega en el repositorio y solo decide si reintentar.
 *
 * Corre como trabajo urgente. Antes de Android 12, WorkManager lo ejecuta como
 * servicio en primer plano y necesita la notificación de [getForegroundInfo],
 * en un canal propio, silencioso y de importancia baja (D13).
 *
 * WorkManager lo crea con el constructor de dos parámetros, que toma el
 * repositorio de Hilt por un punto de entrada; las pruebas usan el interno.
 */
class EnvioDeAsistenciasWorker internal constructor(
    contexto: Context,
    parametros: WorkerParameters,
    private val repositorio: AsistenciasRepositorio,
) : CoroutineWorker(contexto, parametros) {

    constructor(contexto: Context, parametros: WorkerParameters) : this(
        contexto,
        parametros,
        EntryPointAccessors.fromApplication(contexto.applicationContext, PuntoDeEntradaDeAsistencias::class.java)
            .asistencias(),
    )

    override suspend fun doWork(): Result = when (repositorio.enviarPendientes()) {
        ResultadoDelEnvio.REINTENTAR -> Result.retry()
        ResultadoDelEnvio.COMPLETO -> Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        crearCanal(applicationContext)
        val notificacion = NotificationCompat.Builder(applicationContext, CANAL)
            .setSmallIcon(R.drawable.ic_pin_mapa)
            .setContentTitle(applicationContext.getString(R.string.siniestros_asistencia_notificacion_envio))
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(ID_NOTIFICACION, notificacion)
    }

    companion object {
        /** El canal de D13: el del envío y el del aviso de S6. */
        const val CANAL = "asistencias-envio"
        private const val ID_NOTIFICACION = 2301

        /** Crea el canal con el nombre en el idioma del contexto dado. */
        fun crearCanal(contexto: Context) {
            contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CANAL,
                    contexto.getString(R.string.siniestros_asistencia_canal),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }
}
