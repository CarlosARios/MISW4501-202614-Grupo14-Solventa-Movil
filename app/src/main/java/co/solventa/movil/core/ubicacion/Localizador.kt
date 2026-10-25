package co.solventa.movil.core.ubicacion

/**
 * Dónde y cuándo se tomó una captura (HU-M2.1-01).
 *
 * La precisión es el radio en metros dentro del cual el sistema sitúa al
 * dispositivo: cuanto mayor, menos fiable es el punto.
 */
data class Ubicacion(
    val latitud: Double,
    val longitud: Double,
    val altitud: Double?,
    val precisionM: Float,
    /** Instante de la lectura, en milisegundos desde la época. */
    val leidaEn: Long,
) {
    /** Criterio 3: más allá del límite, el punto no respalda nada. */
    val esConfiable: Boolean
        get() = precisionM > 0 && precisionM <= PRECISION_MAXIMA_M

    companion object {
        /** Límite de precisión que el criterio fija, en metros. */
        const val PRECISION_MAXIMA_M = 50f

        /** Más allá de esto la lectura es vieja y deja de representar dónde está. */
        const val VIGENCIA_MS = 30_000L
    }
}

/**
 * La ubicación del dispositivo, vista desde la captura de evidencia.
 *
 * Es una interfaz porque debajo está el `LocationManager` del sistema, que
 * solo existe en el dispositivo: así la captura se puede probar en la JVM.
 */
interface Localizador {

    /**
     * La última ubicación conocida que todavía sirve, o `null` cuando no hay
     * ninguna reciente o falta el permiso. No espera a que llegue una mejor:
     * quien captura no puede quedarse mirando la pantalla.
     */
    suspend fun ubicacionActual(): Ubicacion?
}
