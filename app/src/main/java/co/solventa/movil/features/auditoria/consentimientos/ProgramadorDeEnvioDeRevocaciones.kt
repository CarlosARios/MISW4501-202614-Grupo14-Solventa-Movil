package co.solventa.movil.features.auditoria.consentimientos

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

/**
 * Programa el envio urgente de la cola de revocaciones (HU-M1.3-02,
 * criterio 5: «se envia automaticamente con prioridad alta al restablecerse la
 * conexion»).
 */
fun interface ProgramadorDeEnvioDeRevocaciones {
    fun programar()
}

/**
 * [ProgramadorDeEnvioDeRevocaciones] con WorkManager: un trabajo **urgente**
 * (`setExpedited`) que exige red, unico por nombre (`KEEP`: si ya habia uno
 * pendiente, ese basta) y con espera exponencial. Si el sistema no tiene
 * cupo de trabajo urgente, corre como trabajo normal en vez de perderse.
 */
@Singleton
class ProgramadorDeEnvioDeRevocacionesWorkManager @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : ProgramadorDeEnvioDeRevocaciones {

    override fun programar() {
        val solicitud = OneTimeWorkRequest.Builder(EnvioDeRevocacionesWorker::class.java)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ESPERA_INICIAL_S, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto)
            .enqueueUniqueWork(RevocacionRepositorioImpl.TRABAJO, ExistingWorkPolicy.KEEP, solicitud)
    }

    private companion object {
        const val ESPERA_INICIAL_S = 10L
    }
}
