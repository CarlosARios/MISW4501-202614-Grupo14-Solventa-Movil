package co.solventa.movil.features.originacion.polizas

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors

/**
 * Sincroniza la copia de las pólizas cuando vuelve la red, aunque la
 * aplicación esté cerrada (HU-M1.2-01, criterio 3). Es delgado: delega en el
 * repositorio y solo decide si reintentar.
 *
 * WorkManager lo crea con el constructor de dos parámetros, que toma el
 * repositorio de Hilt por un punto de entrada, como el de los
 * consentimientos. Las pruebas usan el constructor interno.
 */
class SincronizacionDePolizasWorker internal constructor(
    contexto: Context,
    parametros: WorkerParameters,
    private val repositorio: PolizasRepositorio,
) : CoroutineWorker(contexto, parametros) {

    constructor(contexto: Context, parametros: WorkerParameters) : this(
        contexto,
        parametros,
        EntryPointAccessors.fromApplication(contexto.applicationContext, PuntoDeEntradaDePolizas::class.java)
            .polizas(),
    )

    override suspend fun doWork(): Result = when (repositorio.sincronizar()) {
        ResultadoDeSincronizacion.SinConexion -> Result.retry()
        else -> Result.success()
    }
}
