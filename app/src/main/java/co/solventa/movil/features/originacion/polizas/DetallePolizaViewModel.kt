package co.solventa.movil.features.originacion.polizas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * M5 · Detalle de la póliza (HU-M1.2-01, criterio 2). Se lee de la copia
 * local cifrada, así que también sirve sin conexión: número, tipo de seguro,
 * vigencia, coberturas con sus montos y asegurado principal.
 */
@HiltViewModel
class DetallePolizaViewModel internal constructor(
    estadoGuardado: SavedStateHandle,
    repositorio: PolizasRepositorio,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(estadoGuardado: SavedStateHandle, repositorio: PolizasRepositorio) :
        this(estadoGuardado, repositorio, System::currentTimeMillis)

    private val _estado = MutableStateFlow(DetallePolizaUiState())
    val estado: StateFlow<DetallePolizaUiState> = _estado.asStateFlow()

    init {
        // La ruta tipada Ruta.DetallePoliza guarda su argumento con el nombre
        // de la propiedad.
        val id = estadoGuardado.get<String>(ARGUMENTO_ID).orEmpty()
        viewModelScope.launch {
            combine(
                repositorio.observarUna(id),
                repositorio.observar().map { it.sincronizadoEn to it.sinConexion },
                repositorio.cambios,
            ) { poliza, (sincronizadoEn, sinConexion), cambios ->
                val cambio = cambios.firstOrNull { it.poliza.id == id }
                DetallePolizaUiState(
                    cargando = false,
                    poliza = poliza?.let {
                        PolizaEnLista(
                            it,
                            PresentacionDePolizas.situacion(it, reloj()),
                            renovada = it.renovacionDe != null || cambio?.tipo == TipoDeCambio.RENOVADA,
                        )
                    },
                    noDisponible = poliza == null,
                    sinConexion = sinConexion,
                    sincronizadoEn = sincronizadoEn,
                    cambio = cambio,
                )
            }.collect { _estado.value = it }
        }
    }

    companion object {
        const val ARGUMENTO_ID = "id"
    }
}
