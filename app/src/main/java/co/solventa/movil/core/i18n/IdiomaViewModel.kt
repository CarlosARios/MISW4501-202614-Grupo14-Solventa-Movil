package co.solventa.movil.core.i18n

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * El idioma de la aplicación, para la pantalla que lo muestra y la que lo
 * cambia.
 *
 * Vive en `core` porque lo comparten la bienvenida (M0) y, más adelante, la
 * pantalla de idioma del perfil: el idioma no es de ningún flujo.
 */
@HiltViewModel
class IdiomaViewModel @Inject constructor(
    private val repositorio: IdiomaRepositorio,
) : ViewModel() {

    val actual: StateFlow<Idioma> = repositorio.actual

    fun elegir(idioma: Idioma) = repositorio.elegir(idioma)
}
