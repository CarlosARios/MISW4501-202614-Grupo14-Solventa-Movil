package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.solventa.movil.core.persistencia.entidades.EvidenciaCapturada
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenciaCapturadaDao {

    @Upsert
    suspend fun guardar(evidencia: EvidenciaCapturada)

    /**
     * Las evidencias del borrador, en el orden en que se capturaron. Es un
     * flujo porque la pantalla tiene que ver avanzar el envío sin recargar
     * nada (criterio 5).
     */
    @Query("SELECT * FROM evidencia_capturada WHERE borradorId = :borradorId ORDER BY posicion")
    fun deBorrador(borradorId: String): Flow<List<EvidenciaCapturada>>

    @Query("SELECT * FROM evidencia_capturada WHERE borradorId = :borradorId ORDER BY posicion")
    suspend fun listarDeBorrador(borradorId: String): List<EvidenciaCapturada>

    /** Lo que al trabajador de envío le queda por subir. */
    @Query("SELECT * FROM evidencia_capturada WHERE estado <> 'subida' ORDER BY capturadaEn LIMIT 20")
    suspend fun pendientes(): List<EvidenciaCapturada>

    @Query("UPDATE evidencia_capturada SET estado = :estado WHERE id = :id")
    suspend fun marcar(id: String, estado: String)

    @Query("UPDATE evidencia_capturada SET estado = 'subida', llaveDelObjeto = :llave WHERE id = :id")
    suspend fun marcarSubida(id: String, llave: String)

    @Query("DELETE FROM evidencia_capturada WHERE id = :id")
    suspend fun borrar(id: String)

    /** Se borran con el borrador: ya viajaron con la radicación. */
    @Query("DELETE FROM evidencia_capturada WHERE borradorId = :borradorId")
    suspend fun borrarDeBorrador(borradorId: String)
}
