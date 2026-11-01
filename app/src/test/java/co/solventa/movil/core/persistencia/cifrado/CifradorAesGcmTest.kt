package co.solventa.movil.core.persistencia.cifrado

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * El cifrado de los datos locales (HU-M1.2-01, criterio 6), con una clave de
 * software en lugar de la del Keystore: lo que se guarda no se lee sin la
 * clave, ni se puede alterar sin que se note.
 */
class CifradorAesGcmTest {

    private fun clave(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val claveFija = clave()
    private val cifrador = CifradorAesGcm { claveFija }

    private val claro = """{"id":"AU-2025-118204","documento":"1020304050","valor_asegurado":72500000.0}"""

    @Test
    fun `lo cifrado se descifra con la misma clave`() {
        assertEquals(claro, cifrador.descifrar(cifrador.cifrar(claro)))
    }

    @Test
    fun `el texto guardado no contiene nada de lo claro`() {
        val cifrado = cifrador.cifrar(claro)

        listOf("AU-2025-118204", "1020304050", "72500000").forEach { assertFalse(it, cifrado.contains(it)) }
    }

    @Test
    fun `cifrar dos veces lo mismo da textos distintos`() {
        assertNotEquals(cifrador.cifrar(claro), cifrador.cifrar(claro))
    }

    @Test
    fun `con otra clave no se descifra`() {
        val otro = CifradorAesGcm(::clave)

        assertNull(otro.descifrar(cifrador.cifrar(claro)))
    }

    @Test
    fun `un texto alterado no se descifra`() {
        val cifrado = cifrador.cifrar(claro)
        val ultimo = cifrado.last()
        val alterado = cifrado.dropLast(1) + (if (ultimo == 'A') 'B' else 'A')

        assertNull(cifrador.descifrar(alterado))
    }

    @Test
    fun `lo que no tiene la forma de un cifrado no se descifra`() {
        assertNull(cifrador.descifrar("texto en claro"))
        assertNull(cifrador.descifrar(""))
    }
}
