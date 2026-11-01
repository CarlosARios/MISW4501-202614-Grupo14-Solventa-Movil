package co.solventa.movil.features.notificaciones.centro

import co.solventa.movil.core.api.notificaciones.NotificacionDto
import co.solventa.movil.core.persistencia.entidades.NotificacionEntidad
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime

/**
 * Traduce las notificaciones entre la red, el dominio y la copia local
 * (HU-M3.1-01). El pago se guarda como JSON con su propio `@SerialName` en
 * cada campo, para que un cambio de nombre en Kotlin no deje ilegible la copia.
 */
object MapeadorDeNotificaciones {

    private val json = Json { ignoreUnknownKeys = true }

    /** La notificación del servicio. Una fecha que no se entiende hace fallar la lista. */
    fun desdeDto(dto: NotificacionDto): Notificacion = Notificacion(
        id = dto.id,
        tipo = dto.tipo,
        ocurrioEn = instante(dto.ocurrioEn),
        leidaEn = dto.leidaEn?.let(::instante),
        pago = dto.pago?.let { p ->
            PagoParametrico(
                id = p.id, siniestro = p.siniestro, poliza = p.poliza, monto = p.monto, moneda = p.moneda,
                disparador = Disparador(
                    parametro = p.disparador.parametro, evento = p.disparador.evento, umbral = p.disparador.umbral,
                    observado = p.disparador.observado, unidad = p.disparador.unidad,
                    observadoEn = p.disparador.observadoEn?.let(::instante),
                ),
                medioDePago = MedioDePago(p.medioDePago.marca, p.medioDePago.ultimos4),
                claveDeIdempotencia = p.claveDeIdempotencia,
                pagadoEn = instante(p.pagadoEn),
            )
        },
    )

    fun aEntidad(n: Notificacion, propietario: String) = NotificacionEntidad(
        propietario = propietario, id = n.id, tipo = n.tipo, ocurrioEn = n.ocurrioEn, leidaEn = n.leidaEn,
        pago = n.pago?.let { json.encodeToString(PagoGuardado.serializer(), PagoGuardado.de(it)) },
    )

    /** La notificación guardada; un pago ilegible la deja sin pago. */
    fun desdeEntidad(e: NotificacionEntidad) = Notificacion(
        id = e.id, tipo = e.tipo, ocurrioEn = e.ocurrioEn, leidaEn = e.leidaEn,
        pago = e.pago?.let { texto -> runCatching { json.decodeFromString(PagoGuardado.serializer(), texto).aPago() }.getOrNull() },
    )

    private fun instante(texto: String): Long = OffsetDateTime.parse(texto).toInstant().toEpochMilli()
}

/** Forma en que el pago queda guardado en el teléfono. */
@Serializable
private data class PagoGuardado(
    @SerialName("id") val id: String,
    @SerialName("siniestro") val siniestro: String,
    @SerialName("poliza") val poliza: String,
    @SerialName("monto") val monto: Double,
    @SerialName("moneda") val moneda: String,
    @SerialName("parametro") val parametro: String,
    @SerialName("evento") val evento: String,
    @SerialName("umbral") val umbral: Double,
    @SerialName("observado") val observado: Double,
    @SerialName("unidad") val unidad: String,
    @SerialName("observado_en") val observadoEn: Long? = null,
    @SerialName("marca") val marca: String,
    @SerialName("ultimos4") val ultimos4: String,
    @SerialName("clave") val claveDeIdempotencia: String,
    @SerialName("pagado_en") val pagadoEn: Long,
) {
    fun aPago() = PagoParametrico(
        id = id, siniestro = siniestro, poliza = poliza, monto = monto, moneda = moneda,
        disparador = Disparador(parametro, evento, umbral, observado, unidad, observadoEn),
        medioDePago = MedioDePago(marca, ultimos4), claveDeIdempotencia = claveDeIdempotencia, pagadoEn = pagadoEn,
    )

    companion object {
        fun de(p: PagoParametrico) = PagoGuardado(
            id = p.id, siniestro = p.siniestro, poliza = p.poliza, monto = p.monto, moneda = p.moneda,
            parametro = p.disparador.parametro, evento = p.disparador.evento, umbral = p.disparador.umbral,
            observado = p.disparador.observado, unidad = p.disparador.unidad, observadoEn = p.disparador.observadoEn,
            marca = p.medioDePago.marca, ultimos4 = p.medioDePago.ultimos4, claveDeIdempotencia = p.claveDeIdempotencia,
            pagadoEn = p.pagadoEn,
        )
    }
}
