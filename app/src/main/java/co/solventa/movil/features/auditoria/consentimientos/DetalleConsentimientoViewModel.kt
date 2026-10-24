package co.solventa.movil.features.auditoria.consentimientos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * El detalle de un consentimiento (HU-M1.3-01, criterio 2). Se lee de la copia
 * local, asi que tambien sirve sin conexion; es de solo lectura (criterio 4).
 */
@HiltViewModel
class DetalleConsentimientoViewModel internal constructor(
    estadoGuardado: SavedStateHandle,
    repositorio: ConsentimientosRepositorio,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(estadoGuardado: SavedStateHandle, repositorio: ConsentimientosRepositorio) :
        this(estadoGuardado, repositorio, System::currentTimeMillis)

    private val _estado = MutableStateFlow(DetalleConsentimientoUiState())
    val estado: StateFlow<DetalleConsentimientoUiState> = _estado.asStateFlow()

    init {
        // La ruta tipada Ruta.DetalleConsentimiento guarda su argumento con el
        // nombre de la propiedad.
        val id = estadoGuardado.get<String>(ARGUMENTO_ID).orEmpty()
        viewModelScope.launch {
            repositorio.observarUno(id).collect { consentimiento ->
                _estado.value = DetalleConsentimientoUiState(
                    cargando = false,
                    consentimiento = consentimiento?.visibleEn(reloj()),
                    noDisponible = consentimiento == null,
                )
            }
        }
    }

    companion object {
        const val ARGUMENTO_ID = "id"
    }
}
