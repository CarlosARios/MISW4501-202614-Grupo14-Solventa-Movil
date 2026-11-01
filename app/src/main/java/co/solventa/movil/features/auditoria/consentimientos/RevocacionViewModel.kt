package co.solventa.movil.features.auditoria.consentimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * La revocacion de un consentimiento desde su detalle (HU-M1.3-02).
 *
 * - **Criterio 1:** «Revocar consentimiento» abre la hoja con el efecto y la
 *   confirmacion con la huella o, sin biometria habilitada, la contraseña.
 * - **Criterio 2:** cancelar, una huella no reconocida o una contraseña
 *   incorrecta cierran la hoja y dejan el detalle como estaba: **no se escribe
 *   nada** en el telefono ni se llama al servicio. La huella no reconocida y
 *   la contraseña incorrecta ademas dejan un aviso que lo dice.
 * - **Criterios 3 y 5:** confirmada, el repositorio la guarda primero en la
 *   cola y la envia; responde antes de 5 s con revocado o pendiente.
 * - **Criterio 6:** la accion no existe en un revocado, vencido o pendiente.
 *
 * Tambien sirve a la pantalla de revocado (`38:1414`): el comprobante en PDF.
 */
@HiltViewModel
class RevocacionViewModel internal constructor(
    estadoGuardado: SavedStateHandle,
    consentimientos: ConsentimientosRepositorio,
    private val revocaciones: RevocacionRepositorio,
    private val confirmador: ConfirmadorDeRevocacion,
    private val guardador: GuardadorDeComprobante,
    private val idiomas: IdiomaRepositorio,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(
        estadoGuardado: SavedStateHandle,
        consentimientos: ConsentimientosRepositorio,
        revocaciones: RevocacionRepositorio,
        confirmador: ConfirmadorDeRevocacion,
        guardador: GuardadorDeComprobante,
        idiomas: IdiomaRepositorio,
    ) : this(estadoGuardado, consentimientos, revocaciones, confirmador, guardador, idiomas, System::currentTimeMillis)

    /** La ruta del detalle y la del revocado guardan el identificador con este nombre. */
    private val id: String = estadoGuardado.get<String>(DetalleConsentimientoViewModel.ARGUMENTO_ID).orEmpty()

    private val _estado = MutableStateFlow(RevocacionUiState())
    val estado: StateFlow<RevocacionUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                consentimientos.observarUno(id),
                revocaciones.pendientes(),
                revocaciones.conectado,
                revocaciones.avisos,
            ) { consentimiento, pendientes, conectado, avisos ->
                Observado(
                    consentimiento?.visibleEn(reloj()),
                    pendientes.firstOrNull { it.consentimientoId == id },
                    conectado,
                    avisos.firstOrNull { it.consentimientoId == id },
                )
            }.collect { o ->
                _estado.update {
                    it.copy(consentimiento = o.consentimiento, pendiente = o.pendiente, conectado = o.conectado, descarte = o.descarte)
                }
            }
        }
    }

    private data class Observado(
        val consentimiento: Consentimiento?,
        val pendiente: RevocacionPendiente?,
        val conectado: Boolean,
        val descarte: AvisoDeRevocacion?,
    )

    /** Abre la hoja con el efecto y el metodo de confirmacion del telefono. */
    fun abrirHoja() {
        if (!_estado.value.revocarDisponible || _estado.value.paso != PasoDeRevocacion.Oculto) return
        viewModelScope.launch {
            val metodo = confirmador.metodo()
            _estado.update { it.copy(paso = PasoDeRevocacion.Hoja(metodo), aviso = null) }
        }
    }

    /** Cancelar: el detalle queda como estaba (criterio 2). */
    fun cancelar() {
        val paso = _estado.value.paso
        if (paso is PasoDeRevocacion.Hoja && !paso.verificando) {
            _estado.update { it.copy(paso = PasoDeRevocacion.Oculto) }
        }
    }

    fun confirmarConBiometria() {
        val hoja = _estado.value.paso as? PasoDeRevocacion.Hoja ?: return
        if (hoja.verificando) return
        _estado.update { it.copy(paso = hoja.copy(verificando = true, sinConexion = false)) }
        viewModelScope.launch {
            resolver(confirmador.conBiometria(), MetodoDeConfirmacion.BIOMETRIA, hoja)
        }
    }

    fun confirmarConContrasena(contrasena: String) {
        val hoja = _estado.value.paso as? PasoDeRevocacion.Hoja ?: return
        if (hoja.verificando || contrasena.isEmpty()) return
        _estado.update { it.copy(paso = hoja.copy(verificando = true, sinConexion = false)) }
        viewModelScope.launch {
            resolver(confirmador.conContrasena(contrasena), MetodoDeConfirmacion.CONTRASENA, hoja)
        }
    }

    private suspend fun resolver(resultado: ResultadoDeConfirmacion, metodo: MetodoDeConfirmacion, hoja: PasoDeRevocacion.Hoja) {
        when (resultado) {
            ResultadoDeConfirmacion.Confirmada -> enviar(metodo)
            ResultadoDeConfirmacion.NoConfirmada -> _estado.update { it.copy(paso = PasoDeRevocacion.Oculto) }
            ResultadoDeConfirmacion.HuellaNoReconocida ->
                _estado.update { it.copy(paso = PasoDeRevocacion.Oculto, aviso = AvisoDeLaRevocacion.HUELLA_NO_RECONOCIDA) }
            ResultadoDeConfirmacion.ContrasenaIncorrecta ->
                _estado.update { it.copy(paso = PasoDeRevocacion.Oculto, aviso = AvisoDeLaRevocacion.CONTRASENA_INCORRECTA) }
            ResultadoDeConfirmacion.SinConexion -> _estado.update { it.copy(paso = hoja.copy(verificando = false, sinConexion = true)) }
        }
    }

    private suspend fun enviar(metodo: MetodoDeConfirmacion) {
        val consentimiento = _estado.value.consentimiento ?: run {
            _estado.update { it.copy(paso = PasoDeRevocacion.Oculto) }
            return
        }
        _estado.update { it.copy(paso = PasoDeRevocacion.Enviando) }
        val (paso, aviso) = when (val r = revocaciones.revocar(consentimiento, metodo)) {
            is ResultadoDeRevocacion.Revocado -> PasoDeRevocacion.Revocado(r.revocacion) to null
            // La copia y la cola ya dicen «pendiente de envio»: lo muestra el detalle.
            ResultadoDeRevocacion.PendienteDeEnvio -> PasoDeRevocacion.Oculto to null
            ResultadoDeRevocacion.YaRevocado -> PasoDeRevocacion.Oculto to AvisoDeLaRevocacion.YA_REVOCADO
            ResultadoDeRevocacion.YaNoActivo -> PasoDeRevocacion.Oculto to AvisoDeLaRevocacion.YA_NO_ACTIVO
            ResultadoDeRevocacion.SinSesion, ResultadoDeRevocacion.Error -> PasoDeRevocacion.Oculto to AvisoDeLaRevocacion.NO_SE_PUDO
        }
        _estado.update { it.copy(paso = paso, aviso = aviso) }
    }

    /** La pantalla ya navego a la de revocado: el detalle vuelve a su estado de partida. */
    fun alMostrarRevocado() {
        if (_estado.value.paso is PasoDeRevocacion.Revocado) {
            _estado.update { it.copy(paso = PasoDeRevocacion.Oculto) }
        }
    }

    /** «Reintentar envio ahora» (criterio 5): solo con señal. */
    fun reintentarAhora() {
        if (!_estado.value.reintentarHabilitado) return
        _estado.update { it.copy(reenviando = true) }
        viewModelScope.launch {
            revocaciones.enviarPendientes()
            _estado.update { it.copy(reenviando = false) }
        }
    }

    /**
     * Guarda el comprobante en el destino que el titular eligio con el
     * selector del sistema, en el idioma de la aplicacion.
     */
    fun guardarComprobante(destino: String?) {
        if (destino == null || !_estado.value.comprobanteDisponible || _estado.value.descargando) return
        _estado.update { it.copy(descargando = true, aviso = null) }
        viewModelScope.launch {
            val idioma = if (idiomas.actual.value == Idioma.INGLES) "en" else "es"
            val aviso = when (val r = revocaciones.comprobante(id, idioma)) {
                is ResultadoDeComprobante.Descargado ->
                    if (guardador.guardar(destino, r.bytes)) AvisoDeLaRevocacion.COMPROBANTE_GUARDADO else AvisoDeLaRevocacion.COMPROBANTE_ERROR
                ResultadoDeComprobante.SinConexion -> AvisoDeLaRevocacion.COMPROBANTE_SIN_CONEXION
                ResultadoDeComprobante.Error -> AvisoDeLaRevocacion.COMPROBANTE_ERROR
            }
            _estado.update { it.copy(descargando = false, aviso = aviso) }
        }
    }

    fun descartarAviso() {
        _estado.update { it.copy(aviso = null) }
        _estado.value.descarte?.let { revocaciones.descartarAviso(it.consentimientoId) }
    }
}
