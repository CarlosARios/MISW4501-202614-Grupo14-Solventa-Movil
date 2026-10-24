package co.solventa.movil.features.auditoria.consentimientos

import androidx.annotation.StringRes
import co.solventa.movil.R

/**
 * Textos de los codigos que entrega el servicio de auditoria (HU-M1.3-01). Cada
 * funcion devuelve la clave del texto en el idioma elegido, o `null` para un
 * codigo que la aplicacion todavia no conoce: entonces se muestra el codigo
 * tal cual, en lugar de esconderlo.
 */
object EtiquetasDeConsentimiento {

    @StringRes
    fun estado(codigo: String): Int? = when (codigo) {
        EstadosDeConsentimiento.ACTIVO -> R.string.auditoria_consentimientos_estado_activo
        EstadosDeConsentimiento.VENCIDO -> R.string.auditoria_consentimientos_estado_vencido
        EstadosDeConsentimiento.REVOCADO -> R.string.auditoria_consentimientos_estado_revocado
        EstadosDeConsentimiento.REVOCACION_PENDIENTE -> R.string.auditoria_consentimientos_estado_revocacion_pendiente
        EstadosDeConsentimiento.REVOCADO_PARCIAL -> R.string.auditoria_consentimientos_estado_revocado_parcial
        else -> null
    }

    @StringRes
    fun cambio(codigo: String): Int? = when (codigo) {
        CambiosDeEstado.OTORGADO -> R.string.auditoria_consentimientos_historial_otorgado
        CambiosDeEstado.VENCIDO -> R.string.auditoria_consentimientos_historial_vencido
        CambiosDeEstado.REVOCACION_SOLICITADA -> R.string.auditoria_consentimientos_historial_revocacion_solicitada
        CambiosDeEstado.REVOCADO -> R.string.auditoria_consentimientos_historial_revocado
        else -> estado(codigo)
    }

    @StringRes
    fun fuente(codigo: String): Int? = when (codigo) {
        "open_finance" -> R.string.auditoria_consentimientos_fuente_open_finance
        "open_data" -> R.string.auditoria_consentimientos_fuente_open_data
        "kyc" -> R.string.auditoria_consentimientos_fuente_kyc
        else -> null
    }

    @StringRes
    fun categoria(codigo: String): Int? = when (codigo) {
        "credit_products" -> R.string.auditoria_consentimientos_categoria_productos_de_credito
        "balances" -> R.string.auditoria_consentimientos_categoria_saldos
        "payment_behavior" -> R.string.auditoria_consentimientos_categoria_comportamiento_de_pago
        "public_records" -> R.string.auditoria_consentimientos_categoria_registros_publicos
        "identity_verification" -> R.string.auditoria_consentimientos_categoria_verificacion_de_identidad
        else -> null
    }

    @StringRes
    fun vigencia(codigo: String): Int? = when (codigo) {
        "12_months" -> R.string.auditoria_consentimientos_vigencia_12_meses
        "single_use" -> R.string.auditoria_consentimientos_vigencia_un_uso
        else -> null
    }

    @StringRes
    fun finalidad(codigo: String): Int? = when (codigo) {
        "risk_profiling_mortgage_life_offer" -> R.string.auditoria_consentimientos_finalidad_perfil_y_oferta
        else -> null
    }

    @StringRes
    fun canal(codigo: String): Int? = when (codigo) {
        "embedded" -> R.string.auditoria_consentimientos_canal_aliado
        "mobile" -> R.string.auditoria_consentimientos_canal_movil
        else -> null
    }
}
