package co.solventa.movil.core.persistencia

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.persistencia.dao.RevocacionPendienteDao
import co.solventa.movil.core.persistencia.entidades.RevocacionPendienteEntidad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La cola de revocaciones pendientes en la base real (Robolectric,
 * HU-M1.3-02): una por consentimiento, separada por propietario, con sus
 * intentos, y que se vacia al enviarse.
 */
@RunWith(AndroidJUnit4::class)
class RevocacionPendienteDaoTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var dao: RevocacionPendienteDao

    @Before
    fun antes() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
        dao = base.revocacionesPendientes()
    }

    @After
    fun despues() {
        base.close()
    }

    private fun fila(propietario: String, id: String, clave: String = "clave-$propietario-$id", en: Long = 1_000L) =
        RevocacionPendienteEntidad(
            propietario = propietario, consentimientoId = id, clave = clave, entidad = "Banco Aliado",
            solicitadaEn = en, zona = "America/Bogota", dispositivo = "Google Pixel 8", metodo = "biometric",
        )

    @Test
    fun `encolar guarda la revocacion y la primera de un consentimiento se conserva`() = runTest {
        assertEquals(1L, dao.encolar(fila("sub-a", "cns-1", clave = "k1", en = 1_000L)))

        val segunda = dao.encolar(fila("sub-a", "cns-1", clave = "k2", en = 2_000L))

        assertEquals(-1L, segunda)
        val guardada = dao.una("sub-a", "cns-1")!!
        assertEquals("k1", guardada.clave)
        assertEquals(1_000L, guardada.solicitadaEn)
        assertEquals("America/Bogota", guardada.zona)
        assertEquals(0, guardada.intentos)
        assertNull(guardada.ultimoError)
    }

    @Test
    fun `cada propietario ve solo su cola, en el orden en que se confirmo`() = runTest {
        dao.encolar(fila("sub-a", "cns-2", en = 2_000L))
        dao.encolar(fila("sub-a", "cns-1", en = 1_000L))
        dao.encolar(fila("sub-b", "cns-9"))

        assertEquals(listOf("cns-1", "cns-2"), dao.dePropietario("sub-a").map { it.consentimientoId })
        assertEquals(listOf("cns-1", "cns-2"), dao.observarDePropietario("sub-a").first().map { it.consentimientoId })
        assertEquals(listOf("cns-9"), dao.dePropietario("sub-b").map { it.consentimientoId })
        assertEquals(2, dao.contar("sub-a"))
        assertNull(dao.una("sub-b", "cns-1"))
    }

    @Test
    fun `un intento fallido suma y guarda la causa y la enviada se borra`() = runTest {
        dao.encolar(fila("sub-a", "cns-1"))

        dao.registrarIntento("sub-a", "cns-1", "503")
        dao.registrarIntento("sub-a", "cns-1", "sin red")

        val conIntentos = dao.una("sub-a", "cns-1")!!
        assertEquals(2, conIntentos.intentos)
        assertEquals("sin red", conIntentos.ultimoError)

        dao.borrar("sub-a", "cns-1")
        assertNull(dao.una("sub-a", "cns-1"))
        assertEquals(0, dao.contar("sub-a"))
    }

    @Test
    fun `la clave de idempotencia no se repite entre consentimientos`() = runTest {
        dao.encolar(fila("sub-a", "cns-1", clave = "misma"))

        assertEquals(-1L, dao.encolar(fila("sub-a", "cns-2", clave = "misma")))
        assertNull(dao.una("sub-a", "cns-2"))
    }
}
