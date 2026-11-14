package co.solventa.movil.features.siniestros.seguimiento

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
 * M9 · Mis siniestros (HU-M2.4-01), con el estado degradado de M9c (HA-L02).
 *
 * Consulta al abrir y separa los casos en curso de los cerrados, en el orden
 * del servicio (el más reciente primero). Cuando el servicio responde desde
 * su caché, la pantalla lo avisa y la consulta se repite sola cada
 * [ConsultaAutomatica.ESPERA_MS] mientras siga abierta, además del botón
 * *Reintentar ahora*. Un fallo no borra lo que ya se estaba mostrando.
 */
@HiltViewModel
class MisSiniestrosViewModel internal constructor(
    private val repositorio: ConsultaDeSiniestrosRepositorio,
    private val esperaDeReintento: Long,
) : ViewModel() {

    @Inject
    constructor(repositorio: ConsultaDeSiniestrosRepositorio) : this(repositorio, ConsultaAutomatica.ESPERA_MS)

    private val _estado = MutableStateFlow(MisSiniestrosUiState())
    val estado: StateFlow<MisSiniestrosUiState> = _estado.asStateFlow()

    private var consulta: Job? = null
    private var reintentoAutomatico: Job? = null

    init {
        cargar()
    }

    /** *Reintentar ahora* (M9c) o el reintento desde el error. */
    fun reintentar() = cargar()

    private fun cargar() {
        if (consulta?.isActive == true) return
        reintentoAutomatico?.cancel()
        _estado.update { it.copy(recargando = !it.cargando) }
        consulta = viewModelScope.launch {
            val resultado = repositorio.listar()
            _estado.update { anterior ->
                when (resultado) {
                    is ResultadoDeConsulta.Exito -> MisSiniestrosUiState(
                        cargando = false,
                        enCurso = resultado.valor.filterNot { it.estado.cerrado },
                        cerrados = resultado.valor.filter { it.estado.cerrado },
                        frescura = resultado.frescura,
                    )
                    // Sin red, la última lista guardada en el teléfono (HU-M2.4-01, criterio 5).
                    is ResultadoDeConsulta.CopiaLocal -> MisSiniestrosUiState(
                        cargando = false,
                        enCurso = resultado.valor.filterNot { it.estado.cerrado },
                        cerrados = resultado.valor.filter { it.estado.cerrado },
                        copiaLocalDe = resultado.guardadoEn,
                    )
                    else -> anterior.copy(cargando = false, recargando = false, fallo = falloDe(resultado))
                }
            }
            if (resultado is ResultadoDeConsulta.Exito && resultado.frescura.desdeCache) programarReintento()
        }
    }

    private fun programarReintento() {
        reintentoAutomatico = viewModelScope.launch {
            delay(esperaDeReintento)
            cargar()
        }
    }
}

/** El reintento automático de M9c: «reintentaremos automáticamente». */
object ConsultaAutomatica {
    const val ESPERA_MS = 30_000L
}

/** El fallo que se muestra para un resultado que no es un éxito. */
internal fun falloDe(resultado: ResultadoDeConsulta<*>): FalloDeConsulta = when (resultado) {
    ResultadoDeConsulta.SinConexion -> FalloDeConsulta.SIN_CONEXION
    ResultadoDeConsulta.NoEncontrado -> FalloDeConsulta.NO_ENCONTRADO
    else -> FalloDeConsulta.ERROR
}
