package co.solventa.movil.core.imagen

/**
 * Luminancia de una imagen, un valor de 0 a 255 por píxel, fila por fila.
 *
 * Es lo único que necesita la medida de nitidez, y separarla del `Bitmap`
 * permite probar la medida en la JVM sin decodificar fotos.
 */
class Luminancia(val ancho: Int, val alto: Int, val valores: IntArray) {
    init {
        require(ancho > 0 && alto > 0) { "la imagen no tiene píxeles" }
        require(valores.size == ancho * alto) { "hay ${valores.size} valores para ${ancho * alto} píxeles" }
    }
}

/**
 * Varianza del laplaciano: la medida clásica de nitidez de una imagen.
 *
 * El laplaciano de 3×3 responde a los cambios bruscos de luminancia, que son
 * los bordes. Una foto enfocada de un documento tiene muchos —el texto, el
 * contorno de la foto, las líneas de seguridad— y la respuesta varía mucho de
 * un píxel a otro. Una foto movida o desenfocada los difumina, y la respuesta
 * queda cerca de cero en todas partes: la varianza cae.
 *
 * El borde de un píxel se salta porque su vecindario está incompleto. Una
 * imagen de menos de 3×3 no tiene interior y mide cero.
 */
fun varianzaDelLaplaciano(imagen: Luminancia): Double {
    val ancho = imagen.ancho
    val alto = imagen.alto
    if (ancho < 3 || alto < 3) return 0.0
    val v = imagen.valores

    var suma = 0.0
    var sumaDeCuadrados = 0.0
    var n = 0
    for (y in 1 until alto - 1) {
        val fila = y * ancho
        for (x in 1 until ancho - 1) {
            val i = fila + x
            val respuesta = (v[i - ancho] + v[i + ancho] + v[i - 1] + v[i + 1] - 4 * v[i]).toDouble()
            suma += respuesta
            sumaDeCuadrados += respuesta * respuesta
            n++
        }
    }
    val media = suma / n
    return sumaDeCuadrados / n - media * media
}

/** Luminancia de un color ARGB con los pesos de BT.601, en enteros. */
fun luminanciaDe(argb: Int): Int {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return (299 * r + 587 * g + 114 * b) / 1000
}
