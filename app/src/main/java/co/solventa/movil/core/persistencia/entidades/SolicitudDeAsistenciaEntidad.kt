package co.solventa.movil.core.persistencia.entidades

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una solicitud de asistencia en sitio (HU-M2.3-01) en la cola del teléfono.
 * Se escribe **antes** de abrir el marcador y la envía un trabajo urgente: así
 * sobrevive a que Android cierre la aplicación durante la llamada y a la falta
 * de datos (D2).
 *
 * - **La ubicación de una persona es sensible (D8):** [cargaCifrada] —póliza,
 *   coordenadas, precisión, instante de la lectura o motivo— y [lugarCifrado]
 *   son texto cifrado con la clave del Keystore. En claro queda solo lo
 *   operativo: dueño, clave, estado del envío, intentos, fechas y lo que
 *   devolvió el servicio (número `AST-…`, estado), que no revela la ubicación.
 * - [propietario] es el `sub` de la sesión: la cola de una cuenta nunca la ve
 *   ni la envía otra.
 * - [clave] es la clave de idempotencia (UUID) del toque: todos los reintentos
 *   la reusan y el servicio nunca registra dos veces la misma solicitud.
 * - [geocodificada] dice si ya se intentó resolver la dirección (D7): se hace
 *   una sola vez, antes del primer envío, porque la clave exige el mismo cuerpo.
 * - [estadoDeEnvio] es `en_cola`, `confirmada` o `descartada`
 *   (con [motivoDeDescarte]).
 */
@Entity(
    tableName = "solicitud_de_asistencia",
    indices = [Index(value = ["clave"], unique = true), Index(value = ["propietario", "creadaEn"])],
)
data class SolicitudDeAsistenciaEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propietario: String,
    val clave: String,
    val cargaCifrada: String,
    val lugarCifrado: String? = null,
    val geocodificada: Boolean = false,
    val estadoDeEnvio: String,
    val motivoDeDescarte: String? = null,
    val intentos: Int = 0,
    /** Instante del toque, en milisegundos desde la época. */
    val creadaEn: Long,
    val requestId: String? = null,
    val numero: String? = null,
    /** `requested_at` del servicio, en milisegundos desde la época. */
    val registradaEn: Long? = null,
    /** El `status` del servicio la última vez que se leyó. */
    val estadoRemoto: String? = null,
) {
    companion object {
        const val EN_COLA = "en_cola"
        const val CONFIRMADA = "confirmada"
        const val DESCARTADA = "descartada"
    }
}
