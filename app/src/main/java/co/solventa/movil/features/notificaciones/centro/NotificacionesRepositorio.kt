package co.solventa.movil.features.notificaciones.centro

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Resultado de pedirle el centro al servicio de notificaciones (HU-M3.1-01). */
sealed interface ResultadoDeSincronizacion {
    /** La copia quedó igual a la del servidor; [nuevas] son las que todavía no se avisaron. */
    data class Sincronizado(val nuevas: List<Notificacion>) : ResultadoDeSincronizacion

    /** Sin red: la copia se conserva. */
    data object SinConexion : ResultadoDeSincronizacion

    /** Sesión vencida, servicio caído u otra respuesta: la copia se conserva. */
    data object Error : ResultadoDeSincronizacion

    /** No hay sesión con `sub`: no se lee ni se escribe nada. */
    data object SinSesion : ResultadoDeSincronizacion
}

/** Lo que el asegurado ve de su centro. */
data class CopiaDeNotificaciones(
    val notificaciones: List<Notificacion>,
    val sincronizadoEn: Long?,
    val sinConexion: Boolean,
)

/**
 * El centro de notificaciones del asegurado de la sesión (HU-M3.1-01). Room es
 * la fuente de la verdad: las pantallas observan la copia, que sirve sin
 * conexión, y [sincronizar] la actualiza con la del servidor.
 */
interface NotificacionesRepositorio {

    val conectado: StateFlow<Boolean>

    fun observar(): Flow<CopiaDeNotificaciones>

    fun observarUna(id: String): Flow<Notificacion?>

    suspend fun sincronizar(): ResultadoDeSincronizacion

    /** Anota que ya se mostró la notificación del sistema de estas. */
    suspend fun marcarAvisadas(ids: List<String>)

    suspend fun marcarLeida(id: String)

    suspend fun marcarTodasLeidas()

    /**
     * Registra esta instalación con el permiso de notificaciones actual y el
     * idioma de la aplicación. Solo llama al servicio cuando algo cambió.
     */
    suspend fun registrarDispositivo(permisoConcedido: Boolean, idioma: String)
}
