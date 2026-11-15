package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import co.solventa.movil.core.persistencia.entidades.SolicitudDeAsistenciaEntidad
import kotlinx.coroutines.flow.Flow

/**
 * La cola de solicitudes de asistencia (HU-M2.3-01). Toda lectura va por
 * propietario: las solicitudes de una cuenta nunca las ve ni las envía otra.
 */
@Dao
interface SolicitudDeAsistenciaDao {

    @Insert
    suspend fun insertar(fila: SolicitudDeAsistenciaEntidad): Long

    @Update
    suspend fun actualizar(fila: SolicitudDeAsistenciaEntidad)

    /** Las que todavía no llegan al servicio, la más antigua primero. */
    @Query(
        "SELECT * FROM solicitud_de_asistencia WHERE propietario = :propietario AND estadoDeEnvio = 'en_cola' " +
            "ORDER BY creadaEn, id",
    )
    suspend fun pendientes(propietario: String): List<SolicitudDeAsistenciaEntidad>

    /** La más reciente del propietario, para M14 (D9). */
    @Query("SELECT * FROM solicitud_de_asistencia WHERE propietario = :propietario ORDER BY creadaEn DESC, id DESC LIMIT 1")
    fun ultima(propietario: String): Flow<SolicitudDeAsistenciaEntidad?>

    @Query("SELECT * FROM solicitud_de_asistencia WHERE propietario = :propietario AND requestId = :requestId")
    suspend fun porRequestId(propietario: String, requestId: String): SolicitudDeAsistenciaEntidad?

    /**
     * Borra las que ya se resolvieron —confirmadas o descartadas— antes del
     * instante dado. Las que siguen en la cola no se tocan: vencen solas (S7).
     */
    @Query("DELETE FROM solicitud_de_asistencia WHERE estadoDeEnvio != 'en_cola' AND creadaEn < :instante")
    suspend fun borrarResueltasAntesDe(instante: Long)
}
