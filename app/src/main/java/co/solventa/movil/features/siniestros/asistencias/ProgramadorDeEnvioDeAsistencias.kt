package co.solventa.movil.features.siniestros.asistencias

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Programa el envío urgente de la cola de solicitudes de asistencia (HU-M2.3-01, D2). */
fun interface ProgramadorDeEnvioDeAsistencias {
    fun programar()
}

/**
 * [ProgramadorDeEnvioDeAsistencias] con WorkManager, como el de las
 * revocaciones (HU-M1.3-02): un trabajo **urgente** (`setExpedited`) que exige
 * red, único por nombre (`KEEP`: si ya hay uno pendiente, ese recorre toda la
 * cola) y con espera exponencial desde 10 s. Sin cupo de trabajo urgente corre
 * como trabajo normal en vez de perderse.
 */
@Singleton
class ProgramadorDeEnvioDeAsistenciasWorkManager @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : ProgramadorDeEnvioDeAsistencias {

    override fun programar() {
        val solicitud = OneTimeWorkRequest.Builder(EnvioDeAsistenciasWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ESPERA_INICIAL_S, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto)
            .enqueueUniqueWork(AsistenciasRepositorioImpl.TRABAJO, ExistingWorkPolicy.KEEP, solicitud)
    }

    private companion object {
        const val ESPERA_INICIAL_S = 10L
    }
}
