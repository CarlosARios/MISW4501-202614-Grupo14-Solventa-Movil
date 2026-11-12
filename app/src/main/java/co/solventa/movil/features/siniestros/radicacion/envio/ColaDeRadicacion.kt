package co.solventa.movil.features.siniestros.radicacion.envio

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La cola de envío de los reportes completos (HU-M2.2-02).
 *
 * Un reporte que no pudo enviarse en el momento —sin señal, o el servicio no
 * respondió— queda aquí y sale solo cuando vuelve la red, sin que el
 * asegurado tenga que tener la aplicación abierta (criterio 3).
 */
interface ColaDeRadicacion {
    /** Lo encola si no estaba ya: un borrador tiene un solo envío vivo. */
    fun encolar(borradorId: String)

    /**
     * *Reintentar ahora* (M13d): descarta la espera acumulada y arranca en
     * cuanto haya red. Usa la misma clave, así que no duplica el radicado.
     */
    fun reintentar(borradorId: String)

    /** Al descartar el borrador, su envío deja de tener sentido. */
    fun cancelar(borradorId: String)
}

/**
 * [ColaDeRadicacion] sobre WorkManager: un trabajo único por borrador, que
 * espera a que haya red y reintenta con espera creciente (criterio 5).
 */
@Singleton
class ColaDeRadicacionConWorkManager @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : ColaDeRadicacion {

    override fun encolar(borradorId: String) = programar(borradorId, ExistingWorkPolicy.KEEP)

    override fun reintentar(borradorId: String) = programar(borradorId, ExistingWorkPolicy.REPLACE)

    override fun cancelar(borradorId: String) {
        WorkManager.getInstance(contexto).cancelUniqueWork(nombre(borradorId))
    }

    private fun programar(borradorId: String, politica: ExistingWorkPolicy) {
        val trabajo = OneTimeWorkRequestBuilder<TrabajadorDeRadicacion>()
            .setInputData(workDataOf(TrabajadorDeRadicacion.BORRADOR to borradorId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ESPERA_INICIAL_S, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto).enqueueUniqueWork(nombre(borradorId), politica, trabajo)
    }

    private fun nombre(borradorId: String) = "radicacion-$borradorId"

    private companion object {
        /** 30 s, 60 s, 120 s…: la espera creciente entre intentos. */
        const val ESPERA_INICIAL_S = 30L
    }
}
