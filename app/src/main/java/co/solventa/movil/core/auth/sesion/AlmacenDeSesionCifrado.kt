package co.solventa.movil.core.auth.sesion

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda los tokens de Cognito cifrados en el dispositivo.
 *
 * **Por qué cifrados y no en texto plano.** El *refresh token* del cliente
 * móvil dura 30 días: quien lo obtenga puede pedir sesiones nuevas durante un
 * mes sin saber la contraseña. En un teléfono con acceso a los archivos de la
 * aplicación —uno con permisos de superusuario, o una copia de seguridad mal
 * configurada—, un `SharedPreferences` en claro lo entrega entero.
 *
 * **Por qué el Keystore directamente y no `security-crypto`.** La clave vive
 * dentro del Keystore de Android y nunca sale de ahí; la aplicación solo pide
 * cifrar y descifrar. Aquí se guarda el texto cifrado y su vector de
 * inicialización. Es la misma garantía que da `androidx.security:security-crypto`,
 * que está en mantenimiento, sin añadir una dependencia al proyecto.
 *
 * Si el descifrado falla —se reinstaló la aplicación, se restauró una copia
 * en otro dispositivo o el usuario cambió el bloqueo de pantalla— se descarta
 * lo guardado y se pide iniciar sesión otra vez, que es lo correcto: no hay
 * forma de recuperar esos tokens y conservarlos solo deja basura.
 */
@Singleton
class AlmacenDeSesionCifrado @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : AlmacenDeSesion {

    private val preferencias: SharedPreferences by lazy {
        contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    }

    override fun guardarTokens(tokens: TokensDeSesion) {
        val claro = listOf(
            tokens.idToken,
            tokens.accessToken,
            tokens.tokenDeRenovacion,
            tokens.venceEn.toString(),
            tokens.correo,
            tokens.nombre,
        ).joinToString(SEPARADOR)
        preferencias.edit().putString(CLAVE_SESION, cifrar(claro)).apply()
    }

    override fun tokens(): TokensDeSesion? {
        val guardado = preferencias.getString(CLAVE_SESION, null) ?: return null
        val partes = descifrar(guardado)?.split(SEPARADOR) ?: run {
            borrarTokens()
            return null
        }
        if (partes.size != CAMPOS) {
            borrarTokens()
            return null
        }
        val vence = partes[3].toLongOrNull() ?: run {
            borrarTokens()
            return null
        }
        return TokensDeSesion(
            idToken = partes[0],
            accessToken = partes[1],
            tokenDeRenovacion = partes[2],
            venceEn = vence,
            correo = partes[4],
            nombre = partes[5],
        )
    }

    override fun borrarTokens() {
        preferencias.edit().remove(CLAVE_SESION).apply()
    }

    // La preferencia de biometría no es material de autenticación -es un sí o
    // un no- así que se guarda en claro junto a los tokens cifrados.
    override fun biometriaHabilitada(): Boolean = preferencias.getBoolean(CLAVE_BIOMETRIA, true)

    override fun habilitarBiometria(habilitada: Boolean) {
        preferencias.edit().putBoolean(CLAVE_BIOMETRIA, habilitada).apply()
    }

    private fun cifrar(claro: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, claveDelKeystore())
        val cifrado = cifrador.doFinal(claro.toByteArray(Charsets.UTF_8))
        // El vector de inicialización no es secreto, pero sí tiene que viajar
        // con el texto cifrado: sin él no se puede descifrar.
        return base64(cifrador.iv) + SEPARADOR_IV + base64(cifrado)
    }

    private fun descifrar(guardado: String): String? = runCatching {
        val (iv, cifrado) = guardado.split(SEPARADOR_IV).let { it[0] to it[1] }
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.DECRYPT_MODE, claveDelKeystore(), GCMParameterSpec(TAG_BITS, deBase64(iv)))
        String(cifrador.doFinal(deBase64(cifrado)), Charsets.UTF_8)
    }.getOrNull()

    private fun claveDelKeystore(): SecretKey {
        val keystore = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
        (keystore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Sin exigir autenticación del usuario: la sesión se desbloquea
                // con la biometría de HU-M1.1-02, que es una decisión de la
                // aplicación, y atarla también a la clave impediría renovar el
                // token en segundo plano.
                .build(),
        )
        return generador.generateKey()
    }

    private fun base64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun deBase64(texto: String) = Base64.decode(texto, Base64.NO_WRAP)

    private companion object {
        const val ARCHIVO = "sesion-solventa"
        const val CLAVE_SESION = "tokens"
        const val CLAVE_BIOMETRIA = "biometria-habilitada"
        const val PROVEEDOR = "AndroidKeyStore"
        const val ALIAS = "co.solventa.movil.sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val SEPARADOR = "\u001F"
        const val SEPARADOR_IV = ":"
        const val CAMPOS = 6
    }
}
