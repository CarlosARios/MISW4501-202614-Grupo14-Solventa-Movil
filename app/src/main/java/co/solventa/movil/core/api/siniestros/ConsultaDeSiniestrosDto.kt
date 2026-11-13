package co.solventa.movil.core.api.siniestros

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Lo que la aplicación lee de la consulta de siniestros (HU-M2.4-01 y HA-L02).
 * Solo lo que usa; el resto de la respuesta se ignora.
 *
 * `stale` y `as_of` son de HA-L02: `stale` es `true` cuando la base del
 * servicio no respondió a tiempo y la respuesta es la última que guardó la
 * caché de lectura; `as_of` es cuándo se leyeron esos datos.
 */

/** La póliza del siniestro. Uno radicado desde el móvil solo trae el número. */
@Serializable
data class PolizaDelSiniestroDto(
    @SerialName("policy_id") val id: String,
    @SerialName("line") val linea: String? = null,
    @SerialName("product") val producto: String? = null,
    @SerialName("insured_object") val objetoAsegurado: String? = null,
)

/** Un siniestro de la lista. */
@Serializable
data class ResumenDeSiniestroDto(
    @SerialName("claim_id") val id: String,
    @SerialName("filing_number") val radicado: String,
    @SerialName("status") val estado: String,
    @SerialName("policy") val poliza: PolizaDelSiniestroDto,
    @SerialName("incident_type") val tipoDeIncidente: String,
    @SerialName("occurred_at") val ocurrioEn: String,
    @SerialName("filed_at") val radicadoEn: String,
    @SerialName("automatic_resolution") val resolucionAutomatica: ResolucionAutomaticaDto? = null,
)

@Serializable
data class ListaDeSiniestrosDto(
    @SerialName("items") val elementos: List<ResumenDeSiniestroDto> = emptyList(),
    @SerialName("stale") val desdeCache: Boolean = false,
    @SerialName("as_of") val leidoEn: String? = null,
)

/** El encabezado de M10: lo de la lista más el lugar del hecho. */
@Serializable
data class DetalleDeSiniestroDto(
    @SerialName("claim_id") val id: String,
    @SerialName("filing_number") val radicado: String,
    @SerialName("status") val estado: String,
    @SerialName("policy") val poliza: PolizaDelSiniestroDto,
    @SerialName("incident_type") val tipoDeIncidente: String,
    @SerialName("occurred_at") val ocurrioEn: String,
    @SerialName("filed_at") val radicadoEn: String,
    @SerialName("place") val lugar: String = "",
    @SerialName("automatic_resolution") val resolucionAutomatica: ResolucionAutomaticaDto? = null,
    @SerialName("stale") val desdeCache: Boolean = false,
    @SerialName("as_of") val leidoEn: String? = null,
)

/** Un cambio de estado, con la hora en que pasó. */
@Serializable
data class CambioDeEstadoDto(
    @SerialName("status") val estado: String,
    @SerialName("previous_status") val estadoAnterior: String? = null,
    @SerialName("changed_at") val cambioEn: String,
)

@Serializable
data class HistorialDeSiniestroDto(
    @SerialName("claim_id") val id: String,
    @SerialName("status") val estado: String,
    @SerialName("changes") val cambios: List<CambioDeEstadoDto> = emptyList(),
    @SerialName("stale") val desdeCache: Boolean = false,
    @SerialName("as_of") val leidoEn: String? = null,
)

/**
 * Lo que resolvió un siniestro paramétrico sin evaluación manual
 * (HU-M2.4-01, criterio 3): el evento externo que cruzó el umbral de la
 * póliza. `null` en un siniestro evaluado por un analista.
 */
@Serializable
data class ResolucionAutomaticaDto(
    @SerialName("resolved_at") val resueltaEn: String,
    @SerialName("rule_version") val version: String,
    @SerialName("trigger") val disparador: DisparadorDto,
)

/** El parámetro objetivo, su umbral y el valor observado que generó el pago. */
@Serializable
data class DisparadorDto(
    /** `flight_delay`… */
    @SerialName("parameter") val parametro: String,
    /** Lo observado, por ejemplo el vuelo `AV-8421`. */
    @SerialName("event_reference") val referencia: String,
    @SerialName("threshold") val umbral: Double,
    @SerialName("observed") val observado: Double,
    /** `minutes`… */
    @SerialName("unit") val unidad: String,
    @SerialName("observed_at") val observadoEn: String,
)

/**
 * El triage del siniestro en la vista del asegurado (HU-W2.2-01): el nivel y
 * cuántos días hábiles se espera que tome resolverlo (HU-M2.4-01, criterio 2).
 * Mientras no se ha calculado, `state` es `pending` y lo demás llega en `null`.
 */
@Serializable
data class TriajeDelAseguradoDto(
    @SerialName("state") val estado: String,
    @SerialName("level") val nivel: Int? = null,
    @SerialName("estimated_resolution_business_days") val diasHabilesEstimados: Int? = null,
)
