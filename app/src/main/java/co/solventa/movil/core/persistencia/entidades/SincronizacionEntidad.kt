package co.solventa.movil.core.persistencia.entidades

import androidx.room.Entity

/**
 * Cuando se sincronizo por ultima vez un recurso de un propietario
 * (HU-M1.3-01). Es lo que permite decir «estas viendo la informacion
 * sincronizada el … a las …» cuando no hay conexion (criterio 5).
 */
@Entity(tableName = "sincronizacion", primaryKeys = ["propietario", "recurso"])
data class SincronizacionEntidad(
    val propietario: String,
    val recurso: String,
    val sincronizadoEn: Long,
)
