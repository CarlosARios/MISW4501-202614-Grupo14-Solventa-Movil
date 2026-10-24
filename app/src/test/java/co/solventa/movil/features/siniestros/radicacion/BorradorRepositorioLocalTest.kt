package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.core.persistencia.dao.BorradorDeRadicacionDao
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El borrador entre el modelo del asistente y la tabla del dispositivo. Lo
 * que se comprueba es la traducción en los dos sentidos y qué pasa cuando lo
 * guardado viene de una versión anterior de la aplicación.
 */
class BorradorRepositorioLocalTest {

    private val dao = DaoEnMemoria()
    private val repositorio = BorradorRepositorioLocal(dao)

    private fun borrador() = BorradorEnCurso(
        id = "borrador-1",
        etapa = Etapa.HECHOS,
        radicacion = Radicacion(
            poliza = "AU-2025-118204",
            tipo = TipoDeIncidente.COLISION,
            ocurrioEn = 1_792_539_000_000L,
            lugar = "Autopista Norte km 12, Bogotá",
            descripcion = "Choque por alcance en el semáforo.",
            respuestas = mapOf("personas_lesionadas" to "no", "informe_policia" to "si"),
        ),
    )

    @Test
    fun `lo guardado se lee igual a como entro`() = runTest {
        repositorio.guardar(borrador())

        assertEquals(borrador(), repositorio.enCurso())
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
    fun `guardar deja la hora del ultimo cambio`() = runTest {
        repositorio.guardar(borrador())

        assertTrue(dao.guardado!!.actualizadoEn > 0)
    }

    /**
     * Lo guardado puede venir de una versión anterior. Perder el borrador
     * entero por un valor que ya no existe sería peor que volver al principio.
     */
    @Test
    fun `una etapa desconocida devuelve a la primera`() = runTest {
        dao.guardado = entidadCruda(etapa = "REVISION_FINAL", tipo = "COLISION")

        assertEquals(Etapa.IDENTIFICACION, repositorio.enCurso()?.etapa)
    }

    @Test
    fun `un tipo desconocido deja el incidente sin elegir`() = runTest {
        dao.guardado = entidadCruda(etapa = "HECHOS", tipo = "METEORITO")

        assertNull(repositorio.enCurso()?.radicacion?.tipo)
    }

    @Test
    fun `unas respuestas ilegibles no se llevan el borrador`() = runTest {
        dao.guardado = entidadCruda(etapa = "HECHOS", tipo = "COLISION", respuestas = "{")

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
    )
}

private class DaoEnMemoria : BorradorDeRadicacionDao {

    var guardado: BorradorDeRadicacion? = null

    override suspend fun guardar(borrador: BorradorDeRadicacion) {
        guardado = borrador
    }

    override suspend fun enCurso(): BorradorDeRadicacion? = guardado

    override suspend fun borrar(id: String) {
        if (guardado?.id == id) guardado = null
    }
}
