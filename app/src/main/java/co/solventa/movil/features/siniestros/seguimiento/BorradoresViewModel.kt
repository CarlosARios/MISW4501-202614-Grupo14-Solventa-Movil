package co.solventa.movil.features.siniestros.seguimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.solventa.movil.features.siniestros.evidencia.EvidenciaRepositorio
import co.solventa.movil.features.siniestros.radicacion.BorradorRepositorio
import co.solventa.movil.features.siniestros.radicacion.BorradorResumen
import co.solventa.movil.features.siniestros.radicacion.EnvioDelBorrador
import co.solventa.movil.features.siniestros.radicacion.envio.ColaDeRadicacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Los borradores de *Mis siniestros* (HU-M2.2-02, M13b–M13d). */
data class BorradoresUiState(
    /** Los que siguen en el teléfono: en edición, en cola, enviándose o fallidos. */
    val borradores: List<BorradorResumen> = emptyList(),
    /** M13c: el último que se envió solo y quedó radicado, hasta cerrar el aviso. */
    val radicado: BorradorResumen? = null,
    /** M13b: la hoja «Tienes un reporte sin terminar». */
    val hoja: BorradorResumen? = null,
    /** Se eligió continuar: la pantalla abre el asistente. */
    val abrirAsistente: Boolean = false,
) {
    /** M13d: los que agotaron los reintentos automáticos. */
    val fallidos: List<BorradorResumen>
        get() = borradores.filter { it.envio == EnvioDelBorrador.FALLIDO }
}

/**
 * La hoja de M13b se ofrece sola una vez por apertura de la aplicación: al
 * volver a la pestaña ya se sabe que el borrador está ahí.
 */
@Singleton
class RecordatorioDeBorrador @Inject constructor() {
    var mostrado: Boolean = false
}

/**
 * Los borradores del asegurado en *Mis siniestros* (HU-M2.2-02).
 *
 * Al abrir la pantalla vuelve a encolar lo que estaba esperando su envío: si
 * el teléfono se reinició o entró otra cuenta mientras tanto, el trabajo
 * pudo haberse ido sin enviar nada. La cola no duplica un trabajo vivo.
 */
@HiltViewModel
class BorradoresViewModel @Inject constructor(
    private val borradores: BorradorRepositorio,
    private val evidencias: EvidenciaRepositorio,
    private val cola: ColaDeRadicacion,
    private val recordatorio: RecordatorioDeBorrador,
) : ViewModel() {

    private val _estado = MutableStateFlow(BorradoresUiState())
    val estado: StateFlow<BorradoresUiState> = _estado.asStateFlow()

    private var primeraLectura = true

    init {
        viewModelScope.launch {
            borradores.delUsuario().collect { todos ->
                if (primeraLectura) {
                    primeraLectura = false
                    todos.filter { it.envio == EnvioDelBorrador.EN_COLA || it.envio == EnvioDelBorrador.ENVIANDO }
                        .forEach { cola.encolar(it.id) }
                    if (!recordatorio.mostrado) {
                        recordatorio.mostrado = true
                        val enEdicion = todos.firstOrNull { it.envio == EnvioDelBorrador.EDICION }
                        _estado.update { it.copy(hoja = enEdicion) }
                    }
                }
                _estado.update { anterior ->
                    anterior.copy(
                        borradores = todos.filterNot { it.envio == EnvioDelBorrador.RADICADO },
                        radicado = todos.filter { it.envio == EnvioDelBorrador.RADICADO }
                            .maxByOrNull { it.radicadoEn ?: 0L },
                    )
                }
            }
        }
    }

    /** Tocar una tarjeta: en edición o fallido se ofrece continuar o descartar. */
    fun abrir(id: String) {
        val borrador = _estado.value.borradores.firstOrNull { it.id == id } ?: return
        if (borrador.envio == EnvioDelBorrador.EDICION || borrador.envio == EnvioDelBorrador.FALLIDO) {
            _estado.update { it.copy(hoja = borrador) }
        }
    }

    fun cerrarHoja() = _estado.update { it.copy(hoja = null) }

    /** *Continuar el borrador*: queda de primero para que el asistente lo retome. */
    fun continuar() {
        val id = _estado.value.hoja?.id ?: return
        viewModelScope.launch {
            cola.cancelar(id)
            borradores.retomar(id)
            _estado.update { it.copy(hoja = null, abrirAsistente = true) }
        }
    }

    fun asistenteAbierto() = _estado.update { it.copy(abrirAsistente = false) }

    /** *Descartar borrador*: se va con sus evidencias y su envío pendiente. */
    fun descartar() {
        val id = _estado.value.hoja?.id ?: return
        _estado.update { it.copy(hoja = null) }
        viewModelScope.launch {
            cola.cancelar(id)
            evidencias.limpiar(id)
            borradores.descartar(id)
        }
    }

    /**
     * *Reintentar ahora* (M13d): los intentos vuelven a cero y el envío sale
     * en cuanto haya red, con la misma clave: no se duplica el radicado.
     */
    fun reintentarEnvios() {
        val fallidos = _estado.value.fallidos
        viewModelScope.launch {
            fallidos.forEach {
                borradores.encolar(it.id)
                cola.reintentar(it.id)
            }
        }
    }

    /** Cerrar el aviso de M13c: el borrador ya cumplió y deja el teléfono. */
    fun cerrarAvisoDeRadicado() {
        val id = _estado.value.radicado?.id ?: return
        viewModelScope.launch { borradores.descartar(id) }
    }
}
