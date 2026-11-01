package co.solventa.movil.core.persistencia.entidades

import androidx.room.Entity

/**
 * Copia local de una notificación del centro (HU-M3.1-01): la última versión
 * que entregó el servicio de notificaciones, para mostrar el centro y el
 * detalle del pago sin conexión.
 *
 * La llave incluye al **propietario** (el `sub` de la sesión), igual que la
 * copia de los consentimientos. El pago viaja como JSON en texto: lo escribe y
 * lo lee el mapeador del dominio.
 *
 * [avisadaEn] marca cuándo la aplicación mostró la notificación del sistema
 * por ella: cada notificación se avisa una sola vez, aunque se sincronice
 * muchas veces (criterio 6: ninguna se pierde ni se repite).
 */
@Entity(tableName = "notificacion", primaryKeys = ["propietario", "id"])
data class NotificacionEntidad(
    val propietario: String,
    val id: String,
    val tipo: String,
    val ocurrioEn: Long,
    val leidaEn: Long?,
    val pago: String?,
    val avisadaEn: Long? = null,
)
