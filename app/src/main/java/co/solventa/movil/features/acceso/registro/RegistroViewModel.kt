package co.solventa.movil.features.acceso.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.imagen.PreparadorDeCaptura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Registro del asegurado (HU-M1.1-01).
 *
 * Vive en el grafo de navegación del registro, no en una pantalla: el estado
 * tiene que sobrevivir el paso de M1a a M1h.
 *
 * **Las capturas del documento son archivos del teléfono y este ViewModel es
 * su dueño:** las borra cuando dejan de hacer falta —rechazadas por calidad,
 * reemplazadas por otra toma de la misma cara, enviadas al servicio o
 * descartadas al reiniciar—. Son la fotografía de un documento de identidad,
 * así que no se quedan esperando a que el sistema vacíe la caché.
 *
 * **Después del registro la sesión se abre sola.** El servicio acaba de
 * crear la cuenta con el correo y la contraseña que se enviaron, así que
 * pedirle al usuario que los teclee otra vez para entrar no aporta nada. Si
 * el ingreso automático falla, la cuenta igual quedó creada y el usuario
 * puede entrar por M2a.
 */
@HiltViewModel
class RegistroViewModel @Inject constructor(
    private val kyc: RegistroKycRepositorio,
    private val registro: RegistroRepositorio,
    private val generador: GeneradorDeDatosDeDemostracion,
    private val sesion: SesionRepositorio,
    private val capturas: PreparadorDeCaptura,
) : ViewModel() {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    fun capturar(imagen: ImagenDocumento) {
        if (_estado.value.analizando) return
        viewModelScope.launch {
            _estado.update { it.copy(analizando = true, problemaCalidad = null) }
            when (val resultado = kyc.analizarCaptura(imagen)) {
                is ResultadoCaptura.CalidadInsuficiente -> {
                    // La foto que no sirve no se queda en el teléfono.
                    capturas.descartar(imagen.rutaArchivo)
                    _estado.update {
                        it.copy(analizando = false, problemaCalidad = resultado.motivo)
                    }
                }

                ResultadoCaptura.Aceptada -> {
                    // Volver a tomar una cara reemplaza la foto anterior, que
                    // ya no se va a enviar.
                    descartarLaAnteriorDe(imagen)
                    _estado.update {
                        when (imagen.cara) {
                            CaraDocumento.FRENTE -> it.copy(analizando = false, frente = imagen)

                            // Los datos se generan al cerrar la captura, no se
                            // leen de la foto, y se generan una sola vez: si el
                            // usuario ya corrigió algo en M1c y vuelve atrás, no
                            // se le borra.
                            CaraDocumento.REVERSO -> it.copy(
                                analizando = false,
                                reverso = imagen,
                                datos = it.datos ?: generador.datos(),
                                contrasena = it.contrasena.ifBlank { generador.contrasena() },
                            )
                        }
                    }
                }
            }
        }
    }

    /** La toma anterior de esa misma cara, si hubo una y es otro archivo. */
    private suspend fun descartarLaAnteriorDe(nueva: ImagenDocumento) {
        val anterior = when (nueva.cara) {
            CaraDocumento.FRENTE -> _estado.value.frente
            CaraDocumento.REVERSO -> _estado.value.reverso
        }
        if (anterior != null && anterior.rutaArchivo != nueva.rutaArchivo) {
            capturas.descartar(anterior.rutaArchivo)
        }
    }

    /**
     * Criterio 4, medido en el dispositivo: la cámara reporta poca luz y la
     * foto no llega a tomarse.
     */
    fun reportarLuzInsuficiente() {
        if (_estado.value.analizando) return
        _estado.update {
            it.copy(problemaCalidad = ProblemaCalidad.ILUMINACION_INSUFICIENTE)
        }
    }

    /** Cierra el aviso de calidad para volver a intentar la captura. */
    fun descartarProblemaCalidad() {
        _estado.update { it.copy(problemaCalidad = null) }
    }

    /** El usuario corrige un dato en M1c. */
    fun actualizarDatos(datos: DatosDocumento) {
        _estado.update { it.copy(datos = datos).sinErroresDelServicio() }
    }

    fun actualizarContrasena(contrasena: String) {
        _estado.update { it.copy(contrasena = contrasena).sinErroresDelServicio() }
    }

    fun aceptarTerminos(aceptados: Boolean) {
        _estado.update { it.copy(terminosAceptados = aceptados) }
    }

    /**
     * Envía el formulario a originación. El temporizador es el mismo del
     * criterio 6: pasados 30 segundos no se mantiene al usuario esperando.
     */
    fun registrar() {
        val estadoActual = _estado.value
        val datos = estadoActual.datos ?: return
        // Las dos caras son parte de la petición, no un adjunto opcional: sin
        // ellas no hay verificación de identidad que registrar.
        val frente = estadoActual.frente ?: return
        val reverso = estadoActual.reverso ?: return
        if (!estadoActual.puedeVerificar) return

        viewModelScope.launch {
            _estado.update {
                it.copy(
                    verificacion = EstadoVerificacion.EnProceso,
                    camposInvalidos = emptySet(),
                    yaRegistrado = false,
                )
            }

            val reloj = launch {
                delay(LIMITE_DE_ESPERA_MS)
                _estado.update {
                    if (it.verificacion == EstadoVerificacion.EnProceso) {
                        it.copy(verificacion = EstadoVerificacion.Demorada)
                    } else {
                        it
                    }
                }
            }

            val resultado = registro.registrar(datos, estadoActual.contrasena, frente, reverso)
            reloj.cancel()

            if (resultado is ResultadoRegistro.Exitoso) {
                sesion.ingresarConContrasena(datos.correo.trim(), estadoActual.contrasena)
                // Las imágenes ya están guardadas en el servicio: la copia del
                // teléfono deja de hacer falta.
                descartar(frente, reverso)
            }

            _estado.update {
                // Si ya se le avisó al usuario que el resultado llega por
                // notificación, no se le cambia la pantalla debajo de los pies.
                if (it.verificacion == EstadoVerificacion.Demorada) it else it.conResultado(resultado)
            }
        }
    }

    /**
     * M1g · *Avísame por notificación*. El criterio 6 pide no mantener al
     * usuario esperando: puede salir antes de que se cumplan los 30 segundos.
     */
    fun avisarPorNotificacion() {
        _estado.update {
            if (it.verificacion == EstadoVerificacion.EnProceso) {
                it.copy(verificacion = EstadoVerificacion.Demorada)
            } else {
                it
            }
        }
    }

    /**
     * M1e · *Intentar de nuevo* devuelve el flujo a la primera captura. Los
     * datos se generan otra vez, con otro documento y otro correo: reenviar
     * los mismos solo serviría para recibir un `409`.
     */
    fun reiniciar() {
        val anterior = _estado.value
        _estado.value = RegistroUiState()
        viewModelScope.launch { descartar(anterior.frente, anterior.reverso) }
    }

    private suspend fun descartar(vararg imagenes: ImagenDocumento?) {
        imagenes.filterNotNull().forEach { capturas.descartar(it.rutaArchivo) }
    }

    private fun RegistroUiState.conResultado(resultado: ResultadoRegistro) = when (resultado) {
        is ResultadoRegistro.Exitoso -> copy(verificacion = EstadoVerificacion.Exitosa)

        is ResultadoRegistro.DatosInvalidos -> copy(
            verificacion = EstadoVerificacion.NoIniciada,
            camposInvalidos = resultado.campos,
        )

        ResultadoRegistro.YaRegistrado -> copy(
            verificacion = EstadoVerificacion.NoIniciada,
            yaRegistrado = true,
        )

        is ResultadoRegistro.NoDisponible ->
            copy(verificacion = EstadoVerificacion.NoSuperada(resultado.referencia))

        // Criterio 3: el documento no se pudo leer. Va a M1e, que informa el
        // resultado sin el motivo técnico y ofrece volver a intentar y
        // contactar al soporte.
        is ResultadoRegistro.DocumentoNoLegible ->
            copy(verificacion = EstadoVerificacion.NoSuperada(resultado.referencia))
    }

    /** Lo que el usuario acaba de cambiar deja de estar rechazado. */
    private fun RegistroUiState.sinErroresDelServicio() =
        if (camposInvalidos.isEmpty() && !yaRegistrado) {
            this
        } else {
            copy(camposInvalidos = emptySet(), yaRegistrado = false)
        }

    companion object {
        /** Criterio 6: pasados 30 segundos no se mantiene al usuario esperando. */
        const val LIMITE_DE_ESPERA_MS = 30_000L
    }
}
