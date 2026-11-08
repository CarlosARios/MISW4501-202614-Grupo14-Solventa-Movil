package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.core.api.siniestros.CambioDeEstadoDto
import co.solventa.movil.core.api.siniestros.DetalleDeSiniestroDto
import co.solventa.movil.core.api.siniestros.PolizaDelSiniestroDto
import co.solventa.movil.core.api.siniestros.ResumenDeSiniestroDto
import java.time.Instant

/** Del contrato de la consulta de siniestros (HU-M2.4-01, HA-L02) al modelo de la pantalla. */
object MapeadorDeSiniestros {

    fun desdeResumen(dto: ResumenDeSiniestroDto): Siniestro = Siniestro(
        id = dto.id,
        radicado = dto.radicado,
        estado = EstadoDeSiniestro.de(dto.estado),
        codigoDeEstado = dto.estado,
        poliza = dto.poliza.id,
        linea = lineaDe(dto.poliza),
        producto = dto.poliza.producto,
        objetoAsegurado = dto.poliza.objetoAsegurado,
        tipoDeIncidente = dto.tipoDeIncidente,
        ocurrioEn = instante(dto.ocurrioEn),
        radicadoEn = instante(dto.radicadoEn),
    )

    fun desdeDetalle(dto: DetalleDeSiniestroDto): Siniestro = Siniestro(
        id = dto.id,
        radicado = dto.radicado,
        estado = EstadoDeSiniestro.de(dto.estado),
        codigoDeEstado = dto.estado,
        poliza = dto.poliza.id,
        linea = lineaDe(dto.poliza),
        producto = dto.poliza.producto,
        objetoAsegurado = dto.poliza.objetoAsegurado,
        tipoDeIncidente = dto.tipoDeIncidente,
        ocurrioEn = instante(dto.ocurrioEn),
        radicadoEn = instante(dto.radicadoEn),
        lugar = dto.lugar.ifBlank { null },
    )

    /** El historial ordenado del más antiguo al más reciente. */
    fun cambios(dtos: List<CambioDeEstadoDto>): List<CambioDeEstado> =
        dtos.map { CambioDeEstado(EstadoDeSiniestro.de(it.estado), instante(it.cambioEn)) }.sortedBy { it.en }

    /**
     * La frescura de una respuesta (HA-L02). Sin `as_of` (un servicio
     * anterior a HA-L02) se toma la hora en que llegó la respuesta.
     */
    fun frescura(desdeCache: Boolean, leidoEn: String?, ahora: Long): Frescura =
        Frescura(desdeCache, leidoEn?.let(::instante) ?: ahora)

    /**
     * La línea de la póliza. Un siniestro radicado desde el móvil no trae la
     * copia de la póliza, así que se deduce del prefijo del número, que el
     * núcleo de pólizas fija por línea (`AU-`, `HG-`, `VJ-`, `VH-`).
     */
    fun lineaDe(poliza: PolizaDelSiniestroDto): String? = poliza.linea ?: when (poliza.id.substringBefore('-')) {
        "AU" -> "auto"
        "HG" -> "home"
        "VJ" -> "travel"
        "VH" -> "life"
        else -> null
    }

    private fun instante(texto: String): Long = Instant.parse(texto).toEpochMilli()
}
