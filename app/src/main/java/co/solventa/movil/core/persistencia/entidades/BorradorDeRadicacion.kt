package co.solventa.movil.core.persistencia.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * El reporte de siniestro a medio diligenciar (HU-M2.2-01, criterio 7).
 *
 * Vive en el dispositivo para que cerrar la aplicación no pierda lo escrito.
 * Su identificador es además la **clave de idempotencia** con la que se
 * radica: por eso lo genera el teléfono al abrir el asistente y no el
 * servicio.
 *
 * Las respuestas de las preguntas que dependen del tipo se guardan como JSON
 * porque su forma la decide el tipo de incidente, no esta tabla.
 */
@Entity(tableName = "borrador_de_radicacion")
data class BorradorDeRadicacion(
    @PrimaryKey val id: String,
    val etapa: String,
    val poliza: String,
    val tipo: String?,
    val ocurrioEn: Long?,
    val lugar: String,
    val descripcion: String,
    val respuestasJson: String,
    val declaracionAceptada: Boolean,
    val actualizadoEn: Long,
    /** El monto estimado como se escribió (HU-M2.2-01); vacío en un borrador anterior. */
    @ColumnInfo(defaultValue = "")
    val montoEstimado: String = "",
    /**
     * El `sub` de quien lo empezó (HU-M2.2-02, criterio 6). Otra cuenta en el
     * mismo teléfono no lo ve. Los borradores de antes de la versión 7 quedan
     * vacíos y no los ve nadie.
     */
    @ColumnInfo(defaultValue = "")
    val propietario: String = "",
    /** `edicion`, `en_cola`, `enviando`, `fallido` o `radicado` (HU-M2.2-02). */
    @ColumnInfo(defaultValue = "edicion")
    val envio: String = "edicion",
    /** Intentos de envío fallidos desde el último reintento manual. */
    @ColumnInfo(defaultValue = "0")
    val intentos: Int = 0,
    val ultimoIntentoEn: Long? = null,
    /** El número de radicado que dio el servicio; queda hasta que se muestra el aviso. */
    val radicado: String? = null,
    val radicadoEn: Long? = null,
    val casoId: String? = null,
)
