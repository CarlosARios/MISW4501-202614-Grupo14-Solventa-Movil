package co.solventa.movil.core.persistencia.entidades

import androidx.room.Entity

/**
 * Copia local de un consentimiento de Open Finance (HU-M1.3-01): la ultima
 * version que entrego el servicio de auditoria, para consultarla sin
 * conexion (criterio 5).
 *
 * La llave incluye al **propietario** (el `sub` de la sesion): cada lectura
 * filtra por el y nunca se mezclan los consentimientos de dos cuentas que
 * hayan usado el mismo telefono (criterio 6). Las fechas van en milisegundos
 * desde epoca. Entidades, alcance e historial viajan como JSON en texto: los
 * escribe y los lee el mapeador del dominio, asi la tabla no necesita
 * convertidores y su forma sigue siendo la del contrato.
 *
 * Es una copia minima: no guarda el documento ni el nombre del titular, que
 * el servicio tampoco devuelve.
 */
@Entity(tableName = "consentimiento", primaryKeys = ["propietario", "id"])
data class ConsentimientoEntidad(
    val propietario: String,
    val id: String,
    /** Estado que entrego el servidor (`active`, `expired`, `revoked`…). */
    val estado: String,
    val vigente: Boolean,
    val finalidad: String,
    val canal: String,
    val versionDeTerminos: String,
    val otorgadoEn: Long,
    val venceEn: Long,
    val finalizadoEn: Long?,
    val comprobante: String?,
    val entidades: String,
    val alcance: String,
    val historial: String,
    /**
     * La revocacion que lo termino, como JSON en texto (HU-M1.3-02); nula si
     * no fue revocado. La escribe y la lee el mapeador del dominio.
     */
    val revocacion: String? = null,
)
