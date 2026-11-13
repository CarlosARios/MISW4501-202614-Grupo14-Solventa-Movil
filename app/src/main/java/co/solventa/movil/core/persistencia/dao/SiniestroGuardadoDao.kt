package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.solventa.movil.core.persistencia.entidades.SiniestroGuardadoEntidad

/**
 * La copia local cifrada de las consultas de siniestros (HU-M2.4-01,
 * criterio 5). Todas las lecturas van por propietario: lo que se guarda para
 * una cuenta nunca lo ve otra.
 */
@Dao
interface SiniestroGuardadoDao {

    @Upsert
    suspend fun guardar(fila: SiniestroGuardadoEntidad)

    @Query("SELECT * FROM siniestro_guardado WHERE propietario = :propietario AND clave = :clave")
    suspend fun leer(propietario: String, clave: String): SiniestroGuardadoEntidad?

    /** Borra lo que hubiera dejado otra cuenta en este teléfono. */
    @Query("DELETE FROM siniestro_guardado WHERE propietario != :propietario")
    suspend fun borrarDeOtrosPropietarios(propietario: String)

    /** Al cerrar la sesión no queda ningún siniestro en el teléfono. */
    @Query("DELETE FROM siniestro_guardado")
    suspend fun borrarTodo()
}
