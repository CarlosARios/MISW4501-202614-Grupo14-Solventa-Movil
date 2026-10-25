package co.solventa.movil.features.auditoria.consentimientos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import co.solventa.movil.core.persistencia.dao.RevocacionPendienteDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId

/**
 * La revocacion contra la base real (Robolectric) y un servicio simulado
 * (HU-M1.3-02): primero la cola en el telefono, luego el envio; sin red o sin
 * respuesta en 4 s queda pendiente con la misma clave y la hora de la
 * confirmacion, y se envia sola despues.
 */
@RunWith(AndroidJUnit4::class)
class RevocacionRepositorioImplTest {

    private lateinit var base: BaseDeDatosSolventa
    private lateinit var cola: RevocacionPendienteDao
    private lateinit var copia: ConsentimientoDao
    private val api = AuditoriaFalsa()
    private val sesion = SesionFalsa()
    private val conectividad = ConectividadFalsa()
    private val programador = ProgramadorDeEnvioFalso()
    private var ahora = CONFIRMADA
    private var claves = 0

    private val repositorio by lazy {
        RevocacionRepositorioImpl(
            api, cola, copia, sesion, conectividad, programador,
            reloj = { ahora },
            zona = { ZoneId.of("America/Bogota") },
            dispositivo = { "Google Pixel 8" },
            nuevaClave = { "clave-${++claves}" },
        )
    }

