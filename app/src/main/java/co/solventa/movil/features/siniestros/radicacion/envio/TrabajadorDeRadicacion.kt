package co.solventa.movil.features.siniestros.radicacion.envio

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Envía en segundo plano un reporte que quedó en cola (HU-M2.2-02). La
 * lógica vive en [EnvioDelBorradorEnCola]; esto solo la conecta con
 * WorkManager, que decide cuándo hay red y cuánto esperar entre intentos.
 */
@HiltWorker
class TrabajadorDeRadicacion @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val envio: EnvioDelBorradorEnCola,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val borradorId = inputData.getString(BORRADOR) ?: return Result.failure()
        return when (envio.enviar(borradorId)) {
            ResultadoDelEnvio.LISTO -> Result.success()
            ResultadoDelEnvio.REINTENTAR -> Result.retry()
            ResultadoDelEnvio.AGOTADO -> Result.failure()
        }
    }

    companion object {
        const val BORRADOR = "borrador"
    }
}
