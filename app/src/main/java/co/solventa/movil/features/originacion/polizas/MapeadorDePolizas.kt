package co.solventa.movil.features.originacion.polizas

import co.solventa.movil.core.api.originacion.PolizaDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime

/**
 * Traduce las pólizas entre la red, el dominio y la copia local (HU-M1.2-01).
 *
 * La copia guarda cada póliza como JSON y ese JSON es lo que se cifra: la
 * tabla no conoce los campos, así que ningún dato de la póliza queda en claro
 * en la base. El formato guardado tiene su propio `@SerialName` en cada campo
 * para que un cambio de nombre en Kotlin no deje ilegible la copia de nadie.
 */
object MapeadorDePolizas {

    private val json = Json { ignoreUnknownKeys = true }

    /** La póliza del servicio. Una fecha que no se entiende hace fallar la lista entera. */
    fun desdeDto(dto: PolizaDto): Poliza = Poliza(
        id = dto.id,
        origen = dto.origen,
        linea = dto.linea,
        producto = dto.producto?.takeIf { it.isNotBlank() },
        objetoAsegurado = dto.objetoAsegurado?.takeIf { it.isNotBlank() },
        estado = dto.estado,
        asegurado = Asegurado(dto.asegurado.nombre, dto.asegurado.tipoDeDocumento, dto.asegurado.documento),
        prima = dto.prima,
        moneda = dto.moneda,
        valorAsegurado = dto.valorAsegurado,
        coberturas = dto.coberturas.map { Cobertura(it.codigo, it.monto) },
        inicio = instante(dto.inicio),
        fin = instante(dto.fin),
        renovacionDe = dto.renovacionDe?.takeIf { it.isNotBlank() },
    )

    /** El JSON que se cifra y se guarda. */
    fun aJson(poliza: Poliza): String = json.encodeToString(PolizaGuardada.serializer(), PolizaGuardada.de(poliza))

    /** La póliza guardada, o `null` si el JSON no se entiende. */
    fun desdeJson(texto: String): Poliza? =
        runCatching { json.decodeFromString(PolizaGuardada.serializer(), texto).aPoliza() }.getOrNull()

    private fun instante(texto: String): Long = OffsetDateTime.parse(texto).toInstant().toEpochMilli()
}

/** Forma en que una póliza queda guardada (cifrada) en el teléfono. */
@Serializable
private data class PolizaGuardada(
    @SerialName("id") val id: String,
    @SerialName("origen") val origen: String,
    @SerialName("linea") val linea: String,
    @SerialName("producto") val producto: String? = null,
    @SerialName("objeto") val objetoAsegurado: String? = null,
    @SerialName("estado") val estado: String,
    @SerialName("asegurado") val asegurado: AseguradoGuardado,
    @SerialName("prima") val prima: Double? = null,
    @SerialName("moneda") val moneda: String,
    @SerialName("valor_asegurado") val valorAsegurado: Double,
    @SerialName("coberturas") val coberturas: List<CoberturaGuardada> = emptyList(),
    @SerialName("inicio") val inicio: Long,
    @SerialName("fin") val fin: Long,
    @SerialName("renovacion_de") val renovacionDe: String? = null,
) {
    fun aPoliza() = Poliza(
        id = id, origen = origen, linea = linea, producto = producto, objetoAsegurado = objetoAsegurado,
        estado = estado, asegurado = Asegurado(asegurado.nombre, asegurado.tipoDeDocumento, asegurado.documento),
        prima = prima, moneda = moneda, valorAsegurado = valorAsegurado,
        coberturas = coberturas.map { Cobertura(it.codigo, it.monto) }, inicio = inicio, fin = fin,
        renovacionDe = renovacionDe,
    )

    companion object {
        fun de(p: Poliza) = PolizaGuardada(
            id = p.id, origen = p.origen, linea = p.linea, producto = p.producto, objetoAsegurado = p.objetoAsegurado,
            estado = p.estado, asegurado = AseguradoGuardado(p.asegurado.nombre, p.asegurado.tipoDeDocumento, p.asegurado.documento),
            prima = p.prima, moneda = p.moneda, valorAsegurado = p.valorAsegurado,
            coberturas = p.coberturas.map { CoberturaGuardada(it.codigo, it.monto) }, inicio = p.inicio, fin = p.fin,
            renovacionDe = p.renovacionDe,
        )
    }
}

@Serializable
private data class AseguradoGuardado(
    @SerialName("nombre") val nombre: String,
    @SerialName("tipo_documento") val tipoDeDocumento: String? = null,
    @SerialName("documento") val documento: String? = null,
)

@Serializable
private data class CoberturaGuardada(
    @SerialName("codigo") val codigo: String,
    @SerialName("monto") val monto: Double,
)
