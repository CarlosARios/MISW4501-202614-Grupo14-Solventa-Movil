package co.solventa.movil.features.siniestros.seguimiento

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.cifrado.CifradorAesGcm
import co.solventa.movil.core.persistencia.dao.SiniestroGuardadoDao
import co.solventa.movil.core.persistencia.entidades.SiniestroGuardadoEntidad
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.KeyGenerator

/**
 * La copia sin conexión de los siniestros contra la base real (Robolectric)
 * (HU-M2.4-01, criterio 5): cifrada, por cuenta y borrada al cerrar la sesión.
 */
@RunWith(AndroidJUnit4::class)
class CopiaLocalDeSiniestrosRoomTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var dao: SiniestroGuardadoDao
    private val clave = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val sesion = SesionDePrueba()

    private val copia by lazy { CopiaLocalDeSiniestrosRoom(dao, CifradorAesGcm { clave }, sesion) }

    @Before
    fun antes() {
        base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
        dao = base.siniestrosGuardados()
    }

    @After
    fun despues() {
        base.close()
    }

    @Test
    fun `guarda y lee la lista y el detalle con la hora de sus datos`() = runTest {
        val detalle = DetalleDeSiniestro(
            parametrico(),
            listOf(CambioDeEstado(EstadoDeSiniestro.PAGADO, AHORA - HORA)),
            EstimacionDeResolucion(2, 5),
        )

        copia.guardarLista(listOf(siniestro(), parametrico()), AHORA - HORA)
        copia.guardarDetalle(detalle, AHORA - 2 * HORA)

        assertEquals(CopiaGuardada(listOf(siniestro(), parametrico()), AHORA - HORA), copia.lista())
        assertEquals(CopiaGuardada(detalle, AHORA - 2 * HORA), copia.detalle(parametrico().id))
        assertNull(copia.detalle("otro"))
    }

    @Test
    fun `la fila queda cifrada en la base`() = runTest {
        copia.guardarDetalle(DetalleDeSiniestro(parametrico(), emptyList()), AHORA)

        val fila = dao.leer("sub-a", CopiaLocalDeSiniestrosRoom.claveDeDetalle(parametrico().id))!!
        listOf("SIN-2026-01702", "VJ-2026-000931", "AV-8421", "flight_delay").forEach {
            assertFalse("la fila no debe contener $it", fila.contenidoCifrado.contains(it))
        }
    }

    @Test
    fun `otra cuenta no ve la copia y al guardar se borra la de la anterior`() = runTest {
        copia.guardarLista(listOf(siniestro()), AHORA)

        sesion.sub = "sub-b"
        assertNull(copia.lista())

        copia.guardarLista(emptyList(), AHORA)
        sesion.sub = "sub-a"
        assertNull(copia.lista())
    }

    @Test
    fun `sin sesion no se guarda ni se lee`() = runTest {
        sesion.sub = null
        copia.guardarLista(listOf(siniestro()), AHORA)

        assertNull(copia.lista())
        assertNull(dao.leer("", CopiaLocalDeSiniestrosRoom.CLAVE_LISTA))
    }

    @Test
    fun `una fila que no se puede descifrar es como no tener copia`() = runTest {
        dao.guardar(SiniestroGuardadoEntidad("sub-a", CopiaLocalDeSiniestrosRoom.CLAVE_LISTA, "basura", AHORA))

        assertNull(copia.lista())
    }

    @Test
    fun `al cerrar la sesion no queda ningun siniestro`() = runTest {
        copia.guardarLista(listOf(siniestro()), AHORA)

        copia.limpiar()

        assertNull(copia.lista())
    }

    private class SesionDePrueba(var sub: String? = "sub-a") : SesionRepositorio {
        override suspend fun sesionGuardada(): Sesion? = sub?.let { Sesion("asegurado@example.com", "Carolina", it) }
        override suspend fun biometriaHabilitada(): Boolean = false
        override suspend fun desbloquear(): Sesion? = sesionGuardada()
        override suspend fun ingresarConContrasena(correo: String, contrasena: String): ResultadoIngreso =
            error("no se usa")
        override suspend fun habilitarBiometria(habilitada: Boolean) = Unit
        override suspend fun cerrar() = Unit
    }
}
