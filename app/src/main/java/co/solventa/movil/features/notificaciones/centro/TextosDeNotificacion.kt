package co.solventa.movil.features.notificaciones.centro

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import co.solventa.movil.R
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Cómo se presentan las notificaciones (HU-M3.1-01). Es lógica pura, sin
 * Android, para que se pruebe en la JVM y ningún composable la decida: las
 * pantallas y la notificación del sistema escriben con estas claves.
 */
object TextosDeNotificacion {

    /**
     * «$ 180.000 COP» en el idioma elegido, como el prototipo (M15 y M15b):
     * el monto y la moneda del criterio 2.
     */
    fun monto(valor: Double, moneda: String, locale: Locale): String =
        "$ ${NumberFormat.getIntegerInstance(locale).format(Math.round(valor))} $moneda"

    /** Un valor observado o un umbral: «215». */
    fun numero(valor: Double, locale: Locale): String = NumberFormat.getNumberInstance(locale).format(valor)

    /** «Visa •••• 1111»: la marca con mayúscula inicial y los últimos cuatro. */
    fun medioDePago(m: MedioDePago, locale: Locale): String =
        "${m.marca.replaceFirstChar { it.titlecase(locale) }} •••• ${m.ultimos4}"

    /** El texto de la referencia del evento, según el parámetro (criterio 2). */
    @StringRes
    fun evento(parametro: String): Int = when (parametro) {
        ParametrosDePago.RETRASO_DE_VUELO -> R.string.notificaciones_evento_retraso_de_vuelo
        else -> R.string.notificaciones_evento_generico
    }

    /** El cuerpo de la notificación del sistema (M15b), según el parámetro. */
    @StringRes
    fun cuerpoDelAviso(parametro: String): Int = when (parametro) {
        ParametrosDePago.RETRASO_DE_VUELO -> R.string.notificaciones_aviso_cuerpo_retraso_de_vuelo
        else -> R.string.notificaciones_aviso_cuerpo_generico
    }

    /** El cuerpo de la tarjeta del centro (M15), según el parámetro. */
    @StringRes
    fun cuerpoDelCentro(parametro: String): Int = when (parametro) {
        ParametrosDePago.RETRASO_DE_VUELO -> R.string.notificaciones_centro_cuerpo_retraso_de_vuelo
        else -> R.string.notificaciones_centro_cuerpo_generico
    }

    @StringRes
    fun parametro(parametro: String): Int? = when (parametro) {
        ParametrosDePago.RETRASO_DE_VUELO -> R.string.notificaciones_parametro_retraso_de_vuelo
        else -> null
    }

    /** «215 min» o el valor con su unidad tal cual, si la aplicación no la conoce. */
    @StringRes
    fun unidad(unidad: String): Int? = when (unidad) {
        ParametrosDePago.MINUTOS -> R.string.notificaciones_unidad_minutos
        else -> null
    }

    /**
     * El título de un cambio de etapa (HU-M3.3-01): con el monto si la etapa
     * lo trae (criterios 2 y 3), en cuyo caso la clave lleva un parámetro.
     */
    @StringRes
    fun tituloDeEtapa(etapa: String, conMonto: Boolean): Int = when (etapa) {
        EtapasDeSiniestro.EN_EVALUACION -> R.string.notificaciones_etapa_titulo_evaluacion
        EtapasDeSiniestro.APROBADO ->
            if (conMonto) R.string.notificaciones_etapa_titulo_aprobado_monto else R.string.notificaciones_etapa_titulo_aprobado
        EtapasDeSiniestro.PAGADO ->
            if (conMonto) R.string.notificaciones_etapa_titulo_pagado_monto else R.string.notificaciones_etapa_titulo_pagado
        else -> R.string.notificaciones_etapa_titulo_generico
    }

    /**
     * El cuerpo de un cambio de etapa: el radicado y la fecha y hora de la
     * transición (criterio 5); en el pago, la del desembolso (criterio 3).
     * Recibe el radicado, la fecha y la hora.
     */
    @StringRes
    fun cuerpoDeEtapa(etapa: String): Int = when (etapa) {
        EtapasDeSiniestro.EN_EVALUACION -> R.string.notificaciones_etapa_cuerpo_evaluacion
        EtapasDeSiniestro.APROBADO -> R.string.notificaciones_etapa_cuerpo_aprobado
        EtapasDeSiniestro.PAGADO -> R.string.notificaciones_etapa_cuerpo_pagado
        else -> R.string.notificaciones_etapa_cuerpo_generico
    }

    /** El instante que cuenta el cuerpo: el del desembolso en el pago, el del cambio en las demás. */
    fun instanteDeEtapa(e: CambioDeEtapa): Long =
        if (e.etapa == EtapasDeSiniestro.PAGADO) e.pagadoEn ?: e.cambioEn else e.cambioEn

    /** El ícono de cada etapa: el reloj en evaluación, el visto al aprobar y el escudo al pagar. */
    @DrawableRes
    fun iconoDeEtapa(etapa: String): Int = when (etapa) {
        EtapasDeSiniestro.APROBADO -> R.drawable.ic_circulo_check
        EtapasDeSiniestro.PAGADO -> R.drawable.ic_escudo_check
        else -> R.drawable.ic_reloj
    }

    /** Si la notificación es de hoy, para agruparla en «Hoy» o «Anteriores» (M15). */
    fun esDeHoy(instante: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): Boolean {
        val dia = { t: Long -> Instant.ofEpochMilli(t).atZone(zona).toLocalDate() }
        return dia(instante) == dia(ahora)
    }
}
