package co.solventa.movil.features.notificaciones.centro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * M10b · Siniestro paramétrico resuelto automáticamente (HU-M3.1-01): lo que
 * abre la notificación (criterio 4). Se lee de la copia local; si el usuario
 * tocó un aviso de una notificación que la copia todavía no tiene, la trae
 * del servicio una vez. Abrirla la marca leída.
 */
@HiltViewModel
class DetallePagoViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val repositorio: NotificacionesRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow(DetallePagoUiState())
    val estado: StateFlow<DetallePagoUiState> = _estado.asStateFlow()

    // La ruta tipada Ruta.DetallePagoParametrico guarda su argumento con el
    // nombre de la propiedad.
    private val id = estadoGuardado.get<String>(ARGUMENTO_ID).orEmpty()
    private var sincronizado = false
    private var leida = false

    init {
        viewModelScope.launch {
            combine(repositorio.observarUna(id), repositorio.conectado) { n, red -> n to red }.collect { (n, red) ->
                if (n == null && !sincronizado) {
                    sincronizado = true
                    _estado.value = DetallePagoUiState(cargando = true, sinConexion = !red)
                    repositorio.sincronizar()
                    // Si la trajo, la copia la emite enseguida; si no, no está.
                    if (_estado.value.cargando) {
                        _estado.value = DetallePagoUiState(cargando = false, noDisponible = true, sinConexion = !red)
                    }
                    return@collect
                }
                if (n != null && !leida) {
                    leida = true
                    repositorio.marcarLeida(id)
                }
                _estado.value = DetallePagoUiState(cargando = false, notificacion = n, noDisponible = n == null, sinConexion = !red)
            }
        }
    }

    companion object {
        const val ARGUMENTO_ID = "id"
    }
}
