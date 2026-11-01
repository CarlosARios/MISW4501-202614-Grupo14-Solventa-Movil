package co.solventa.movil.features.notificaciones.centro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * M15 · Centro de notificaciones (HU-M3.1-01). Muestra siempre la copia local
 * y la sincroniza al abrir la pantalla y cada vez que la red vuelve. Lo que el
 * asegurado ve aquí ya no se le avisa con una notificación del sistema.
 */
@HiltViewModel
class CentroViewModel internal constructor(
    private val repositorio: NotificacionesRepositorio,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(repositorio: NotificacionesRepositorio) : this(repositorio, System::currentTimeMillis)

    private val _estado = MutableStateFlow(CentroUiState())
    val estado: StateFlow<CentroUiState> = _estado.asStateFlow()

    private var ultimo: ResultadoDeSincronizacion? = null
    private var copia = CopiaDeNotificaciones(emptyList(), null, sinConexion = false)

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
                if (conectado && !habiaRed) sincronizar()
                habiaRed = conectado
            }
        }
        sincronizar()
    }

    fun reintentar() = sincronizar()

    fun marcarTodasLeidas() {
        viewModelScope.launch { repositorio.marcarTodasLeidas() }
    }

    private fun sincronizar() {
        viewModelScope.launch {
            val resultado = repositorio.sincronizar()
            if (resultado is ResultadoDeSincronizacion.Sincronizado) {
                repositorio.marcarAvisadas(resultado.nuevas.map { it.id })
            }
            ultimo = resultado
            componer()
        }
    }

    private fun componer() {
        val ahora = reloj()
        val (hoy, anteriores) = copia.notificaciones.partition { TextosDeNotificacion.esDeHoy(it.ocurrioEn, ahora) }
        val resultado = ultimo
        _estado.value = CentroUiState(
            cargando = resultado == null && copia.notificaciones.isEmpty(),
            hoy = hoy,
            anteriores = anteriores,
            sinLeer = copia.notificaciones.count { !it.leida },
            sinConexion = copia.sinConexion || resultado == ResultadoDeSincronizacion.SinConexion,
            sincronizadoEn = copia.sincronizadoEn,
            error = resultado == ResultadoDeSincronizacion.Error,
        )
    }
}
