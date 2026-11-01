package co.solventa.movil.features.acceso.registro

/** Cara del documento que se está capturando. */
enum class CaraDocumento { FRENTE, REVERSO }

/**
 * Motivo observable por el que una captura no sirve. Es lo único que la
 * aplicación puede explicarle al usuario sobre la imagen, porque son
 * condiciones que él puede corregir (criterio 4).
 */
enum class ProblemaCalidad { ILUMINACION_INSUFICIENTE, DOCUMENTO_INCOMPLETO, IMAGEN_BORROSA }

/** Imagen capturada. La ruta apunta al archivo temporal del dispositivo. */
data class ImagenDocumento(val cara: CaraDocumento, val rutaArchivo: String)

/**
 * Datos personales que el usuario confirma en M1c antes de registrarse.
 *
 * **Los lee el proveedor de identidad, no el teléfono** (criterio 1). El
 * reconocimiento de texto en el dispositivo se descartó el 17/10/2026; desde
 * el 29/10/2026 la aplicación envía las dos caras a
 * `POST /v1/registrations/document-reads` y M1c muestra lo que el proveedor
 * leyó para que el usuario lo confirme o lo corrija. El correo y el celular
 * no están impresos en el documento: los escribe el usuario.
 *
 * `tipoDocumento` y `celular` se conservan porque están en el frame M1c. El
 * contrato del registro no los recibe todavía.
 */
data class DatosDocumento(
    val nombres: String,
    val apellidos: String,
    val tipoDocumento: String,
    val numeroDocumento: String,
    val fechaNacimiento: String,
    val correo: String = "",
    val celular: String = "",
)

/**
 * Juicio sobre la imagen capturada. No trae datos: la calidad de la foto y
 * los datos personales dejaron de ser lo mismo.
 */
sealed interface ResultadoCaptura {
    data object Aceptada : ResultadoCaptura

    data class CalidadInsuficiente(val motivo: ProblemaCalidad) : ResultadoCaptura
}

/**
 * Juzga la imagen capturada antes de aceptarla. Lo implementa
 * [AnalizadorDeNitidez], en el propio teléfono.
 */
interface RegistroKycRepositorio {
    suspend fun analizarCaptura(imagen: ImagenDocumento): ResultadoCaptura
}
