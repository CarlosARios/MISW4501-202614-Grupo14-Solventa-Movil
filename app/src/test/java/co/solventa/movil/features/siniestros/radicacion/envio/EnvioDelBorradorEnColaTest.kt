package co.solventa.movil.features.siniestros.radicacion.envio

import co.solventa.movil.core.api.siniestros.EvidenciaRadicada
import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.features.siniestros.evidencia.EvidenciaDelReporte
import co.solventa.movil.features.siniestros.evidencia.EvidenciaRepositorio
import co.solventa.movil.features.siniestros.evidencia.TipoDeEvidencia
import co.solventa.movil.features.siniestros.radicacion.BorradorEnCurso
import co.solventa.movil.features.siniestros.radicacion.BorradorRepositorio
import co.solventa.movil.features.siniestros.radicacion.BorradorResumen
import co.solventa.movil.features.siniestros.radicacion.CampoDeRadicacion
import co.solventa.movil.features.siniestros.radicacion.CasoRadicado
import co.solventa.movil.features.siniestros.radicacion.EnvioDelBorrador
import co.solventa.movil.features.siniestros.radicacion.Etapa
import co.solventa.movil.features.siniestros.radicacion.Radicacion
import co.solventa.movil.features.siniestros.radicacion.RadicacionRepositorio
import co.solventa.movil.features.siniestros.radicacion.ResultadoRadicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * El envío en segundo plano de un reporte en cola (HU-M2.2-02): misma clave
 * en cada intento, borrador conservado hasta la confirmación, tres intentos
 * automáticos y luego el reintento manual.
 */
class EnvioDelBorradorEnColaTest {

    private val borradores = BorradoresEnCola()
    private val evidencias = EvidenciaDeLaCola()
    private val radicaciones = ServicioDeRadicacion()
    private val avisos = mutableListOf<CasoRadicado>()
    private val envio = EnvioDelBorradorEnCola(
        borradores,
        evidencias,
        radicaciones,
        AvisadorDeRadicacion { avisos += it },
        esperaDeEvidencia = 10_000L,
    )

    @Test
    fun `al confirmar el servicio se guarda el radicado, se avisa y se limpia la evidencia`() = runTest {
        val resultado = envio.enviar(ID)

        assertEquals(ResultadoDelEnvio.LISTO, resultado)
        assertEquals(EnvioDelBorrador.RADICADO, borradores.envio)
        assertEquals("SIN-2026-01877", borradores.radicado?.radicado)
        assertEquals(listOf("SIN-2026-01877"), avisos.map { it.radicado })
        assertEquals(ID, evidencias.limpiado)
        assertEquals(listOf(ID), radicaciones.claves)
    }

    @Test
    fun `dos fallas y luego exito usan siempre la misma clave`() = runTest {
        radicaciones.respuestas += listOf(NO_DISPONIBLE, NO_DISPONIBLE)

        assertEquals(ResultadoDelEnvio.REINTENTAR, envio.enviar(ID))
        assertEquals(EnvioDelBorrador.EN_COLA, borradores.envio)
        assertEquals(ResultadoDelEnvio.REINTENTAR, envio.enviar(ID))
        assertEquals(ResultadoDelEnvio.LISTO, envio.enviar(ID))

        assertEquals(listOf(ID, ID, ID), radicaciones.claves)
        assertEquals(2, borradores.intentos)
        assertEquals(EnvioDelBorrador.RADICADO, borradores.envio)
    }

    @Test
    fun `al tercer fallo se detiene y el borrador queda fallido con su evidencia`() = runTest {
        radicaciones.respuestas += listOf(NO_DISPONIBLE, NO_DISPONIBLE, NO_DISPONIBLE)

        envio.enviar(ID)
        envio.enviar(ID)
        val ultimo = envio.enviar(ID)

        assertEquals(ResultadoDelEnvio.AGOTADO, ultimo)
        assertEquals(EnvioDelBorrador.FALLIDO, borradores.envio)
        assertEquals(3, borradores.intentos)
        assertNull("la evidencia sigue en el teléfono", evidencias.limpiado)
        assertTrue(avisos.isEmpty())
    }

    @Test
    fun `un rechazo de datos no se reintenta`() = runTest {
        radicaciones.respuestas += ResultadoRadicacion.DatosInvalidos(setOf(CampoDeRadicacion.LUGAR))

        assertEquals(ResultadoDelEnvio.AGOTADO, envio.enviar(ID))
        assertEquals(EnvioDelBorrador.FALLIDO, borradores.envio)
    }

