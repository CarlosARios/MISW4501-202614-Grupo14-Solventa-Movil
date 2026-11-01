package co.solventa.movil.features.originacion.polizas

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.dao.PolizaDao
import co.solventa.movil.core.persistencia.entidades.PolizaEntidad
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
 * (HU-M1.2-01): la copia cifrada es la fuente de la verdad, sirve sin red,
 * no se pierde con una lista parcial y avisa los cambios.
 */
@RunWith(AndroidJUnit4::class)
class PolizasRepositorioImplTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var dao: PolizaDao
    private val api = OriginacionFalsa()
    private val cifrador = cifradorDePrueba()
    private val sesion = SesionFalsa()
    private val conectividad = ConectividadFalsa()
    private val programador = ProgramadorFalso()
    private var ahora = AHORA

    private val repositorio by lazy {
        PolizasRepositorioImpl(api, dao, cifrador, sesion, conectividad, programador) { ahora }
    }

    @Before
    fun antes() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
        dao = base.polizas()
    }

    @After
    fun despues() {
        base.close()
    }

    @Test
    fun `en linea reemplaza la copia y anota cuando se sincronizo`() = runTest {
        api.lista = listOf(polizaDto(), polizaDto(id = "HG-2025-880231", linea = "home"))

        val resultado = repositorio.sincronizar()

        assertEquals(ResultadoDeSincronizacion.Sincronizado(emptyList()), resultado)
        val copia = repositorio.observar().first()
        assertEquals(listOf("AU-2025-118204", "HG-2025-880231"), copia.polizas.map { it.id })
        assertEquals(AHORA, copia.sincronizadoEn)
        assertFalse(copia.sinConexion)
    }

    @Test
    fun `la copia queda cifrada en la base (criterio 6)`() = runTest {
        api.lista = listOf(polizaDto())

        repositorio.sincronizar()

        val fila = dao.dePropietario("sub-a").single()
        assertEquals("AU-2025-118204", fila.id)
        listOf("1020304050", "Carolina", "KJT-482", "72500000", "collision").forEach {
            assertFalse("la fila no debe contener $it", fila.contenidoCifrado.contains(it))
        }
    }

    @Test
    fun `sin red conserva la copia y programa la sincronizacion (criterio 3)`() = runTest {
        api.lista = listOf(polizaDto())
        repositorio.sincronizar()
        api.sinRed = true
        conectividad.estado.value = false

        val resultado = repositorio.sincronizar()

        assertEquals(ResultadoDeSincronizacion.SinConexion, resultado)
        assertEquals(listOf(PolizasRepositorioImpl.TRABAJO to SincronizacionDePolizasWorker::class.java), programador.programados)
        val copia = repositorio.observar().first()
        assertEquals(listOf("AU-2025-118204"), copia.polizas.map { it.id })
        assertTrue(copia.sinConexion)
        assertEquals(AHORA, copia.sincronizadoEn)
    }

    @Test
    fun `una lista parcial no reemplaza la copia`() = runTest {
        api.lista = listOf(polizaDto(), polizaDto(id = "HG-2025-880231", linea = "home"))
        repositorio.sincronizar()
        api.lista = listOf(polizaDto())
        api.parcial = true
        ahora += DIA

        assertEquals(ResultadoDeSincronizacion.Error, repositorio.sincronizar())

        val copia = repositorio.observar().first()
        assertEquals(2, copia.polizas.size)
        assertEquals(AHORA, copia.sincronizadoEn)
        assertTrue(repositorio.cambios.value.isEmpty())
    }

    @Test
    fun `un error del servicio o un cuerpo ilegible no borran la copia`() = runTest {
        api.lista = listOf(polizaDto())
        repositorio.sincronizar()

        api.codigo = 503
        assertEquals(ResultadoDeSincronizacion.Error, repositorio.sincronizar())
        api.codigo = 200
        api.lista = listOf(polizaDto(fin = "pronto"))
        assertEquals(ResultadoDeSincronizacion.Error, repositorio.sincronizar())

        assertEquals(listOf("AU-2025-118204"), repositorio.observar().first().polizas.map { it.id })
    }

    @Test
    fun `detecta y conserva los cambios hasta que se descartan (criterio 4)`() = runTest {
        api.lista = listOf(polizaDto(), polizaDto(id = "HG-2025-880231", linea = "home"))
        repositorio.sincronizar()
        assertTrue("la primera sincronizacion no avisa", repositorio.cambios.value.isEmpty())

        api.lista = listOf(
            polizaDto(fin = "2027-10-02T00:00:00Z"),
            polizaDto(id = "HG-2025-880231", linea = "home", estado = "cancelled"),
        )
        val resultado = repositorio.sincronizar() as ResultadoDeSincronizacion.Sincronizado

        assertEquals(listOf(TipoDeCambio.RENOVADA, TipoDeCambio.CANCELADA), resultado.cambios.map { it.tipo })
        assertEquals(resultado.cambios, repositorio.cambios.value)

        // Una sincronización sin novedades no borra el aviso pendiente.
        repositorio.sincronizar()
        assertEquals(2, repositorio.cambios.value.size)

        repositorio.descartarCambios()
        assertTrue(repositorio.cambios.value.isEmpty())
    }

    @Test
    fun `una fila que no se puede descifrar no se muestra`() = runTest {
        api.lista = listOf(polizaDto())
        repositorio.sincronizar()
        dao.reemplazarDePropietario("sub-a", listOf(PolizaEntidad("sub-a", "X", 1, "basura")) + dao.dePropietario("sub-a"), AHORA)

        assertEquals(listOf("AU-2025-118204"), repositorio.observar().first().polizas.map { it.id })
        assertNull(repositorio.observarUna("X").first())
        assertEquals("AU-2025-118204", repositorio.observarUna("AU-2025-118204").first()?.id)
    }

    @Test
    fun `nunca mezcla cuentas`() = runTest {
        api.lista = listOf(polizaDto())
        repositorio.sincronizar()

        sesion.sub = "sub-b"
        api.lista = listOf(polizaDto(id = "HG-2025-880231", linea = "home"))
        repositorio.sincronizar()

        assertEquals(listOf("HG-2025-880231"), repositorio.observar().first().polizas.map { it.id })
        assertTrue("se borró lo de la otra cuenta", dao.dePropietario("sub-a").isEmpty())
    }

    @Test
    fun `sin sesion no lee ni escribe`() = runTest {
        sesion.sub = ""

        assertEquals(ResultadoDeSincronizacion.SinSesion, repositorio.sincronizar())
        assertEquals(0, api.llamadas)
        assertTrue(repositorio.observar().first().polizas.isEmpty())
        assertNull(repositorio.observarUna("AU-2025-118204").first())
    }

    @Test
    fun `al cerrar la sesion no queda ninguna poliza ni aviso`() = runTest {
        api.lista = listOf(polizaDto())
        repositorio.sincronizar()
        api.lista = listOf(polizaDto(estado = "cancelled"))
        repositorio.sincronizar()

        repositorio.limpiar()

        assertTrue(dao.dePropietario("sub-a").isEmpty())
        assertTrue(repositorio.cambios.value.isEmpty())
        assertNull(repositorio.observar().first().sincronizadoEn)
    }
}
