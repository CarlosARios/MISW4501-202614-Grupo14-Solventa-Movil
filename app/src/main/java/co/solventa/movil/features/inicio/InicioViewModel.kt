package co.solventa.movil.features.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de M3 · Inicio: a quién se saluda y cómo se cierra la sesión.
 *
 * El nombre y el correo salen del *id token* que emitió el *user pool*, así
 * que lo que se ve en pantalla es la identidad real de quien entró, no un
 * dato de ejemplo.
 */
@HiltViewModel
class InicioViewModel @Inject constructor(
    private val repositorio: SesionRepositorio,
) : ViewModel() {

    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    init {
        viewModelScope.launch { _sesion.value = repositorio.sesionGuardada() }
    }

    /** Borra los tokens del dispositivo. Quien llama decide a dónde navegar. */
    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            repositorio.cerrar()
            _sesion.value = null
            alTerminar()
        }
    }
}
