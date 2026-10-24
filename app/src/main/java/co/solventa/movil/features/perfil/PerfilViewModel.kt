package co.solventa.movil.features.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.solventa.movil.BuildConfig
import co.solventa.movil.core.auth.biometria.AutenticadorBiometrico
import co.solventa.movil.core.auth.biometria.DisponibilidadBiometrica
import co.solventa.movil.core.auth.sesion.ControlDeInactividad
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaRepositorio
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * M19 · Perfil (HU-M1.3-01, D-7 e): la identidad de la sesion, la biometria,
 * el idioma, los consentimientos activos y el cierre de la sesion. Las demas
 * filas del prototipo quedan en espera en la pantalla, sin datos inventados.
 */
@HiltViewModel
class PerfilViewModel internal constructor(
    private val sesion: SesionRepositorio,
    private val autenticador: AutenticadorBiometrico,
    private val idiomas: IdiomaRepositorio,
    private val consentimientos: ConsentimientoDao,
    version: String,
    private val reloj: () -> Long,
) : ViewModel() {

    @Inject
    constructor(
        sesion: SesionRepositorio,
        autenticador: AutenticadorBiometrico,
        idiomas: IdiomaRepositorio,
        consentimientos: ConsentimientoDao,
    ) : this(sesion, autenticador, idiomas, consentimientos, BuildConfig.VERSION_NAME, System::currentTimeMillis)

    private val _estado = MutableStateFlow(
        PerfilUiState(
            idioma = idiomas.actual.value,
            version = version,
            minutosDeCierre = (ControlDeInactividad.LIMITE_DE_INACTIVIDAD_MS / MS_POR_MINUTO).toInt(),
        ),
    )
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val guardada = sesion.sesionGuardada()
            val nombre = guardada?.nombre.orEmpty()
            _estado.update {
                it.copy(
                    nombre = nombre,
                    correo = guardada?.correo.orEmpty(),
                    iniciales = iniciales(nombre),
                    biometriaHabilitada = sesion.biometriaHabilitada(),
                    biometriaDisponible = autenticador.disponibilidad() == DisponibilidadBiometrica.DISPONIBLE,
                )
            }
            val sub = guardada?.sub.orEmpty()
            if (sub.isNotEmpty()) {
                consentimientos.contarVigentes(sub, reloj()).collect { n -> _estado.update { it.copy(activos = n) } }
            }
        }
        viewModelScope.launch {
            idiomas.actual.collect { idioma -> _estado.update { it.copy(idioma = idioma) } }
        }
    }

    /** Sin huella o rostro configurado el interruptor no hace nada. */
    fun alternarBiometria(habilitada: Boolean) {
        if (!_estado.value.biometriaDisponible) return
        viewModelScope.launch {
            sesion.habilitarBiometria(habilitada)
            _estado.update { it.copy(biometriaHabilitada = habilitada) }
        }
    }

    fun elegirIdioma(idioma: Idioma) = idiomas.elegir(idioma)

    /**
     * Cierra la sesion como desde Inicio. El repositorio de la sesion borra
     * ademas la copia local de los consentimientos (criterio 6 de HU-M1.3-01).
     */
    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            sesion.cerrar()
            alTerminar()
        }
    }

    companion object {
        private const val MS_POR_MINUTO = 60_000L

        /** «Carolina Pérez Ruiz» → «CP»: las iniciales de las dos primeras palabras. */
        fun iniciales(nombre: String): String =
            nombre.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    }
}
