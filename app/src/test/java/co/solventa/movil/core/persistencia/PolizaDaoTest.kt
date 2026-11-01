package co.solventa.movil.core.persistencia

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import co.solventa.movil.core.persistencia.dao.PolizaDao
import co.solventa.movil.core.persistencia.entidades.ConsentimientoEntidad
import co.solventa.movil.core.persistencia.entidades.PolizaEntidad
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
 * La copia local de las pólizas en la base real (Robolectric, HU-M1.2-01): el
 * reemplazo es atómico y conserva el orden, cada propietario ve solo lo suyo y
 * borrarla no toca la sincronización de los consentimientos.
 */
@RunWith(AndroidJUnit4::class)
class PolizaDaoTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var dao: PolizaDao

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

    private fun fila(propietario: String, id: String, orden: Int) = PolizaEntidad(propietario, id, orden, "cifrado-$id")

    @Test
    fun `reemplazar deja solo las filas nuevas en su orden y anota la sincronizacion`() = runTest {
        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "HG-2025-880231", 0)), 1_000L)

        dao.reemplazarDePropietario(
            "sub-a",
            listOf(fila("sub-a", "VH-2026-004512", 1), fila("sub-a", "AU-2025-118204", 0)),
            2_000L,
        )

        assertEquals(listOf("AU-2025-118204", "VH-2026-004512"), dao.observarDePropietario("sub-a").first().map { it.id })
        assertEquals(listOf("AU-2025-118204", "VH-2026-004512"), dao.dePropietario("sub-a").map { it.id })
        assertEquals(2_000L, dao.observarSincronizacion("sub-a").first())
        assertEquals("cifrado-AU-2025-118204", dao.observarUna("sub-a", "AU-2025-118204").first()?.contenidoCifrado)
    }

    @Test
    fun `cada propietario ve solo lo suyo y se borra lo de las otras cuentas`() = runTest {
        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "AU-2025-118204", 0)), 1_000L)
        dao.reemplazarDePropietario("sub-b", listOf(fila("sub-b", "HG-2025-880231", 0)), 1_000L)

        assertNull(dao.observarUna("sub-b", "AU-2025-118204").first())

        dao.borrarDeOtrosPropietarios("sub-a")

        assertTrue(dao.dePropietario("sub-b").isEmpty())
        assertNull(dao.observarSincronizacion("sub-b").first())
        assertEquals(1, dao.dePropietario("sub-a").size)
    }

    @Test
    fun `borrar todo no toca la sincronizacion de los consentimientos`() = runTest {
        val consentimientos = base.consentimientos()
        consentimientos.reemplazarDePropietario("sub-a", listOf(consentimiento()), 5_000L)
        dao.reemplazarDePropietario("sub-a", listOf(fila("sub-a", "AU-2025-118204", 0)), 1_000L)

        dao.borrarTodo()

        assertTrue(dao.dePropietario("sub-a").isEmpty())
        assertNull(dao.observarSincronizacion("sub-a").first())
        assertEquals(5_000L, consentimientos.observarSincronizacion("sub-a", ConsentimientoDao.RECURSO_CONSENTIMIENTOS).first())
    }

    private fun consentimiento() = ConsentimientoEntidad(
        propietario = "sub-a", id = "cns-1", estado = "active", vigente = true,
        finalidad = "risk_profiling_mortgage_life_offer", canal = "embedded", versionDeTerminos = "v1",
        otorgadoEn = 1L, venceEn = 2L, finalizadoEn = null, comprobante = null,
        entidades = "[]", alcance = "[]", historial = "[]",
    )
}
