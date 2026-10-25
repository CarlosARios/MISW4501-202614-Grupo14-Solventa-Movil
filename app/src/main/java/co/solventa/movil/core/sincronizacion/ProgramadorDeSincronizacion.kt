package co.solventa.movil.core.sincronizacion

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa una sincronizacion en segundo plano que corre cuando vuelve la red
 * (HU-M1.3-01, criterio 5), aunque la aplicacion este cerrada.
 *
 * Recibe la clase del trabajador y no la conoce: asi `core` no importa de
 * `features` y cada dominio programa el suyo.
 */
interface ProgramadorDeSincronizacion {
    fun programar(nombreUnico: String, trabajador: Class<out ListenableWorker>)
}

/**
 * [ProgramadorDeSincronizacion] con WorkManager: un trabajo unico por nombre
 * que exige red, se conserva si ya habia uno pendiente (`KEEP`) y se reintenta
 * con espera exponencial.
 */
@Singleton
class ProgramadorDeSincronizacionWorkManager @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : ProgramadorDeSincronizacion {

    override fun programar(nombreUnico: String, trabajador: Class<out ListenableWorker>) {
        val solicitud = OneTimeWorkRequest.Builder(trabajador)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ESPERA_INICIAL_S, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto).enqueueUniqueWork(nombreUnico, ExistingWorkPolicy.KEEP, solicitud)
    }

    private companion object {
        const val ESPERA_INICIAL_S = 30L
    }
}
