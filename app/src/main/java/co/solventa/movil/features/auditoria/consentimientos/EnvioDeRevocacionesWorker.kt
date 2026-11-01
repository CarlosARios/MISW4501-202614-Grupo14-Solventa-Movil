package co.solventa.movil.features.auditoria.consentimientos

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
 * Envia la cola de revocaciones cuando hay red, aunque la aplicacion este
 * cerrada (HU-M1.3-02, criterio 5). Es delgado: delega en el repositorio y
 * solo decide si reintentar.
 *
 * Corre como trabajo urgente. En Android 12 o superior el sistema no lo
 * anuncia; antes de Android 12, WorkManager lo ejecuta como servicio en primer
 * plano y necesita la notificacion de [getForegroundInfo].
 *
 * Como el trabajo de HU-M1.3-01, WorkManager lo crea con el constructor de dos
 * parametros, que toma el repositorio de Hilt por un punto de entrada; las
 * pruebas usan el constructor interno.
 */
class EnvioDeRevocacionesWorker internal constructor(
    contexto: Context,
    parametros: WorkerParameters,
    private val repositorio: RevocacionRepositorio,
) : CoroutineWorker(contexto, parametros) {

    constructor(contexto: Context, parametros: WorkerParameters) : this(
        contexto,
        parametros,
        EntryPointAccessors.fromApplication(contexto.applicationContext, PuntoDeEntradaDeRevocaciones::class.java)
            .revocaciones(),
    )

    override suspend fun doWork(): Result = when (repositorio.enviarPendientes()) {
        ResultadoDeEnvio.SinConexion, ResultadoDeEnvio.ConFallas -> Result.retry()
        is ResultadoDeEnvio.Enviadas, ResultadoDeEnvio.SinSesion -> Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val gestor = applicationContext.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(
            NotificationChannel(
                CANAL,
                applicationContext.getString(R.string.auditoria_revocacion_notificacion_canal),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notificacion = NotificationCompat.Builder(applicationContext, CANAL)
            .setSmallIcon(R.drawable.ic_sincronizar)
            .setContentTitle(applicationContext.getString(R.string.auditoria_revocacion_notificacion_titulo))
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(ID_NOTIFICACION, notificacion)
    }

    private companion object {
        const val CANAL = "envio-revocaciones"
        const val ID_NOTIFICACION = 1302
    }
}