    @Test
    fun `sin un borrador enviable de la sesion no se envia nada`() = runTest {
        borradores.enviable = false

        assertEquals(ResultadoDelEnvio.LISTO, envio.enviar(ID))
        assertTrue(radicaciones.claves.isEmpty())
    }

    @Test
    fun `espera a que la evidencia termine de subir antes de radicar`() = runTest {
        evidencias.subidaTrasConsultas = 3

        assertEquals(ResultadoDelEnvio.LISTO, envio.enviar(ID))
        assertTrue("pidió reintentar la subida", evidencias.reintentos > 0)
        assertEquals(listOf(ID), radicaciones.claves)
    }

    @Test
    fun `si la evidencia no termina de subir cuenta como un intento fallido`() = runTest {
        evidencias.subidaTrasConsultas = Int.MAX_VALUE

        assertEquals(ResultadoDelEnvio.REINTENTAR, envio.enviar(ID))
        assertTrue(radicaciones.claves.isEmpty())
        assertEquals(1, borradores.intentos)
        assertFalse(borradores.envio == EnvioDelBorrador.RADICADO)
    }

    private companion object {
        const val ID = "borrador-1"
        val NO_DISPONIBLE = ResultadoRadicacion.NoDisponible("ref-1")
    }
}

private class BorradoresEnCola : BorradorRepositorio {
    var enviable = true
    var envio = EnvioDelBorrador.EN_COLA
    var intentos = 0
    var radicado: CasoRadicado? = null

    override suspend fun enCurso(): BorradorEnCurso? = null
    override suspend fun guardar(borrador: BorradorEnCurso) = Unit
    override suspend fun descartar(id: String) = Unit
    override fun nuevoIdentificador(): String = "nuevo"
    override fun delUsuario(): Flow<List<BorradorResumen>> = flowOf(emptyList())
    override suspend fun retomar(id: String) = Unit
    override suspend fun encolar(id: String) {
        envio = EnvioDelBorrador.EN_COLA
        intentos = 0
    }

    override suspend fun paraEnviar(id: String): BorradorEnCurso? =
        if (enviable && (envio == EnvioDelBorrador.EN_COLA || envio == EnvioDelBorrador.ENVIANDO)) {
            BorradorEnCurso(id, Etapa.DECLARACION, Radicacion(poliza = "AU-1", declaracionAceptada = true), intentos = intentos)
        } else {
            null
        }

    override suspend fun marcarEnviando(id: String) {
        envio = EnvioDelBorrador.ENVIANDO
    }

    override suspend fun registrarFallo(id: String, envio: EnvioDelBorrador) {
        this.envio = envio
        intentos++
    }

    override suspend fun marcarRadicado(id: String, caso: CasoRadicado) {
        envio = EnvioDelBorrador.RADICADO
        radicado = caso
    }
}

private class EvidenciaDeLaCola : EvidenciaRepositorio {
    var subidaTrasConsultas = 0
    private var consultas = 0
    var reintentos = 0
    var limpiado: String? = null

    override fun observar(borradorId: String): Flow<List<EvidenciaDelReporte>> = flowOf(emptyList())
    override suspend fun capturar(
        borradorId: String,
        archivo: File,
        tipo: TipoDeEvidencia,
        ubicacion: Ubicacion?,
        duracionS: Int,
    ): Boolean = true

    override suspend fun descartar(id: String) = Unit
    override suspend fun reintentarEnvios(borradorId: String) {
        reintentos++
    }

    override suspend fun paraRadicar(borradorId: String): List<EvidenciaRadicada> = emptyList()
    override suspend fun todoSubido(borradorId: String): Boolean = consultas++ >= subidaTrasConsultas
    override suspend fun limpiar(borradorId: String) {
        limpiado = borradorId
    }
}

private class ServicioDeRadicacion : RadicacionRepositorio {
    val respuestas = ArrayDeque<ResultadoRadicacion>()
    val claves = mutableListOf<String>()

    override suspend fun radicar(
        borrador: String,
        radicacion: Radicacion,
        evidencias: List<EvidenciaRadicada>,
    ): ResultadoRadicacion {
        claves += borrador
        return respuestas.removeFirstOrNull()
            ?: ResultadoRadicacion.Exitoso(CasoRadicado("caso-9", "SIN-2026-01877", "2026-11-11T12:00:00Z"))
    }
}
