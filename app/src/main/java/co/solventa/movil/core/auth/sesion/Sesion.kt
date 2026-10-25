package co.solventa.movil.core.auth.sesion

/**
 * Sesión del asegurado. El único rol humano de este canal.
 *
 * No guarda el token: ese vive en el almacenamiento cifrado del dispositivo y
 * lo adjunta el interceptor de `core/auth/interceptores`. Aquí queda lo que la
 * interfaz necesita mostrar.
 */
data class Sesion(
    val correo: String,
    val nombre: String,
    /**
     * Identificador estable de la cuenta en el *user pool* (`sub` del token),
     * derivado del *id token* guardado (HU-M1.3-01). Es el propietario de lo
     * que la aplicacion guarda en el dispositivo; vacio si el token no lo trae.
     */
    val sub: String = "",
)

/**
 * Lo que un dominio borra del dispositivo al cerrar la sesion (HU-M1.3-01,
 * criterio 6): cada uno se registra en un conjunto de Hilt y `core` lo llama
 * sin conocer a ningun dominio.
 */
fun interface AlCerrarSesion {
    suspend fun limpiar()
}

sealed interface ResultadoIngreso {
    data class Exitoso(val sesion: Sesion) : ResultadoIngreso

    /**
     * No se dice si falló el correo o la contraseña: distinguirlos le permite
     * a un atacante descubrir qué correos están registrados.
     */
    data object CredencialesInvalidas : ResultadoIngreso

    data object SinConexion : ResultadoIngreso
}

/**
 * Resultado de verificar la contraseña de la sesion para confirmar una accion
 * sensible (HU-M1.3-02, decision D-6 a).
 */
sealed interface VerificacionDeContrasena {
    data object Correcta : VerificacionDeContrasena

    /** Como en el ingreso, no se distingue por que fallo. */
    data object Incorrecta : VerificacionDeContrasena

    /** Verificarla exige hablar con el proveedor de identidad: sin red no se puede. */
    data object SinConexion : VerificacionDeContrasena
}

/**
 * Verifica la contraseña de la cuenta que tiene la sesion abierta, **sin
 * reemplazar los tokens guardados**: es una confirmacion, no un ingreso
 * (HU-M1.3-02, criterio 1: «la contraseña, segun HU-M1.1-02», cuando el
 * telefono no tiene biometria habilitada para Solventa).
 */
interface VerificadorDeContrasena {
    suspend fun verificarContrasena(contrasena: String): VerificacionDeContrasena
}

interface SesionRepositorio {

    /** La sesión que quedó guardada en el dispositivo, si la hay. */
    suspend fun sesionGuardada(): Sesion?

    /** Si el usuario habilitó la biometría para Solventa en este dispositivo. */
    suspend fun biometriaHabilitada(): Boolean

    /**
     * Criterio 2 de HU-M1.1-02: desbloquea **localmente** la sesión guardada.
     * No hay petición al proveedor de identidad; los tokens ya estaban
     * emitidos y solo se liberan. Devuelve `null` si no había sesión.
     */
    suspend fun desbloquear(): Sesion?

    suspend fun ingresarConContrasena(correo: String, contrasena: String): ResultadoIngreso

    suspend fun habilitarBiometria(habilitada: Boolean)

    suspend fun cerrar()
}
