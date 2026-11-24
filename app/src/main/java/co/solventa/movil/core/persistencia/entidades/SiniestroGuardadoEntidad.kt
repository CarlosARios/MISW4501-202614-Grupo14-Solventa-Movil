package co.solventa.movil.core.persistencia.entidades

import androidx.room.Entity

/**
 * Copia local de una consulta de siniestros (HU-M2.4-01, criterio 5): la
 * última versión que entregó el servicio de siniestros de *Mis siniestros*
 * (clave `lista`) o del detalle de un caso con su línea de tiempo (clave
 * `detalle:<id>`), para mostrarla sin conexión con la hora de su última
 * actualización.
 *
 * **Todo el contenido va cifrado** con el cifrador de `core/persistencia/
 * cifrado`, como la copia de las pólizas: lleva el lugar del hecho, la póliza
 * y lo que se pagó. La llave lleva al **propietario** (el `sub` de la sesión)
 * para que dos cuentas que usen el mismo teléfono nunca vean los siniestros
 * de la otra.
 *
 * [guardadoEn] es la hora de los datos (el `as_of` del servicio), no la hora
 * en que se escribió la fila.
 */
@Entity(tableName = "siniestro_guardado", primaryKeys = ["propietario", "clave"])
data class SiniestroGuardadoEntidad(
    val propietario: String,
    val clave: String,
    val contenidoCifrado: String,
    val guardadoEn: Long,
)
