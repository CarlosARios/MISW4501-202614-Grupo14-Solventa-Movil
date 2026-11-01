package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import co.solventa.movil.core.persistencia.entidades.NotificacionEntidad
import co.solventa.movil.core.persistencia.entidades.SincronizacionEntidad
import kotlinx.coroutines.flow.Flow

/**
 * La copia local del centro de notificaciones (HU-M3.1-01). Todas las
 * lecturas van por propietario: lo que se guarda para una cuenta nunca lo ve
 * otra.
 */
@Dao
abstract class NotificacionDao {

    @Query("SELECT * FROM notificacion WHERE propietario = :propietario ORDER BY ocurrioEn DESC, id DESC")
    abstract fun observarDePropietario(propietario: String): Flow<List<NotificacionEntidad>>

    @Query("SELECT * FROM notificacion WHERE propietario = :propietario AND id = :id")
    abstract fun observarUna(propietario: String, id: String): Flow<NotificacionEntidad?>

    /** Las que todavía no se avisaron con una notificación del sistema, la más antigua primero. */
    @Query("SELECT * FROM notificacion WHERE propietario = :propietario AND avisadaEn IS NULL ORDER BY ocurrioEn, id")
    abstract suspend fun sinAvisar(propietario: String): List<NotificacionEntidad>

    @Query("SELECT sincronizadoEn FROM sincronizacion WHERE propietario = :propietario AND recurso = :recurso")
    abstract fun observarSincronizacion(propietario: String, recurso: String = RECURSO_NOTIFICACIONES): Flow<Long?>

    /**
     * Guarda lo que entregó el servicio sin perder lo que solo sabe el
     * teléfono: si cada una ya se avisó. Las de la **primera** sincronización
     * de la cuenta en este teléfono se guardan como ya avisadas, para no
     * inundar al asegurado con todo su historial; las demás quedan por avisar.
     */
    @Transaction
    open suspend fun guardarSincronizadas(propietario: String, filas: List<NotificacionEntidad>, sincronizadoEn: Long) {
        val primera = cuantas(propietario) == 0
        filas.forEach { fila ->
            val avisada = avisadaEn(propietario, fila.id) ?: if (primera) sincronizadoEn else null
            guardar(fila.copy(avisadaEn = avisada))
        }
        guardarSincronizacion(SincronizacionEntidad(propietario, RECURSO_NOTIFICACIONES, sincronizadoEn))
    }

    @Query("UPDATE notificacion SET avisadaEn = :en WHERE propietario = :propietario AND id IN (:ids) AND avisadaEn IS NULL")
    abstract suspend fun marcarAvisadas(propietario: String, ids: List<String>, en: Long)

    @Query("UPDATE notificacion SET leidaEn = :en WHERE propietario = :propietario AND id = :id AND leidaEn IS NULL")
    abstract suspend fun marcarLeida(propietario: String, id: String, en: Long)

    @Query("UPDATE notificacion SET leidaEn = :en WHERE propietario = :propietario AND leidaEn IS NULL")
    abstract suspend fun marcarTodasLeidas(propietario: String, en: Long)

    /** Borra lo que hubiera dejado otra cuenta en este teléfono. */
    @Query("DELETE FROM notificacion WHERE propietario != :propietario")
    abstract suspend fun borrarDeOtrosPropietarios(propietario: String)

    /** Al cerrar la sesión no queda ninguna notificación en el teléfono. */
    @Transaction
    open suspend fun borrarTodo() {
        borrarNotificaciones()
        borrarSincronizaciones()
    }

    @Query("SELECT COUNT(*) FROM notificacion WHERE propietario = :propietario")
    protected abstract suspend fun cuantas(propietario: String): Int

    @Query("SELECT avisadaEn FROM notificacion WHERE propietario = :propietario AND id = :id")
    protected abstract suspend fun avisadaEn(propietario: String, id: String): Long?

    @Upsert
    protected abstract suspend fun guardar(fila: NotificacionEntidad)

    @Upsert
    protected abstract suspend fun guardarSincronizacion(sincronizacion: SincronizacionEntidad)

    @Query("DELETE FROM notificacion")
    protected abstract suspend fun borrarNotificaciones()

    @Query("DELETE FROM sincronizacion WHERE recurso = '$RECURSO_NOTIFICACIONES'")
    protected abstract suspend fun borrarSincronizaciones()

    companion object {
        /** Recurso de la tabla de sincronización para esta copia. */
        const val RECURSO_NOTIFICACIONES = "notificaciones"
    }
}
