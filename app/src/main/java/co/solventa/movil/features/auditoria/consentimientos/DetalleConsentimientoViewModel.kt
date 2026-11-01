package co.solventa.movil.features.auditoria.consentimientos

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
 * El detalle de un consentimiento (HU-M1.3-01, criterio 2). Se lee de la copia
 * local, asi que tambien sirve sin conexion; es de solo lectura (criterio 4).
 * Sigue ademas la conexion, para que el ultimo paso del historial diga si lo
 * que se ve esta sincronizado (frame 37:1418), y la cola de revocaciones, para
 * mostrarlo «pendiente de envio» (HU-M1.3-02).
 */
@HiltViewModel
class DetalleConsentimientoViewModel internal constructor(
    estadoGuardado: SavedStateHandle,
    repositorio: ConsentimientosRepositorio,
    revocaciones: RevocacionRepositorio,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(estadoGuardado: SavedStateHandle, repositorio: ConsentimientosRepositorio, revocaciones: RevocacionRepositorio) :
        this(estadoGuardado, repositorio, revocaciones, System::currentTimeMillis)

    private val _estado = MutableStateFlow(DetalleConsentimientoUiState())
    val estado: StateFlow<DetalleConsentimientoUiState> = _estado.asStateFlow()

    init {
        // La ruta tipada Ruta.DetalleConsentimiento guarda su argumento con el
        // nombre de la propiedad.
        val id = estadoGuardado.get<String>(ARGUMENTO_ID).orEmpty()
        viewModelScope.launch {
            combine(repositorio.observarUno(id), repositorio.conectado, revocaciones.pendientes()) { consentimiento, conectado, pendientes ->
                DetalleConsentimientoUiState(
                    cargando = false,
                    consentimiento = consentimiento?.visibleEn(reloj()),
                    noDisponible = consentimiento == null,
                    sinConexion = !conectado,
                    pendiente = pendientes.firstOrNull { it.consentimientoId == id },
                )
            }.collect { _estado.value = it }
        }
    }

    companion object {
        const val ARGUMENTO_ID = "id"
    }
}
