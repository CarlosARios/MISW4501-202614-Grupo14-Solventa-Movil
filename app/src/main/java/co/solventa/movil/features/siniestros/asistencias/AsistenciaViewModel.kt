package co.solventa.movil.features.siniestros.asistencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.solventa.movil.core.ubicacion.Localizador
import co.solventa.movil.core.ubicacion.VerificadorDeUbicacion
import co.solventa.movil.features.siniestros.radicacion.PolizaElegible
import co.solventa.movil.features.siniestros.radicacion.PolizasRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * M14, M14b y M14c · Solicitud de asistencia con GPS y llamada (HU-M2.3-01).
 *
 * - **Criterio 1 (S2):** «Solicitar asistencia y llamar» revisa permiso y
 *   servicio; si falta algo explica por qué (M14b) y lleva al flujo del
 *   sistema que lo arregla. Al volver de los ajustes revisa otra vez.
 * - **Criterios 2 y 3 (D2, D4):** con la ubicación leída, la solicitud se
 *   escribe en la cola y **en seguida** se abre el marcador: nunca espera al
 *   servicio. «Ubicación enviada» aparece solo con su confirmación (D10).
 * - **Criterio 4 (S1, S3):** sin permiso, con el GPS apagado o sin lectura se
 *   registra el motivo y el marcador se abre igual (M14c).
 * - **Criterio 5 (D6):** solo las pólizas activas; sin ninguna no hay
 *   solicitud, pero sí el botón de llamar.
 * - **D9:** una solicitud de hace menos de 30 minutos se retoma al abrir.
 *
 * Lo que toca el sistema operativo lo pide como [EventoDeAsistencia]: la
 * pantalla lo ejecuta y avisa el resultado.
 */
