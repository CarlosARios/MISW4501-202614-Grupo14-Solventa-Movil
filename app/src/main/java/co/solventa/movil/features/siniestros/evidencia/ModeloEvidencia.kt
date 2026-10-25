package co.solventa.movil.features.siniestros.evidencia

import co.solventa.movil.core.ubicacion.Ubicacion

/** Lo que el asegurado puede capturar en el lugar (HU-M2.1-01). */
enum class TipoDeEvidencia(val delContrato: String, val tipoDeContenido: String) {
    FOTO("photo", "image/jpeg"),
    VIDEO("video", "video/mp4"),
}

/** En qué va el envío de una evidencia. Criterio 5: se muestra en la lista. */
enum class EstadoDeEvidencia(val guardado: String) {
    PENDIENTE("pendiente"),
    SUBIENDO("subiendo"),
    SUBIDA("subida"),
    FALLIDA("fallida"),
    ;

    companion object {
        fun de(guardado: String?): EstadoDeEvidencia =
            entries.firstOrNull { it.guardado == guardado } ?: PENDIENTE
    }
}

/**
 * Una evidencia del reporte, tal como la ve la pantalla: el archivo en el
 * teléfono, dónde y cuándo se tomó, y en qué va su envío.
 */
data class EvidenciaDelReporte(
    val id: String,
    val tipo: TipoDeEvidencia,
    val rutaArchivo: String,
    val capturadaEn: Long,
    val ubicacion: Ubicacion?,
    val duracionS: Int,
    val estado: EstadoDeEvidencia,
) {
    /** Criterio 3: sin ubicación confiable la evidencia se conserva, marcada. */
    val ubicacionConfiable: Boolean
        get() = ubicacion?.esConfiable == true
}

/** Mínimo y tope que el servicio exige, repetidos aquí para no enviar de más. */
object LimitesDeEvidencia {
    const val MINIMO_DE_FOTOS = 4
    const val MAXIMO_DE_FOTOS = 6
    const val MAXIMO_DE_VIDEOS = 1

    /** Criterio 5: el video dura a lo sumo medio minuto. */
    const val MAXIMO_DE_SEGUNDOS = 30
}
