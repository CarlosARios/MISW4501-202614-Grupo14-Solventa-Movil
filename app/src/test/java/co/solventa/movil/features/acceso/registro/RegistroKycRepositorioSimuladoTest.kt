package co.solventa.movil.features.acceso.registro

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El juicio simulado sobre la imagen se prueba porque hoy es el que corre en
 * la aplicación, y porque su guion es lo que hace demostrable M1f: si deja de
 * fallar la primera vez, esa pantalla se vuelve inalcanzable sin que nadie se
 * entere. La poca luz, en cambio, la mide la cámara. Cuando algún servicio
 * reciba la imagen, estas pruebas se van con esta clase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistroKycRepositorioSimuladoTest {

    private val repositorio = RegistroKycRepositorioSimulado()

    private val frente = ImagenDocumento(CaraDocumento.FRENTE, "/tmp/frente.jpg")
    private val reverso = ImagenDocumento(CaraDocumento.REVERSO, "/tmp/reverso.jpg")

    @Test
    fun `el frente se acepta siempre`() = runTest {
        assertEquals(ResultadoCaptura.Aceptada, repositorio.analizarCaptura(frente))
    }

    /**
     * La borrosidad es un juicio del proveedor de verificación y no algo que
     * el teléfono decida por su cuenta.
     */
    @Test
    fun `el primer intento del reverso falla por borrosidad para poder mostrar M1f`() = runTest {
        val resultado = repositorio.analizarCaptura(reverso)

        assertTrue(resultado is ResultadoCaptura.CalidadInsuficiente)
        assertEquals(
            ProblemaCalidad.IMAGEN_BORROSA,
            (resultado as ResultadoCaptura.CalidadInsuficiente).motivo,
        )
    }

    @Test
    fun `el segundo intento del reverso se acepta`() = runTest {
        repositorio.analizarCaptura(reverso)

        assertEquals(ResultadoCaptura.Aceptada, repositorio.analizarCaptura(reverso))
    }
}
