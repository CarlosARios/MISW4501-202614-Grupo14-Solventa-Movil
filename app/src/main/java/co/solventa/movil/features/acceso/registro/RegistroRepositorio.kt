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
     * La foto del documento no sirvió: el servicio rechazó una de las caras,
     * o el archivo ya no estaba en la caché del teléfono. Es el criterio 3 de
     * la historia —documento no legible— y no se corrige en M1c: hay que
     * volver a capturar, que es lo que ofrece M1e.
     */
    data class DocumentoNoLegible(val referencia: String) : ResultadoRegistro

    /**
     * `422 identity_not_verified`. El proveedor de identidad no aprobó a la
     * persona y la cuenta no se creó (criterio 5). El servicio no dice por
     * qué —lista restrictiva, documento ilegible o identidad no verificable—
     * y la aplicación tampoco lo intenta adivinar (criterio 3): M1e ofrece el
     * canal de soporte con la referencia.
     */
    data class IdentidadNoVerificada(val referencia: String) : ResultadoRegistro

    /**
     * `503`, red o tiempo agotado. La referencia es el identificador de
     * correlación de la petición, que es con lo que soporte la encuentra en
     * los registros del backend.
     */
    data class NoDisponible(val referencia: String) : ResultadoRegistro
}

/** Lo que devolvió la lectura del documento (criterio 1). */
sealed interface ResultadoLectura {
    /** `200`. Los datos impresos en el documento, para confirmarlos en M1c. */
    data class Leido(val datos: DatosDocumento) : ResultadoLectura

    /**
     * El documento no se pudo leer: el servicio rechazó una cara, el
     * proveedor no respondió o no hubo red. Se resuelve igual en todos los
     * casos —volver a tomar el reverso—, así que la aplicación no los
     * distingue.
     */
    data class NoLeido(val referencia: String) : ResultadoLectura
}

interface RegistroRepositorio {
    /**
     * Pide al proveedor de identidad los datos impresos en el documento. No
     * guarda nada: el registro vuelve a enviar las dos caras.
     */
    suspend fun leerDocumento(frente: ImagenDocumento, reverso: ImagenDocumento): ResultadoLectura

    /**
     * Envía el formulario con las dos caras del documento. Son parte de la
     * petición: sin ellas no hay verificación de identidad que registrar.
     */
    suspend fun registrar(
        datos: DatosDocumento,
        contrasena: String,
        frente: ImagenDocumento,
        reverso: ImagenDocumento,
    ): ResultadoRegistro
}
