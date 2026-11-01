package co.solventa.movil.features.auditoria.consentimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * La consulta de los consentimientos del titular (HU-M1.3-01).
 *
 * Muestra siempre la copia local (criterio 5) y la actualiza al abrir la
 * pantalla y, mientras esta abierta, cada vez que la red vuelve: el titular no
 * tiene que tocar nada para ver lo nuevo. La actualizacion envia primero la
 * cola de revocaciones (HU-M1.3-02), asi una revocacion en cola sale sola al
 * volver la red con la aplicacion abierta.
 *
 * HU-M1.3-02: los consentimientos con una revocacion pendiente de envio se
 * listan aparte, con «Reintentar envio ahora» (`38:1503`).
 */
@HiltViewModel
class ConsentimientosViewModel internal constructor(
    private val repositorio: ConsentimientosRepositorio,
    private val revocaciones: RevocacionRepositorio,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(repositorio: ConsentimientosRepositorio, revocaciones: RevocacionRepositorio) :
        this(repositorio, revocaciones, System::currentTimeMillis)

    private val _estado = MutableStateFlow(ConsentimientosUiState())
    val estado: StateFlow<ConsentimientosUiState> = _estado.asStateFlow()

    /** El ultimo resultado de pedir la lista; null mientras no ha terminado ninguno. */
    private var ultimo: ResultadoDeActualizacion? = null
    private var copia = CopiaDeConsentimientos(emptyList(), null, sinConexion = false)
    private var pendientes: List<RevocacionPendiente> = emptyList()
    private var descartes: List<AvisoDeRevocacion> = emptyList()
    private var reenviando = false

    init {
        viewModelScope.launch {
            combine(repositorio.observar(), revocaciones.pendientes(), revocaciones.avisos) { c, p, a -> Triple(c, p, a) }
                .collect { (c, p, a) ->
                    copia = c
                    pendientes = p
                    descartes = a
                    componer()
                }
        }
        viewModelScope.launch {
            var habiaRed = repositorio.conectado.value
            repositorio.conectado.collect { conectado ->
                if (conectado && !habiaRed) actualizar()
                habiaRed = conectado
            }
        }
        actualizar()
    }

    /** Reintento del titular desde el error o la cuenta sin vincular. */
    fun reintentar() = actualizar()

    /** «Reintentar envio ahora» (HU-M1.3-02, criterio 5): solo con señal. */
    fun reintentarEnvio() {
        if (!_estado.value.reintentarHabilitado) return
        reenviando = true
        componer()
        viewModelScope.launch {
            revocaciones.enviarPendientes()
            reenviando = false
            componer()
        }
    }

    fun descartarAviso(consentimientoId: String) = revocaciones.descartarAviso(consentimientoId)

    private fun actualizar() {
        viewModelScope.launch {
            ultimo = repositorio.actualizar()
            componer()
        }
    }

    private fun componer() {
        val enCola = pendientes.associateBy { it.consentimientoId }
        val (conPendiente, sinPendiente) = copia.consentimientos.partition { it.id in enCola }
        val grupos = agrupar(sinPendiente, reloj())
        val resultado = ultimo
        _estado.update {
            ConsentimientosUiState(
                cargando = resultado == null && copia.consentimientos.isEmpty(),
                activos = grupos.activos,
                historial = grupos.historial,
                sinConexion = copia.sinConexion || resultado == ResultadoDeActualizacion.SinConexion,
                sincronizadoEn = copia.sincronizadoEn,
                error = resultado == ResultadoDeActualizacion.Error,
                cuentaSinVincular = resultado == ResultadoDeActualizacion.CuentaSinVincular,
                pendientes = conPendiente
                    .map { ConsentimientoPendiente(it.visibleEn(reloj()), enCola.getValue(it.id)) }
                    .sortedByDescending { it.revocacion.solicitadaEn },
                reenviando = reenviando,
                descartes = descartes,
            )
        }
    }
}
