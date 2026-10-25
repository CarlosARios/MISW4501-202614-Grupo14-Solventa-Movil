package co.solventa.movil.features.auditoria.consentimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * La consulta de los consentimientos del titular (HU-M1.3-01).
 *
 * Muestra siempre la copia local (criterio 5) y la actualiza al abrir la
 * pantalla y, mientras esta abierta, cada vez que la red vuelve: el titular no
 * tiene que tocar nada para ver lo nuevo.
 */
@HiltViewModel
class ConsentimientosViewModel internal constructor(
    private val repositorio: ConsentimientosRepositorio,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(repositorio: ConsentimientosRepositorio) : this(repositorio, System::currentTimeMillis)

    private val _estado = MutableStateFlow(ConsentimientosUiState())
    val estado: StateFlow<ConsentimientosUiState> = _estado.asStateFlow()

    /** El ultimo resultado de pedir la lista; null mientras no ha terminado ninguno. */
    private var ultimo: ResultadoDeActualizacion? = null
    private var copia = CopiaDeConsentimientos(emptyList(), null, sinConexion = false)

    init {
        viewModelScope.launch {
            repositorio.observar().collect {
                copia = it
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

    private fun actualizar() {
        viewModelScope.launch {
            ultimo = repositorio.actualizar()
            componer()
        }
    }

    private fun componer() {
        val grupos = agrupar(copia.consentimientos, reloj())
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
            )
        }
    }
}
