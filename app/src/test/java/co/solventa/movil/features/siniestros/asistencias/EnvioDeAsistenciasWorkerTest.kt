package co.solventa.movil.features.siniestros.asistencias

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * El envío urgente de la cola de asistencias (Robolectric, HU-M2.3-01, D2):
 * termina al confirmar o descartar, reintenta con `503` o sin red, y antes de
 * Android 12 da la notificación silenciosa del canal de D13.
 */
@RunWith(AndroidJUnit4::class)
class EnvioDeAsistenciasWorkerTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()

    private fun trabajador(repositorio: AsistenciasRepositorio): EnvioDeAsistenciasWorker {
        val fabrica = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                EnvioDeAsistenciasWorker(appContext, workerParameters, repositorio)
        }
        return TestListenableWorkerBuilder<EnvioDeAsistenciasWorker>(contexto).setWorkerFactory(fabrica).build()
    }

    @Test
    fun `con la cola resuelta termina`() = runTest {
        val repositorio = RepositorioDeAsistenciaFalso().apply { resultadoDelEnvio = ResultadoDelEnvio.COMPLETO }

        assertEquals(ListenableWorker.Result.success(), trabajador(repositorio).doWork())
        assertEquals(1, repositorio.envios)
    }

    @Test
    fun `sin red o con el servicio caido pide reintentar`() = runTest {
        val repositorio = RepositorioDeAsistenciaFalso().apply { resultadoDelEnvio = ResultadoDelEnvio.REINTENTAR }

        assertEquals(ListenableWorker.Result.retry(), trabajador(repositorio).doWork())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `antes de Android 12 da la notificacion del canal de asistencias`() = runTest {
        val info = trabajador(RepositorioDeAsistenciaFalso()).getForegroundInfo()

        assertNotNull(info.notification)
        assertEquals(2301, info.notificationId)
        assertEquals(EnvioDeAsistenciasWorker.CANAL, info.notification.channelId)
        val canal = contexto.getSystemService(NotificationManager::class.java).getNotificationChannel(EnvioDeAsistenciasWorker.CANAL)
        assertEquals(NotificationManager.IMPORTANCE_LOW, canal.importance)
    }

    @Test
    fun `el programador encola un unico trabajo urgente que exige red`() {
        WorkManagerTestInitHelper.initializeTestWorkManager(contexto)
        val programador = ProgramadorDeEnvioDeAsistenciasWorkManager(contexto)

        programador.programar()
        programador.programar()

        val trabajos = WorkManager.getInstance(contexto).getWorkInfosForUniqueWork(AsistenciasRepositorioImpl.TRABAJO).get()
        assertEquals(1, trabajos.size)
        assertEquals(WorkInfo.State.ENQUEUED, trabajos.single().state)
        assertTrue(trabajos.single().constraints.requiredNetworkType == androidx.work.NetworkType.CONNECTED)
    }
}
