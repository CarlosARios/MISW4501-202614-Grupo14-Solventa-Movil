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

/**
 * La notificación de cambio de etapa con las piezas de Android (Robolectric,
 * HU-M3.3-01): la notificación del sistema, la apertura que lleva a la línea
 * de tiempo y la copia en la base real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CambioDeEtapaAndroidTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val gestor = contexto.getSystemService(NotificationManager::class.java)
    private lateinit var base: BaseDeDatosSolventa
    private val api = NotificacionesFalsas()

    private val repositorio by lazy {
        NotificacionesRepositorioImpl(api, base.notificaciones(), SesionFalsa(), ConectividadFalsa(), InstalacionFalsa()) {
            PAGADO + 60_000
        }
    }

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java).allowMainThreadQueries().build()
    }

    @After
    fun despues() {
        base.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `cada cambio es su notificacion del sistema, en su canal, con la etapa, el monto y el siniestro que abre (criterios 2, 4 y 5)`() {
        val aprobado = MapeadorDeNotificaciones.desdeDto(etapaDto())

        val mostro = AvisadorDelSistema(contexto, PermisoFalso(true)).avisar(listOf(aprobado), Idioma.ESPANOL)

        assertTrue(mostro)
        val aviso = shadowOf(gestor).getNotification("ntf-stage-42", 3201)
        assertNotNull(aviso)
        assertEquals("Tu siniestro fue aprobado: $ 4.132.500 COP", aviso.extras.getCharSequence("android.title").toString())
        assertTrue(aviso.extras.getCharSequence("android.text").toString().startsWith("SIN-2026-01655 fue aprobado el "))
        assertEquals(AvisadorDelSistema.GRUPO_ETAPAS, aviso.group)
        assertNotNull(gestor.getNotificationChannel(AvisadorDelSistema.CANAL_ETAPAS))
        val intent = shadowOf(aviso.contentIntent).savedIntent
        assertEquals("ntf-stage-42", intent.getStringExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION))
        assertEquals("8f14e45f-ceea-467a-9f3a-5a2d0b1c7e90", intent.getStringExtra(AperturaDesdeNotificacion.EXTRA_SINIESTRO))
    }

    @Test
    fun `en ingles y sin monto, la de evaluacion`() {
        val evaluacion = MapeadorDeNotificaciones.desdeDto(etapaDto(etapa = "in_evaluation", monto = null))

        AvisadorDelSistema(contexto, PermisoFalso(true)).avisar(listOf(evaluacion), Idioma.INGLES)

        val aviso = shadowOf(gestor).getNotification("ntf-stage-42", 3201)
        assertEquals("Your claim is under evaluation", aviso.extras.getCharSequence("android.title").toString())
    }

    @Test
    fun `tocar la notificacion del sistema abre la linea de tiempo del siniestro`() {
        val apertura = AperturaDesdeNotificacion()

        apertura.recibir(
            Intent()
                .putExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION, "ntf-stage-42")
                .putExtra(AperturaDesdeNotificacion.EXTRA_SINIESTRO, "cl-1"),
        )

        assertEquals(DestinoDeNotificacion.Siniestro("cl-1", "ntf-stage-42"), apertura.consumir())
    }

    @Test
    fun `el centro guarda y muestra el cambio de etapa junto a los pagos, y uno nuevo se avisa (criterio 4)`() = runTest {
        api.lista = listOf(notificacionDto("ntf-ppo-1"))
        repositorio.sincronizar()

        api.lista = listOf(etapaDto(), notificacionDto("ntf-ppo-1"))
        val resultado = repositorio.sincronizar()

        assertEquals(listOf("ntf-stage-42"), (resultado as ResultadoDeSincronizacion.Sincronizado).nuevas.map { it.id })
        val copia = repositorio.observar().first()
        assertEquals(setOf("ntf-stage-42", "ntf-ppo-1"), copia.notificaciones.map { it.id }.toSet())
        assertEquals("SIN-2026-01655", copia.notificaciones.first { it.id == "ntf-stage-42" }.etapa?.radicado)
    }

    @Test
    fun `un cambio de etapa sin su etapa no se muestra`() = runTest {
        api.lista = listOf(etapaDto().copy(etapa = null))

        repositorio.sincronizar()

        assertTrue(repositorio.observar().first().notificaciones.isEmpty())
    }

    @Test
    fun `el cambio de etapa tocado en el sistema se entrega una vez y queda leido`() {
        val repositorio = RepositorioFalso()
        val revisor = RevisorDeNotificaciones(repositorio, AvisadorFalso(), PermisoFalso(), IdiomasFalsos())
        val apertura = AperturaDesdeNotificacion()
        val vm = SondeoDeNotificacionesViewModel(revisor, ProgramadorFalso(), apertura, repositorio, 20_000)
        apertura.recibir(
            Intent()
                .putExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION, "ntf-stage-42")
                .putExtra(AperturaDesdeNotificacion.EXTRA_SINIESTRO, "cl-1"),
        )

        assertEquals(DestinoDeNotificacion.Siniestro("cl-1", "ntf-stage-42"), vm.abrirPendiente())

        assertEquals(listOf("ntf-stage-42"), repositorio.leidas)
        assertNull(vm.abrirPendiente())
    }
}
