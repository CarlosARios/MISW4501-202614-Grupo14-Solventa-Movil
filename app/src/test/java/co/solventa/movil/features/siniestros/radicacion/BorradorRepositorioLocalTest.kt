package co.solventa.movil.features.siniestros.radicacion

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
import co.solventa.movil.core.persistencia.entidades.EvidenciaCapturada
import co.solventa.movil.features.siniestros.UsuarioActual
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El borrador entre el modelo del asistente y la tabla del dispositivo, sobre
 * la base real en memoria: la traducción en los dos sentidos, lo guardado por
 * una versión anterior y, desde HU-M2.2-02, que cada borrador sea solo de
 * quien lo empezó y lleve el estado de su envío.
 */
@RunWith(AndroidJUnit4::class)
class BorradorRepositorioLocalTest {

    private lateinit var base: BaseDeDatosSolventa
    private var sesion = ASEGURADO
    private var ahora = 1_000L
    private lateinit var repositorio: BorradorRepositorioLocal

    @Before
    fun antes() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
        repositorio = BorradorRepositorioLocal(
            base.borradoresDeRadicacion(),
            base.evidenciasCapturadas(),
            UsuarioActual { sesion },
        ) { ahora }
    }

    @After
    fun despues() {
        base.close()
    }

    private fun borrador(id: String = "borrador-1") = BorradorEnCurso(
        id = id,
        etapa = Etapa.HECHOS,
        radicacion = Radicacion(
            poliza = "AU-2025-118204",
            tipo = TipoDeIncidente.COLISION,
            ocurrioEn = 1_792_539_000_000L,
            lugar = "Autopista Norte km 12, Bogotá",
            descripcion = "Choque por alcance en el semáforo.",
            respuestas = mapOf("personas_lesionadas" to "no", "informe_policia" to "si"),
            montoEstimado = "4.800.000",
        ),
    )

    @Test
    fun `lo guardado se lee igual a como entro`() = runTest {
        repositorio.guardar(borrador())

        assertEquals(borrador().copy(actualizadoEn = ahora), repositorio.enCurso())
    }

    @Test
    fun `sin borrador guardado no hay nada que retomar`() = runTest {
        assertNull(repositorio.enCurso())
    }

    @Test
    fun `cada reporte estrena identificador`() {
        assertNotEquals(repositorio.nuevoIdentificador(), repositorio.nuevoIdentificador())
    }

    @Test
    fun `descartar borra el borrador`() = runTest {
        repositorio.guardar(borrador())

        repositorio.descartar("borrador-1")

        assertNull(repositorio.enCurso())
    }

    @Test
    fun `el borrador de una cuenta no lo ve otra en el mismo telefono`() = runTest {
        repositorio.guardar(borrador())

        sesion = OTRO
        assertNull(repositorio.enCurso())
        assertTrue(repositorio.delUsuario().first().isEmpty())
        assertNull(repositorio.paraEnviar("borrador-1"))

        sesion = ASEGURADO
        assertEquals("borrador-1", repositorio.enCurso()?.id)
    }

    @Test
    fun `sin sesion no se guarda ni se lee nada`() = runTest {
        sesion = ""
        repositorio.guardar(borrador())

        assertNull(repositorio.enCurso())
        assertTrue(repositorio.delUsuario().first().isEmpty())
    }

    @Test
    fun `un borrador en cola deja de retomarse y queda listo para enviar`() = runTest {
        repositorio.guardar(borrador())

        repositorio.encolar("borrador-1")

        assertNull(repositorio.enCurso())
        assertEquals("borrador-1", repositorio.paraEnviar("borrador-1")?.id)
        assertEquals(EnvioDelBorrador.EN_COLA, repositorio.delUsuario().first().single().envio)
    }

    @Test
    fun `uno en edicion no se envia`() = runTest {
        repositorio.guardar(borrador())

        assertNull(repositorio.paraEnviar("borrador-1"))
    }

    @Test
    fun `los fallos se cuentan con la hora del ultimo`() = runTest {
        repositorio.guardar(borrador())
        repositorio.encolar("borrador-1")

        ahora = 5_000L
        repositorio.registrarFallo("borrador-1", EnvioDelBorrador.EN_COLA)
        ahora = 9_000L
        repositorio.registrarFallo("borrador-1", EnvioDelBorrador.FALLIDO)

        val resumen = repositorio.delUsuario().first().single()
        assertEquals(EnvioDelBorrador.FALLIDO, resumen.envio)
        assertEquals(2, resumen.intentos)
        assertEquals(9_000L, resumen.ultimoIntentoEn)
        // Un fallido no se envía solo: espera el reintento manual.
        assertNull(repositorio.paraEnviar("borrador-1"))
    }

    @Test
    fun `volver a encolar deja los intentos en cero`() = runTest {
        repositorio.guardar(borrador())
        repositorio.encolar("borrador-1")
        repositorio.registrarFallo("borrador-1", EnvioDelBorrador.FALLIDO)

        repositorio.encolar("borrador-1")

        assertEquals(0, repositorio.paraEnviar("borrador-1")?.intentos)
    }

    @Test
    fun `el radicado queda guardado hasta mostrar el aviso`() = runTest {
        repositorio.guardar(borrador())
        repositorio.encolar("borrador-1")

        repositorio.marcarRadicado("borrador-1", CasoRadicado("caso-9", "SIN-2026-01877", "2026-11-11T12:00:00Z"))

        val resumen = repositorio.delUsuario().first().single()
        assertEquals(EnvioDelBorrador.RADICADO, resumen.envio)
        assertEquals("SIN-2026-01877", resumen.radicado)
        assertEquals("caso-9", resumen.casoId)
    }

    @Test
    fun `retomar devuelve a edicion y pone de primero`() = runTest {
        ahora = 1_000L
        repositorio.guardar(borrador("viejo"))
        ahora = 2_000L
        repositorio.guardar(borrador("nuevo"))
        repositorio.encolar("viejo")
        repositorio.registrarFallo("viejo", EnvioDelBorrador.FALLIDO)

        ahora = 3_000L
        repositorio.retomar("viejo")

        assertEquals("viejo", repositorio.enCurso()?.id)
    }

    @Test
    fun `cada tarjeta cuenta sus fotos y su video`() = runTest {
        repositorio.guardar(borrador())
        val dao = base.evidenciasCapturadas()
        dao.guardar(evidencia("e1", "photo"))
        dao.guardar(evidencia("e2", "photo"))
        dao.guardar(evidencia("e3", "video"))

        val resumen = repositorio.delUsuario().first().single()

        assertEquals(2, resumen.fotos)
        assertEquals(1, resumen.videos)
    }

    /**
     * Lo guardado puede venir de una versión anterior. Perder el borrador
     * entero por un valor que ya no existe sería peor que volver al principio.
     */
    @Test
    fun `una etapa desconocida devuelve a la primera`() = runTest {
        base.borradoresDeRadicacion().guardar(entidadCruda(etapa = "REVISION_FINAL", tipo = "COLISION"))

        assertEquals(Etapa.IDENTIFICACION, repositorio.enCurso()?.etapa)
    }

    @Test
    fun `un tipo desconocido deja el incidente sin elegir`() = runTest {
        base.borradoresDeRadicacion().guardar(entidadCruda(etapa = "HECHOS", tipo = "METEORITO"))

        assertNull(repositorio.enCurso()?.radicacion?.tipo)
    }

    @Test
    fun `unas respuestas ilegibles no se llevan el borrador`() = runTest {
        base.borradoresDeRadicacion().guardar(entidadCruda(etapa = "HECHOS", tipo = "COLISION", respuestas = "{"))

        val leido = repositorio.enCurso()

        assertEquals(Etapa.HECHOS, leido?.etapa)
        assertTrue(leido?.radicacion?.respuestas.orEmpty().isEmpty())
    }

    private fun entidadCruda(
        etapa: String,
        tipo: String?,
        respuestas: String = "{}",
    ) = BorradorDeRadicacion(
        id = "borrador-1",
        etapa = etapa,
        poliza = "AU-2025-118204",
        tipo = tipo,
        ocurrioEn = null,
        lugar = "",
        descripcion = "",
        respuestasJson = respuestas,
        declaracionAceptada = false,
        actualizadoEn = 1L,
        propietario = ASEGURADO,
    )

    private fun evidencia(id: String, tipo: String) = EvidenciaCapturada(
        id = id,
        borradorId = "borrador-1",
        posicion = 0,
        tipo = tipo,
        rutaArchivo = "/no/existe/$id",
        capturadaEn = 1L,
        latitud = 0.0,
        longitud = 0.0,
        altitud = null,
        precisionM = 0f,
        ubicacionConfiable = false,
        sha256 = id,
        tamanoBytes = 100,
        duracionS = 0,
        llaveDelObjeto = null,
        estado = "pendiente",
        propietario = ASEGURADO,
    )

    private companion object {
        const val ASEGURADO = "sub-asegurado"
        const val OTRO = "sub-otra-cuenta"
    }
}
