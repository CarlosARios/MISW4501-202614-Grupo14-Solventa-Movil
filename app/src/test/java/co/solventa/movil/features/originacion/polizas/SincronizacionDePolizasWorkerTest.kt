package co.solventa.movil.features.originacion.polizas

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
 * El trabajo en segundo plano (Robolectric, HU-M1.2-01): reintenta mientras
 * no haya red y termina en lo demás, porque reintentar no arregla un servicio
 * que responde mal ni una sesión cerrada.
 */
@RunWith(AndroidJUnit4::class)
class SincronizacionDePolizasWorkerTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()

    private suspend fun ejecutarCon(resultado: ResultadoDeSincronizacion): Pair<ListenableWorker.Result, Int> {
        val repositorio = RepositorioFalso().apply { this.resultado = resultado }
        val fabrica = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = SincronizacionDePolizasWorker(appContext, workerParameters, repositorio)
        }
        val trabajador = TestListenableWorkerBuilder<SincronizacionDePolizasWorker>(contexto)
            .setWorkerFactory(fabrica)
            .build()
        return trabajador.doWork() to repositorio.sincronizaciones
    }

    @Test
    fun `sin red pide reintentar`() = runTest {
        val (resultado, sincronizaciones) = ejecutarCon(ResultadoDeSincronizacion.SinConexion)

        assertEquals(ListenableWorker.Result.retry(), resultado)
        assertEquals(1, sincronizaciones)
    }

    @Test
    fun `en lo demas termina`() = runTest {
        listOf(
            ResultadoDeSincronizacion.Sincronizado(emptyList()),
            ResultadoDeSincronizacion.Error,
            ResultadoDeSincronizacion.SinSesion,
        ).forEach {
            assertEquals(it.toString(), ListenableWorker.Result.success(), ejecutarCon(it).first)
        }
    }
}
