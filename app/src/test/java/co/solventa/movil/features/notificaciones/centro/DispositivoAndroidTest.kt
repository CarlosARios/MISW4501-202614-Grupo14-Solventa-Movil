package co.solventa.movil.features.notificaciones.centro

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import co.solventa.movil.core.i18n.Idioma
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * Las piezas de Android del centro (Robolectric, HU-M3.1-01): la notificación
 * del sistema, la apertura desde ella, la identidad de la instalación y el
 * trabajo en segundo plano.
 */
@RunWith(AndroidJUnit4::class)
class DispositivoAndroidTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val gestor = contexto.getSystemService(NotificationManager::class.java)

    @Test
    fun `cada pago es su propia notificacion del sistema con el monto, el evento y que abrir (criterios 2, 3, 4 y 6)`() {
        val avisador = AvisadorDelSistema(contexto, PermisoFalso(true))

        val mostro = avisador.avisar(listOf(notificacion("ntf-1"), notificacion("ntf-2", evento = "LA-4012")), Idioma.ESPANOL)

        assertTrue(mostro)
        val activas = shadowOf(gestor).allNotifications
        assertEquals("dos pagos y el resumen del grupo", 3, activas.size)
        val pago = shadowOf(gestor).getNotification("ntf-1", 3101)
        assertNotNull(pago)
        val extras = pago.extras
        assertEquals("Pago automático recibido: $ 180.000 COP", extras.getCharSequence("android.title").toString())
        assertTrue(extras.getCharSequence("android.text").toString().contains("AV-8421"))
        assertEquals(AvisadorDelSistema.GRUPO, pago.group)
        val intent = shadowOf(pago.contentIntent).savedIntent
        assertEquals("ntf-1", intent.getStringExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION))
        assertNotNull(gestor.getNotificationChannel(AvisadorDelSistema.CANAL))
    }

    @Test
    fun `la notificacion del sistema se escribe en el idioma de la aplicacion`() {
        AvisadorDelSistema(contexto, PermisoFalso(true)).avisar(listOf(notificacion("ntf-1")), Idioma.INGLES)

        val pago = shadowOf(gestor).getNotification("ntf-1", 3101)
        assertEquals("Automatic payment received: $ 180,000 COP", pago.extras.getCharSequence("android.title").toString())
    }

    @Test
    fun `sin permiso no se muestra nada (criterio 3)`() {
        val mostro = AvisadorDelSistema(contexto, PermisoFalso(false)).avisar(listOf(notificacion()), Idioma.ESPANOL)

        assertFalse(mostro)
        assertTrue(shadowOf(gestor).allNotifications.isEmpty())
    }

    @Test
    fun `la apertura entrega la notificacion tocada una sola vez`() {
        val apertura = AperturaDesdeNotificacion()

        apertura.recibir(Intent())
        assertNull(apertura.pendiente.value)
        apertura.recibir(Intent().putExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION, "ntf-1"))

        assertEquals("ntf-1", apertura.consumir())
        assertNull(apertura.consumir())
    }

    @Test
    fun `la instalacion conserva su identidad`() {
        val primera = IdentidadDeInstalacionEnPreferencias(contexto).id()

        assertTrue(primera.startsWith("inst-"))
        assertEquals(primera, IdentidadDeInstalacionEnPreferencias(contexto).id())
    }

    @Test
    fun `el permiso del sistema se puede leer`() {
        shadowOf(gestor).setNotificationsEnabled(false)
        assertFalse(PermisoDeNotificacionesDelSistema(contexto).concedido())
        shadowOf(gestor).setNotificationsEnabled(true)
        assertTrue(PermisoDeNotificacionesDelSistema(contexto).concedido())
    }

    private suspend fun trabajarCon(resultado: ResultadoDeSincronizacion): ListenableWorker.Result {
        val repositorio = RepositorioFalso().apply { this.resultado = resultado }
        val revisor = RevisorDeNotificaciones(repositorio, AvisadorFalso(), PermisoFalso(), IdiomasFalsos())
        val fabrica = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
                RevisionDeNotificacionesWorker(appContext, workerParameters, revisor)
        }
        return TestListenableWorkerBuilder<RevisionDeNotificacionesWorker>(contexto).setWorkerFactory(fabrica).build().doWork()
    }

    @Test
    fun `el trabajo en segundo plano reintenta sin red y termina en lo demas`() = runTest {
        assertEquals(ListenableWorker.Result.retry(), trabajarCon(ResultadoDeSincronizacion.SinConexion))
        assertEquals(ListenableWorker.Result.success(), trabajarCon(ResultadoDeSincronizacion.Sincronizado(emptyList())))
        assertEquals(ListenableWorker.Result.success(), trabajarCon(ResultadoDeSincronizacion.Error))
    }
}
