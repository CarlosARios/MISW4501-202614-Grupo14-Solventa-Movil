package co.solventa.movil.features.siniestros.asistencias

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.api.siniestros.RespuestaDeAsistenciaDto
import co.solventa.movil.core.api.siniestros.SiniestrosApi
import co.solventa.movil.core.api.siniestros.SolicitudDeAsistenciaDto
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.dao.SolicitudDeAsistenciaDao
import co.solventa.movil.core.persistencia.entidades.SolicitudDeAsistenciaEntidad
import co.solventa.movil.features.siniestros.UsuarioActual
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.AHORA
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.compartida
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.poliza
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import retrofit2.Response

/**
 * La cola de solicitudes contra la base real (Robolectric, HU-M2.3-01): la
 * carga queda cifrada (D8), solo se envían las del `sub` con sesión (S9), la
 * vencida no sale (S7), la dirección se resuelve una sola vez (D7) y las
 * resueltas de más de 24 h se borran (D9).
 */
@RunWith(AndroidJUnit4::class)
class AsistenciasRepositorioRoomTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var dao: SolicitudDeAsistenciaDao
    private val api = mockk<SiniestrosApi>()
    private val cifrador = DatosDeAsistencia.cifrador()
    private val traductor = TraductorFijo()
    private val avisador = AvisadorRegistro()
    private val programador = ProgramadorContador()
    private var sub = "sub-a"
    private var ahora = AHORA
    private var claves = 0

    private val repositorio by lazy {
        AsistenciasRepositorioImpl(
            api, dao, cifrador, UsuarioActual { sub }, ConectividadFija(), programador, traductor, avisador,
            reloj = { ahora },
            nuevaClave = { "clave-${++claves}" },
            registrar = {},
        )
    }

    @Before
    fun antes() {
        base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
        dao = base.solicitudesDeAsistencia()
    }

    @After
    fun despues() {
        base.close()
    }

    private fun confirmar(numero: String = "AST-2026-0001") {
        coEvery { api.solicitarAsistencia(any(), any(), any()) } returns Response.success(
            201,
            RespuestaDeAsistenciaDto(
                id = "id-$numero",
                numero = numero,
                estado = "new",
                registradaEn = "2026-11-15T02:38:57Z",
            ),
        )
    }

    @Test
    fun `la fila guarda la carga y el lugar cifrados`() = runTest {
        confirmar()

        val id = repositorio.solicitar(poliza, compartida)!!
        repositorio.enviarPendientes()

        val fila = dao.ultima("sub-a").first()!!
        assertEquals(id, fila.id)
        assertEquals("clave-1", fila.clave)
        assertTrue(fila.geocodificada)
        val enClaro = listOf(fila.cargaCifrada, fila.lugarCifrado.orEmpty()).joinToString()
        listOf("4.7712", "74.0452", "AU-2025-118204", "KJT-482", "Autopista").forEach {
            assertFalse("la fila no debe contener $it en claro", enClaro.contains(it))
        }
        assertEquals("Autopista Norte km 12, Bogotá", repositorio.observarUltima().first()!!.lugar)
        assertEquals(1, programador.veces)
    }

    @Test
    fun `solo se envian las del sub con sesion`() = runTest {
        confirmar()
        sub = "sub-b"
        repositorio.solicitar(poliza, compartida)
        sub = "sub-a"

        assertNull(repositorio.observarUltima().first())
        assertEquals(ResultadoDelEnvio.COMPLETO, repositorio.enviarPendientes())
        coVerify(exactly = 0) { api.solicitarAsistencia(any(), any(), any()) }

        sub = "sub-b"
        repositorio.enviarPendientes()
        coVerify(exactly = 1) { api.solicitarAsistencia(any(), any(), any()) }
    }

    @Test
    fun `la que vence no se envia y se avisa`() = runTest {
        confirmar()
        repositorio.solicitar(poliza, compartida)
        ahora += ReglasDeAsistencia.VENCIMIENTO_MS

        assertEquals(ResultadoDelEnvio.COMPLETO, repositorio.enviarPendientes())

        coVerify(exactly = 0) { api.solicitarAsistencia(any(), any(), any()) }
        assertEquals(EstadoDeEnvio.Descartada(MotivoDeDescarte.VENCIDA), repositorio.observarUltima().first()!!.envio)
        assertEquals(listOf(MotivoDeDescarte.VENCIDA), avisador.avisos)
    }

    @Test
    fun `el lugar se resuelve una vez y se reusa en los reintentos`() = runTest {
        val cuerpos = mutableListOf<SolicitudDeAsistenciaDto>()
        val cuerpo = slot<SolicitudDeAsistenciaDto>()
        coEvery { api.solicitarAsistencia(any(), any(), capture(cuerpo)) } answers {
            cuerpos += cuerpo.captured
            Response.error(503, okhttp3.ResponseBody.Companion.create(null, "{}"))
        }
        repositorio.solicitar(poliza, compartida)

        repositorio.enviarPendientes()
        repositorio.enviarPendientes()

        assertEquals(1, traductor.llamadas)
        assertEquals(2, cuerpos.size)
        assertEquals(cuerpos[0], cuerpos[1])
        assertEquals("Autopista Norte km 12, Bogotá", cuerpos[1].lugar)
        assertEquals(2, dao.ultima("sub-a").first()!!.intentos)
    }

    @Test
    fun `sin ubicacion compartida no se geocodifica y si falla el geocodificador sale sin lugar`() = runTest {
        confirmar()
        repositorio.solicitar(poliza, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA))
        repositorio.enviarPendientes()
        assertEquals(0, traductor.llamadas)

        traductor.falla = IllegalStateException("sin geocodificador")
        repositorio.solicitar(poliza, compartida)
        repositorio.enviarPendientes()

        assertEquals(1, traductor.llamadas)
        assertNull(repositorio.observarUltima().first()!!.lugar)
        assertEquals(EstadoDeEnvio.Confirmada, repositorio.observarUltima().first()!!.envio)
    }

    @Test
    fun `una carga que no se puede descifrar se descarta`() = runTest {
        confirmar()
        dao.insertar(
            SolicitudDeAsistenciaEntidad(
                propietario = "sub-a",
                clave = "otra",
                cargaCifrada = "no-es-cifrado",
                estadoDeEnvio = SolicitudDeAsistenciaEntidad.EN_COLA,
                creadaEn = AHORA,
            ),
        )

        assertEquals(ResultadoDelEnvio.COMPLETO, repositorio.enviarPendientes())

        assertEquals(SolicitudDeAsistenciaEntidad.DESCARTADA, dao.ultima("sub-a").first()!!.estadoDeEnvio)
        assertNull(repositorio.observarUltima().first())
    }

    @Test
    fun `las resueltas de mas de 24 h se borran y las pendientes no`() = runTest {
        confirmar()
        repositorio.solicitar(poliza, compartida)
        repositorio.enviarPendientes()
        ahora += ReglasDeAsistencia.CONSERVACION_MS - 1
        repositorio.enviarPendientes()
        assertEquals(EstadoDeEnvio.Confirmada, repositorio.observarUltima().first()!!.envio)

        // Una nueva queda en la cola sin red: no se borra con la limpieza.
        coEvery { api.solicitarAsistencia(any(), any(), any()) } throws java.io.IOException("sin red")
        val pendiente = repositorio.solicitar(poliza, compartida)!!
        ahora += 2
        repositorio.enviarPendientes()

        val ultima = repositorio.observarUltima().first()!!
        assertEquals(pendiente, ultima.id)
        assertEquals(1, dao.pendientes("sub-a").size)
        assertNull(dao.porRequestId("sub-a", "id-AST-2026-0001"))
    }

    @Test
    fun `reanudar programa el envio solo si hay pendientes`() = runTest {
        repositorio.reanudarPendientes()
        assertEquals(0, programador.veces)

        coEvery { api.solicitarAsistencia(any(), any(), any()) } throws java.io.IOException("sin red")
        repositorio.solicitar(poliza, compartida)
        repositorio.reanudarPendientes()

        assertEquals(2, programador.veces)
    }
}
