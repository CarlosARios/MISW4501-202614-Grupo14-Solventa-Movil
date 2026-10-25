package co.solventa.movil.core.persistencia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import co.solventa.movil.core.persistencia.entidades.ConsentimientoEntidad
import co.solventa.movil.core.persistencia.entidades.SincronizacionEntidad
import kotlinx.coroutines.flow.Flow

/**
 * La copia local de los consentimientos (HU-M1.3-01). Todas las lecturas van
 * por propietario: lo que se guarda para una cuenta nunca lo ve otra
 * (criterio 6).
 *
 * Es una clase abstracta y no una interfaz porque el reemplazo y el borrado
 * total son transacciones de varias sentencias.
 */
@Dao
abstract class ConsentimientoDao {

    @Query("SELECT * FROM consentimiento WHERE propietario = :propietario")
    abstract fun observarDePropietario(propietario: String): Flow<List<ConsentimientoEntidad>>

    @Query("SELECT * FROM consentimiento WHERE propietario = :propietario AND id = :id")
    abstract fun observarUno(propietario: String, id: String): Flow<ConsentimientoEntidad?>

    /** Un consentimiento del propietario, para actualizarlo tras revocarlo (HU-M1.3-02). */
    @Query("SELECT * FROM consentimiento WHERE propietario = :propietario AND id = :id")
    abstract suspend fun uno(propietario: String, id: String): ConsentimientoEntidad?

    /**
     * Guarda una fila de la copia: la revocacion que respondio el servicio
     * (HU-M1.3-02) se ve en seguida, sin esperar la siguiente sincronizacion.
     */
    @Upsert
    abstract suspend fun guardar(fila: ConsentimientoEntidad)

    @Query("SELECT sincronizadoEn FROM sincronizacion WHERE propietario = :propietario AND recurso = :recurso")
    abstract fun observarSincronizacion(propietario: String, recurso: String): Flow<Long?>

    /**
     * Activos del propietario a la hora dada, para la insignia «N activos» del
     * perfil: un `active` que ya vencio no cuenta aunque la copia sea vieja.
     */
    @Query(
        "SELECT COUNT(*) FROM consentimiento " +
            "WHERE propietario = :propietario AND estado = 'active' AND venceEn > :ahora",
    )
    abstract fun contarVigentes(propietario: String, ahora: Long): Flow<Int>

    /**
     * Reemplaza de una vez toda la copia del propietario y anota cuando se
     * sincronizo: quien observa nunca ve una lista a medias.
     */
    @Transaction
    open suspend fun reemplazarDePropietario(
        propietario: String,
        filas: List<ConsentimientoEntidad>,
        sincronizadoEn: Long,
    ) {
        borrarDePropietario(propietario)
        insertar(filas)
        guardarSincronizacion(SincronizacionEntidad(propietario, RECURSO_CONSENTIMIENTOS, sincronizadoEn))
    }

    /** Borra lo que hubiera dejado otra cuenta en este telefono. */
    @Transaction
    open suspend fun borrarDeOtrosPropietarios(propietario: String) {
        borrarConsentimientosDeOtros(propietario)
        borrarSincronizacionesDeOtros(propietario)
    }

    /** Al cerrar sesion no queda nada en el telefono (criterio 6, D-6 a). */
    @Transaction
    open suspend fun borrarTodo() {
        borrarConsentimientos()
        borrarSincronizaciones()
    }

    @Query("DELETE FROM consentimiento WHERE propietario = :propietario")
    protected abstract suspend fun borrarDePropietario(propietario: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertar(filas: List<ConsentimientoEntidad>)

    @Upsert
    protected abstract suspend fun guardarSincronizacion(sincronizacion: SincronizacionEntidad)

    @Query("DELETE FROM consentimiento WHERE propietario != :propietario")
    protected abstract suspend fun borrarConsentimientosDeOtros(propietario: String)

    @Query("DELETE FROM sincronizacion WHERE propietario != :propietario")
    protected abstract suspend fun borrarSincronizacionesDeOtros(propietario: String)

    @Query("DELETE FROM consentimiento")
    protected abstract suspend fun borrarConsentimientos()

    @Query("DELETE FROM sincronizacion")
    protected abstract suspend fun borrarSincronizaciones()

    companion object {
        /** Recurso de la tabla de sincronizacion para esta copia. */
        const val RECURSO_CONSENTIMIENTOS = "consentimientos"
    }
}
