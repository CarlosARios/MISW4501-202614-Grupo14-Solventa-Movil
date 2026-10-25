package co.solventa.movil.features.auditoria.consentimientos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
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
 * (HU-M1.3-01): la copia local es la fuente de la verdad, sirve sin red y
 * nunca mezcla cuentas.
 */
@RunWith(AndroidJUnit4::class)
class ConsentimientosRepositorioImplTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var dao: ConsentimientoDao
    private val api = AuditoriaFalsa()
    private val sesion = SesionFalsa()
    private val conectividad = ConectividadFalsa()
    private val programador = ProgramadorFalso()
    private var ahora = AHORA

    private val repositorio by lazy {
        ConsentimientosRepositorioImpl(api, dao, sesion, conectividad, programador) { ahora }
    }

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

    @Test
    fun `antes de pedir la lista envia la cola de revocaciones pendientes`() = runTest {
        val orden = mutableListOf<String>()
        val conCola = ConsentimientosRepositorioImpl(
            api, dao, sesion, conectividad, programador,
            enviador = { orden += "cola"; ResultadoDeEnvio.Enviadas(1) },
        ) { ahora }
        api.lista = listOf(dto("cns-1"))
        val antes = api.llamadas

        assertEquals(ResultadoDeActualizacion.Actualizado, conCola.actualizar())

        assertEquals(listOf("cola"), orden)
        assertEquals(antes + 1, api.llamadas)
    }

    @Test
    fun `en linea reemplaza la copia y anota cuando se sincronizo`() = runTest {
        api.lista = listOf(dto("cns-1"), dto("cns-2", "revoked", finalizado = "2026-10-01T10:00:00Z", comprobante = "REV-1"))

        val resultado = repositorio.actualizar()

        assertEquals(ResultadoDeActualizacion.Actualizado, resultado)
        val copia = repositorio.observar().first()
        assertEquals(setOf("cns-1", "cns-2"), copia.consentimientos.map { it.id }.toSet())
        assertEquals(AHORA, copia.sincronizadoEn)
        assertFalse(copia.sinConexion)
        assertEquals("REV-1", copia.consentimientos.first { it.id == "cns-2" }.comprobante)
        assertEquals("Banco Aliado", repositorio.observarUno("cns-1").first()?.nombreDeEntidad)
    }

    @Test
    fun `al actualizar borra la copia de otra cuenta que uso el telefono`() = runTest {
        api.lista = listOf(dto("cns-de-b"))
        sesion.sub = "sub-b"
        repositorio.actualizar()

        sesion.sub = "sub-a"
        api.lista = listOf(dto("cns-de-a"))
        repositorio.actualizar()

        assertTrue(dao.observarDePropietario("sub-b").first().isEmpty())
        assertEquals(listOf("cns-de-a"), repositorio.observar().first().consentimientos.map { it.id })
    }

    @Test
    fun `sin red conserva la copia, avisa sin conexion y programa la sincronizacion`() = runTest {
        api.lista = listOf(dto("cns-1"))
        repositorio.actualizar()
        api.sinRed = true
        conectividad.estado.value = false

        val resultado = repositorio.actualizar()

        assertEquals(ResultadoDeActualizacion.SinConexion, resultado)
        val copia = repositorio.observar().first()
        assertEquals(listOf("cns-1"), copia.consentimientos.map { it.id })
        assertTrue(copia.sinConexion)
        assertEquals(AHORA, copia.sincronizadoEn)
        assertEquals(
            listOf(ConsentimientosRepositorioImpl.TRABAJO to SincronizacionDeConsentimientosWorker::class.java),
            programador.programados,
        )
    }

    @Test
    fun `una cuenta sin vincular se informa como tal`() = runTest {
        api.codigo = 403
        api.cuerpoDeError = """{"error":"holder_not_linked","message":"later"}"""

        assertEquals(ResultadoDeActualizacion.CuentaSinVincular, repositorio.actualizar())
    }

    @Test
    fun `un 403 de otro tipo, una sesion vencida o el servicio caido son error y conservan la copia`() = runTest {
        api.lista = listOf(dto("cns-1"))
        repositorio.actualizar()

        for ((codigo, cuerpo) in listOf(
            403 to """{"error":"forbidden","message":"x"}""",
            401 to """{"error":"unauthorized","message":"x"}""",
            503 to """{"error":"service_unavailable","message":"x"}""",
            500 to "no es json",
        )) {
            api.codigo = codigo
            api.cuerpoDeError = cuerpo
            assertEquals("codigo $codigo", ResultadoDeActualizacion.Error, repositorio.actualizar())
        }

        assertEquals(listOf("cns-1"), repositorio.observar().first().consentimientos.map { it.id })
        assertTrue(programador.programados.isEmpty())
    }

    @Test
    fun `sin sub no lee ni escribe nada`() = runTest {
        api.lista = listOf(dto("cns-1"))
        sesion.sub = ""

        assertEquals(ResultadoDeActualizacion.SinSesion, repositorio.actualizar())
        assertEquals(0, api.llamadas)
        assertTrue(repositorio.observar().first().consentimientos.isEmpty())
        assertNull(repositorio.observarUno("cns-1").first())
    }

    @Test
    fun `limpiar al cerrar sesion borra toda la copia`() = runTest {
        api.lista = listOf(dto("cns-1"))
        repositorio.actualizar()

        repositorio.limpiar()

        val copia = repositorio.observar().first()
        assertTrue(copia.consentimientos.isEmpty())
        assertNull(copia.sincronizadoEn)
    }

    @Test
    fun `la conectividad del repositorio es la del observador`() {
        conectividad.estado.value = false
        assertFalse(repositorio.conectado.value)
    }
}
