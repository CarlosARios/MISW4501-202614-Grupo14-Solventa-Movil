package co.solventa.movil.features.siniestros.asistencias

import app.cash.turbine.test
import co.solventa.movil.core.ubicacion.Localizador
import co.solventa.movil.core.ubicacion.PermisoDeUbicacion
import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.core.ubicacion.VerificadorDeUbicacion
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.AHORA
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.lectura
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.otraPoliza
import co.solventa.movil.features.siniestros.asistencias.DatosDeAsistencia.poliza
import co.solventa.movil.features.siniestros.radicacion.PolizaElegible
import co.solventa.movil.features.siniestros.radicacion.PolizasRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * M14, M14b y M14c (HU-M2.3-01) desde el `ViewModel`: pólizas (D6), cada rama
 * de S2 y S3, el marcador antes de cualquier confirmación (criterio 3), los
 * textos de D10 y S6, «Volver a llamar», «Ver estado» y la solicitud que se
 * retoma (D9).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AsistenciaViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val repositorio = RepositorioDeAsistenciaFalso()
    private val verificador = VerificadorFijo()
    private var leida: Ubicacion? = lectura
    private var ahora = AHORA
    private var activas: () -> List<PolizaElegible> = { listOf(poliza) }

    private class VerificadorFijo : VerificadorDeUbicacion {
        var permiso = PermisoDeUbicacion.PRECISO
        var servicio = true
        override fun permiso() = permiso
        override fun servicioEncendido() = servicio
    }

    @Before
    fun antes() = Dispatchers.setMain(despachador)

    @After
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.vm(): AsistenciaViewModel {
        val vm = AsistenciaViewModel(
            polizas = object : PolizasRepositorio {
                override suspend fun activas() = this@AsistenciaViewModelTest.activas()
            },
            repositorio = repositorio,
            verificador = verificador,
            localizador = object : Localizador {
                override suspend fun ubicacionActual() = leida
            },
            reloj = { ahora },
        )
        advanceUntilIdle()
        return vm
    }

    private val marcador = EventoDeAsistencia.AbrirMarcador(LineaDeAtencion.MARCABLE)

    // --- D6: pólizas -----------------------------------------------------------------------------

    @Test
    fun `una poliza queda elegida y se puede solicitar`() = runTest(despachador) {
        val vm = vm()

        assertEquals(PolizasDeAsistencia.Lista(listOf(poliza), poliza), vm.estado.value.polizas)
        assertTrue(vm.estado.value.puedeSolicitar)
        assertEquals(1, repositorio.reanudaciones)
    }

    @Test
    fun `con varias hay que elegir antes de solicitar`() = runTest(despachador) {
        activas = { listOf(poliza, otraPoliza) }
        val vm = vm()

        assertNull(vm.estado.value.polizaElegida)
        vm.solicitar()
        advanceUntilIdle()
        assertTrue(repositorio.solicitadas.isEmpty())

        vm.elegirPoliza("no-existe")
        assertNull(vm.estado.value.polizaElegida)
        vm.elegirPoliza(otraPoliza.id)
        assertEquals(otraPoliza, vm.estado.value.polizaElegida)
        vm.solicitar()
        advanceUntilIdle()
        assertEquals(otraPoliza, repositorio.solicitadas.single().first)
    }

    @Test
    fun `sin polizas no hay solicitud pero si el boton de llamar`() = runTest(despachador) {
        activas = { emptyList() }
        val vm = vm()

        assertEquals(PolizasDeAsistencia.SinPolizas, vm.estado.value.polizas)
        assertFalse(vm.estado.value.puedeSolicitar)
        vm.elegirPoliza(poliza.id)
        vm.llamarSinSolicitud()

        assertEquals(marcador, vm.estado.value.evento)
        assertTrue(repositorio.solicitadas.isEmpty())
    }

    @Test
    fun `si las polizas fallan se trata como sin polizas`() = runTest(despachador) {
        activas = { error("billetera ilegible") }

        assertEquals(PolizasDeAsistencia.SinPolizas, vm().estado.value.polizas)
    }

    // --- Criterios 2 y 3: con ubicación ----------------------------------------------------------

    @Test
    fun `con permiso y servicio obtiene la ubicacion la encola y abre el marcador antes de confirmar`() = runTest(despachador) {
        val vm = vm()

        vm.estado.test {
            assertEquals(PasoDeAsistencia.Inicial, awaitItem().paso)
            vm.solicitar()
            assertEquals(PasoDeAsistencia.ObteniendoUbicacion, awaitItem().paso)
            val solicitada = awaitItem()
            assertEquals(PasoDeAsistencia.Solicitada, solicitada.paso)
            assertEquals(marcador, solicitada.evento)
            assertEquals(EstadoDeEnvio.EnCola, solicitada.solicitud!!.envio)
            assertEquals(EncabezadoDeAsistencia.ENVIANDO, solicitada.encabezado)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(poliza to ResultadoDeUbicacion.Compartida(lectura)), repositorio.solicitadas)

        vm.marcadorAbierto()
        assertNull(vm.estado.value.evento)
        assertTrue(vm.estado.value.marcadorAbierto)
        assertEquals(listOf(PasoDelEstado.ENVIANDO, PasoDelEstado.LLAMADA_EN_CURSO), vm.estado.value.pasosDelEstado)
    }

    @Test
    fun `sin lectura a tiempo se registra no_fix y se llama igual`() = runTest(despachador) {
        leida = null
        val vm = vm()

        vm.solicitar()
        advanceUntilIdle()

        assertEquals(ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA), repositorio.solicitadas.single().second)
        assertEquals(MotivoSinUbicacion.SIN_LECTURA, vm.estado.value.motivoSinUbicacion)
        assertEquals(marcador, vm.estado.value.evento)
    }

    @Test
    fun `sin ubicacion en la cola con red dice que envia la solicitud y no la ubicacion`() = runTest(despachador) {
        leida = null
        val vm = vm()

        vm.solicitar()
        advanceUntilIdle()

        assertEquals(EncabezadoDeAsistencia.ENVIANDO_SOLICITUD, vm.estado.value.encabezado)
        assertEquals(listOf(PasoDelEstado.ENVIANDO_SOLICITUD), vm.estado.value.pasosDelEstado)
    }

    @Test
    fun `sin datos la solicitud dice que se enviara con senal`() = runTest(despachador) {
        val sinRed = RepositorioDeAsistenciaFalso(conectado = false)
        val vm = AsistenciaViewModel(
            object : PolizasRepositorio {
                override suspend fun activas() = listOf(poliza)
            },
            sinRed, verificador,
            object : Localizador {
                override suspend fun ubicacionActual() = lectura
            },
        ) { ahora }
        advanceUntilIdle()

        vm.solicitar()
        advanceUntilIdle()

        assertEquals(EncabezadoDeAsistencia.SIN_CONEXION, vm.estado.value.encabezado)
        assertEquals(listOf(PasoDelEstado.SIN_CONEXION), vm.estado.value.pasosDelEstado)
    }

    // --- Criterio 1 y S2/S3: M14b ----------------------------------------------------------------

    @Test
    fun `falta el permiso abre M14b y activar pide el permiso del sistema`() = runTest(despachador) {
        verificador.permiso = PermisoDeUbicacion.NINGUNO
        val vm = vm()

        vm.solicitar()
        assertEquals(PasoDeAsistencia.Dialogo(TipoDeDialogo.FALTA_PERMISO), vm.estado.value.paso)
        vm.activarUbicacion()
        assertEquals(EventoDeAsistencia.PedirPermiso, vm.estado.value.evento)
        vm.eventoAtendido()

        verificador.permiso = PermisoDeUbicacion.APROXIMADO
        vm.permisoRespondido(concedido = true, bloqueado = false)
        advanceUntilIdle()

        assertEquals(PasoDeAsistencia.Solicitada, vm.estado.value.paso)
        assertEquals(ResultadoDeUbicacion.Compartida(lectura), repositorio.solicitadas.single().second)
    }

    @Test
    fun `concedido el permiso con el servicio apagado muestra la variante del servicio`() = runTest(despachador) {
        verificador.permiso = PermisoDeUbicacion.NINGUNO
        verificador.servicio = false
        val vm = vm()
        vm.solicitar()
        vm.activarUbicacion()

        verificador.permiso = PermisoDeUbicacion.PRECISO
        vm.permisoRespondido(concedido = true, bloqueado = false)

        assertEquals(PasoDeAsistencia.Dialogo(TipoDeDialogo.SERVICIO_APAGADO), vm.estado.value.paso)
    }

    @Test
    fun `negado en el dialogo del sistema registra permission_denied y abre el marcador`() = runTest(despachador) {
        verificador.permiso = PermisoDeUbicacion.NINGUNO
        val vm = vm()
        vm.solicitar()
        vm.activarUbicacion()

        vm.permisoRespondido(concedido = false, bloqueado = false)
        advanceUntilIdle()

        assertEquals(ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO), repositorio.solicitadas.single().second)
        assertEquals(marcador, vm.estado.value.evento)
        assertEquals(MotivoSinUbicacion.PERMISO_NEGADO, vm.estado.value.motivoSinUbicacion)
    }

    @Test
    fun `una respuesta del sistema que nadie pidio se ignora`() = runTest(despachador) {
        val vm = vm()

        vm.permisoRespondido(concedido = false, bloqueado = true)
        advanceUntilIdle()

        assertTrue(repositorio.solicitadas.isEmpty())
        assertEquals(PasoDeAsistencia.Inicial, vm.estado.value.paso)
    }

    @Test
    fun `negado para siempre la siguiente vez manda a los ajustes de la app y al volver revisa`() = runTest(despachador) {
        verificador.permiso = PermisoDeUbicacion.NINGUNO
        val vm = vm()
        vm.solicitar()
        vm.activarUbicacion()
        vm.permisoRespondido(concedido = false, bloqueado = true)
        advanceUntilIdle()
        vm.solicitarOtra()

        vm.solicitar()
        assertEquals(PasoDeAsistencia.Dialogo(TipoDeDialogo.PERMISO_NEGADO_PARA_SIEMPRE), vm.estado.value.paso)
        vm.activarUbicacion()
        assertEquals(EventoDeAsistencia.AbrirAjustesDeLaApp, vm.estado.value.evento)
        vm.eventoAtendido()

        verificador.permiso = PermisoDeUbicacion.PRECISO
        vm.alVolver()
        advanceUntilIdle()

        assertEquals(2, repositorio.solicitadas.size)
        assertEquals(ResultadoDeUbicacion.Compartida(lectura), repositorio.solicitadas.last().second)
    }

    @Test
    fun `servicio apagado abre los ajustes de ubicacion y al volver apagado muestra M14b otra vez`() = runTest(despachador) {
        verificador.servicio = false
        val vm = vm()

        vm.solicitar()
        assertEquals(PasoDeAsistencia.Dialogo(TipoDeDialogo.SERVICIO_APAGADO), vm.estado.value.paso)
        vm.activarUbicacion()
        assertEquals(EventoDeAsistencia.AbrirAjustesDeUbicacion, vm.estado.value.evento)
        vm.eventoAtendido()

        vm.alVolver()
        assertEquals(PasoDeAsistencia.Dialogo(TipoDeDialogo.SERVICIO_APAGADO), vm.estado.value.paso)
        assertEquals(true, repositorio.visible)

        vm.llamarSinUbicacion()
        advanceUntilIdle()
        assertEquals(ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.UBICACION_APAGADA), repositorio.solicitadas.single().second)
        assertEquals(marcador, vm.estado.value.evento)
    }

    @Test
    fun `llamar sin ubicacion sin permiso registra permission_denied`() = runTest(despachador) {
        verificador.permiso = PermisoDeUbicacion.NINGUNO
        val vm = vm()
        vm.solicitar()

        vm.llamarSinUbicacion()
        advanceUntilIdle()

        assertEquals(ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO), repositorio.solicitadas.single().second)
    }

    @Test
    fun `cancelar no registra nada ni abre el marcador`() = runTest(despachador) {
        verificador.servicio = false
        val vm = vm()
        vm.solicitar()

        vm.cancelar()
        advanceUntilIdle()

        assertEquals(PasoDeAsistencia.Inicial, vm.estado.value.paso)
        assertNull(vm.estado.value.evento)
        assertTrue(repositorio.solicitadas.isEmpty())
        // Fuera del diálogo, ni «Activar» ni «Llamar sin ubicación» hacen nada.
        vm.activarUbicacion()
        vm.llamarSinUbicacion()
        vm.cancelar()
        advanceUntilIdle()
        assertTrue(repositorio.solicitadas.isEmpty())
    }

    // --- D10 y S6: lo que dice la pantalla ------------------------------------------------------

    @Test
    fun `confirmada y descartada cambian el encabezado y el estado`() = runTest(despachador) {
        val vm = vm()
        vm.solicitar()
        advanceUntilIdle()
        vm.marcadorAbierto()
        val enCola = vm.estado.value.solicitud!!

        repositorio.ultima.value = enCola.copy(envio = EstadoDeEnvio.Confirmada, requestId = "r-1", numero = "AST-2026-0001", registradaEn = AHORA)
        advanceUntilIdle()
        assertEquals(EncabezadoDeAsistencia.ENVIADA, vm.estado.value.encabezado)
        assertEquals(
            listOf(PasoDelEstado.UBICACION_RECIBIDA, PasoDelEstado.LLAMADA_EN_CURSO, PasoDelEstado.POR_DESPACHAR),
            vm.estado.value.pasosDelEstado,
        )
        assertTrue(vm.estado.value.puedeVerEstado)

        repositorio.ultima.value = enCola.copy(envio = EstadoDeEnvio.Descartada(MotivoDeDescarte.POLIZA_NO_ACTIVA))
        advanceUntilIdle()
        assertEquals(EncabezadoDeAsistencia.NO_REGISTRADA, vm.estado.value.encabezado)
        assertEquals(MotivoDeDescarte.POLIZA_NO_ACTIVA, vm.estado.value.motivoDeDescarte)
        assertEquals(listOf(PasoDelEstado.NO_REGISTRADA, PasoDelEstado.LLAMADA_EN_CURSO), vm.estado.value.pasosDelEstado)
    }

    @Test
    fun `sin ubicacion confirmada se lee como solicitud registrada`() = runTest(despachador) {
        verificador.servicio = false
        val vm = vm()
        vm.solicitar()
        vm.llamarSinUbicacion()
        advanceUntilIdle()

        repositorio.ultima.value = vm.estado.value.solicitud!!.copy(envio = EstadoDeEnvio.Confirmada, estado = EstadoDeLaSolicitud.CERRADA)
        advanceUntilIdle()

        assertEquals(EncabezadoDeAsistencia.REGISTRADA, vm.estado.value.encabezado)
        assertEquals(listOf(PasoDelEstado.SOLICITUD_RECIBIDA, PasoDelEstado.CERRADA), vm.estado.value.pasosDelEstado)
    }

    @Test
    fun `cada estado del servicio tiene su fila`() {
        val base = SolicitudDeAsistencia(1, poliza.id, poliza.etiqueta, DatosDeAsistencia.compartida, EstadoDeEnvio.Confirmada, AHORA)
        mapOf(
            EstadoDeLaSolicitud.NUEVA to PasoDelEstado.POR_DESPACHAR,
            EstadoDeLaSolicitud.EN_LLAMADA to PasoDelEstado.EN_ATENCION,
            EstadoDeLaSolicitud.DESPACHADA to PasoDelEstado.DESPACHADA,
            EstadoDeLaSolicitud.CERRADA to PasoDelEstado.CERRADA,
            EstadoDeLaSolicitud.DESCONOCIDO to PasoDelEstado.DESCONOCIDO,
        ).forEach { (estado, fila) ->
            assertEquals(fila, AsistenciaUiState(solicitud = base.copy(estado = estado)).pasosDelEstado.last())
        }
        assertEquals(emptyList<PasoDelEstado>(), AsistenciaUiState().pasosDelEstado)
        assertEquals(listOf(PasoDelEstado.LLAMADA_EN_CURSO), AsistenciaUiState(marcadorAbierto = true).pasosDelEstado)
        assertNull(AsistenciaUiState().encabezado)
    }

    @Test
    fun `sin sesion se abre el marcador sin solicitud`() = runTest(despachador) {
        repositorio.sinSesion = true
        val vm = vm()

        vm.solicitar()
        advanceUntilIdle()

        assertEquals(PasoDeAsistencia.Solicitada, vm.estado.value.paso)
        assertNull(vm.estado.value.solicitud)
        assertEquals(marcador, vm.estado.value.evento)
    }

    // --- Criterio 3 y D4: marcador y «Volver a llamar» ------------------------------------------

    @Test
    fun `volver a llamar solo reabre el marcador`() = runTest(despachador) {
        val vm = vm()
        vm.solicitar()
        advanceUntilIdle()
        vm.marcadorAbierto()

        vm.volverALlamar()
        advanceUntilIdle()

        assertEquals(marcador, vm.estado.value.evento)
        assertEquals(1, repositorio.solicitadas.size)
    }

    @Test
    fun `sin marcador se muestra el numero`() = runTest(despachador) {
        val vm = vm()
        vm.solicitar()
        advanceUntilIdle()

        vm.marcadorNoDisponible()

        assertTrue(vm.estado.value.sinMarcador)
        assertNull(vm.estado.value.evento)
        assertFalse(vm.estado.value.marcadorAbierto)
    }

    // --- D11: «Ver estado» ------------------------------------------------------------------------

    @Test
    fun `ver estado consulta el servicio y pinta el estado`() = runTest(despachador) {
        val vm = vm()
        vm.solicitar()
        advanceUntilIdle()
        vm.verEstado()
        advanceUntilIdle()
        assertTrue("sin request_id no hay qué consultar", repositorio.consultas.isEmpty())

        repositorio.ultima.value = vm.estado.value.solicitud!!.copy(envio = EstadoDeEnvio.Confirmada, requestId = "r-1")
        advanceUntilIdle()
        repositorio.estadoRemoto = EstadoDeLaSolicitud.DESPACHADA
        vm.verEstado()
        advanceUntilIdle()

        assertEquals(listOf("r-1"), repositorio.consultas)
        assertEquals(EstadoDeLaSolicitud.DESPACHADA, vm.estado.value.solicitud!!.estado)
        assertFalse(vm.estado.value.consultandoEstado)
        assertFalse(vm.estado.value.errorAlConsultar)
    }

    @Test
    fun `un error al consultar no borra lo mostrado y al volver se consulta otra vez`() = runTest(despachador) {
        val vm = vm()
        vm.solicitar()
        advanceUntilIdle()
        repositorio.ultima.value = vm.estado.value.solicitud!!.copy(
            envio = EstadoDeEnvio.Confirmada,
            requestId = "r-1",
            estado = EstadoDeLaSolicitud.EN_LLAMADA,
        )
        advanceUntilIdle()

        repositorio.estadoRemoto = null
        vm.alVolver()
        advanceUntilIdle()

        assertEquals(listOf("r-1"), repositorio.consultas)
        assertTrue(vm.estado.value.errorAlConsultar)
        assertEquals(EstadoDeLaSolicitud.EN_LLAMADA, vm.estado.value.solicitud!!.estado)
    }

    // --- D9: retomar y «Solicitar otra asistencia» ----------------------------------------------

    @Test
    fun `una solicitud de menos de 30 minutos se retoma al abrir`() = runTest(despachador) {
        repositorio.ultima.value = SolicitudDeAsistencia(
            7, poliza.id, poliza.etiqueta, DatosDeAsistencia.compartida, EstadoDeEnvio.Confirmada,
            creadaEn = AHORA - ReglasDeAsistencia.RETOMABLE_MS + 1_000,
        )
        val vm = vm()

        assertEquals(PasoDeAsistencia.Solicitada, vm.estado.value.paso)
        assertEquals(7L, vm.estado.value.solicitud!!.id)

        vm.solicitarOtra()
        assertEquals(PasoDeAsistencia.Inicial, vm.estado.value.paso)
        assertNull(vm.estado.value.solicitud)
        // La que se dejó no vuelve aunque Room la emita otra vez.
        repositorio.ultima.value = repositorio.ultima.value!!.copy(estado = EstadoDeLaSolicitud.NUEVA)
        advanceUntilIdle()
        assertEquals(PasoDeAsistencia.Inicial, vm.estado.value.paso)
    }

    @Test
    fun `una solicitud de 30 minutos o mas no se retoma`() = runTest(despachador) {
        repositorio.ultima.value = SolicitudDeAsistencia(
            7, poliza.id, poliza.etiqueta, DatosDeAsistencia.compartida, EstadoDeEnvio.Confirmada,
            creadaEn = AHORA - ReglasDeAsistencia.RETOMABLE_MS,
        )

        val vm = vm()

        assertEquals(PasoDeAsistencia.Inicial, vm.estado.value.paso)
        assertNull(vm.estado.value.solicitud)
    }

    @Test
    fun `al salir la pantalla deja de estar a la vista`() = runTest(despachador) {
        val vm = vm()
        vm.alVolver()
        assertEquals(true, repositorio.visible)

        vm.alSalir()

        assertEquals(false, repositorio.visible)
    }
}
