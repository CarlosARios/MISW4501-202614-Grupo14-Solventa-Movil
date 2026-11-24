package co.solventa.movil.features.notificaciones.centro

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Firebase Cloud Messaging en la aplicación (HU-M3.3-01): el dispositivo se
 * registra con el token de Firebase, y un *push* pide una revisión en ese
 * momento en lugar de esperar la periódica.
 */
@RunWith(AndroidJUnit4::class)
class PushDeFirebaseTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var base: BaseDeDatosSolventa
    private val api = NotificacionesFalsas()
    private val token = TokenFalso()

    private val repositorio by lazy {
        NotificacionesRepositorioImpl(api, base.notificaciones(), SesionFalsa(), ConectividadFalsa(), InstalacionFalsa(), token) {
            PAGADO
        }
    }

    @Before
    fun antes() {
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java).allowMainThreadQueries().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            contexto,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    @After
    fun despues() {
        base.close()
    }

    @Test
    fun `con Firebase el dispositivo se registra con su token y no con el id de la instalacion`() = runTest {
        repositorio.registrarDispositivo(permisoConcedido = true, idioma = "es")

        assertEquals("tok-fcm-1", api.dispositivos.single().second.token)
        assertEquals("inst-1", api.dispositivos.single().first)
    }

    @Test
    fun `un token nuevo de Firebase vuelve a registrar el dispositivo`() = runTest {
        repositorio.registrarDispositivo(true, "es")
        repositorio.registrarDispositivo(true, "es")
        token.valor = "tok-fcm-2"
        repositorio.registrarDispositivo(true, "es")

        assertEquals(listOf("tok-fcm-1", "tok-fcm-2"), api.dispositivos.map { it.second.token })
    }

    @Test
    fun `sin Firebase se registra con el id de la instalacion y sin permiso sin token`() = runTest {
        token.valor = null
        repositorio.registrarDispositivo(true, "es")
        repositorio.registrarDispositivo(false, "es")

        assertEquals("inst-1", api.dispositivos[0].second.token)
        assertNull(api.dispositivos[1].second.token)
    }

    @Test
    fun `un push pide una revision en ese momento, una sola aunque lleguen varios`() {
        val programador = ProgramadorDeRevisionWorkManager(contexto)

        programador.revisarAhora()
        programador.revisarAhora()

        val trabajos = WorkManager.getInstance(contexto)
            .getWorkInfosForUniqueWork(ProgramadorDeRevisionWorkManager.TRABAJO_AHORA).get()
        assertEquals(1, trabajos.size)
        assertEquals(WorkInfo.State.ENQUEUED, trabajos.single().state)
    }

    @Test
    fun `sin Firebase inicializado no hay token`() = runTest {
        assertNull(TokenDeFirebase(contexto).token())
        assertNull(TokenDePush.Ninguno.token())
    }
}
