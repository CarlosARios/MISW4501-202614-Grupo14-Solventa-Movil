package co.solventa.movil.core.persistencia.entidades

import androidx.room.Entity
import androidx.room.Index

/**
 * Una revocacion que el titular confirmo y que todavia no llega al servicio
 * de auditoria (HU-M1.3-02, criterio 5). Se escribe **antes** de intentar el
 * envio, asi nada se pierde si la red falla o la aplicacion se cierra, y se
 * borra cuando el servicio la registra.
 *
 * - La llave incluye al **propietario** (el `sub` de la sesion): la cola de una
 *   cuenta nunca la ve ni la envia otra, y al cerrar sesion se conserva para
 *   cuando ese titular vuelva a entrar (decision D-7 a).
 * - [clave] es la clave de idempotencia (UUID) que se genero al confirmar:
 *   todos los reintentos la reusan y el servicio nunca revoca dos veces.
 * - [solicitadaEn] (milisegundos desde epoca) y [zona] son el instante de la
 *   confirmacion y la zona del telefono: se envian como la fecha de la
 *   solicitud aunque salga horas despues.
 */
@Entity(
    tableName = "revocacion_pendiente",
    primaryKeys = ["propietario", "consentimientoId"],
    indices = [Index(value = ["clave"], unique = true)],
)
data class RevocacionPendienteEntidad(
    val propietario: String,
    val consentimientoId: String,
    val clave: String,
    /** Nombre visible de la entidad, para la lista y el aviso sin conexion. */
    val entidad: String,
    val solicitadaEn: Long,
    val zona: String,
    val dispositivo: String,
    /** `biometric` o `password`, como lo pide el contrato. */
    val metodo: String,
    val intentos: Int = 0,
    val ultimoError: String? = null,
)
