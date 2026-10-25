package co.solventa.movil.features.siniestros.radicacion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import co.solventa.movil.core.api.siniestros.EvidenciaRadicada
import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.features.siniestros.evidencia.EstadoDeEvidencia
import co.solventa.movil.features.siniestros.evidencia.EvidenciaDelReporte
import co.solventa.movil.features.siniestros.evidencia.EvidenciaRepositorio
import co.solventa.movil.features.siniestros.evidencia.LimitesDeEvidencia
import co.solventa.movil.features.siniestros.evidencia.TipoDeEvidencia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var evidencias: EvidenciasFalsas

    @Before
    fun antes() {
        Dispatchers.setMain(dispatcher)
        borradores = BorradoresEnMemoria()
        radicaciones = RadicacionFalsa()
        evidencias = EvidenciasFalsas().apply { conElMinimoEnviado() }
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() =
        RadicacionViewModel(borradores, radicaciones, PolizasFalsas(), evidencias)

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

    var evidenciaRecibida: List<EvidenciaRadicada> = emptyList()
        private set

    override suspend fun radicar(
        borrador: String,
        radicacion: Radicacion,
        evidencias: List<EvidenciaRadicada>,
    ): ResultadoRadicacion {
        claveRecibida = borrador
        recibida = radicacion
        evidenciaRecibida = evidencias
        return resultado
    }
}

/**
 * Doble de la evidencia capturada: la lista se fija en la prueba y el envío
 * se da por hecho, que es lo que el asistente ve.
 */
private class EvidenciasFalsas : EvidenciaRepositorio {

    private val capturadas = MutableStateFlow<List<EvidenciaDelReporte>>(emptyList())
    var limpiadas: String? = null
        private set

    /** Deja el mínimo ya enviado, que es lo que deja avanzar el paso 3. */
    fun conElMinimoEnviado(cuantas: Int = LimitesDeEvidencia.MINIMO_DE_FOTOS) {
        capturadas.value = (1..cuantas).map { numero ->
            EvidenciaDelReporte(
                id = "ev-$numero",
                tipo = TipoDeEvidencia.FOTO,
                rutaArchivo = "/tmp/evidencia-$numero.jpg",
                capturadaEn = 1_792_000_000_000,
                ubicacion = Ubicacion(4.7712, -74.0452, null, 8f, 1_792_000_000_000),
                duracionS = 0,
                estado = EstadoDeEvidencia.SUBIDA,
            )
        }
    }

    override fun observar(borradorId: String): Flow<List<EvidenciaDelReporte>> = capturadas

    override suspend fun capturar(
        borradorId: String,
        archivo: java.io.File,
        tipo: TipoDeEvidencia,
        ubicacion: Ubicacion?,
        duracionS: Int,
    ): Boolean = true

    override suspend fun descartar(id: String) {
        capturadas.value = capturadas.value.filterNot { it.id == id }
    }

    override suspend fun reintentarEnvios(borradorId: String) = Unit

    override suspend fun paraRadicar(borradorId: String): List<EvidenciaRadicada> =
        capturadas.value.map { evidencia ->
            EvidenciaRadicada(
                llaveDelObjeto = "evidence/sub/${evidencia.id}.jpg",
                tipo = evidencia.tipo.delContrato,
                capturadaEn = "2026-10-20T23:32:14Z",
                latitud = 4.7712,
                longitud = -74.0452,
                precisionM = 8.0,
                ubicacionConfiable = true,
                sha256 = "a".repeat(64),
                tamanoBytes = 312_044,
            )
        }

    override suspend fun todoSubido(borradorId: String): Boolean = true

    override suspend fun limpiar(borradorId: String) {
        limpiadas = borradorId
        // Limpiar borra las filas de ese borrador: lo que la pantalla observa
        // se queda sin nada.
        capturadas.value = emptyList()
    }
}

// Criterio 3 de HU-M2.2-01 con la evidencia de HU-M2.1-01: el paso 3 no deja
// avanzar hasta que el mínimo esté en la plataforma.

class EvidenciaDelAsistenteTest {

    @Test
    fun `sin el minimo enviado no se puede avanzar del paso de evidencias`() {
        val estado = estadoConEvidencias(enviadas = LimitesDeEvidencia.MINIMO_DE_FOTOS - 1)

        assertEquals(setOf(CampoDeRadicacion.EVIDENCIAS), estado.faltantesDe(Etapa.EVIDENCIAS))
        assertFalse(estado.puedeAvanzar)
    }

