package co.solventa.movil.features.notificaciones.centro

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.dao.NotificacionDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El repositorio contra la base real (Robolectric) y un servicio simulado
 * (HU-M3.1-01): la copia es la fuente de la verdad, cada notificación se avisa
 * una sola vez y nunca se mezclan cuentas.
 */
@RunWith(AndroidJUnit4::class)
class NotificacionesRepositorioImplTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var dao: NotificacionDao
    private val api = NotificacionesFalsas()
    private val sesion = SesionFalsa()
    private val conectividad = ConectividadFalsa()
    private var ahora = PAGADO + 60_000

    private val repositorio by lazy {
        NotificacionesRepositorioImpl(api, dao, sesion, conectividad, InstalacionFalsa()) { ahora }
    }

    @Before
    fun antes() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java).allowMainThreadQueries().build()
        dao = base.notificaciones()
    }

    @After
    fun despues() {
        base.close()
    }

    @Test
    fun `la primera sincronizacion guarda el centro sin avisar el historial`() = runTest {
        api.lista = listOf(notificacionDto("ntf-ppo-1"), notificacionDto("ntf-ppo-2", ocurrio = "2026-09-10T08:00:00Z"))

        val resultado = repositorio.sincronizar()

        assertEquals(ResultadoDeSincronizacion.Sincronizado(emptyList()), resultado)
        val copia = repositorio.observar().first()
        assertEquals(listOf("ntf-ppo-1", "ntf-ppo-2"), copia.notificaciones.map { it.id })
        assertEquals(ahora, copia.sincronizadoEn)
        assertFalse(copia.sinConexion)
    }

    @Test
    fun `una notificacion nueva se avisa una sola vez (criterios 1 y 6)`() = runTest {
        api.lista = listOf(notificacionDto("ntf-ppo-1"))
        repositorio.sincronizar()

        api.lista = listOf(notificacionDto("ntf-ppo-2", ocurrio = "2026-09-11T22:00:00Z"), notificacionDto("ntf-ppo-1"))
        val primera = repositorio.sincronizar() as ResultadoDeSincronizacion.Sincronizado
        assertEquals(listOf("ntf-ppo-2"), primera.nuevas.map { it.id })

        repositorio.marcarAvisadas(primera.nuevas.map { it.id })
        val segunda = repositorio.sincronizar() as ResultadoDeSincronizacion.Sincronizado
        assertTrue("ya avisada, no se repite", segunda.nuevas.isEmpty())
    }

    @Test
    fun `varios pagos el mismo dia son todos nuevos y ninguno se pierde (criterio 6)`() = runTest {
        api.lista = listOf(notificacionDto("ntf-viejo", ocurrio = "2026-09-01T10:00:00Z"))
        repositorio.sincronizar()
        api.lista = listOf(
            notificacionDto("ntf-av8421", ocurrio = "2026-09-11T21:40:00Z"),
            notificacionDto("ntf-la4012", ocurrio = "2026-09-11T21:41:00Z", evento = "LA-4012"),
            notificacionDto("ntf-av9310", ocurrio = "2026-09-11T21:42:00Z", evento = "AV-9310"),
        ) + api.lista

        val nuevas = (repositorio.sincronizar() as ResultadoDeSincronizacion.Sincronizado).nuevas

        assertEquals(listOf("ntf-av8421", "ntf-la4012", "ntf-av9310"), nuevas.map { it.id })
        assertEquals(4, repositorio.observar().first().notificaciones.size)
    }

    @Test
    fun `sin red o con el servicio caido se conserva la copia`() = runTest {
        api.lista = listOf(notificacionDto())
        repositorio.sincronizar()

        api.sinRed = true
        conectividad.estado.value = false
        assertEquals(ResultadoDeSincronizacion.SinConexion, repositorio.sincronizar())
        api.sinRed = false
        api.codigo = 503
        assertEquals(ResultadoDeSincronizacion.Error, repositorio.sincronizar())
        api.codigo = 200
        api.lista = listOf(notificacionDto(ocurrio = "nunca"))
        assertEquals(ResultadoDeSincronizacion.Error, repositorio.sincronizar())

        val copia = repositorio.observar().first()
        assertEquals(1, copia.notificaciones.size)
        assertTrue(copia.sinConexion)
    }

    @Test
    fun `leer se ve enseguida y se le avisa al servicio aunque falle la red`() = runTest {
        api.lista = listOf(notificacionDto("ntf-ppo-1"), notificacionDto("ntf-ppo-2"))
        repositorio.sincronizar()

        repositorio.marcarLeida("ntf-ppo-1")
        assertEquals(listOf("ntf-ppo-1"), api.leidas)
        assertTrue(repositorio.observarUna("ntf-ppo-1").first()!!.leida)

        api.sinRed = true
        repositorio.marcarTodasLeidas()
        assertTrue(repositorio.observar().first().notificaciones.all { it.leida })
    }

    @Test
    fun `solo se muestran los tipos que la aplicacion conoce`() = runTest {
        api.lista = listOf(notificacionDto("ntf-ppo-1"), notificacionDto("ntf-otro", tipo = "renewal_due"))

        repositorio.sincronizar()

        assertEquals(listOf("ntf-ppo-1"), repositorio.observar().first().notificaciones.map { it.id })
        assertNull(repositorio.observarUna("ntf-otro").first())
    }

    @Test
    fun `el dispositivo se registra con el permiso y el idioma y no se repite sin cambios (criterio 5)`() = runTest {
        repositorio.registrarDispositivo(permisoConcedido = true, idioma = "es")
        repositorio.registrarDispositivo(permisoConcedido = true, idioma = "es")
        repositorio.registrarDispositivo(permisoConcedido = false, idioma = "es")

        assertEquals(2, api.dispositivos.size)
        val (instalacion, con) = api.dispositivos[0]
        assertEquals("inst-1", instalacion)
        assertEquals("inst-1", con.token)
        assertTrue(con.pushHabilitado)
        val sin = api.dispositivos[1].second
        assertNull("sin permiso no se manda token", sin.token)
        assertFalse(sin.pushHabilitado)
    }

    @Test
    fun `un registro que falla se reintenta en la siguiente revision`() = runTest {
        api.codigoDispositivo = 503
        repositorio.registrarDispositivo(true, "es")
        api.codigoDispositivo = 200
        repositorio.registrarDispositivo(true, "es")

        assertEquals(2, api.dispositivos.size)
    }

    @Test
    fun `nunca mezcla cuentas y sin sesion no hace nada`() = runTest {
        api.lista = listOf(notificacionDto("ntf-a"))
        repositorio.sincronizar()
        sesion.sub = "sub-b"
        api.lista = listOf(notificacionDto("ntf-b"))
        repositorio.sincronizar()
        assertEquals(listOf("ntf-b"), repositorio.observar().first().notificaciones.map { it.id })

        sesion.sub = ""
        assertEquals(ResultadoDeSincronizacion.SinSesion, repositorio.sincronizar())
        assertTrue(repositorio.observar().first().notificaciones.isEmpty())
        assertNull(repositorio.observarUna("ntf-b").first())
        repositorio.marcarLeida("ntf-b")
        repositorio.marcarTodasLeidas()
        repositorio.marcarAvisadas(listOf("ntf-b"))
        repositorio.registrarDispositivo(true, "es")
        assertTrue(api.dispositivos.isEmpty())
    }

    @Test
    fun `al cerrar la sesion no queda ninguna notificacion`() = runTest {
        api.lista = listOf(notificacionDto())
        repositorio.sincronizar()
        repositorio.registrarDispositivo(true, "es")

        repositorio.limpiar()

        assertTrue(repositorio.observar().first().notificaciones.isEmpty())
        assertNull(repositorio.observar().first().sincronizadoEn)
        repositorio.registrarDispositivo(true, "es")
        assertEquals("tras cerrar la sesion se registra otra vez", 2, api.dispositivos.size)
    }
}
