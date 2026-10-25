package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion

@Dao
interface BorradorDeRadicacionDao {

    /** Guardar el borrador es reemplazarlo: solo interesa su último estado. */
    @Upsert
    suspend fun guardar(borrador: BorradorDeRadicacion)

    /**
     * El borrador que se retoma al volver a entrar. Si hubiera más de uno, el
     * último que se tocó.
     */
    @Query("SELECT * FROM borrador_de_radicacion ORDER BY actualizadoEn DESC LIMIT 1")
    suspend fun enCurso(): BorradorDeRadicacion?

    /** Se borra cuando el siniestro quedó radicado: ya no es un borrador. */
    @Query("DELETE FROM borrador_de_radicacion WHERE id = :id")
    suspend fun borrar(id: String)
}
