package co.solventa.movil.core.auth.sesion

/** Tokens de la sesión y hasta cuándo sirve el de identidad. */
data class TokensDeSesion(
    val idToken: String,
    val accessToken: String,
    val tokenDeRenovacion: String,
    /** Instante de vencimiento del *id token*, en milisegundos desde época. */
    val venceEn: Long,
    val correo: String,
    val nombre: String,
)

/**
 * Lo que la sesión deja guardado en el dispositivo.
 *
 * Es una interfaz para que el repositorio de sesión no dependa del Keystore
 * de Android y se pueda probar en la JVM. La implementación real
 * ([AlmacenDeSesionCifrado]) se prueba en el dispositivo, que es el único
 * lugar donde el Keystore existe de verdad.
 */
interface AlmacenDeSesion {

    fun guardarTokens(tokens: TokensDeSesion)

    /** Los tokens guardados, o `null` si no hay o no se pudieron leer. */
    fun tokens(): TokensDeSesion?

    fun borrarTokens()

    /** Si el usuario habilitó la biometría para Solventa en este dispositivo. */
    fun biometriaHabilitada(): Boolean

    fun habilitarBiometria(habilitada: Boolean)
}
