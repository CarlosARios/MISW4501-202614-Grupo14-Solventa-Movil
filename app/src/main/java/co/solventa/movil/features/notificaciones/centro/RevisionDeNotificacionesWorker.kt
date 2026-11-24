package co.solventa.movil.features.notificaciones.centro

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Revisa el centro con la aplicación en segundo plano o cerrada (HU-M3.1-01,
 * criterio 3): el sistema muestra la notificación de cada pago nuevo aunque la
 * aplicación no esté abierta. Sin proveedor de *push* real, es la forma de
 * enterarse con la aplicación cerrada; Android no deja repetirlo más seguido
 * que cada 15 minutos.
 *
 * WorkManager lo crea con el constructor de dos parámetros, que toma el
 * revisor de Hilt por un punto de entrada. Las pruebas usan el interno.
 */
class RevisionDeNotificacionesWorker internal constructor(
    contexto: Context,
    parametros: WorkerParameters,
    private val revisor: RevisorDeNotificaciones,
) : CoroutineWorker(contexto, parametros) {

    constructor(contexto: Context, parametros: WorkerParameters) : this(
        contexto,
        parametros,
        EntryPointAccessors.fromApplication(contexto.applicationContext, PuntoDeEntradaDeNotificaciones::class.java)
            .revisorDeNotificaciones(),
    )

    override suspend fun doWork(): Result = when (revisor.revisar()) {
        ResultadoDeSincronizacion.SinConexion -> Result.retry()
        else -> Result.success()
    }
}

/** Programa la revisión del centro (HU-M3.1-01). */
interface ProgramadorDeRevision {
    /** La revisión periódica, de respaldo. */
    fun programar()

    /**
     * Una revisión en cuanto haya red: la pide el *push* de Firebase
     * (HU-M3.3-01), para que el aviso llegue en segundos con la aplicación
     * cerrada.
     */
    fun revisarAhora()
}

/**
 * [ProgramadorDeRevision] con WorkManager: un trabajo periódico único cada 15
 * minutos que exige red; programarlo otra vez conserva el que había (`KEEP`).
 */
@Singleton
class ProgramadorDeRevisionWorkManager @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : ProgramadorDeRevision {

    override fun programar() {
        val solicitud = PeriodicWorkRequestBuilder<RevisionDeNotificacionesWorker>(INTERVALO_MIN, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, solicitud)
    }

    /**
     * Un trabajo único de una vez, con red: dos *push* seguidos no lo
     * duplican (`KEEP`), porque una revisión trae todo lo nuevo. El *push* de
     * prioridad alta deja a la aplicación correr en ese momento aunque el
     * teléfono esté en reposo.
     */
    override fun revisarAhora() {
        val solicitud = OneTimeWorkRequestBuilder<RevisionDeNotificacionesWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(contexto).enqueueUniqueWork(TRABAJO_AHORA, ExistingWorkPolicy.KEEP, solicitud)
    }

    companion object {
        const val TRABAJO = "revisar-notificaciones"

        /** La revisión que pide un *push* (HU-M3.3-01). */
        const val TRABAJO_AHORA = "revisar-notificaciones-ahora"

        /** El mínimo que admite WorkManager para un trabajo periódico. */
        const val INTERVALO_MIN = 15L
    }
}
