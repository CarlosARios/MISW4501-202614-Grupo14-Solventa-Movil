package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import co.solventa.movil.core.persistencia.entidades.RevocacionPendienteEntidad
import kotlinx.coroutines.flow.Flow

/**
 * La cola local de revocaciones pendientes de envio (HU-M1.3-02). Toda lectura
 * va por propietario: la cola de una cuenta nunca la ve otra.
 */
@Dao
interface RevocacionPendienteDao {

    /**
     * Encola la revocacion. Si el consentimiento ya tenia una pendiente, se
     * conserva la primera (su clave y su hora) y devuelve -1.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun encolar(fila: RevocacionPendienteEntidad): Long

    @Query("SELECT * FROM revocacion_pendiente WHERE propietario = :propietario ORDER BY solicitadaEn, consentimientoId")
    fun observarDePropietario(propietario: String): Flow<List<RevocacionPendienteEntidad>>

    @Query("SELECT * FROM revocacion_pendiente WHERE propietario = :propietario ORDER BY solicitadaEn, consentimientoId")
    suspend fun dePropietario(propietario: String): List<RevocacionPendienteEntidad>

    @Query("SELECT * FROM revocacion_pendiente WHERE propietario = :propietario AND consentimientoId = :consentimientoId")
    suspend fun una(propietario: String, consentimientoId: String): RevocacionPendienteEntidad?

    @Query("DELETE FROM revocacion_pendiente WHERE propietario = :propietario AND consentimientoId = :consentimientoId")
    suspend fun borrar(propietario: String, consentimientoId: String)

    /** Un intento fallido mas, con su causa; la fila sigue en la cola. */
    @Query(
        "UPDATE revocacion_pendiente SET intentos = intentos + 1, ultimoError = :error " +
            "WHERE propietario = :propietario AND consentimientoId = :consentimientoId",
    )
    suspend fun registrarIntento(propietario: String, consentimientoId: String, error: String)

    @Query("SELECT COUNT(*) FROM revocacion_pendiente WHERE propietario = :propietario")
    suspend fun contar(propietario: String): Int
}
