package co.solventa.movil.features.acceso.registro

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Juicio simulado sobre la calidad de la imagen capturada.
 *
 * **Qué queda simulado y qué no.** El registro ya habla con el servicio de
 * originación, así que de aquí salió la verificación de identidad. Lo que
 * sigue sin backend es la lectura del documento: ningún servicio recibe la
 * imagen todavía.
 *
 * **Se comporta como un guion**, no como un camino feliz, para que M1f se
 * pueda mostrar en la demostración sin tener que desenfocar la cámara a
 * propósito:
 *
 * | Momento | Resultado | Pantalla |
 * |---|---|---|
 * | Primera captura del reverso | Imagen borrosa | M1f |
 * | Capturas siguientes | Aceptada | M1a → M1b → M1c |
 *
 * La poca luz no se finge aquí: la mide la cámara y el aviso sale antes de
 * tomar la foto, que es lo que pide el criterio 4. Lo que el guion conserva
 * es la borrosidad, porque juzgarla no es tarea del teléfono.
 *
 * El contador vive en el singleton, así que sobrevive al `reiniciar()` del
 * `ViewModel`: después de *Intentar de nuevo* el recorrido fluye hasta el
 * final.
 */
@Singleton
class RegistroKycRepositorioSimulado @Inject constructor() : RegistroKycRepositorio {

    private val intentosDelReverso = AtomicInteger(0)

    override suspend fun analizarCaptura(imagen: ImagenDocumento): ResultadoCaptura {
        delay(DEMORA_ANALISIS_MS)
        return when (imagen.cara) {
            CaraDocumento.FRENTE -> ResultadoCaptura.Aceptada

            CaraDocumento.REVERSO ->
                if (intentosDelReverso.incrementAndGet() == 1) {
                    ResultadoCaptura.CalidadInsuficiente(ProblemaCalidad.IMAGEN_BORROSA)
                } else {
                    ResultadoCaptura.Aceptada
                }
        }
    }

    private companion object {
        const val DEMORA_ANALISIS_MS = 1_200L
    }
}
