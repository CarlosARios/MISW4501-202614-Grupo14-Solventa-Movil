package co.solventa.movil.core.persistencia.entidades

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
)
