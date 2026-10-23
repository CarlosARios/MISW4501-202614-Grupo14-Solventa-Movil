package co.solventa.movil.features.siniestros.radicacion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Una prueba por criterio de aceptación de HU-M2.2-01.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RadicacionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var borradores: BorradoresEnMemoria
    private lateinit var radicaciones: RadicacionFalsa

    @Before
    fun antes() {
        Dispatchers.setMain(dispatcher)
        borradores = BorradoresEnMemoria()
        radicaciones = RadicacionFalsa()
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() =
        RadicacionViewModel(borradores, radicaciones, PolizasFalsas())

    // Criterio 1 — el asistente recorre Identificación, Hechos, Evidencias y
    // Declaración, en ese orden.
    @Test
    fun `el asistente arranca en identificacion y recorre las cuatro etapas`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            advanceUntilIdle()
            assertEquals(Etapa.IDENTIFICACION, viewModel.estado.value.etapa)

            completarIdentificacion(viewModel)
            viewModel.continuar()
            assertEquals(Etapa.HECHOS, viewModel.estado.value.etapa)

            completarHechos(viewModel)
            viewModel.continuar()
            assertEquals(Etapa.EVIDENCIAS, viewModel.estado.value.etapa)

            viewModel.continuar()
            assertEquals(Etapa.DECLARACION, viewModel.estado.value.etapa)
        }

    // Criterio 5 — la barra de avance dice en qué etapa va.
    @Test
    fun `el avance crece con cada etapa`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        assertEquals(0.25f, viewModel.estado.value.avance, 0.001f)

        completarIdentificacion(viewModel)
        viewModel.continuar()
        assertEquals(0.5f, viewModel.estado.value.avance, 0.001f)

        completarHechos(viewModel)
        viewModel.continuar()
        viewModel.continuar()
        assertEquals(1f, viewModel.estado.value.avance, 0.001f)
    }

    // Criterio 3 — sin lo obligatorio no se avanza, y se sabe qué falta.
    @Test
    fun `sin poliza ni tipo no se puede avanzar y se dice que falta`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.estado.value.puedeAvanzar)
            assertEquals(
                setOf(CampoDeRadicacion.POLIZA, CampoDeRadicacion.TIPO),
                viewModel.estado.value.faltantes,
            )

            viewModel.continuar()
            assertEquals(Etapa.IDENTIFICACION, viewModel.estado.value.etapa)
        }

    @Test
    fun `en hechos falta la fecha, el lugar, la descripcion y las respuestas`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            advanceUntilIdle()
            completarIdentificacion(viewModel)
            viewModel.continuar()

            assertEquals(
                setOf(
                    CampoDeRadicacion.FECHA,
                    CampoDeRadicacion.LUGAR,
                    CampoDeRadicacion.DESCRIPCION,
                    CampoDeRadicacion.RESPUESTAS,
                ),
                viewModel.estado.value.faltantes,
            )
        }

    // Criterio 2 — las preguntas dependen del tipo de incidente.
    @Test
    fun `cambiar el tipo cambia las preguntas y suelta lo respondido`() =
        runTest(dispatcher) {
            val viewModel = crearViewModel()
            advanceUntilIdle()
            viewModel.elegirTipo(TipoDeIncidente.COLISION)
            viewModel.responder("personas_lesionadas", "si")
            assertEquals("si", viewModel.estado.value.radicacion.respuestas["personas_lesionadas"])

            viewModel.elegirTipo(TipoDeIncidente.HURTO)

            assertTrue(viewModel.estado.value.radicacion.respuestas.isEmpty())
            assertEquals(
                listOf("alcance_del_hurto", "denuncia_presentada"),
                preguntasDe(TipoDeIncidente.HURTO).map { it.id },
            )
        }

    @Test
    fun `elegir el mismo tipo no borra lo respondido`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.elegirTipo(TipoDeIncidente.COLISION)
        viewModel.responder("personas_lesionadas", "si")

        viewModel.elegirTipo(TipoDeIncidente.COLISION)

        assertEquals("si", viewModel.estado.value.radicacion.respuestas["personas_lesionadas"])
    }

    // Criterio 7 — el progreso se guarda solo.
    @Test
    fun `lo diligenciado se guarda como borrador sin pedirlo`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.elegirPoliza("AU-2025-118204")
        advanceTimeBy(RadicacionViewModel.ESPERA_DE_GUARDADO_MS + 1)
        runCurrent()

        assertEquals("AU-2025-118204", borradores.guardado?.radicacion?.poliza)
    }

    /** Escribir no puede significar una escritura en disco por tecla. */
    @Test
    fun `los cambios seguidos se agrupan en un solo guardado`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        borradores.guardados = 0

        viewModel.fijarLugar("Calle")
        viewModel.fijarLugar("Calle 80")
        viewModel.fijarLugar("Calle 80 con Carrera 15")
        advanceTimeBy(RadicacionViewModel.ESPERA_DE_GUARDADO_MS + 1)
        runCurrent()

        assertEquals(1, borradores.guardados)
        assertEquals("Calle 80 con Carrera 15", borradores.guardado?.radicacion?.lugar)
    }

    @Test
    fun `cambiar de etapa guarda de inmediato`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarIdentificacion(viewModel)
        borradores.guardados = 0

        viewModel.continuar()
        runCurrent()

        assertEquals(1, borradores.guardados)
        assertEquals(Etapa.HECHOS, borradores.guardado?.etapa)
    }

    // Criterio 7 — se retoma en la misma etapa.
    @Test
    fun `al volver a entrar se retoma el borrador en su etapa`() = runTest(dispatcher) {
        borradores.existente = BorradorEnCurso(
            id = "borrador-1",
            etapa = Etapa.HECHOS,
            radicacion = Radicacion(poliza = "AU-2025-118204", tipo = TipoDeIncidente.HURTO),
        )

        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals("borrador-1", estado.borradorId)
        assertEquals(Etapa.HECHOS, estado.etapa)
        assertEquals("AU-2025-118204", estado.radicacion.poliza)
        assertFalse(estado.cargando)
    }

    @Test
    fun `sin borrador guardado se empieza uno nuevo`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.borradorId.isNotBlank())
        assertEquals(Etapa.IDENTIFICACION, viewModel.estado.value.etapa)
    }

    // Criterio 4 — al confirmar la declaración se genera el radicado.
    @Test
    fun `la radicacion devuelve el numero de radicado`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarTodo(viewModel)

        viewModel.radicar()
        advanceUntilIdle()

        val envio = viewModel.estado.value.envio
        assertTrue(envio is EstadoDelEnvio.Radicado)
        assertEquals("SIN-2026-00001", (envio as EstadoDelEnvio.Radicado).caso.radicado)
    }

    /** El borrador deja de serlo en cuanto el caso existe. */
    @Test
    fun `al quedar radicado el borrador se descarta`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarTodo(viewModel)
        val id = viewModel.estado.value.borradorId

        viewModel.radicar()
        advanceUntilIdle()

        assertEquals(listOf(id), borradores.descartados)
    }

    /** La clave con la que se radica es la del borrador, no una nueva. */
    @Test
    fun `se radica con el identificador del borrador`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarTodo(viewModel)
        val id = viewModel.estado.value.borradorId

        viewModel.radicar()
        advanceUntilIdle()

        assertEquals(id, radicaciones.claveRecibida)
    }

    @Test
    fun `sin aceptar la declaracion no se radica`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarTodo(viewModel)
        viewModel.aceptarDeclaracion(false)

        viewModel.radicar()
        advanceUntilIdle()

        assertNull(radicaciones.claveRecibida)
        assertEquals(EstadoDelEnvio.NoIniciado, viewModel.estado.value.envio)
    }

    /** Un dato rechazado se corrige en su etapa, no en la última. */
    @Test
    fun `un dato rechazado devuelve a la etapa donde se corrige`() = runTest(dispatcher) {
        radicaciones.resultado = ResultadoRadicacion.DatosInvalidos(
            setOf(CampoDeRadicacion.DESCRIPCION, CampoDeRadicacion.DECLARACION),
        )
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarTodo(viewModel)

        viewModel.radicar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertEquals(Etapa.HECHOS, estado.etapa)
        assertTrue(estado.camposRechazados.contains(CampoDeRadicacion.DESCRIPCION))
        assertEquals(EstadoDelEnvio.NoIniciado, estado.envio)
    }

    @Test
    fun `corregir el dato retira la marca de su etapa`() = runTest(dispatcher) {
        radicaciones.resultado =
            ResultadoRadicacion.DatosInvalidos(setOf(CampoDeRadicacion.DESCRIPCION))
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarTodo(viewModel)
        viewModel.radicar()
        advanceUntilIdle()

        viewModel.fijarDescripcion("Choque por alcance en el semáforo de la 80.")

        assertTrue(viewModel.estado.value.camposRechazados.isEmpty())
    }

    /** Si el servicio no responde, el borrador sigue ahí para reintentar. */
    @Test
    fun `una falla del envio conserva el borrador y su clave`() = runTest(dispatcher) {
        radicaciones.resultado = ResultadoRadicacion.NoDisponible("a1b2c3")
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarTodo(viewModel)
        val id = viewModel.estado.value.borradorId

        viewModel.radicar()
        advanceUntilIdle()

        val envio = viewModel.estado.value.envio
        assertTrue(envio is EstadoDelEnvio.NoDisponible)
        assertEquals("a1b2c3", (envio as EstadoDelEnvio.NoDisponible).referencia)
        assertTrue(borradores.descartados.isEmpty())
        assertEquals(id, viewModel.estado.value.borradorId)
    }

    @Test
    fun `el aviso de falla se cierra para volver a intentar`() = runTest(dispatcher) {
        radicaciones.resultado = ResultadoRadicacion.NoDisponible("a1b2c3")
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarTodo(viewModel)
        viewModel.radicar()
        advanceUntilIdle()

        viewModel.descartarAvisoDeEnvio()

        assertEquals(EstadoDelEnvio.NoIniciado, viewModel.estado.value.envio)
    }

    @Test
    fun `abandonar el reporte descarta el borrador y empieza otro`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        completarIdentificacion(viewModel)
        val id = viewModel.estado.value.borradorId

        viewModel.descartarBorrador()
        advanceUntilIdle()

        assertEquals(listOf(id), borradores.descartados)
        assertNotEquals(id, viewModel.estado.value.borradorId)
        assertEquals(Etapa.IDENTIFICACION, viewModel.estado.value.etapa)
        assertEquals(Radicacion(), viewModel.estado.value.radicacion)
    }

    @Test
    fun `volver atras no pasa de la primera etapa`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.volver()

        assertEquals(Etapa.IDENTIFICACION, viewModel.estado.value.etapa)
    }

    private fun completarIdentificacion(viewModel: RadicacionViewModel) {
        viewModel.elegirPoliza("AU-2025-118204")
        viewModel.elegirTipo(TipoDeIncidente.COLISION)
    }

    private fun completarHechos(viewModel: RadicacionViewModel) {
        viewModel.fijarFecha(1_792_539_000_000L)
        viewModel.fijarLugar("Calle 80 con Carrera 15, Bogotá")
        viewModel.fijarDescripcion("Choque por alcance en el semáforo.")
        preguntasDe(TipoDeIncidente.COLISION).forEach { viewModel.responder(it.id, "no") }
    }

    private fun completarTodo(viewModel: RadicacionViewModel) {
        completarIdentificacion(viewModel)
        viewModel.continuar()
        completarHechos(viewModel)
        viewModel.continuar()
        viewModel.continuar()
        viewModel.aceptarDeclaracion(true)
    }
}

