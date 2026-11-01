package co.solventa.movil.features.auditoria.consentimientos

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El trabajo en segundo plano (Robolectric, HU-M1.3-01): reintenta mientras
 * no haya red y termina en lo demas, porque reintentar no arregla una cuenta
 * sin vincular ni un servicio que rechaza.
 */
@RunWith(AndroidJUnit4::class)
class SincronizacionDeConsentimientosWorkerTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()

    private suspend fun ejecutarCon(resultado: ResultadoDeActualizacion): Pair<ListenableWorker.Result, Int> {
        val repositorio = RepositorioFalso().apply { this.resultado = resultado }
        val fabrica = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = SincronizacionDeConsentimientosWorker(appContext, workerParameters, repositorio)
        }
        val trabajador = TestListenableWorkerBuilder<SincronizacionDeConsentimientosWorker>(contexto)
            .setWorkerFactory(fabrica)
            .build()
        return trabajador.doWork() to repositorio.actualizaciones
    }

    @Test
    fun `sin red pide reintentar`() = runTest {
        val (resultado, actualizaciones) = ejecutarCon(ResultadoDeActualizacion.SinConexion)

        assertEquals(ListenableWorker.Result.retry(), resultado)
        assertEquals(1, actualizaciones)
    }

    @Test
    fun `en lo demas termina`() = runTest {
        listOf(
            ResultadoDeActualizacion.Actualizado,
            ResultadoDeActualizacion.CuentaSinVincular,
            ResultadoDeActualizacion.Error,
            ResultadoDeActualizacion.SinSesion,
        ).forEach {
            assertEquals(it.toString(), ListenableWorker.Result.success(), ejecutarCon(it).first)
        }
    }
}
