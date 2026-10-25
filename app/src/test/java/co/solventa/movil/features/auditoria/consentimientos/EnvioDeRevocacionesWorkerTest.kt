package co.solventa.movil.features.auditoria.consentimientos

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * El envio urgente de la cola (Robolectric, HU-M1.3-02): reintenta sin red o
 * con fallas del servicio y termina cuando la cola quedo vacia. En Android 11
 * (antes de 12) WorkManager lo corre como servicio en primer plano y pide su
 * notificacion.
 */
@RunWith(AndroidJUnit4::class)
class EnvioDeRevocacionesWorkerTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()

    private class RepositorioDeEnvio(private val resultado: ResultadoDeEnvio) : RevocacionRepositorio {
        var envios = 0
        override val conectado: StateFlow<Boolean> = MutableStateFlow(true)
        override val avisos: StateFlow<List<AvisoDeRevocacion>> = MutableStateFlow(emptyList())
        override fun pendientes(): Flow<List<RevocacionPendiente>> = flowOf(emptyList())
        override fun descartarAviso(consentimientoId: String) = Unit
        override suspend fun revocar(consentimiento: Consentimiento, metodo: MetodoDeConfirmacion): ResultadoDeRevocacion =
            ResultadoDeRevocacion.Error
        override suspend fun comprobante(consentimientoId: String, idioma: String): ResultadoDeComprobante =
            ResultadoDeComprobante.Error
        override suspend fun cuantasPendientes(): Int = 0
        override suspend fun enviarPendientes(): ResultadoDeEnvio {
            envios++
            return resultado
        }
    }

    private fun trabajador(repositorio: RevocacionRepositorio): EnvioDeRevocacionesWorker {
        val fabrica = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                EnvioDeRevocacionesWorker(appContext, workerParameters, repositorio)
        }
        return TestListenableWorkerBuilder<EnvioDeRevocacionesWorker>(contexto).setWorkerFactory(fabrica).build()
    }

    @Test
    fun `sin red o con fallas del servicio pide reintentar`() = runTest {
        listOf(ResultadoDeEnvio.SinConexion, ResultadoDeEnvio.ConFallas).forEach { resultado ->
            val repositorio = RepositorioDeEnvio(resultado)
            assertEquals(resultado.toString(), ListenableWorker.Result.retry(), trabajador(repositorio).doWork())
            assertEquals(1, repositorio.envios)
        }
    }

    @Test
    fun `con la cola enviada o sin sesion termina`() = runTest {
        listOf(ResultadoDeEnvio.Enviadas(2), ResultadoDeEnvio.Enviadas(0), ResultadoDeEnvio.SinSesion).forEach {
            assertEquals(it.toString(), ListenableWorker.Result.success(), trabajador(RepositorioDeEnvio(it)).doWork())
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `antes de Android 12 da la notificacion del servicio en primer plano`() = runTest {
        val info = trabajador(RepositorioDeEnvio(ResultadoDeEnvio.Enviadas(0))).getForegroundInfo()

        assertNotNull(info.notification)
        assertEquals(1302, info.notificationId)
    }
}
