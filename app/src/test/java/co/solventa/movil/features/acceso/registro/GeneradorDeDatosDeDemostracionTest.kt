package co.solventa.movil.features.acceso.registro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Lo que se comprueba no es qué nombre sale, sino que lo generado pase las
 * reglas del contrato y la política del *user pool*: si no las pasa, el
 * registro responde `422` y la demostración se cae en vivo.
 */
class GeneradorDeDatosDeDemostracionTest {

    private val generador = GeneradorDeDatosDeDemostracion()

    @Test
    fun `el documento son diez digitos aunque se muestre con puntos`() {
        val documento = generador.datos().numeroDocumento

        assertTrue("Se muestra como en M1c: $documento", documento.contains("."))
        val digitos = documento.filter(Char::isDigit)
        assertEquals(10, digitos.length)
    }

    @Test
    fun `el correo no lleva tildes ni mayusculas`() {
        val correo = generador.datos().correo

        assertTrue("Falta el dominio: $correo", correo.endsWith("@example.com"))
        assertEquals(correo.lowercase(), correo)
        assertTrue("Quedaron tildes: $correo", correo.all { it.code < 128 })
    }

    /** Dos registros seguidos no pueden chocar entre sí con un `409`. */
    @Test
    fun `dos documentos generados no coinciden`() {
        assertNotEquals(generador.datos().numeroDocumento, generador.datos().numeroDocumento)
    }

    @Test
    fun `la fecha de nacimiento es de una persona mayor de edad`() {
        val fecha = LocalDate.parse(
            generador.datos().fechaNacimiento,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        )

        val anios = java.time.temporal.ChronoUnit.YEARS.between(fecha, LocalDate.now())
        assertTrue("Nació hace $anios años", anios >= 18)
    }

    @Test
    fun `la contrasena cumple la politica del user pool`() {
        val contrasena = generador.contrasena()

        assertTrue("Son 12 o más: $contrasena", contrasena.length >= 12)
        assertTrue(contrasena.any(Char::isLowerCase))
        assertTrue(contrasena.any(Char::isUpperCase))
        assertTrue(contrasena.any(Char::isDigit))
        assertTrue(contrasena.any { !it.isLetterOrDigit() })
    }
}