@HiltViewModel
class AsistenciaViewModel internal constructor(
    private val polizas: PolizasRepositorio,
    private val repositorio: AsistenciasRepositorio,
    private val verificador: VerificadorDeUbicacion,
    private val localizador: Localizador,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(
        polizas: PolizasRepositorio,
        repositorio: AsistenciasRepositorio,
        verificador: VerificadorDeUbicacion,
        localizador: Localizador,
    ) : this(polizas, repositorio, verificador, localizador, System::currentTimeMillis)

    private val _estado = MutableStateFlow(AsistenciaUiState())
    val estado: StateFlow<AsistenciaUiState> = _estado.asStateFlow()

    /** El sistema ya no muestra su diálogo de permisos: M14b manda a los ajustes de la app. */
    private var permisoBloqueado = false

    /** Se abrió el diálogo de permisos del sistema y falta su respuesta. */
    private var esperandoPermiso = false

    /** Se abrieron los ajustes: al volver se revisa otra vez (S2). */
    private var esperandoAjustes = false

    /** El id local de la solicitud que se muestra. */
    private var actual: Long? = null

    /** La que se dejó con «Solicitar otra asistencia»: ya no se retoma. */
    private var ignorada: Long? = null

    init {
        viewModelScope.launch { cargarPolizas() }
        viewModelScope.launch { repositorio.conectado.collect { conectado -> _estado.update { it.copy(conectado = conectado) } } }
        viewModelScope.launch { repositorio.observarUltima().collect(::alCambiarLaUltima) }
        viewModelScope.launch { repositorio.reanudarPendientes() }
    }

    fun elegirPoliza(id: String) {
        val lista = _estado.value.polizas as? PolizasDeAsistencia.Lista ?: return
        val elegida = lista.elegibles.firstOrNull { it.id == id } ?: return
        _estado.update { it.copy(polizas = lista.copy(elegida = elegida)) }
    }

    /** «Solicitar asistencia y llamar». */
    fun solicitar() {
        if (!_estado.value.puedeSolicitar) return
        evaluar()
    }

    /** «Activar ubicación» en M14b: el diálogo del sistema o los ajustes, según la variante (D3). */
    fun activarUbicacion() {
        val dialogo = (_estado.value.paso as? PasoDeAsistencia.Dialogo)?.tipo ?: return
        val evento = when (dialogo) {
            TipoDeDialogo.FALTA_PERMISO -> {
                esperandoPermiso = true
                EventoDeAsistencia.PedirPermiso
            }
            TipoDeDialogo.PERMISO_NEGADO_PARA_SIEMPRE -> {
                esperandoAjustes = true
                EventoDeAsistencia.AbrirAjustesDeLaApp
            }
            TipoDeDialogo.SERVICIO_APAGADO -> {
                esperandoAjustes = true
                EventoDeAsistencia.AbrirAjustesDeUbicacion
            }
        }
        _estado.update { it.copy(paso = PasoDeAsistencia.Inicial, evento = evento) }
    }

    /**
     * La respuesta del diálogo de permisos del sistema. Concedido, sigue el
     * flujo; negado, se registra `permission_denied` y el marcador se abre
     * igual (S3). [bloqueado] dice que el sistema ya no volverá a mostrarlo.
     */
    fun permisoRespondido(concedido: Boolean, bloqueado: Boolean) {
        if (!esperandoPermiso) return
        esperandoPermiso = false
        if (concedido) {
            permisoBloqueado = false
            evaluar()
        } else {
            permisoBloqueado = bloqueado
            registrar(ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO))
        }
    }

    /** «Llamar sin ubicación» en M14b: se registra el motivo de lo que falta (S1). */
    fun llamarSinUbicacion() {
        if (_estado.value.paso !is PasoDeAsistencia.Dialogo) return
        val motivo = ReglasDeAsistencia.motivoSinUbicacion(verificador.permiso(), verificador.servicioEncendido())
        registrar(ResultadoDeUbicacion.NoDisponible(motivo))
    }

    /** «Cancelar» en M14b: no se registra nada ni se abre el marcador (S3). */
    fun cancelar() {
        if (_estado.value.paso is PasoDeAsistencia.Dialogo) _estado.update { it.copy(paso = PasoDeAsistencia.Inicial) }
    }

    /** La pantalla volvió a estar a la vista (`ON_RESUME`). */
    fun alVolver() {
        repositorio.pantallaVisible(true)
        if (esperandoAjustes) {
            esperandoAjustes = false
            evaluar()
        } else if (_estado.value.paso == PasoDeAsistencia.Solicitada && _estado.value.puedeVerEstado) {
            consultar()
        }
    }

    /** La pantalla dejó de estar a la vista (`ON_PAUSE`). */
    fun alSalir() {
        repositorio.pantallaVisible(false)
    }

    /** Sin pólizas activas: solo se abre el marcador, sin solicitud (D6). */
    fun llamarSinSolicitud() {
        _estado.update { it.copy(evento = EventoDeAsistencia.AbrirMarcador(LineaDeAtencion.MARCABLE)) }
    }

    /** «Volver a llamar»: solo reabre el marcador, no crea otra solicitud (D4). */
    fun volverALlamar() {
        _estado.update { it.copy(evento = EventoDeAsistencia.AbrirMarcador(LineaDeAtencion.MARCABLE)) }
    }

    /** «Ver estado de la solicitud» (D11). */
    fun verEstado() {
        consultar()
    }

    /** «Solicitar otra asistencia»: vuelve a la pantalla inicial (D9). */
    fun solicitarOtra() {
        ignorada = actual
        actual = null
        _estado.update {
            it.copy(
                paso = PasoDeAsistencia.Inicial,
                solicitud = null,
                marcadorAbierto = false,
                sinMarcador = false,
                errorAlConsultar = false,
            )
        }
    }

    /** La pantalla ejecutó el evento (los que no son el marcador). */
    fun eventoAtendido() {
        _estado.update { it.copy(evento = null) }
    }

    fun marcadorAbierto() {
        _estado.update { it.copy(evento = null, marcadorAbierto = true) }
    }

    /** No hay marcador en el dispositivo: se muestra el número para copiarlo (S4). */
    fun marcadorNoDisponible() {
        _estado.update { it.copy(evento = null, sinMarcador = true) }
    }

    override fun onCleared() {
        repositorio.pantallaVisible(false)
    }

    private suspend fun cargarPolizas() {
        val activas = try {
            polizas.activas()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
        _estado.update {
            it.copy(
                polizas = if (activas.isEmpty()) {
                    PolizasDeAsistencia.SinPolizas
                } else {
                    PolizasDeAsistencia.Lista(activas, elegida = activas.singleOrNull())
                },
            )
        }
    }

    private fun evaluar() {
        when (val siguiente = ReglasDeAsistencia.siguientePaso(verificador.permiso(), verificador.servicioEncendido(), permisoBloqueado)) {
            is SiguientePaso.Explicar -> _estado.update { it.copy(paso = PasoDeAsistencia.Dialogo(siguiente.dialogo)) }
            SiguientePaso.ObtenerUbicacion -> obtener()
        }
    }

    private fun obtener() {
        _estado.update { it.copy(paso = PasoDeAsistencia.ObteniendoUbicacion) }
        viewModelScope.launch {
            val ubicacion = localizador.ubicacionActual()
            registrarAhora(
                ReglasDeAsistencia.resultadoDe(ubicacion, verificador.permiso(), verificador.servicioEncendido(), reloj()),
            )
        }
    }

    private fun registrar(resultado: ResultadoDeUbicacion) {
        viewModelScope.launch { registrarAhora(resultado) }
    }

    /**
     * S2: se escribe en la cola y **después** se abre el marcador. La
     * solicitud se muestra en seguida «en cola»; la confirmación llega por la
     * observación de Room.
     */
    private suspend fun registrarAhora(resultado: ResultadoDeUbicacion) {
        val poliza: PolizaElegible = _estado.value.polizaElegida ?: return
        val creadaEn = reloj()
        val id = repositorio.solicitar(poliza, resultado)
        actual = id
        _estado.update {
            it.copy(
                paso = PasoDeAsistencia.Solicitada,
                solicitud = id?.let { local ->
                    SolicitudDeAsistencia(
                        id = local,
                        poliza = poliza.id,
                        etiquetaDePoliza = poliza.etiqueta,
                        resultado = resultado,
                        envio = EstadoDeEnvio.EnCola,
                        creadaEn = creadaEn,
                    )
                },
                evento = EventoDeAsistencia.AbrirMarcador(LineaDeAtencion.MARCABLE),
                marcadorAbierto = false,
                sinMarcador = false,
                errorAlConsultar = false,
            )
        }
    }

    private fun alCambiarLaUltima(solicitud: SolicitudDeAsistencia?) {
        if (solicitud == null) return
        val estado = _estado.value
        when {
            solicitud.id == actual -> _estado.update { it.copy(solicitud = solicitud) }
            actual == null && solicitud.id != ignorada && estado.paso == PasoDeAsistencia.Inicial &&
                ReglasDeAsistencia.retomable(solicitud.creadaEn, reloj()) -> {
                actual = solicitud.id
                _estado.update { it.copy(paso = PasoDeAsistencia.Solicitada, solicitud = solicitud) }
            }
        }
    }

    private fun consultar() {
        val requestId = _estado.value.solicitud?.requestId ?: return
        if (_estado.value.consultandoEstado) return
        _estado.update { it.copy(consultandoEstado = true) }
        viewModelScope.launch {
            val estadoRemoto = repositorio.consultarEstado(requestId)
            _estado.update { actual ->
                val solicitud = actual.solicitud
                if (estadoRemoto == null || solicitud == null || solicitud.requestId != requestId) {
                    actual.copy(consultandoEstado = false, errorAlConsultar = estadoRemoto == null)
                } else {
                    actual.copy(consultandoEstado = false, errorAlConsultar = false, solicitud = solicitud.copy(estado = estadoRemoto))
                }
            }
        }
    }
}
