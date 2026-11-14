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
        etapa = dto.etapa?.let { e ->
            CambioDeEtapa(
                cambio = e.cambio, siniestroId = e.siniestroId, radicado = e.radicado, poliza = e.poliza, etapa = e.etapa,
                cambioEn = instante(e.cambioEn), monto = e.monto, moneda = e.moneda, pagadoEn = e.pagadoEn?.let(::instante),
            )
        },
        vencimiento = dto.vencimiento?.let { v ->
            AlertaDeVencimiento(
                poliza = v.poliza, linea = v.linea, producto = v.producto, objetoAsegurado = v.objetoAsegurado,
                venceEn = instante(v.venceEn), hito = v.hito, diasRestantes = v.diasRestantes,
            )
        },
    )

    fun aEntidad(n: Notificacion, propietario: String) = NotificacionEntidad(
        propietario = propietario, id = n.id, tipo = n.tipo, ocurrioEn = n.ocurrioEn, leidaEn = n.leidaEn,
        pago = n.pago?.let { json.encodeToString(PagoGuardado.serializer(), PagoGuardado.de(it)) },
        etapa = n.etapa?.let { json.encodeToString(EtapaGuardada.serializer(), EtapaGuardada.de(it)) },
        vencimiento = n.vencimiento?.let { json.encodeToString(VencimientoGuardado.serializer(), VencimientoGuardado.de(it)) },
    )

    /** La notificación guardada; un pago, una etapa o un vencimiento ilegibles la dejan sin ellos. */
    fun desdeEntidad(e: NotificacionEntidad) = Notificacion(
        id = e.id, tipo = e.tipo, ocurrioEn = e.ocurrioEn, leidaEn = e.leidaEn,
        pago = e.pago?.let { texto -> runCatching { json.decodeFromString(PagoGuardado.serializer(), texto).aPago() }.getOrNull() },
        etapa = e.etapa?.let { texto -> runCatching { json.decodeFromString(EtapaGuardada.serializer(), texto).aEtapa() }.getOrNull() },
        vencimiento = e.vencimiento?.let { texto ->
            runCatching { json.decodeFromString(VencimientoGuardado.serializer(), texto).aAlerta() }.getOrNull()
        },
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

/** Forma en que el cambio de etapa queda guardado en el teléfono (HU-M3.3-01). */
@Serializable
private data class EtapaGuardada(
    @SerialName("cambio") val cambio: Long,
    @SerialName("siniestro_id") val siniestroId: String,
    @SerialName("radicado") val radicado: String,
    @SerialName("poliza") val poliza: String,
    @SerialName("etapa") val etapa: String,
    @SerialName("cambio_en") val cambioEn: Long,
    @SerialName("monto") val monto: Double? = null,
    @SerialName("moneda") val moneda: String? = null,
    @SerialName("pagado_en") val pagadoEn: Long? = null,
) {
    fun aEtapa() = CambioDeEtapa(cambio, siniestroId, radicado, poliza, etapa, cambioEn, monto, moneda, pagadoEn)

    companion object {
        fun de(e: CambioDeEtapa) = EtapaGuardada(
            cambio = e.cambio, siniestroId = e.siniestroId, radicado = e.radicado, poliza = e.poliza, etapa = e.etapa,
            cambioEn = e.cambioEn, monto = e.monto, moneda = e.moneda, pagadoEn = e.pagadoEn,
        )
    }
}

/** Forma en que la alerta de vencimiento queda guardada en el teléfono (HU-M3.2-01). */
@Serializable
private data class VencimientoGuardado(
    @SerialName("poliza") val poliza: String,
    @SerialName("linea") val linea: String,
    @SerialName("producto") val producto: String? = null,
    @SerialName("objeto_asegurado") val objetoAsegurado: String? = null,
    @SerialName("vence_en") val venceEn: Long,
    @SerialName("hito") val hito: Int,
    @SerialName("dias_restantes") val diasRestantes: Int,
) {
    fun aAlerta() = AlertaDeVencimiento(poliza, linea, producto, objetoAsegurado, venceEn, hito, diasRestantes)

    companion object {
        fun de(a: AlertaDeVencimiento) = VencimientoGuardado(
            poliza = a.poliza, linea = a.linea, producto = a.producto, objetoAsegurado = a.objetoAsegurado,
            venceEn = a.venceEn, hito = a.hito, diasRestantes = a.diasRestantes,
        )
    }
}
