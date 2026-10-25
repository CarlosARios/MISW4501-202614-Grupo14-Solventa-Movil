package co.solventa.movil.features.siniestros.evidencia

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.solventa.movil.core.api.siniestros.SiniestrosApi
import co.solventa.movil.core.api.siniestros.SolicitudDeSubida
import co.solventa.movil.core.api.siniestros.SubidorDeArchivos
import co.solventa.movil.core.errores.Correlacion
import co.solventa.movil.core.persistencia.dao.EvidenciaCapturadaDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Pide que lo capturado se envíe. */
interface EnvioDeEvidencia {
    /** Encola el envío de lo que acaba de capturarse. */
    fun pedir()

    /**
     * Vuelve a intentarlo ahora. Tras varios fallos seguidos —el servicio
     * caído, o un rato largo sin red— la espera entre reintentos crece hasta
     * horas, y quien está mirando la pantalla no tiene por qué esperarlas.
     */
    fun reintentar()
}

/**
 * Encola el envío en WorkManager: espera a que haya red y reintenta solo
 * cuando vuelve, que es lo que pide el criterio 5. El trabajo es único, así
 * que capturar cinco fotos seguidas no lanza cinco trabajos.
 */
@Singleton
class EnvioConWorkManager @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : EnvioDeEvidencia {

    override fun pedir() = encolar(ExistingWorkPolicy.APPEND_OR_REPLACE)

    // REPLACE descarta la espera acumulada y arranca de nuevo en cuanto haya
    // red; APPEND_OR_REPLACE la respetaría.
    override fun reintentar() = encolar(ExistingWorkPolicy.REPLACE)

    private fun encolar(politica: ExistingWorkPolicy) {
        val trabajo = OneTimeWorkRequestBuilder<TrabajadorDeSubida>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(contexto).enqueueUniqueWork(TRABAJO, politica, trabajo)
    }

    private companion object {
        const val TRABAJO = "subida-de-evidencia"
    }
}

/**
 * Sube las evidencias pendientes (HU-M2.1-01).
 *
 * Cada una son dos pasos: el servicio firma la subida y el archivo va directo
 * al repositorio de evidencia. El archivo no pasa por el API Gateway, que es
 * lo que permite que un video de 30 segundos quepa.
 *
 * Una evidencia que falla queda marcada y el trabajo se reintenta; las demás
 * siguen su camino, porque el asegurado no tiene por qué perder cuatro fotos
 * por una.
 */
@HiltWorker
class TrabajadorDeSubida @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val dao: EvidenciaCapturadaDao,
    private val api: SiniestrosApi,
    private val subidor: SubidorDeArchivos,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val pendientes = dao.pendientes()
        if (pendientes.isEmpty()) return Result.success()

        var algunaFallo = false
        for (evidencia in pendientes) {
            val archivo = File(evidencia.rutaArchivo)
            if (!archivo.exists()) {
                // Sin archivo no hay nada que subir: la evidencia se descarta
                // en lugar de reintentarse para siempre.
                dao.borrar(evidencia.id)
                continue
            }
            dao.marcar(evidencia.id, EstadoDeEvidencia.SUBIENDO.guardado)

            val llave = runCatching { subir(evidencia.tipo, evidencia.sha256, evidencia.tamanoBytes, archivo) }
                .getOrNull()
            if (llave == null) {
                dao.marcar(evidencia.id, EstadoDeEvidencia.FALLIDA.guardado)
                algunaFallo = true
                continue
            }
            dao.marcarSubida(evidencia.id, llave)
        }
        return if (algunaFallo) Result.retry() else Result.success()
    }

    /** Devuelve la llave del objeto, o `null` si la subida no quedó. */
    private suspend fun subir(tipo: String, sha256: String, tamano: Long, archivo: File): String? {
        val respuesta = api.urlDeSubida(
            Correlacion.nuevo(),
            SolicitudDeSubida(
                tipo = tipo,
                tipoDeContenido = tipoDeContenido(tipo),
                tamanoBytes = tamano,
                sha256 = sha256,
            ),
        )
        val firmada = respuesta.body()
        if (!respuesta.isSuccessful || firmada == null) return null
        val subida = subidor.subir(archivo, firmada.url, firmada.encabezados)
        return if (subida) firmada.llaveDelObjeto else null
    }

    private fun tipoDeContenido(tipo: String): String =
        if (tipo == TipoDeEvidencia.VIDEO.delContrato) {
            TipoDeEvidencia.VIDEO.tipoDeContenido
        } else {
            TipoDeEvidencia.FOTO.tipoDeContenido
        }
}
