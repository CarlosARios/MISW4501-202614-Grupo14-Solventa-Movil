package co.solventa.movil.features.siniestros.seguimiento

import java.time.Instant
import java.time.ZoneId

/** Lo que la pantalla decide sobre las horas, fuera del composable para poder probarlo. */
object PresentacionDeSiniestros {

    /** Si [instante] cae el mismo día que [ahora] en la zona del teléfono: «hoy 14:30» o la fecha completa. */
    fun esDeHoy(instante: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Boolean =
        Instant.ofEpochMilli(instante).atZone(zona).toLocalDate() == Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
}
