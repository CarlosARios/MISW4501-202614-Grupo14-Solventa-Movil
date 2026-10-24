package co.solventa.movil.features.acceso.registro

/**
 * Campo del formulario que el servicio puede rechazar. El contrato los
 * nombra en inglés y devuelve `full_name` como uno solo, así que *Nombres* y
 * *Apellidos* se marcan juntos: no se sabe cuál de los dos sobra.
 */
enum class CampoDeRegistro { DOCUMENTO, NOMBRE, CORREO, FECHA_NACIMIENTO, CONTRASENA }

sealed interface ResultadoRegistro {
    /** `201`. La cuenta existe en el *user pool* y ya se puede usar. */
    data class Exitoso(val cuenta: String) : ResultadoRegistro

    /** `422`. El usuario corrige en M1c y vuelve a enviar. */
    data class DatosInvalidos(val campos: Set<CampoDeRegistro>) : ResultadoRegistro

    /** `409`. Ese documento o ese correo ya tienen cuenta. */
    data object YaRegistrado : ResultadoRegistro

    /**
     * `503`, red o tiempo agotado. La referencia es el identificador de
     * correlación de la petición, que es con lo que soporte la encuentra en
     * los registros del backend.
     */
    data class NoDisponible(val referencia: String) : ResultadoRegistro
}

interface RegistroRepositorio {
    suspend fun registrar(datos: DatosDocumento, contrasena: String): ResultadoRegistro
}
