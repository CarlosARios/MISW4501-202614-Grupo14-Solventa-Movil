package co.solventa.movil.core.persistencia.entidades

import androidx.room.Entity

/**
 * Copia local de una póliza (HU-M1.2-01): la última versión que entregó el
 * servicio de originación, para consultarla sin conexión (criterio 1).
 *
 * **Todo el contenido va cifrado** (criterio 6): número de póliza aparte, la
 * fila solo guarda el texto que produjo el cifrador de `core/persistencia/
 * cifrado` con la póliza completa —tipo, vigencia, coberturas con sus montos
 * y el documento del asegurado—. La llave lleva al **propietario** (el `sub`
 * de la sesión) para que dos cuentas que usen el mismo teléfono nunca vean las
 * pólizas de la otra, igual que la copia de los consentimientos.
 *
 * [orden] conserva el orden en que llegó la lista.
 */
@Entity(tableName = "poliza", primaryKeys = ["propietario", "id"])
data class PolizaEntidad(
    val propietario: String,
    val id: String,
    val orden: Int,
    val contenidoCifrado: String,
)
