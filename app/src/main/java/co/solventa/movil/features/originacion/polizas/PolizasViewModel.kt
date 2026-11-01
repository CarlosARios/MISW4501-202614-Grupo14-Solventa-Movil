package co.solventa.movil.features.originacion.polizas

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
 * M4 · Mis pólizas (HU-M1.2-01).
 *
 * Muestra siempre la copia local cifrada (criterio 1) y la sincroniza al abrir
 * la pantalla y, mientras está abierta, cada vez que la red vuelve (criterio
 * 3): la copia se reemplaza por debajo y la pantalla se recompone sola, sin
 * bloquear lo que el asegurado está viendo. Los cambios que detecte la
 * sincronización quedan en el aviso hasta que se descartan (criterio 4), y una
 * copia de más de 30 días se advierte sin impedir consultarla (criterio 5).
 */
@HiltViewModel
class PolizasViewModel internal constructor(
    private val repositorio: PolizasRepositorio,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(repositorio: PolizasRepositorio) : this(repositorio, System::currentTimeMillis)

    private val _estado = MutableStateFlow(PolizasUiState())
    val estado: StateFlow<PolizasUiState> = _estado.asStateFlow()

    /** El último resultado de sincronizar; null mientras no ha terminado ninguno. */
    private var ultimo: ResultadoDeSincronizacion? = null
    private var copia = CopiaDePolizas(emptyList(), null, sinConexion = false)
    private var cambios: List<CambioDePoliza> = emptyList()
    private var sincronizando = false

    init {
        viewModelScope.launch {
            combine(repositorio.observar(), repositorio.cambios) { c, cs -> c to cs }.collect { (c, cs) ->
                copia = c
                cambios = cs
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

    /** Reintento del asegurado desde el error. */
    fun reintentar() = sincronizar()

    /** Cierra el aviso de cambios (criterio 4). */
    fun descartarCambios() = repositorio.descartarCambios()

    private fun sincronizar() {
        if (sincronizando) return
        sincronizando = true
        componer()
        viewModelScope.launch {
            ultimo = repositorio.sincronizar()
            sincronizando = false
            componer()
        }
    }

    private fun componer() {
        val ahora = reloj()
        val resultado = ultimo
        _estado.value = PolizasUiState(
            cargando = resultado == null && copia.polizas.isEmpty(),
            polizas = PresentacionDePolizas.tarjetas(copia.polizas, cambios, ahora),
            sinConexion = copia.sinConexion || resultado == ResultadoDeSincronizacion.SinConexion,
            sincronizadoEn = copia.sincronizadoEn,
            sincronizando = sincronizando,
            error = resultado == ResultadoDeSincronizacion.Error,
            desactualizada = PresentacionDePolizas.desactualizada(copia.sincronizadoEn, ahora, DIAS_SIN_SINCRONIZAR),
            cambios = cambios,
        )
    }

    companion object {
        /** Tiempo máximo sin sincronizar antes de advertir (criterio 5, «por ejemplo, 30 días»). */
        const val DIAS_SIN_SINCRONIZAR = 30L
    }
}
