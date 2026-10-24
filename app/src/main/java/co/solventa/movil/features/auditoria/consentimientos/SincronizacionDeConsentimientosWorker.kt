package co.solventa.movil.features.auditoria.consentimientos

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors

/**
 * Actualiza la copia de los consentimientos cuando vuelve la red, aunque la
 * aplicacion este cerrada (HU-M1.3-01, criterio 5). Es delgado: delega en el
 * repositorio y solo decide si reintentar.
 *
 * WorkManager lo crea con el constructor de dos parametros, que toma el
 * repositorio de Hilt por un punto de entrada; la aplicacion no cambia su
 * inicializacion de WorkManager. Las pruebas usan el constructor interno.
 */
class SincronizacionDeConsentimientosWorker internal constructor(
    contexto: Context,
    parametros: WorkerParameters,
    private val repositorio: ConsentimientosRepositorio,
) : CoroutineWorker(contexto, parametros) {

    constructor(contexto: Context, parametros: WorkerParameters) : this(
        contexto,
        parametros,
        EntryPointAccessors.fromApplication(contexto.applicationContext, PuntoDeEntradaDeConsentimientos::class.java)
            .repositorio(),
    )

    override suspend fun doWork(): Result = when (repositorio.actualizar()) {
        ResultadoDeActualizacion.SinConexion -> Result.retry()
        else -> Result.success()
    }
}