    @Before
    fun antes() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
        cola = base.revocacionesPendientes()
        copia = base.consentimientos()
    }

    @After
    fun despues() {
        base.close()
    }

    private suspend fun conCopia(vararg consentimientos: Consentimiento) {
        copia.reemplazarDePropietario(
            sesion.sub,
            consentimientos.map { MapeadorDeConsentimientos.aEntidad(it, sesion.sub) },
            AHORA,
        )
    }

    private suspend fun enCopia(id: String): Consentimiento? =
        copia.uno(sesion.sub, id)?.let(MapeadorDeConsentimientos::desdeEntidad)

    @Test
    fun `en linea envia con la clave y la hora de la confirmacion, borra la fila y deja la copia revocada`() = runTest {
        conCopia(consentimiento("cns-1"))

        val resultado = repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)

        assertTrue(resultado is ResultadoDeRevocacion.Revocado)
        assertEquals("REV-2026-0088", (resultado as ResultadoDeRevocacion.Revocado).revocacion.comprobante)
        val (id, clave, solicitud) = api.revocaciones.single()
        assertEquals("cns-1", id)
        assertEquals("clave-1", clave)
        assertEquals("2026-10-24T14:59:58-05:00", solicitud.solicitadaEn)
        assertEquals("Google Pixel 8", solicitud.dispositivo)
        assertEquals("biometric", solicitud.metodoDeConfirmacion)
        assertEquals(0, cola.contar(sesion.sub))
        val revocado = enCopia("cns-1")!!
        assertEquals(EstadosDeConsentimiento.REVOCADO, revocado.estado)
        assertFalse(revocado.revocable)
        assertEquals("REV-2026-0088", revocado.comprobante)
        assertNotNull(revocado.revocacion)
        assertEquals(
            listOf(CambiosDeEstado.OTORGADO, CambiosDeEstado.REVOCACION_SOLICITADA, CambiosDeEstado.REVOCADO),
            revocado.historial.map { it.estado },
        )
        assertTrue(programador.programados == 0)
    }

    @Test
    fun `la fila se escribe antes de enviar`() = runTest {
        conCopia(consentimiento("cns-1"))
        var filasDuranteElEnvio = -1
        // Sin suspender: la espera de 4 s corre en tiempo virtual y una consulta
        // suspendida en otro hilo la haria vencer.
        api.alRevocar = { filasDuranteElEnvio = kotlinx.coroutines.runBlocking { cola.contar(sesion.sub) } }

        repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.CONTRASENA)

        assertEquals(1, filasDuranteElEnvio)
        assertEquals("password", api.revocaciones.single().third.metodoDeConfirmacion)
    }

    @Test
    fun `sin red queda pendiente y se programa el envio urgente, sin llamar al servicio`() = runTest {
        conectividad.estado.value = false

        val resultado = repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)

        assertEquals(ResultadoDeRevocacion.PendienteDeEnvio, resultado)
        assertTrue(api.revocaciones.isEmpty())
        assertEquals(1, programador.programados)
        val pendiente = repositorio.pendientes().first().single()
        assertEquals(RevocacionPendiente("cns-1", "Banco Aliado", CONFIRMADA, 0), pendiente)
        assertEquals(1, repositorio.cuantasPendientes())
    }

    @Test
    fun `sin respuesta en 4 s queda pendiente con la misma clave`() = runTest {
        api.demoraDeRevocacion = 5_000L

        val resultado = repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)

        assertEquals(ResultadoDeRevocacion.PendienteDeEnvio, resultado)
        assertEquals(1, programador.programados)
        assertEquals("clave-1", cola.una(sesion.sub, "cns-1")!!.clave)
    }

    @Test
    fun `una falla de red o del servicio deja la revocacion pendiente`() = runTest {
        api.revocacionSinRed = true
        assertEquals(ResultadoDeRevocacion.PendienteDeEnvio, repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA))

        api.revocacionSinRed = false
        api.codigoDeRevocacion = 503
        assertEquals(ResultadoDeRevocacion.PendienteDeEnvio, repositorio.revocar(consentimiento("cns-2"), MetodoDeConfirmacion.BIOMETRIA))

        assertEquals(2, cola.contar(sesion.sub))
        assertEquals(1, cola.una(sesion.sub, "cns-2")!!.intentos)
        assertEquals("http 503", cola.una(sesion.sub, "cns-2")!!.ultimoError)
    }

    @Test
    fun `si el consentimiento ya tenia una pendiente se envia esa, con su clave y su hora`() = runTest {
        conectividad.estado.value = false
        repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)
        conectividad.estado.value = true
        ahora = CONFIRMADA + 60_000L

        repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.CONTRASENA)

        val (_, clave, solicitud) = api.revocaciones.single()
        assertEquals("clave-1", clave)
        assertEquals("2026-10-24T14:59:58-05:00", solicitud.solicitadaEn)
        assertEquals("biometric", solicitud.metodoDeConfirmacion)
    }

    @Test
    fun `un 409 quita la fila, refresca el consentimiento y dice por que`() = runTest {
        conCopia(consentimiento("cns-1"), consentimiento("cns-2"))
        api.lista = listOf(dto("cns-1", "expired"), dto("cns-2", "revoked", finalizado = "2026-10-20T10:00:00Z", comprobante = "REV-9"))
        api.codigoDeRevocacion = 409
        api.errorDeRevocacion = """{"error":"consent_not_active","message":"x"}"""

        assertEquals(ResultadoDeRevocacion.YaNoActivo, repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA))
        assertEquals(EstadosDeConsentimiento.VENCIDO, enCopia("cns-1")!!.estado)

        api.errorDeRevocacion = """{"error":"consent_already_revoked","message":"x"}"""
        assertEquals(ResultadoDeRevocacion.YaRevocado, repositorio.revocar(consentimiento("cns-2"), MetodoDeConfirmacion.BIOMETRIA))
        assertEquals(EstadosDeConsentimiento.REVOCADO, enCopia("cns-2")!!.estado)

        assertEquals(0, cola.contar(sesion.sub))
        assertTrue("una revocacion en linea no deja aviso", repositorio.avisos.value.isEmpty())
    }

    @Test
    fun `un rechazo definitivo quita la fila sin reintentar`() = runTest {
        api.codigoDeRevocacion = 404

        assertEquals(ResultadoDeRevocacion.Error, repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA))
        assertEquals(0, cola.contar(sesion.sub))
        assertEquals(0, programador.programados)
    }

    @Test
    fun `sin sesion no se escribe nada`() = runTest {
        sesion.sub = ""

        assertEquals(ResultadoDeRevocacion.SinSesion, repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA))
        assertEquals(ResultadoDeEnvio.SinSesion, repositorio.enviarPendientes())
        assertTrue(repositorio.pendientes().first().isEmpty())
        assertEquals(0, repositorio.cuantasPendientes())
    }

    @Test
    fun `al volver la red la cola se envia horas despues con la hora de la confirmacion`() = runTest {
        conCopia(consentimiento("cns-1"))
        conectividad.estado.value = false
        repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)
        ahora = CONFIRMADA + 3 * 60 * 60_000L
        conectividad.estado.value = true

        val resultado = repositorio.enviarPendientes()

        assertEquals(ResultadoDeEnvio.Enviadas(1), resultado)
        val (_, clave, solicitud) = api.revocaciones.single()
        assertEquals("clave-1", clave)
        assertEquals("criterio 5", "2026-10-24T14:59:58-05:00", solicitud.solicitadaEn)
        assertEquals(0, cola.contar(sesion.sub))
        assertEquals(EstadosDeConsentimiento.REVOCADO, enCopia("cns-1")!!.estado)
    }

    @Test
    fun `sin red el envio de la cola se detiene y cuenta el intento`() = runTest {
        conectividad.estado.value = false
        repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)
        repositorio.revocar(consentimiento("cns-2"), MetodoDeConfirmacion.BIOMETRIA)
        api.revocacionSinRed = true

        assertEquals(ResultadoDeEnvio.SinConexion, repositorio.enviarPendientes())

        assertEquals("se detiene en la primera", 1, api.revocaciones.size)
        assertEquals(1, cola.una(sesion.sub, "cns-1")!!.intentos)
        assertEquals(2, cola.contar(sesion.sub))
    }

    @Test
    fun `una de la cola que el servicio ya no acepta se quita y queda el aviso`() = runTest {
        conCopia(consentimiento("cns-1"))
        conectividad.estado.value = false
        repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)
        api.lista = listOf(dto("cns-1", "expired"))
        api.codigoDeRevocacion = 409
        api.errorDeRevocacion = """{"error":"consent_not_active","message":"x"}"""

        assertEquals(ResultadoDeEnvio.Enviadas(1), repositorio.enviarPendientes())

        assertEquals(listOf(AvisoDeRevocacion("cns-1", "Banco Aliado", MotivoDeDescarte.VENCIO)), repositorio.avisos.value)
        repositorio.descartarAviso("cns-1")
        assertTrue(repositorio.avisos.value.isEmpty())
    }

    @Test
    fun `una falla del servicio en la cola se reintenta despues`() = runTest {
        conectividad.estado.value = false
        repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)
        api.codigoDeRevocacion = 401

        assertEquals(ResultadoDeEnvio.ConFallas, repositorio.enviarPendientes())
        assertEquals("http 401", cola.una(sesion.sub, "cns-1")!!.ultimoError)
    }

    @Test
    fun `la cola de una cuenta no la envia otra`() = runTest {
        conectividad.estado.value = false
        repositorio.revocar(consentimiento("cns-1"), MetodoDeConfirmacion.BIOMETRIA)
        sesion.sub = "sub-b"

        assertEquals(ResultadoDeEnvio.Enviadas(0), repositorio.enviarPendientes())
        assertTrue(api.revocaciones.isEmpty())
        assertTrue(repositorio.pendientes().first().isEmpty())
        sesion.sub = "sub-a"
        assertEquals(1, repositorio.pendientes().first().size)
    }

    @Test
    fun `el comprobante llega con su nombre o informa la falta de red`() = runTest {
        val descargado = repositorio.comprobante("cns-1", "en")
        assertTrue(descargado is ResultadoDeComprobante.Descargado)
        assertEquals("%PDF-1.3", String((descargado as ResultadoDeComprobante.Descargado).bytes))
        assertEquals("en", api.idiomaDelComprobante)

        api.codigoDeComprobante = 404
        assertEquals(ResultadoDeComprobante.Error, repositorio.comprobante("cns-1", "es"))
        api.sinRed = true
        assertEquals(ResultadoDeComprobante.SinConexion, repositorio.comprobante("cns-1", "es"))
        assertEquals("REV-2026-0088.pdf", RevocacionRepositorioImpl.nombreDelArchivo("attachment; filename=\"REV-2026-0088.pdf\""))
        assertNull(RevocacionRepositorioImpl.nombreDelArchivo("attachment"))
    }

    private companion object {
        /** 2026-10-24T19:59:58Z, 14:59:58 en Colombia. */
        const val CONFIRMADA = 1_792_871_998_000L
    }
}
