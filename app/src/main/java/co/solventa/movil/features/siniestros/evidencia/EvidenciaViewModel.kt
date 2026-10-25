package co.solventa.movil.features.siniestros.evidencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.solventa.movil.core.ubicacion.Localizador
import co.solventa.movil.core.ubicacion.Ubicacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Estado de la captura de evidencia (M12).
 *
 * La lista viene de la base del dispositivo, así que el estado de envío de
 * cada evidencia se ve avanzar sin que la pantalla pregunte nada.
 */
data class EvidenciaUiState(
    val borradorId: String = "",
    val evidencias: List<EvidenciaDelReporte> = emptyList(),
    val ubicacion: Ubicacion? = null,
    val capturando: Boolean = false,
    val grabando: Boolean = false,
    /** Cuando la precisión no alcanza, M12c pregunta antes de capturar. */
    val confirmandoPrecision: TipoDeEvidencia? = null,
) {
    val fotos: Int
        get() = evidencias.count { it.tipo == TipoDeEvidencia.FOTO }

    val videos: Int
        get() = evidencias.count { it.tipo == TipoDeEvidencia.VIDEO }

    /** «Evidencia 5 de 6» del encabezado del frame. */
    val numeroDeLaSiguiente: Int
        get() = (fotos + 1).coerceAtMost(LimitesDeEvidencia.MAXIMO_DE_FOTOS)

    val puedeTomarFoto: Boolean
        get() = fotos < LimitesDeEvidencia.MAXIMO_DE_FOTOS && !capturando && !grabando

    val puedeGrabarVideo: Boolean
        get() = videos < LimitesDeEvidencia.MAXIMO_DE_VIDEOS && !capturando

    val ubicacionConfiable: Boolean
        get() = ubicacion?.esConfiable == true
}

@HiltViewModel
class EvidenciaViewModel @Inject constructor(
    private val repositorio: EvidenciaRepositorio,
    private val localizador: Localizador,
) : ViewModel() {

    private val _estado = MutableStateFlow(EvidenciaUiState())
    val estado: StateFlow<EvidenciaUiState> = _estado.asStateFlow()

    /** La pantalla llama a esto al abrirse con el borrador en curso. */
    fun abrir(borradorId: String) {
        if (_estado.value.borradorId == borradorId) return
        _estado.update { it.copy(borradorId = borradorId) }
        viewModelScope.launch {
            repositorio.observar(borradorId).collect { evidencias ->
                _estado.update { it.copy(evidencias = evidencias) }
            }
        }
        refrescarUbicacion()
        viewModelScope.launch { repositorio.reintentarEnvios(borradorId) }
    }

    /** Vuelve a leer la ubicación: la precisión mejora al quedarse quieto. */
    fun refrescarUbicacion() {
        viewModelScope.launch {
            val ubicacion = localizador.ubicacionActual()
            _estado.update { it.copy(ubicacion = ubicacion) }
        }
    }

    /**
     * Criterio 3: con la ubicación poco precisa se avisa antes de capturar.
     * Devuelve `true` cuando se puede capturar de una vez.
     */
    fun intentarCapturar(tipo: TipoDeEvidencia): Boolean {
        if (_estado.value.ubicacionConfiable) return true
        _estado.update { it.copy(confirmandoPrecision = tipo) }
        return false
    }

    /** M12c · *Esperar mejor señal*. */
    fun esperarMejorSenal() {
        _estado.update { it.copy(confirmandoPrecision = null) }
        refrescarUbicacion()
    }

    /** M12c · *Capturar de todas formas*: la evidencia queda marcada. */
    fun capturarDeTodasFormas(): TipoDeEvidencia? {
        val tipo = _estado.value.confirmandoPrecision
        _estado.update { it.copy(confirmandoPrecision = null) }
        return tipo
    }

    /** La pantalla avisa que la cámara está tomando la foto o grabando. */
    fun marcarCapturando(capturando: Boolean) {
        _estado.update { it.copy(capturando = capturando) }
    }

    fun marcarGrabando(grabando: Boolean) {
        _estado.update { it.copy(grabando = grabando) }
    }

    /**
     * Registra lo capturado con la ubicación del momento y pide su envío.
     * El archivo se borra si no se puede leer: no sirve de evidencia.
     */
    fun registrar(archivo: File, tipo: TipoDeEvidencia, duracionS: Int = 0) {
        val estadoActual = _estado.value
        viewModelScope.launch {
            _estado.update { it.copy(capturando = false, grabando = false) }
            val guardada = repositorio.capturar(
                borradorId = estadoActual.borradorId,
                archivo = archivo,
                tipo = tipo,
                ubicacion = estadoActual.ubicacion,
                duracionS = duracionS,
            )
            if (!guardada) {
                runCatching { archivo.delete() }
            }
            refrescarUbicacion()
        }
    }

    fun descartar(id: String) {
        viewModelScope.launch { repositorio.descartar(id) }
    }
}
