package co.solventa.movil.features.notificaciones.centro

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La notificación que el usuario tocó y todavía no se abrió (HU-M3.1-01,
 * criterio 4). `MainActivity` la deja aquí al recibir el *intent* y el
 * `NavHost` abre su destino en cuanto hay una sesión abierta: si la sesión
 * estaba cerrada, primero se autentica y luego va directo. La de un cambio de
 * etapa trae además el siniestro, y abre su línea de tiempo (HU-M3.3-01).
 */
@Singleton
class AperturaDesdeNotificacion @Inject constructor() {

    private val _pendiente = MutableStateFlow<DestinoDeNotificacion?>(null)
    val pendiente: StateFlow<DestinoDeNotificacion?> = _pendiente.asStateFlow()

    /** Lee el *intent* de la notificación; uno sin ella no cambia nada. */
    fun recibir(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_NOTIFICACION)?.takeIf { it.isNotBlank() } ?: return
        val siniestro = intent.getStringExtra(EXTRA_SINIESTRO)?.takeIf { it.isNotBlank() }
        _pendiente.value = siniestro?.let { DestinoDeNotificacion.Siniestro(it, id) } ?: DestinoDeNotificacion.Pago(id)
    }

    /** La entrega una sola vez: quien la abre la consume. */
    fun consumir(): DestinoDeNotificacion? = _pendiente.value.also { _pendiente.value = null }

    companion object {
        const val EXTRA_NOTIFICACION = "co.solventa.movil.notificacion"

        /** El siniestro de una notificación de cambio de etapa (HU-M3.3-01). */
        const val EXTRA_SINIESTRO = "co.solventa.movil.siniestro"
    }
}
