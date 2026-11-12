package co.solventa.movil.features.siniestros.radicacion.envio

import co.solventa.movil.features.siniestros.evidencia.EvidenciaRepositorio
import co.solventa.movil.features.siniestros.radicacion.BorradorRepositorio
import co.solventa.movil.features.siniestros.radicacion.EnvioDelBorrador
import co.solventa.movil.features.siniestros.radicacion.RadicacionRepositorio
import co.solventa.movil.features.siniestros.radicacion.ResultadoRadicacion
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** Qué hacer con el trabajo después de un intento. */
enum class ResultadoDelEnvio { LISTO, REINTENTAR, AGOTADO }

/**
 * Un intento de envío de un borrador en cola (HU-M2.2-02).
 *
 * - **La clave es el identificador del borrador**, la misma en cada intento:
 *   el servicio devuelve el mismo caso y no crea un segundo radicado
 *   (criterio 5).
 * - **El borrador se borra solo cuando el servicio confirma** (criterio 4):
 *   hasta entonces el formulario y las fotos siguen en el teléfono.
 * - **Tras [MAXIMO_DE_INTENTOS] fallos** se detiene y el borrador queda
 *   *Envío fallido*, con el reintento manual (M13d).
 * - **Solo envía lo de la sesión abierta** (criterio 6). Si hay otra cuenta,
 *   el borrador espera a su dueño: *Mis siniestros* lo vuelve a encolar
 *   cuando él entra.
 */
@Singleton
class EnvioDelBorradorEnCola internal constructor(
    private val borradores: BorradorRepositorio,
    private val evidencias: EvidenciaRepositorio,
    private val radicaciones: RadicacionRepositorio,
    private val avisador: AvisadorDeRadicacion,
    private val esperaDeEvidencia: Long,
) {

    @Inject
    constructor(
        borradores: BorradorRepositorio,
        evidencias: EvidenciaRepositorio,
        radicaciones: RadicacionRepositorio,
        avisador: AvisadorDeRadicacion,
    ) : this(borradores, evidencias, radicaciones, avisador, ESPERA_DE_EVIDENCIA_MS)

    suspend fun enviar(borradorId: String): ResultadoDelEnvio {
        val borrador = borradores.paraEnviar(borradorId) ?: return ResultadoDelEnvio.LISTO
        borradores.marcarEnviando(borradorId)

        // La evidencia sube por su cuenta y puede ir llegando justo cuando
        // vuelve la señal. Se le da un rato antes de contar un fallo.
        if (!esperarEvidencia(borradorId)) return fallo(borradorId, borrador.intentos)

        val capturado = evidencias.paraRadicar(borradorId)
        return when (val resultado = radicaciones.radicar(borradorId, borrador.radicacion, capturado)) {
            is ResultadoRadicacion.Exitoso -> {
                borradores.marcarRadicado(borradorId, resultado.caso)
                evidencias.limpiar(borradorId)
                avisador.radicado(resultado.caso)
                ResultadoDelEnvio.LISTO
            }

            // El servicio rechazó lo declarado: reintentar no lo arregla.
            is ResultadoRadicacion.DatosInvalidos -> {
                borradores.registrarFallo(borradorId, EnvioDelBorrador.FALLIDO)
                ResultadoDelEnvio.AGOTADO
            }

            is ResultadoRadicacion.NoDisponible -> fallo(borradorId, borrador.intentos)
        }
    }

    private suspend fun fallo(borradorId: String, previos: Int): ResultadoDelEnvio =
        if (previos + 1 >= MAXIMO_DE_INTENTOS) {
            borradores.registrarFallo(borradorId, EnvioDelBorrador.FALLIDO)
            ResultadoDelEnvio.AGOTADO
        } else {
            borradores.registrarFallo(borradorId, EnvioDelBorrador.EN_COLA)
            ResultadoDelEnvio.REINTENTAR
        }

    private suspend fun esperarEvidencia(borradorId: String): Boolean {
        if (evidencias.todoSubido(borradorId)) return true
        evidencias.reintentarEnvios(borradorId)
        var esperado = 0L
        while (esperado < esperaDeEvidencia) {
            delay(PAUSA_MS)
            esperado += PAUSA_MS
            if (evidencias.todoSubido(borradorId)) return true
        }
        return false
    }

    companion object {
        /** Los intentos automáticos antes de pedir el reintento manual (M13d). */
        const val MAXIMO_DE_INTENTOS = 3
        const val ESPERA_DE_EVIDENCIA_MS = 60_000L
        private const val PAUSA_MS = 2_000L
    }
}
