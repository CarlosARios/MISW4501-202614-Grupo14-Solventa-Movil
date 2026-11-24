package co.solventa.movil.features.siniestros.seguimiento

import androidx.lifecycle.SavedStateHandle
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
 * M10 · Detalle del siniestro con su línea de tiempo (HU-M2.4-01), con el
 * aviso de versión en caché de HA-L02. Pide el detalle y el historial a la
 * vez y arma la línea de tiempo con [LineaDeTiempo]: la automática si un
 * evento externo resolvió el siniestro. Como en M9, una respuesta desde la
 * caché se vuelve a consultar sola. Sin red muestra la última versión guardada
 * en el teléfono (M10c), con su hora.
 */
@HiltViewModel
class DetalleSiniestroViewModel internal constructor(
    estadoGuardado: SavedStateHandle,
    private val repositorio: ConsultaDeSiniestrosRepositorio,
    private val esperaDeReintento: Long,
) : ViewModel() {

    @Inject
    constructor(estadoGuardado: SavedStateHandle, repositorio: ConsultaDeSiniestrosRepositorio) :
        this(estadoGuardado, repositorio, ConsultaAutomatica.ESPERA_MS)

    // La ruta tipada Ruta.DetalleSiniestro guarda su argumento con el nombre
    // de la propiedad.
    private val id: String = estadoGuardado.get<String>(ARGUMENTO_ID).orEmpty()

    private val _estado = MutableStateFlow(DetalleSiniestroUiState())
    val estado: StateFlow<DetalleSiniestroUiState> = _estado.asStateFlow()

    private var consulta: Job? = null
    private var reintentoAutomatico: Job? = null

    init {
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        if (consulta?.isActive == true) return
        reintentoAutomatico?.cancel()
        _estado.update { it.copy(recargando = !it.cargando) }
        consulta = viewModelScope.launch {
            val resultado = repositorio.detalle(id)
            _estado.update { anterior ->
                when (resultado) {
                    is ResultadoDeConsulta.Exito -> estadoDe(resultado.valor).copy(frescura = resultado.frescura)
                    is ResultadoDeConsulta.CopiaLocal -> estadoDe(resultado.valor).copy(copiaLocalDe = resultado.guardadoEn)
                    else -> anterior.copy(cargando = false, recargando = false, fallo = falloDe(resultado))
                }
            }
            if (resultado is ResultadoDeConsulta.Exito && resultado.frescura.desdeCache) {
                reintentoAutomatico = viewModelScope.launch {
                    delay(esperaDeReintento)
                    cargar()
                }
            }
        }
    }

    private fun estadoDe(detalle: DetalleDeSiniestro): DetalleSiniestroUiState {
        val siniestro = detalle.siniestro
        return DetalleSiniestroUiState(
            cargando = false,
            siniestro = siniestro,
            etapas = LineaDeTiempo.armar(siniestro, detalle.cambios),
            progreso = LineaDeTiempo.progreso(siniestro),
            estimacion = detalle.estimacion,
        )
    }

    companion object {
        const val ARGUMENTO_ID = "id"
    }
}
