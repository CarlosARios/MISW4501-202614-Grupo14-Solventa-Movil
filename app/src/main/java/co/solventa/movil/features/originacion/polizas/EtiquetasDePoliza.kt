package co.solventa.movil.features.originacion.polizas

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import co.solventa.movil.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Textos e íconos de los códigos que entrega el servicio de originación
 * (HU-M1.2-01). Cada función de texto devuelve la clave en el idioma elegido,
 * o `null` para un código que la aplicación todavía no conoce: entonces se
 * muestra el código tal cual, en lugar de esconderlo.
 */
object EtiquetasDePoliza {

    @StringRes
    fun linea(codigo: String): Int? = when (codigo) {
        LineasDePoliza.VIDA -> R.string.originacion_polizas_linea_vida
        LineasDePoliza.AUTOS -> R.string.originacion_polizas_linea_autos
        LineasDePoliza.HOGAR -> R.string.originacion_polizas_linea_hogar
        LineasDePoliza.VIAJE -> R.string.originacion_polizas_linea_viaje
        else -> null
    }

    @DrawableRes
    fun icono(codigo: String): Int = when (codigo) {
        LineasDePoliza.AUTOS -> R.drawable.ic_carro
        LineasDePoliza.HOGAR -> R.drawable.ic_inicio
        LineasDePoliza.VIAJE -> R.drawable.ic_avion
        else -> R.drawable.ic_escudo
    }

    @StringRes
    fun situacion(situacion: Situacion): Int = when (situacion) {
        Situacion.ACTIVA -> R.string.originacion_polizas_situacion_activa
        Situacion.POR_VENCER -> R.string.originacion_polizas_situacion_por_vencer
        Situacion.VENCIDA -> R.string.originacion_polizas_situacion_vencida
        Situacion.CANCELADA -> R.string.originacion_polizas_situacion_cancelada
    }

    @StringRes
    fun cobertura(codigo: String): Int? = when (codigo) {
        "death" -> R.string.originacion_polizas_cobertura_muerte
        "total_permanent_disability" -> R.string.originacion_polizas_cobertura_incapacidad
        "collision" -> R.string.originacion_polizas_cobertura_choque
        "theft" -> R.string.originacion_polizas_cobertura_hurto
        "third_party_damage" -> R.string.originacion_polizas_cobertura_terceros
        "glass" -> R.string.originacion_polizas_cobertura_vidrios
        "other" -> R.string.originacion_polizas_cobertura_otros
        "water_damage" -> R.string.originacion_polizas_cobertura_agua
        "flight_delay" -> R.string.originacion_polizas_cobertura_retraso_de_vuelo
        else -> null
    }

    @StringRes
    fun cambio(tipo: TipoDeCambio): Int = when (tipo) {
        TipoDeCambio.RENOVADA -> R.string.originacion_polizas_cambio_renovada
        TipoDeCambio.CANCELADA -> R.string.originacion_polizas_cambio_cancelada
        TipoDeCambio.MODIFICADA -> R.string.originacion_polizas_cambio_modificada
    }

    /**
     * «$ 72.500.000» en el idioma elegido, como el prototipo. Una moneda que
     * no es el peso colombiano lleva su código, para no confundirlas.
     */
    fun monto(valor: Double, moneda: String, locale: Locale): String {
        val numero = NumberFormat.getIntegerInstance(locale).format(Math.round(valor))
        return if (moneda == MONEDA_LOCAL) "$ $numero" else "$ $numero $moneda"
    }

    private const val MONEDA_LOCAL = "COP"
}
