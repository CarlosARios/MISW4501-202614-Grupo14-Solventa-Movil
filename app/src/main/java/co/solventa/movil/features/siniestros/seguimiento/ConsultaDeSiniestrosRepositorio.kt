package co.solventa.movil.features.siniestros.seguimiento

/** Resultado de consultar los siniestros (HU-M2.4-01, HA-L02). */
sealed interface ResultadoDeConsulta<out T> {
    /** El servicio respondió; [frescura] dice si fue desde su caché y de qué hora es. */
    data class Exito<T>(val valor: T, val frescura: Frescura) : ResultadoDeConsulta<T>

    /** El teléfono no tiene red. */
    data object SinConexion : ResultadoDeConsulta<Nothing>

    /** El siniestro no existe o no es del asegurado (el servicio no distingue). */
    data object NoEncontrado : ResultadoDeConsulta<Nothing>

    /**
     * El servicio falló o no tenía ni la base ni una versión guardada
     * (`503 claims_unavailable`).
     */
    data object Error : ResultadoDeConsulta<Nothing>
}

/**
 * La consulta de los siniestros del asegurado de la sesión contra el servicio
 * de siniestros. No guarda copia local: lo que se muestra cuando el servicio
 * está degradado es la versión que guardó su caché de lectura (HA-L02).
 */
interface ConsultaDeSiniestrosRepositorio {

    suspend fun listar(): ResultadoDeConsulta<List<Siniestro>>

    /** El detalle y el historial del siniestro, que se piden a la vez. */
    suspend fun detalle(id: String): ResultadoDeConsulta<DetalleDeSiniestro>
}
