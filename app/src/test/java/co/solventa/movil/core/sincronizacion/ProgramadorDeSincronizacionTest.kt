package co.solventa.movil.core.sincronizacion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La sincronizacion en segundo plano con WorkManager (Robolectric,
 * HU-M1.3-01): un trabajo unico que espera la red y que no se duplica.
 */
@RunWith(AndroidJUnit4::class)
class ProgramadorDeSincronizacionTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()

    class TrabajadorDePrueba(contexto: Context, parametros: WorkerParameters) : Worker(contexto, parametros) {
        override fun doWork(): Result = Result.success()
    }

    @Before
    fun antes() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            contexto,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    private fun trabajos(nombre: String): List<WorkInfo> =
        WorkManager.getInstance(contexto).getWorkInfosForUniqueWork(nombre).get()

    @Test
    fun `programa un trabajo unico que espera la red`() {
        ProgramadorDeSincronizacionWorkManager(contexto).programar("sincronizar-prueba", TrabajadorDePrueba::class.java)

        val trabajos = trabajos("sincronizar-prueba")
        assertEquals(1, trabajos.size)
        assertEquals(WorkInfo.State.ENQUEUED, trabajos.single().state)
        assertEquals(NetworkType.CONNECTED, trabajos.single().constraints.requiredNetworkType)
    }

    @Test
    fun `programar otra vez conserva el trabajo pendiente`() {
        val programador = ProgramadorDeSincronizacionWorkManager(contexto)
        programador.programar("sincronizar-prueba", TrabajadorDePrueba::class.java)
        val primero = trabajos("sincronizar-prueba").single().id

        programador.programar("sincronizar-prueba", TrabajadorDePrueba::class.java)

        assertEquals(listOf(primero), trabajos("sincronizar-prueba").map { it.id })
    }
}
