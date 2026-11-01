package co.solventa.movil.core.persistencia.cifrado

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra lo que se guarda en Room y no debe quedar expuesto si el teléfono se
 * pierde, lo roban o alguien lee los archivos de la aplicación (HU-M1.2-01,
 * criterio 6): la copia sin conexión de las pólizas, con el documento del
 * asegurado y los montos asegurados.
 *
 * Es una interfaz para que los repositorios se prueben sin el Keystore de
 * Android, que no existe en la JVM.
 */
interface CifradorDeDatos {

    fun cifrar(claro: String): String

    /** El texto claro, o `null` si no se puede descifrar con la clave actual. */
    fun descifrar(cifrado: String): String?
}

/**
 * [CifradorDeDatos] con AES-GCM: cada valor lleva su propio vector de
 * inicialización y GCM autentica el contenido, así que un valor alterado en
 * la base no se descifra a algo distinto, simplemente no se descifra.
 *
 * La clave la entrega [clave] y nunca se guarda aquí: en el teléfono es la
 * del Keystore ([ClaveDelKeystore]); en las pruebas, una clave de software.
 */
class CifradorAesGcm(private val clave: () -> SecretKey) : CifradorDeDatos {

    override fun cifrar(claro: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, clave())
        val cifrado = cifrador.doFinal(claro.toByteArray(Charsets.UTF_8))
        // El vector de inicialización no es secreto, pero viaja con el texto
        // cifrado: sin él no se puede descifrar.
        return base64(cifrador.iv) + SEPARADOR + base64(cifrado)
    }

    override fun descifrar(cifrado: String): String? = runCatching {
        val (iv, contenido) = cifrado.split(SEPARADOR, limit = 2).let { it[0] to it[1] }
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(TAG_BITS, deBase64(iv)))
        String(cifrador.doFinal(deBase64(contenido)), Charsets.UTF_8)
    }.getOrNull()

    private fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun deBase64(texto: String): ByteArray = Base64.getDecoder().decode(texto)

    private companion object {
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val SEPARADOR = ":"
    }
}
