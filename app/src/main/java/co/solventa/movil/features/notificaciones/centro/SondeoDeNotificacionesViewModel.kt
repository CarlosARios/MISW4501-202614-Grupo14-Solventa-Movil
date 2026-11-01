package co.solventa.movil.features.notificaciones.centro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * El sondeo del centro mientras la aplicación está a la vista (HU-M3.1-01).
 * Sin proveedor de *push* real, es lo que hace que un pago nuevo se avise en
 * pocos segundos con la aplicación abierta (criterio 1); con la aplicación en
 * segundo plano lo hace la revisión periódica, que este sondeo programa.
 *
 * Vive en el `NavHost` porque la aplicación puede estar en cualquier pantalla,
 * y es también por donde llega la notificación que el usuario tocó
 * (criterio 4).
 */
@HiltViewModel
class SondeoDeNotificacionesViewModel internal constructor(
    private val revisor: RevisorDeNotificaciones,
    private val programador: ProgramadorDeRevision,
    private val apertura: AperturaDesdeNotificacion,
    private val intervaloMs: Long,
) : ViewModel() {

    @Inject
    constructor(revisor: RevisorDeNotificaciones, programador: ProgramadorDeRevision, apertura: AperturaDesdeNotificacion) :
        this(revisor, programador, apertura, INTERVALO_MS)

    /** La notificación que el usuario tocó y espera abrirse. */
    val pendiente: StateFlow<String?> = apertura.pendiente

    /**
     * Revisa el centro cada [INTERVALO_MS] mientras quien llama lo mantenga
     * activo: el `NavHost` lo corre solo con la aplicación a la vista.
     */
    suspend fun mientrasVisible() {
        programador.programar()
        while (true) {
            revisor.revisar()
            delay(intervaloMs)
        }
    }

    /** El usuario respondió al permiso de notificaciones: se registra en seguida. */
    fun permisoRespondido() {
        viewModelScope.launch { revisor.revisar() }
    }

    /** Entrega la notificación tocada, una sola vez, para abrir su detalle. */
    fun abrirPendiente(): String? = apertura.consumir()

    companion object {
        /** Cada cuánto se revisa con la aplicación a la vista: muy por debajo de los 60 s. */
        const val INTERVALO_MS = 20_000L
    }
}
