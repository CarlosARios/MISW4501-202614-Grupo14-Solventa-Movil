package co.solventa.movil.core.persistencia.cifrado

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * La clave AES de los datos locales, dentro del Keystore de Android
 * (HU-M1.2-01, criterio 6). La clave nunca sale del Keystore: la aplicación
 * solo le pide cifrar y descifrar. Es el mismo mecanismo de
 * `AlmacenDeSesionCifrado`, con su propio alias, así que borrar la sesión no
 * invalida la copia ni al revés.
 *
 * No exige autenticación del usuario para usarse: la sincronización corre en
 * segundo plano, con la aplicación cerrada, y la copia ya solo se muestra con
 * la sesión desbloqueada (HU-M1.1-02). Si la clave se pierde —se reinstaló la
 * aplicación o se restauró en otro teléfono—, lo cifrado deja de descifrarse
 * y el repositorio lo descarta: la siguiente sincronización lo repone.
 */
object ClaveDelKeystore {

    private const val PROVEEDOR = "AndroidKeyStore"
    private const val ALIAS = "co.solventa.movil.datos-locales"

    @Synchronized
    fun obtener(): SecretKey {
        val keystore = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
        (keystore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generador.generateKey()
    }
}