private class BorradoresEnMemoria : BorradorRepositorio {

    var existente: BorradorEnCurso? = null
    var guardado: BorradorEnCurso? = null
        private set
    var guardados = 0
    val descartados = mutableListOf<String>()
    private var secuencia = 0

    override suspend fun enCurso(): BorradorEnCurso? = existente

    override suspend fun guardar(borrador: BorradorEnCurso) {
        guardado = borrador
        guardados++
    }

    override suspend fun descartar(id: String) {
        descartados += id
    }

    override fun nuevoIdentificador(): String = "borrador-nuevo-${++secuencia}"
}

private class PolizasFalsas : PolizasRepositorio {
    override suspend fun activas(): List<PolizaElegible> = listOf(
        PolizaElegible(id = "AU-2025-118204", etiqueta = "Autos · AU-2025-118204 · KJT-482"),
    )
}

private class RadicacionFalsa : RadicacionRepositorio {

    var resultado: ResultadoRadicacion = ResultadoRadicacion.Exitoso(
        CasoRadicado(
            casoId = "cl-1",
            radicado = "SIN-2026-00001",
            radicadoEn = "2026-10-23T03:10:00Z",
        ),
    )

    var claveRecibida: String? = null
        private set
    var recibida: Radicacion? = null
        private set

    override suspend fun radicar(borrador: String, radicacion: Radicacion): ResultadoRadicacion {
        claveRecibida = borrador
        recibida = radicacion
        return resultado
    }
}
