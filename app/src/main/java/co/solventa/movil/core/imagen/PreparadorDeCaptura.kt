package co.solventa.movil.core.imagen

/**
 * Deja una captura de la cámara lista para enviarla: la reduce y la codifica
 * en base64, que es como viaja en el cuerpo del registro (HU-M1.1-01).
 *
 * La foto que produce CameraX en el Moto G20 pesa varios megabytes, muy por
 * encima del tope que el contrato acepta por cara, así que reducirla no es
 * una optimización sino parte de poder enviarla.
 *
 * Es una interfaz porque lo que hay debajo es `Bitmap`, que solo existe en el
 * dispositivo: así el repositorio del registro se puede probar en la JVM.
 */
interface PreparadorDeCaptura {

    /**
     * Devuelve el base64 del JPEG reducido, o `null` si el archivo ya no se
     * puede leer. La caché del sistema se puede vaciar en cualquier momento,
     * de modo que una captura que estaba ahí hace un minuto puede no estar.
     */
    suspend fun aBase64(rutaArchivo: String): String?

    /**
     * Borra la captura del teléfono. Se llama cuando la imagen ya llegó al
     * servicio: el documento de identidad no tiene por qué sobrevivir al
     * envío, que es lo que la pantalla le promete al usuario.
     */
    suspend fun descartar(rutaArchivo: String)
}