    @Test
    fun `con el minimo enviado se avanza`() {
        val estado = estadoConEvidencias(enviadas = LimitesDeEvidencia.MINIMO_DE_FOTOS)

        assertTrue(estado.faltantesDe(Etapa.EVIDENCIAS).isEmpty())
        assertTrue(estado.puedeAvanzar)
    }

    /**
     * Criterio 5: el envío no bloquea a quien está en el lugar, así que el
     * paso 3 cuenta lo capturado. Quien espera por las subidas es la
     * radicación, que viaja con las llaves del repositorio.
     */
    @Test
    fun `lo capturado deja avanzar aunque una evidencia siga viajando`() {
        val estado = estadoConEvidencias(
            enviadas = LimitesDeEvidencia.MINIMO_DE_FOTOS - 1,
            enCurso = 2,
        )

        assertEquals(LimitesDeEvidencia.MINIMO_DE_FOTOS + 1, estado.fotosCapturadas)
        assertEquals(LimitesDeEvidencia.MINIMO_DE_FOTOS - 1, estado.fotosEnviadas)
        assertTrue(estado.hayEnviosEnCurso)
        assertTrue(estado.faltantesDe(Etapa.EVIDENCIAS).isEmpty())
        assertTrue(estado.puedeAvanzar)
    }

    @Test
    fun `la radicacion espera a que la evidencia llegue a la plataforma`() {
        val enCurso = estadoConEvidencias(
            enviadas = LimitesDeEvidencia.MINIMO_DE_FOTOS,
            enCurso = 1,
        ).copy(etapa = Etapa.DECLARACION, radicacion = Radicacion(declaracionAceptada = true))

        assertTrue(enCurso.faltantes.isEmpty())
        assertFalse("la evidencia todavia viaja", enCurso.puedeAvanzar)

        val todoEnviado = estadoConEvidencias(enviadas = LimitesDeEvidencia.MINIMO_DE_FOTOS)
            .copy(etapa = Etapa.DECLARACION, radicacion = Radicacion(declaracionAceptada = true))

        assertTrue(todoEnviado.puedeAvanzar)
    }

    @Test
    fun `el video no cuenta para el minimo de fotos`() {
        val estado = estadoConEvidencias(enviadas = LimitesDeEvidencia.MINIMO_DE_FOTOS - 1, videos = 1)

        assertEquals(1, estado.videosCapturados)
        assertFalse(estado.puedeAvanzar)
    }

    private fun estadoConEvidencias(enviadas: Int, enCurso: Int = 0, videos: Int = 0) =
        RadicacionUiState(
            etapa = Etapa.EVIDENCIAS,
            cargando = false,
            evidencias = buildList {
                repeat(enviadas) { add(evidencia("ok-$it", EstadoDeEvidencia.SUBIDA)) }
                repeat(enCurso) { add(evidencia("curso-$it", EstadoDeEvidencia.SUBIENDO)) }
                repeat(videos) {
                    add(evidencia("video-$it", EstadoDeEvidencia.SUBIDA, TipoDeEvidencia.VIDEO))
                }
            },
        )

    private fun evidencia(
        id: String,
        estado: EstadoDeEvidencia,
        tipo: TipoDeEvidencia = TipoDeEvidencia.FOTO,
    ) = EvidenciaDelReporte(
        id = id,
        tipo = tipo,
        rutaArchivo = "/tmp/$id",
        capturadaEn = 1_792_000_000_000,
        ubicacion = Ubicacion(4.7712, -74.0452, null, 8f, 1_792_000_000_000),
        duracionS = 0,
        estado = estado,
    )
}

// Descartar el reporte también descarta lo capturado: sin eso, el reporte
// nuevo heredaba las fotos del anterior.

class DescarteDelBorradorTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var borradores: BorradoresEnMemoria
    private lateinit var evidencias: EvidenciasFalsas

    @Before
    fun antes() {
        Dispatchers.setMain(dispatcher)
        borradores = BorradoresEnMemoria()
        evidencias = EvidenciasFalsas().apply { conElMinimoEnviado() }
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    @Test
    fun `descartar el borrador borra sus evidencias y arranca uno nuevo`() = runTest(dispatcher) {
        val viewModel = RadicacionViewModel(borradores, RadicacionFalsa(), PolizasFalsas(), evidencias)
        advanceUntilIdle()
        val primero = viewModel.estado.value.borradorId

        viewModel.descartarBorrador()
        advanceUntilIdle()

        assertEquals("se limpió el borrador que se descartó", primero, evidencias.limpiadas)
        assertNotEquals(primero, viewModel.estado.value.borradorId)
        assertTrue("el reporte nuevo arranca sin evidencias", viewModel.estado.value.evidencias.isEmpty())
    }
}
