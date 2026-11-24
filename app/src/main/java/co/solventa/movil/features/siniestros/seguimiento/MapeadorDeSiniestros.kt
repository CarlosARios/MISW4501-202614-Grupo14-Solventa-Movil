package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.core.api.siniestros.CambioDeEstadoDto
import co.solventa.movil.core.api.siniestros.DetalleDeSiniestroDto
import co.solventa.movil.core.api.siniestros.PolizaDelSiniestroDto
import co.solventa.movil.core.api.siniestros.ResolucionAutomaticaDto
import co.solventa.movil.core.api.siniestros.ResumenDeSiniestroDto
import co.solventa.movil.core.api.siniestros.TriajeDelAseguradoDto
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
        resolucionAutomatica = dto.resolucionAutomatica?.let(::resolucion),
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
        resolucionAutomatica = dto.resolucionAutomatica?.let(::resolucion),
    )

    /** La resolución automática de un siniestro paramétrico (criterio 3). */
    fun resolucion(dto: ResolucionAutomaticaDto): ResolucionAutomatica = ResolucionAutomatica(
        resueltaEn = instante(dto.resueltaEn),
        version = dto.version,
        disparador = Disparador(
            parametro = dto.disparador.parametro,
            referencia = dto.disparador.referencia,
            umbral = dto.disparador.umbral,
            observado = dto.disparador.observado,
            unidad = dto.disparador.unidad,
            observadoEn = instante(dto.disparador.observadoEn),
        ),
    )

    /**
     * La estimación del triage (criterio 2): solo cuando ya se calculó y trae
     * los días hábiles; mientras tanto la etapa no muestra estimado.
     */
    fun estimacion(dto: TriajeDelAseguradoDto): EstimacionDeResolucion? {
        val dias = dto.diasHabilesEstimados
        return if (dto.estado == TRIAJE_CALCULADO && dias != null) EstimacionDeResolucion(dto.nivel, dias) else null
    }

    private const val TRIAJE_CALCULADO = "assessed"

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
