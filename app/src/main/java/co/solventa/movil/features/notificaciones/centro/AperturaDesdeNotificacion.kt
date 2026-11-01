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
 * `NavHost` abre su detalle en cuanto hay una sesión abierta: si la sesión
 * estaba cerrada, primero se autentica y luego va directo al detalle.
 */
@Singleton
class AperturaDesdeNotificacion @Inject constructor() {

    private val _pendiente = MutableStateFlow<String?>(null)
    val pendiente: StateFlow<String?> = _pendiente.asStateFlow()

    /** Lee el *intent* de la notificación; uno sin ella no cambia nada. */
    fun recibir(intent: Intent?) {
        intent?.getStringExtra(EXTRA_NOTIFICACION)?.takeIf { it.isNotBlank() }?.let { _pendiente.value = it }
    }

    /** La entrega una sola vez: quien la abre la consume. */
    fun consumir(): String? = _pendiente.value.also { _pendiente.value = null }

    companion object {
        const val EXTRA_NOTIFICACION = "co.solventa.movil.notificacion"
    }
}
