package co.solventa.movil.core.persistencia

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import co.solventa.movil.core.persistencia.entidades.ConsentimientoEntidad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La copia local de los consentimientos en la base real (Robolectric,
 * HU-M1.3-01): el reemplazo es atomico, cada propietario ve solo lo suyo y el
 * conteo de activos usa la hora que se le da.
 */
@RunWith(AndroidJUnit4::class)
class ConsentimientoDaoTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var dao: ConsentimientoDao

    @Before
    fun antes() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
        dao = base.consentimientos()
    }

    @After
    fun despues() {
        base.close()
    }

    private fun fila(propietario: String, id: String, estado: String = "active", venceEn: Long = VENCE) =
        ConsentimientoEntidad(
            propietario = propietario, id = id, estado = estado, vigente = estado == "active",
            finalidad = "risk_profiling_mortgage_life_offer", canal = "embedded", versionDeTerminos = "v1",
            otorgadoEn = OTORGADO, venceEn = venceEn, finalizadoEn = null, comprobante = null,
            entidades = "[]", alcance = "[]", historial = "[]",
        )

    @Test
    fun `reemplazar deja solo las filas nuevas y anota la sincronizacion`() = runTest {
        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "cns-1"), fila("sub-a", "cns-2")), 1_000L)

        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "cns-3")), 2_000L)

        assertEquals(listOf("cns-3"), dao.observarDePropietario("sub-a").first().map { it.id })
        assertEquals(2_000L, dao.observarSincronizacion("sub-a", ConsentimientoDao.RECURSO_CONSENTIMIENTOS).first())
    }

    @Test
    fun `quien observa nunca ve una lista a medias`() = runTest {
        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "cns-1"), fila("sub-a", "cns-2")), 1_000L)

        dao.observarDePropietario("sub-a").test {
            assertEquals(2, awaitItem().size)
            dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "cns-3"), fila("sub-a", "cns-4")), 2_000L)
            assertEquals(setOf("cns-3", "cns-4"), awaitItem().map { it.id }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cada propietario ve solo lo suyo`() = runTest {
        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "cns-1")), 1_000L)
        dao.reemplazarDePropietario("sub-b", listOf(fila("sub-b", "cns-9")), 1_000L)

        assertEquals(listOf("cns-1"), dao.observarDePropietario("sub-a").first().map { it.id })
        assertEquals(listOf("cns-9"), dao.observarDePropietario("sub-b").first().map { it.id })
        assertNull(dao.observarUno("sub-b", "cns-1").first())
        assertEquals("cns-1", dao.observarUno("sub-a", "cns-1").first()?.id)
    }

    @Test
    fun `borrar de otros propietarios conserva solo la cuenta actual`() = runTest {
        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "cns-1")), 1_000L)
        dao.reemplazarDePropietario("sub-b", listOf(fila("sub-b", "cns-9")), 1_000L)

        dao.borrarDeOtrosPropietarios("sub-b")

        assertTrue(dao.observarDePropietario("sub-a").first().isEmpty())
        assertNull(dao.observarSincronizacion("sub-a", ConsentimientoDao.RECURSO_CONSENTIMIENTOS).first())
        assertEquals(1, dao.observarDePropietario("sub-b").first().size)
    }

    @Test
    fun `borrar todo deja el telefono sin copia`() = runTest {
        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "cns-1")), 1_000L)

        dao.borrarTodo()

        assertTrue(dao.observarDePropietario("sub-a").first().isEmpty())
        assertNull(dao.observarSincronizacion("sub-a", ConsentimientoDao.RECURSO_CONSENTIMIENTOS).first())
    }

    @Test
    fun `contar vigentes usa la hora dada y solo cuenta activos sin vencer`() = runTest {
        dao.reemplazarDePropietario(
            "sub-a",
            listOf(
                fila("sub-a", "cns-1"),
                fila("sub-a", "cns-2", venceEn = VENCE + 10_000L),
                fila("sub-a", "cns-3", estado = "revoked"),
                fila("sub-a", "cns-4", estado = "expired"),
            ),
            1_000L,
        )
        dao.reemplazarDePropietario("sub-b", listOf(fila("sub-b", "cns-9")), 1_000L)

        assertEquals(2, dao.contarVigentes("sub-a", VENCE - 1).first())
        assertEquals("vence justo en venceEn", 1, dao.contarVigentes("sub-a", VENCE).first())
        assertEquals(0, dao.contarVigentes("sub-a", VENCE + 10_000L).first())
    }


    private companion object {
        const val OTORGADO = 1_789_000_000_000L
        const val VENCE = 1_820_000_000_000L
    }
}
