package co.solventa.movil.features.originacion.polizas

import java.util.concurrent.TimeUnit

/**
 * Cómo se ordena y se presenta la billetera (HU-M1.2-01). Es lógica pura, sin
 * Android, para que las reglas se prueben en la JVM y ningún composable las
 * decida.
 */
object PresentacionDePolizas {

    /** Una póliza activa que termina dentro de estos días va «Por vencer». */
    const val DIAS_POR_VENCER = 30L

    fun situacion(poliza: Poliza, ahora: Long): Situacion = when {
        poliza.estado == EstadosDePoliza.CANCELADA -> Situacion.CANCELADA
        poliza.estado == EstadosDePoliza.VENCIDA || poliza.fin < ahora -> Situacion.VENCIDA
        poliza.fin - ahora <= TimeUnit.DAYS.toMillis(DIAS_POR_VENCER) -> Situacion.POR_VENCER
        else -> Situacion.ACTIVA
    }

    /**
     * Las tarjetas de M4. Una póliza renovada con número nuevo **reemplaza** a
     * la anterior: se muestra solo la nueva, marcada como renovada, como en el
     * prototipo. Las vigentes van primero; dentro de cada grupo se respeta el
     * orden del servicio.
     */
    fun tarjetas(polizas: List<Poliza>, cambios: List<CambioDePoliza>, ahora: Long): List<PolizaEnLista> {
        val renovadasPorOtra = polizas.mapNotNull { it.renovacionDe }.toSet()
        val renovadasAhora = cambios.filter { it.tipo == TipoDeCambio.RENOVADA }.map { it.poliza.id }.toSet()
        return polizas
            .filterNot { it.id in renovadasPorOtra }
            .map { PolizaEnLista(it, situacion(it, ahora), renovada = it.renovacionDe != null || it.id in renovadasAhora) }
            .sortedBy { if (it.situacion == Situacion.ACTIVA || it.situacion == Situacion.POR_VENCER) 0 else 1 }
    }

    /** Qué dice la línea bajo el nombre de la póliza. */
    enum class Subtitulo { VIGENCIA, VENCE, RENOVADA, VENCIO, CANCELADA }

    fun subtitulo(p: PolizaEnLista): Subtitulo = when {
        p.situacion == Situacion.CANCELADA -> Subtitulo.CANCELADA
        p.situacion == Situacion.VENCIDA -> Subtitulo.VENCIO
        p.renovada -> Subtitulo.RENOVADA
        p.situacion == Situacion.POR_VENCER -> Subtitulo.VENCE
        else -> Subtitulo.VIGENCIA
    }

    /** Una póliza cancelada o vencida ya no cubre: se atenúa. */
    fun sinCobertura(p: PolizaEnLista): Boolean = p.situacion == Situacion.CANCELADA || p.situacion == Situacion.VENCIDA

    /** «Activa · renovada»: una cancelada no se anuncia como renovada. */
    fun insigniaRenovada(p: PolizaEnLista): Boolean = p.renovada && p.situacion != Situacion.CANCELADA

    /** Si la copia lleva más de [dias] días sin sincronizar (criterio 5). */
    fun desactualizada(sincronizadoEn: Long?, ahora: Long, dias: Long): Boolean =
        sincronizadoEn != null && ahora - sincronizadoEn > TimeUnit.DAYS.toMillis(dias)
}
