package co.solventa.movil.core.imagen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NitidezTest {

    @Test
    fun `una imagen uniforme no tiene bordes y mide cero`() {
        assertEquals(0.0, varianzaDelLaplaciano(uniforme(40, 30, 128)), 1e-9)
    }

    @Test
    fun `un patron con bordes netos mide mucho mas que el mismo patron difuminado`() {
        val nitida = tablero(64, 48, celda = 4)
        val borrosa = difuminar(difuminar(difuminar(nitida)))

        val medidaNitida = varianzaDelLaplaciano(nitida)
        val medidaBorrosa = varianzaDelLaplaciano(borrosa)

        assertTrue("nítida $medidaNitida", medidaNitida > 1_000)
        assertTrue("borrosa $medidaBorrosa frente a nítida $medidaNitida", medidaBorrosa < medidaNitida / 10)
    }

    @Test
    fun `el brillo no cambia la medida, solo los bordes`() {
        val base = tablero(32, 32, celda = 4, oscuro = 20, claro = 120)
        val masClara = Luminancia(32, 32, IntArray(base.valores.size) { base.valores[it] + 100 })

        assertEquals(varianzaDelLaplaciano(base), varianzaDelLaplaciano(masClara), 1e-6)
    }

    @Test
    fun `una imagen sin interior mide cero`() {
        assertEquals(0.0, varianzaDelLaplaciano(tablero(2, 50, celda = 1)), 0.0)
        assertEquals(0.0, varianzaDelLaplaciano(tablero(50, 2, celda = 1)), 0.0)
    }

    @Test
    fun `la luminancia pondera el verde mas que el rojo y el rojo mas que el azul`() {
        assertEquals(0, luminanciaDe(0xFF000000.toInt()))
        assertEquals(255, luminanciaDe(0xFFFFFFFF.toInt()))
        val rojo = luminanciaDe(0xFFFF0000.toInt())
        val verde = luminanciaDe(0xFF00FF00.toInt())
        val azul = luminanciaDe(0xFF0000FF.toInt())
        assertTrue(verde > rojo && rojo > azul)
        assertEquals(luminanciaDe(0x00808080), luminanciaDe(0xFF808080.toInt()), "el alfa no cuenta")
    }

    @Test
    fun `la luminancia exige un valor por pixel`() {
        assertThrows(IllegalArgumentException::class.java) { Luminancia(3, 3, IntArray(8)) }
        assertThrows(IllegalArgumentException::class.java) { Luminancia(0, 3, IntArray(0)) }
    }

    private fun assertEquals(esperado: Int, real: Int, mensaje: String) = assertEquals(mensaje, esperado, real)
}

internal fun uniforme(ancho: Int, alto: Int, valor: Int) = Luminancia(ancho, alto, IntArray(ancho * alto) { valor })

/** Un tablero de ajedrez: bordes netos cada [celda] píxeles, como el texto de un documento. */
internal fun tablero(ancho: Int, alto: Int, celda: Int, oscuro: Int = 30, claro: Int = 220) =
    Luminancia(ancho, alto, IntArray(ancho * alto) { i ->
        val x = i % ancho
        val y = i / ancho
        if ((x / celda + y / celda) % 2 == 0) oscuro else claro
    })

/** Promedio de 3×3: lo que le pasa a una foto movida o desenfocada. */
internal fun difuminar(imagen: Luminancia): Luminancia {
    val (ancho, alto, v) = Triple(imagen.ancho, imagen.alto, imagen.valores)
    return Luminancia(ancho, alto, IntArray(v.size) { i ->
        val x = i % ancho
        val y = i / ancho
        var suma = 0
        var n = 0
        for (dy in -1..1) for (dx in -1..1) {
            val xx = x + dx
            val yy = y + dy
            if (xx in 0 until ancho && yy in 0 until alto) {
                suma += v[yy * ancho + xx]
                n++
            }
        }
        suma / n
    })
}

/** Un degradado suave de izquierda a derecha: luz que cambia sin bordes. */
internal fun degradado(ancho: Int, alto: Int) =
    Luminancia(ancho, alto, IntArray(ancho * alto) { i -> 30 + (i % ancho) * 190 / (ancho - 1) })
