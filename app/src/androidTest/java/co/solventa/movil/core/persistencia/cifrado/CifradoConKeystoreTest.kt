package co.solventa.movil.core.persistencia.cifrado

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * El cifrado de los datos locales con la clave real del Keystore de Android
 * (HU-M1.2-01, criterio 6), que solo existe en el dispositivo: la clave se
 * crea una vez y se reutiliza, y lo guardado no deja nada en claro.
 */
class CifradoConKeystoreTest {

    private val cifrador = CifradorAesGcm(ClaveDelKeystore::obtener)

    @Test
    fun cifra_y_descifra_con_la_clave_del_keystore() {
        val claro = """{"id":"AU-2025-118204","documento":"1020304050"}"""

        val cifrado = cifrador.cifrar(claro)

        assertFalse(cifrado.contains("1020304050"))
        assertEquals(claro, cifrador.descifrar(cifrado))
    }

    @Test
    fun la_clave_sobrevive_a_un_cifrador_nuevo() {
        val cifrado = cifrador.cifrar("VH-2026-004512")

        assertEquals("VH-2026-004512", CifradorAesGcm(ClaveDelKeystore::obtener).descifrar(cifrado))
    }
}
