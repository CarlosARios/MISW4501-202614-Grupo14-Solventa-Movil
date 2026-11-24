package co.solventa.movil.features.notificaciones.centro

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.ZoneId
import java.util.TimeZone

/**
 * La alerta de vencimiento con las piezas de Android (Robolectric,
 * HU-M3.2-01): la notificación del sistema con la fecha exacta, la apertura que
 * lleva al detalle de la póliza y la copia en la base real (criterios 1 y 4).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VencimientoDePolizaAndroidTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val gestor = contexto.getSystemService(NotificationManager::class.java)
    private lateinit var base: BaseDeDatosSolventa
    private val api = NotificacionesFalsas()
    private val zonaOriginal = TimeZone.getDefault()

    private val repositorio by lazy {
        NotificacionesRepositorioImpl(api, base.notificaciones(), SesionFalsa(), ConectividadFalsa(), InstalacionFalsa()) {
            PAGADO + 60_000
        }
    }

    @Before
    fun antes() {
        // Las fechas se escriben en la zona del dispositivo: la del asegurado en Colombia.
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("America/Bogota")))
        Dispatchers.setMain(UnconfinedTestDispatcher())
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java).allowMainThreadQueries().build()
    }

    @After
    fun despues() {
        base.close()
        Dispatchers.resetMain()
        TimeZone.setDefault(zonaOriginal)
    }

    @Test
    fun `cada alerta es su notificacion del sistema, en su canal, con los dias, la fecha exacta y la poliza que abre (criterios 1 y 4)`() {
        val alerta = MapeadorDeNotificaciones.desdeDto(vencimientoDto())

        val mostro = AvisadorDelSistema(contexto, PermisoFalso(true)).avisar(listOf(alerta), Idioma.ESPANOL)

        assertTrue(mostro)
        val aviso = shadowOf(gestor).getNotification(alerta.id, 3301)
        assertNotNull(aviso)
        assertEquals("Tu póliza vence en 15 días", aviso.extras.getCharSequence("android.title").toString())
        assertEquals(
            "La póliza AU-2025-118204 (Autos · KJT-482) vence el 26/09/2026. Decide a tiempo si la renuevas o ajustas tu cobertura.",
            aviso.extras.getCharSequence("android.text").toString(),
        )
        assertEquals(AvisadorDelSistema.GRUPO_VENCIMIENTOS, aviso.group)
        assertNotNull(gestor.getNotificationChannel(AvisadorDelSistema.CANAL_VENCIMIENTOS))
        val intent = shadowOf(aviso.contentIntent).savedIntent
        assertEquals(alerta.id, intent.getStringExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION))
        assertEquals("AU-2025-118204", intent.getStringExtra(AperturaDesdeNotificacion.EXTRA_POLIZA))
        assertNull(intent.getStringExtra(AperturaDesdeNotificacion.EXTRA_SINIESTRO))
    }

    @Test
    fun `en ingles, con un dia y sin producto, nombra el tipo de seguro`() {
        val alerta = MapeadorDeNotificaciones.desdeDto(vencimientoDto(hito = 5, dias = 1, producto = null))

        AvisadorDelSistema(contexto, PermisoFalso(true)).avisar(listOf(alerta), Idioma.INGLES)

        val aviso = shadowOf(gestor).getNotification(alerta.id, 3301)
        assertEquals("Your policy expires in 1 day", aviso.extras.getCharSequence("android.title").toString())
        assertEquals(
            "Policy AU-2025-118204 (Auto · KJT-482) expires on 09/26/2026. Decide in time whether to renew it or adjust your coverage.",
            aviso.extras.getCharSequence("android.text").toString(),
        )
    }

    @Test
    fun `el dia del vencimiento dice que vence hoy`() {
        val alerta = MapeadorDeNotificaciones.desdeDto(vencimientoDto(hito = 5, dias = 0))

        AvisadorDelSistema(contexto, PermisoFalso(true)).avisar(listOf(alerta), Idioma.ESPANOL)

        val aviso = shadowOf(gestor).getNotification(alerta.id, 3301)
        assertEquals("Tu póliza vence hoy", aviso.extras.getCharSequence("android.title").toString())
    }

    @Test
    fun `sin permiso no se muestra nada (criterio 4)`() {
        val alerta = MapeadorDeNotificaciones.desdeDto(vencimientoDto())

        val mostro = AvisadorDelSistema(contexto, PermisoFalso(false)).avisar(listOf(alerta), Idioma.ESPANOL)

        assertEquals(false, mostro)
        assertNull(shadowOf(gestor).getNotification(alerta.id, 3301))
    }

    @Test
    fun `tocar la notificacion del sistema abre el detalle de la poliza`() {
        val apertura = AperturaDesdeNotificacion()

        apertura.recibir(
            Intent()
                .putExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION, "ntf-expiry-1")
                .putExtra(AperturaDesdeNotificacion.EXTRA_POLIZA, "AU-2025-118204"),
        )

        assertEquals(DestinoDeNotificacion.Poliza("AU-2025-118204", "ntf-expiry-1"), apertura.consumir())
    }

    @Test
    fun `el centro guarda la alerta junto a las demas, y una nueva se avisa al reingresar (criterio 4)`() = runTest {
        api.lista = listOf(notificacionDto("ntf-ppo-1"))
        repositorio.sincronizar()

        api.lista = listOf(vencimientoDto(), notificacionDto("ntf-ppo-1"))
        val resultado = repositorio.sincronizar()

        val id = "ntf-expiry-AU-2025-118204-20260926-15"
        assertEquals(listOf(id), (resultado as ResultadoDeSincronizacion.Sincronizado).nuevas.map { it.id })
        val copia = repositorio.observar().first()
        assertEquals(setOf(id, "ntf-ppo-1"), copia.notificaciones.map { it.id }.toSet())
        assertEquals("AU-2025-118204", copia.notificaciones.first { it.id == id }.vencimiento?.poliza)
    }

    @Test
    fun `una alerta sin su vencimiento no se muestra`() = runTest {
        api.lista = listOf(vencimientoDto().copy(vencimiento = null))

        repositorio.sincronizar()

        assertTrue(repositorio.observar().first().notificaciones.isEmpty())
    }

    @Test
    fun `la alerta tocada en el sistema se entrega una vez y queda leida`() {
        val repositorio = RepositorioFalso()
        val revisor = RevisorDeNotificaciones(repositorio, AvisadorFalso(), PermisoFalso(), IdiomasFalsos())
        val apertura = AperturaDesdeNotificacion()
        val vm = SondeoDeNotificacionesViewModel(revisor, ProgramadorFalso(), apertura, repositorio, 20_000)
        apertura.recibir(
            Intent()
                .putExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION, "ntf-expiry-1")
                .putExtra(AperturaDesdeNotificacion.EXTRA_POLIZA, "AU-2025-118204"),
        )

        assertEquals(DestinoDeNotificacion.Poliza("AU-2025-118204", "ntf-expiry-1"), vm.abrirPendiente())

        assertEquals(listOf("ntf-expiry-1"), repositorio.leidas)
        assertNull(vm.abrirPendiente())
    }
}
