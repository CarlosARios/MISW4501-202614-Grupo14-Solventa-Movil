package co.solventa.movil.features.acceso.registro

import android.util.Log
import co.solventa.movil.BuildConfig
import co.solventa.movil.core.imagen.LectorDeLuminancia
import co.solventa.movil.core.imagen.varianzaDelLaplaciano
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Juzga en el teléfono si la foto del documento está enfocada (criterio 4 de
 * HU-M1.1-01, pantalla M1f).
 *
 * Reemplaza al repositorio simulado, que rechazaba como borrosa la primera
 * foto del reverso de **cualquier** usuario para que M1f se pudiera mostrar
 * en la demostración. Ahora M1f sale cuando la foto de verdad está movida o
 * desenfocada, y para mostrarla basta con mover el teléfono al disparar.
 *
 * La poca luz no se juzga aquí: la mide la cámara antes de disparar
 * (`MedidorDeLuz`). Este análisis corre después, sobre la foto ya tomada.
 */
class AnalizadorDeNitidez internal constructor(
    private val lector: LectorDeLuminancia,
    private val umbral: Double,
    private val despachador: CoroutineDispatcher,
    /** Dónde queda la medida de cada foto; en la JVM de las pruebas no hay logcat. */
    private val registrar: (String) -> Unit,
) : RegistroKycRepositorio {

    @Inject
    constructor(lector: LectorDeLuminancia) : this(lector, UMBRAL_DE_NITIDEZ, Dispatchers.Default, ::registrarEnLogcat)

    override suspend fun analizarCaptura(imagen: ImagenDocumento): ResultadoCaptura =
        withContext(despachador) {
            // Un archivo que no se decodifica no es una foto que se pueda
            // enviar: se pide repetirla, igual que una borrosa.
            val luminancia = lector.leer(imagen.rutaArchivo)
                ?: return@withContext ResultadoCaptura.CalidadInsuficiente(ProblemaCalidad.IMAGEN_BORROSA)
            val nitidez = varianzaDelLaplaciano(luminancia)
            registrar("nitidez de ${imagen.cara}: ${"%.1f".format(nitidez)} (umbral $umbral)")
            if (nitidez < umbral) {
                ResultadoCaptura.CalidadInsuficiente(ProblemaCalidad.IMAGEN_BORROSA)
            } else {
                ResultadoCaptura.Aceptada
            }
        }

    internal companion object {
        private const val ETIQUETA = "Nitidez"

        /** Solo en debug: es lo que permite calibrar el umbral en un teléfono real. */
        private fun registrarEnLogcat(linea: String) {
            if (BuildConfig.DEBUG) Log.d(ETIQUETA, linea)
        }

        /**
         * Por debajo de este valor la foto se considera borrosa, medida a 640 px
         * de lado largo.
         *
         * Calibrado contra el Moto G20 el 29/10/2026 con una cédula real: las
         * fotos enfocadas midieron 209 y 524, y las tomadas moviendo el teléfono
         * 63 y 67. El umbral queda entre los dos grupos, más cerca de las
         * movidas, porque rechazar una foto buena frena el registro y una dudosa
         * la vuelve a juzgar el proveedor de identidad. Para recalibrar en otro
         * teléfono, el registro `Nitidez` de logcat da la medida de cada foto.
         */
        const val UMBRAL_DE_NITIDEZ = 120.0
    }
}
