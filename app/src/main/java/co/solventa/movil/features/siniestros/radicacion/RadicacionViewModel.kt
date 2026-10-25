package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.features.siniestros.evidencia.EvidenciaRepositorio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Asistente guiado de radicación (HU-M2.2-01).
 *
 * Vive en el grafo de navegación del asistente, no en una pantalla: lo
 * diligenciado tiene que sobrevivir el paso entre las cuatro etapas.
 *
 * **Todo cambio se guarda como borrador.** El criterio 7 pide retomar donde
 * se quedó aunque la aplicación se haya cerrado, así que la escritura no
 * espera a un botón de guardar. Se agrupan los cambios seguidos para no
 * escribir en disco en cada tecla.
 */
@HiltViewModel
class RadicacionViewModel @Inject constructor(
    private val borradores: BorradorRepositorio,
    private val radicaciones: RadicacionRepositorio,
    private val polizas: PolizasRepositorio,
    private val evidencias: EvidenciaRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow(RadicacionUiState())
    val estado: StateFlow<RadicacionUiState> = _estado.asStateFlow()

    private var guardado: Job? = null
    private var evidenciasObservadas: Job? = null

    init {
        viewModelScope.launch {
            val vigentes = polizas.activas()
            val borrador = borradores.enCurso()
            _estado.value = if (borrador == null) {
                RadicacionUiState(
                    borradorId = borradores.nuevoIdentificador(),
                    polizas = vigentes,
                    cargando = false,
                )
            } else {
                RadicacionUiState(
                    borradorId = borrador.id,
                    etapa = borrador.etapa,
                    radicacion = borrador.radicacion,
                    polizas = vigentes,
                    cargando = false,
                    retomandoBorrador = true,
                )
            }
            observarEvidencias(_estado.value.borradorId)
        }
    }

    /**
     * Lo capturado para este borrador, con el estado de su envío. Viene de la
     * base del dispositivo, así que la pantalla ve avanzar cada subida sin
     * preguntar nada (HU-M2.1-01, criterio 5).
     */
    private fun observarEvidencias(borradorId: String) {
        evidenciasObservadas?.cancel()
        // Al retomar el reporte se vuelve a intentar lo que quedó pendiente:
        // puede llevar horas de espera acumulada entre reintentos.
        viewModelScope.launch { evidencias.reintentarEnvios(borradorId) }
        evidenciasObservadas = viewModelScope.launch {
            evidencias.observar(borradorId).collect { capturadas ->
                _estado.update { it.copy(evidencias = capturadas) }
            }
        }
    }

    /** El usuario retoma el reporte que habia dejado a medias. */
    fun continuarBorrador() {
        _estado.update { it.copy(retomandoBorrador = false) }
    }

    fun elegirPoliza(poliza: String) = cambiar { it.copy(poliza = poliza) }

    /** Cambiar de tipo cambia las preguntas, así que las respuestas se sueltan. */
    fun elegirTipo(tipo: TipoDeIncidente) = cambiar {
        if (it.tipo == tipo) it else it.copy(tipo = tipo, respuestas = emptyMap())
    }

    fun fijarFecha(instante: Long) = cambiar { it.copy(ocurrioEn = instante) }

    fun fijarLugar(lugar: String) = cambiar { it.copy(lugar = lugar) }

    fun fijarDescripcion(descripcion: String) = cambiar { it.copy(descripcion = descripcion) }

    fun responder(pregunta: String, respuesta: String) = cambiar {
        it.copy(respuestas = it.respuestas + (pregunta to respuesta))
    }

    fun aceptarDeclaracion(aceptada: Boolean) = cambiar {
        it.copy(declaracionAceptada = aceptada)
    }

    /** Avanza solo si la etapa está completa (criterio 3). */
    fun continuar() {
        val actual = _estado.value
        if (!actual.puedeAvanzar || actual.esUltimaEtapa) return
        irA(Etapa.entries[actual.etapa.ordinal + 1])
    }

    fun volver() {
        val actual = _estado.value
        if (actual.esPrimeraEtapa) return
        irA(Etapa.entries[actual.etapa.ordinal - 1])
    }

    /** Quitar una evidencia de la cuadrícula para volver a tomarla. */
    fun descartarEvidencia(id: String) {
        viewModelScope.launch { evidencias.descartar(id) }
    }

    /**
     * Envía la radicación con el identificador del borrador como clave, de
     * modo que un reintento no crea un segundo caso.
     */
    fun radicar() {
        val actual = _estado.value
        if (!actual.puedeAvanzar || !actual.esUltimaEtapa) return

        viewModelScope.launch {
            _estado.update {
                it.copy(envio = EstadoDelEnvio.Enviando, camposRechazados = emptySet())
            }
            // Solo viaja lo que ya está en el repositorio de evidencia: el
            // servicio comprueba cada llave antes de crear el caso.
            val capturado = evidencias.paraRadicar(actual.borradorId)
            when (val resultado = radicaciones.radicar(actual.borradorId, actual.radicacion, capturado)) {
                is ResultadoRadicacion.Exitoso -> {
                    // El borrador deja de serlo en cuanto el caso existe, y
                    // las evidencias ya no tienen que ocupar el teléfono.
                    borradores.descartar(actual.borradorId)
                    evidencias.limpiar(actual.borradorId)
                    _estado.update { it.copy(envio = EstadoDelEnvio.Radicado(resultado.caso)) }
                }

                is ResultadoRadicacion.DatosInvalidos -> _estado.update {
                    it.copy(
                        envio = EstadoDelEnvio.NoIniciado,
                        camposRechazados = resultado.campos,
                        // Se vuelve a la primera etapa que tiene un campo
                        // rechazado: es donde se corrige.
                        etapa = resultado.campos.minByOrNull { campo -> campo.etapa.ordinal }
                            ?.etapa ?: it.etapa,
                    )
                }

                is ResultadoRadicacion.NoDisponible -> _estado.update {
                    it.copy(envio = EstadoDelEnvio.NoDisponible(resultado.referencia))
                }
            }
        }
    }

    /** Cierra el aviso de falla para volver a intentar el envío. */
    fun descartarAvisoDeEnvio() {
        _estado.update {
            if (it.envio is EstadoDelEnvio.NoDisponible) {
                it.copy(envio = EstadoDelEnvio.NoIniciado)
            } else {
                it
            }
        }
    }

    /**
     * El usuario abandona el reporte: el borrador se descarta, y con él las
     * evidencias que había capturado. Sin borrarlas, el reporte nuevo las
     * heredaba: se descartaba el borrador y las fotos viejas reaparecían.
     */
    fun descartarBorrador() {
        val id = _estado.value.borradorId
        guardado?.cancel()
        viewModelScope.launch {
            borradores.descartar(id)
            evidencias.limpiar(id)
            val nuevo = borradores.nuevoIdentificador()
            _estado.value = RadicacionUiState(
                borradorId = nuevo,
                polizas = _estado.value.polizas,
                cargando = false,
            )
            observarEvidencias(nuevo)
        }
    }

    private fun cambiar(cambio: (Radicacion) -> Radicacion) {
        _estado.update {
            it.copy(radicacion = cambio(it.radicacion)).sinRechazosDe(it.etapa)
        }
        programarGuardado()
    }

    private fun irA(etapa: Etapa) {
        _estado.update { it.copy(etapa = etapa) }
        // Cambiar de etapa es el momento en que más duele perder lo escrito.
        guardado?.cancel()
        viewModelScope.launch { guardar() }
    }

    private fun programarGuardado() {
        guardado?.cancel()
        guardado = viewModelScope.launch {
            delay(ESPERA_DE_GUARDADO_MS)
            guardar()
        }
    }

    private suspend fun guardar() {
        val actual = _estado.value
        if (actual.cargando) return
        borradores.guardar(
            BorradorEnCurso(
                id = actual.borradorId,
                etapa = actual.etapa,
                radicacion = actual.radicacion,
            ),
        )
    }

    /** Lo que el usuario acaba de corregir deja de estar marcado. */
    private fun RadicacionUiState.sinRechazosDe(etapa: Etapa) =
        if (camposRechazados.none { it.etapa == etapa }) {
            this
        } else {
            copy(camposRechazados = camposRechazados.filterNot { it.etapa == etapa }.toSet())
        }

    companion object {
        /** Los cambios seguidos se agrupan antes de escribir en disco. */
        const val ESPERA_DE_GUARDADO_MS = 500L
    }
}
