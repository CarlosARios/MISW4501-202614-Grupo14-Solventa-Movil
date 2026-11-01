package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import co.solventa.movil.core.persistencia.entidades.PolizaEntidad
import co.solventa.movil.core.persistencia.entidades.SincronizacionEntidad
import kotlinx.coroutines.flow.Flow

/**
 * La copia local cifrada de las pólizas (HU-M1.2-01). Todas las lecturas van
 * por propietario: lo que se guarda para una cuenta nunca lo ve otra.
 *
 * Comparte con los consentimientos la tabla `sincronizacion`, con su propio
 * recurso: borrar las pólizas solo borra su fila de sincronización.
 */
@Dao
abstract class PolizaDao {

    @Query("SELECT * FROM poliza WHERE propietario = :propietario ORDER BY orden")
    abstract fun observarDePropietario(propietario: String): Flow<List<PolizaEntidad>>

    /** La copia tal como está, para compararla con la lista nueva antes de reemplazarla. */
    @Query("SELECT * FROM poliza WHERE propietario = :propietario ORDER BY orden")
    abstract suspend fun dePropietario(propietario: String): List<PolizaEntidad>

    @Query("SELECT * FROM poliza WHERE propietario = :propietario AND id = :id")
    abstract fun observarUna(propietario: String, id: String): Flow<PolizaEntidad?>

    @Query("SELECT sincronizadoEn FROM sincronizacion WHERE propietario = :propietario AND recurso = :recurso")
    abstract fun observarSincronizacion(propietario: String, recurso: String = RECURSO_POLIZAS): Flow<Long?>

    /**
     * Reemplaza de una vez toda la copia del propietario y anota cuándo se
     * sincronizó: quien observa nunca ve una lista a medias.
     */
    @Transaction
    open suspend fun reemplazarDePropietario(propietario: String, filas: List<PolizaEntidad>, sincronizadoEn: Long) {
        borrarDePropietario(propietario)
        insertar(filas)
        guardarSincronizacion(SincronizacionEntidad(propietario, RECURSO_POLIZAS, sincronizadoEn))
    }

    /** Borra lo que hubiera dejado otra cuenta en este teléfono. */
    @Transaction
    open suspend fun borrarDeOtrosPropietarios(propietario: String) {
        borrarPolizasDeOtros(propietario)
        borrarSincronizacionesDeOtros(propietario)
    }

    /** Al cerrar la sesión no queda ninguna póliza en el teléfono. */
    @Transaction
    open suspend fun borrarTodo() {
        borrarPolizas()
        borrarSincronizaciones()
    }

    @Query("DELETE FROM poliza WHERE propietario = :propietario")
    protected abstract suspend fun borrarDePropietario(propietario: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertar(filas: List<PolizaEntidad>)

    @Upsert
    protected abstract suspend fun guardarSincronizacion(sincronizacion: SincronizacionEntidad)

    @Query("DELETE FROM poliza WHERE propietario != :propietario")
    protected abstract suspend fun borrarPolizasDeOtros(propietario: String)

    @Query("DELETE FROM sincronizacion WHERE propietario != :propietario AND recurso = '$RECURSO_POLIZAS'")
    protected abstract suspend fun borrarSincronizacionesDeOtros(propietario: String)

    @Query("DELETE FROM poliza")
    protected abstract suspend fun borrarPolizas()

    @Query("DELETE FROM sincronizacion WHERE recurso = '$RECURSO_POLIZAS'")
    protected abstract suspend fun borrarSincronizaciones()

    companion object {
        /** Recurso de la tabla de sincronización para esta copia. */
        const val RECURSO_POLIZAS = "polizas"
    }
}
