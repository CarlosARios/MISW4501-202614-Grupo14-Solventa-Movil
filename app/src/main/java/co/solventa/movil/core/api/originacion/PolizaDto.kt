package co.solventa.movil.core.api.originacion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Respuesta de `GET /v1/policies` (HU-M1.2-01): todas las pólizas del
 * asegurado, las que emitió originación y las del núcleo de pólizas.
 *
 * `parcial` es `true` cuando el núcleo no respondió y sus pólizas faltan: la
 * aplicación **no** reemplaza su copia con una lista parcial, porque las que
 * faltan no están canceladas, solo no llegaron.
 */
@Serializable
data class ListaDePolizasDto(
    @SerialName("items") val elementos: List<PolizaDto> = emptyList(),
    @SerialName("partial") val parcial: Boolean = false,
)

/**
 * Una póliza tal como la entrega el servicio. Las fechas viajan en RFC 3339
 * (UTC) y los montos en pesos; el mapeador del dominio convierte las fechas.
 * Solo lo que la billetera usa: el resto de la renovación (HU-W1.5-02) se
 * ignora.
 */
@Serializable
data class PolizaDto(
    @SerialName("policy_id") val id: String,
    /** `origination` o `policy_core`. */
    @SerialName("source") val origen: String,
    /** `life`, `auto`, `home` o `travel`; la aplicación lo nombra en su idioma. */
    @SerialName("line") val linea: String,
    @SerialName("product") val producto: String? = null,
    @SerialName("insured_object") val objetoAsegurado: String? = null,
    /** `active`, `expired` o `cancelled`. */
    @SerialName("status") val estado: String,
    @SerialName("holder") val asegurado: AseguradoDto,
    /** Prima mensual; nula en las del núcleo, que no guarda primas. */
    @SerialName("premium") val prima: Double? = null,
    @SerialName("currency") val moneda: String,
    @SerialName("insured_amount") val valorAsegurado: Double,
    @SerialName("coverages") val coberturas: List<CoberturaDto> = emptyList(),
    @SerialName("coverage_start") val inicio: String,
    @SerialName("coverage_end") val fin: String,
    /** La póliza que esta renueva; nula si es nueva. */
    @SerialName("renewal_of") val renovacionDe: String? = null,
)

/** El asegurado principal: quién es, nunca cómo contactarlo. */
@Serializable
data class AseguradoDto(
    @SerialName("full_name") val nombre: String,
    @SerialName("document_type") val tipoDeDocumento: String? = null,
    @SerialName("document_id") val documento: String? = null,
)

/** Una cobertura con su monto asegurado, en pesos. */
@Serializable
data class CoberturaDto(
    @SerialName("code") val codigo: String,
    @SerialName("insured_amount") val monto: Double,
)
