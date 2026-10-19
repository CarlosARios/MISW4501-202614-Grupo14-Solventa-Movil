package co.solventa.movil.core.auth.cognito

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

/** Lectura de las afirmaciones del *id token* que emite el *pool*. */
class TokenDeIdentidadTest {

    @Test
    fun `lee el correo, el nombre y el vencimiento`() {
        val token = jwt("{\"email\":\"asegurado@example.com\",\"name\":\"Carolina\",\"exp\":1760000300}")

        val afirmaciones = TokenDeIdentidad.afirmaciones(token)

        assertEquals("asegurado@example.com", afirmaciones?.correo)
        assertEquals("Carolina", afirmaciones?.nombre)
        // `exp` viene en segundos y la aplicación trabaja en milisegundos.
        assertEquals(1_760_000_300_000L, afirmaciones?.venceEn)
    }

    /**
     * El alta administrativa no siempre fija `name`. Mostrar la parte local
     * del correo es mejor que dejar la pantalla con un saludo vacío.
     */
    @Test
    fun `sin nombre usa la parte local del correo`() {
        val token = jwt("{\"email\":\"operaciones@example.com\",\"exp\":1760000300}")

        assertEquals("operaciones", TokenDeIdentidad.afirmaciones(token)?.nombre)
    }

    @Test
    fun `un token que no es un JWT no revienta`() {
        assertNull(TokenDeIdentidad.afirmaciones("esto-no-es-un-token"))
        assertNull(TokenDeIdentidad.afirmaciones(""))
        assertNull(TokenDeIdentidad.afirmaciones("a.no-es-base64-valido!!.c"))
    }

    /** Un JWT viaja en base64url sin relleno; hay que completarlo al leerlo. */
    @Test
    fun `decodifica una carga de cualquier longitud`() {
        for (nombre in listOf("Ana", "Carol", "Carolina", "Carolina P")) {
            val token = jwt("{\"email\":\"a@b.co\",\"name\":\"$nombre\",\"exp\":1}")
            assertEquals(nombre, TokenDeIdentidad.afirmaciones(token)?.nombre)
        }
    }

    private fun jwt(carga: String): String =
        "encabezado." + Base64.getUrlEncoder().withoutPadding().encodeToString(carga.toByteArray()) + ".firma"
}
