package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
import kotlinx.coroutines.flow.Flow

/**
 * Los borradores de la radicación. Toda lectura pide el dueño: un borrador
 * solo existe para quien lo empezó (HU-M2.2-02, criterio 6).
 */
@Dao
interface BorradorDeRadicacionDao {

    /** Guardar el borrador es reemplazarlo: solo interesa su último estado. */
    @Upsert
    suspend fun guardar(borrador: BorradorDeRadicacion)

    /**
     * El borrador que se retoma al volver a entrar: el último que se tocó de
     * los que siguen en edición. Los que esperan su envío no se editan.
     */
    @Query(
        "SELECT * FROM borrador_de_radicacion WHERE propietario = :propietario " +
            "AND envio = 'edicion' ORDER BY actualizadoEn DESC LIMIT 1",
    )
    suspend fun enCurso(propietario: String): BorradorDeRadicacion?

    @Query("SELECT * FROM borrador_de_radicacion WHERE id = :id AND propietario = :propietario")
    suspend fun porId(id: String, propietario: String): BorradorDeRadicacion?

    /** El borrador tal cual, para el trabajador de envío, que compara el dueño. */
    @Query("SELECT * FROM borrador_de_radicacion WHERE id = :id")
    suspend fun buscar(id: String): BorradorDeRadicacion?

    /** Los borradores de *Mis siniestros* (M13b–M13d), el más reciente primero. */
    @Query(
        "SELECT * FROM borrador_de_radicacion WHERE propietario = :propietario " +
            "ORDER BY actualizadoEn DESC",
    )
    fun deUsuario(propietario: String): Flow<List<BorradorDeRadicacion>>

    @Query("UPDATE borrador_de_radicacion SET envio = :envio WHERE id = :id")
    suspend fun marcarEnvio(id: String, envio: String)

    /** Pasa a la cola de envío con la cuenta de intentos en cero. */
    @Query("UPDATE borrador_de_radicacion SET envio = 'en_cola', intentos = 0 WHERE id = :id")
    suspend fun encolar(id: String)

    @Query(
        "UPDATE borrador_de_radicacion SET envio = :envio, intentos = intentos + 1, " +
            "ultimoIntentoEn = :en WHERE id = :id",
    )
    suspend fun registrarIntentoFallido(id: String, envio: String, en: Long)

    @Query(
        "UPDATE borrador_de_radicacion SET envio = 'radicado', radicado = :radicado, " +
            "casoId = :casoId, radicadoEn = :en WHERE id = :id",
    )
    suspend fun marcarRadicado(id: String, radicado: String, casoId: String, en: Long)

    /** Vuelve a ser el que se retoma, para continuarlo desde *Mis siniestros*. */
    @Query(
        "UPDATE borrador_de_radicacion SET envio = 'edicion', actualizadoEn = :en " +
            "WHERE id = :id AND propietario = :propietario",
    )
    suspend fun retomar(id: String, propietario: String, en: Long)

    /** Se borra cuando el siniestro quedó radicado y se avisó, o cuando se abandona. */
    @Query("DELETE FROM borrador_de_radicacion WHERE id = :id")
    suspend fun borrar(id: String)
}
