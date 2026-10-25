package co.solventa.movil.core.persistencia.entidades

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una evidencia capturada en el lugar del siniestro (HU-M2.1-01).
 *
 * Vive en el dispositivo desde que se toma hasta que el siniestro queda
 * radicado: el archivo se sube en segundo plano, y mientras tanto el usuario
 * sigue capturando. Por eso el estado del envío se guarda aquí y no solo en
 * memoria, igual que el borrador al que pertenece.
 *
 * `llaveDelObjeto` queda vacía hasta que el servicio la entrega. Una
 * evidencia sin llave todavía no está en la plataforma y no puede viajar con
 * la radicación.
 */
@Entity(tableName = "evidencia_capturada")
data class EvidenciaCapturada(
    @PrimaryKey val id: String,
    /** El borrador al que pertenece; es la clave con la que se radica. */
    val borradorId: String,
    /** Orden de captura, que es el que ve quien evalúa el caso. */
    val posicion: Int,
    /** `foto` o `video`, como los nombra el contrato. */
    val tipo: String,
    /** Ruta del archivo en la caché de la aplicación. */
    val rutaArchivo: String,
    val capturadaEn: Long,
    val latitud: Double,
    val longitud: Double,
    val altitud: Double?,
    val precisionM: Float,
    val ubicacionConfiable: Boolean,
    val sha256: String,
    val tamanoBytes: Long,
    val duracionS: Int,
    val llaveDelObjeto: String?,
    /** `pendiente`, `subiendo`, `subida` o `fallida`. */
    val estado: String,
)
