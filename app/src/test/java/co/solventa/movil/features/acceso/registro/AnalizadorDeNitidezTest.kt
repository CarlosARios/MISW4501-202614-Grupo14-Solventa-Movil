package co.solventa.movil.features.acceso.registro

import co.solventa.movil.core.imagen.LectorDeLuminancia
import co.solventa.movil.core.imagen.degradado
import co.solventa.movil.core.imagen.tablero
import co.solventa.movil.core.imagen.uniforme
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalizadorDeNitidezTest {

    private val nitida = tablero(64, 48, celda = 4)

    /**
     * Lo que queda de un documento muy movido: transiciones suaves, sin un
     * borde neto. El tablero difuminado unas pocas veces no sirve aquí,
     * porque su contraste de 30 a 220 sigue siendo mucho mayor que el de una
     * foto real.
     */
    private val borrosa = degradado(64, 48)

    private fun analizador(lector: LectorDeLuminancia, despachador: TestDispatcher) =
        AnalizadorDeNitidez(lector, AnalizadorDeNitidez.UMBRAL_DE_NITIDEZ, despachador) {}

    @Test
    fun `una foto nitida se acepta`() = runTest {
        val resultado = analizador({ nitida }, StandardTestDispatcher(testScheduler))
            .analizarCaptura(ImagenDocumento(CaraDocumento.REVERSO, "reverso.jpg"))

        assertEquals(ResultadoCaptura.Aceptada, resultado)
    }

    @Test
    fun `una foto borrosa se rechaza con el motivo que M1f explica`() = runTest {
        val resultado = analizador({ borrosa }, StandardTestDispatcher(testScheduler))
            .analizarCaptura(ImagenDocumento(CaraDocumento.FRENTE, "frente.jpg"))

        assertEquals(ResultadoCaptura.CalidadInsuficiente(ProblemaCalidad.IMAGEN_BORROSA), resultado)
    }

    @Test
    fun `la primera foto del reverso ya no se rechaza por guion`() = runTest {
        val sut = analizador({ nitida }, StandardTestDispatcher(testScheduler))
        val reverso = ImagenDocumento(CaraDocumento.REVERSO, "reverso.jpg")

        repeat(3) { assertEquals(ResultadoCaptura.Aceptada, sut.analizarCaptura(reverso)) }
    }

    @Test
    fun `una foto que no se puede leer se pide de nuevo`() = runTest {
        val resultado = analizador({ null }, StandardTestDispatcher(testScheduler))
            .analizarCaptura(ImagenDocumento(CaraDocumento.FRENTE, "danada.jpg"))

        assertEquals(ResultadoCaptura.CalidadInsuficiente(ProblemaCalidad.IMAGEN_BORROSA), resultado)
    }

    @Test
    fun `una superficie lisa, sin documento enfocado, se rechaza`() = runTest {
        val resultado = analizador({ uniforme(64, 48, 200) }, StandardTestDispatcher(testScheduler))
            .analizarCaptura(ImagenDocumento(CaraDocumento.FRENTE, "pared.jpg"))

        assertEquals(ResultadoCaptura.CalidadInsuficiente(ProblemaCalidad.IMAGEN_BORROSA), resultado)
    }

    @Test
    fun `lee la foto de la ruta capturada`() = runTest {
        var leida: String? = null
        analizador({ ruta -> leida = ruta; nitida }, StandardTestDispatcher(testScheduler))
            .analizarCaptura(ImagenDocumento(CaraDocumento.FRENTE, "/cache/frente-123.jpg"))

        assertEquals("/cache/frente-123.jpg", leida)
    }
}
