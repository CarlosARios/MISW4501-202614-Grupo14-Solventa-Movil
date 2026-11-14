package co.solventa.movil.features.siniestros.seguimiento

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import co.solventa.movil.R
import co.solventa.movil.shared.ui.TipoInsignia

/**
 * Textos, íconos y tonos de los códigos de la consulta de siniestros
 * (HU-M2.4-01). Cada función de texto devuelve la clave, o `null` para un
 * código que la aplicación todavía no conoce: entonces se muestra el código
 * tal cual, en lugar de esconderlo.
 */
object EtiquetasDeSiniestro {

    @StringRes
    fun estado(estado: EstadoDeSiniestro): Int? = when (estado) {
        EstadoDeSiniestro.RADICADO -> R.string.siniestros_seguimiento_estado_radicado
        EstadoDeSiniestro.DEVUELTO -> R.string.siniestros_seguimiento_estado_devuelto
        EstadoDeSiniestro.EN_EVALUACION -> R.string.siniestros_seguimiento_estado_en_evaluacion
        EstadoDeSiniestro.APROBADO -> R.string.siniestros_seguimiento_estado_aprobado
        EstadoDeSiniestro.PAGADO -> R.string.siniestros_seguimiento_estado_pagado
        EstadoDeSiniestro.RECHAZADO -> R.string.siniestros_seguimiento_estado_rechazado
        EstadoDeSiniestro.PAGO_CON_NOVEDAD -> R.string.siniestros_seguimiento_estado_pago_con_novedad
        EstadoDeSiniestro.DESCONOCIDO -> null
    }

    /** El tono de la insignia: lo que pide algo al asegurado va en advertencia. */
    fun tono(estado: EstadoDeSiniestro): TipoInsignia = when (estado) {
        EstadoDeSiniestro.DEVUELTO, EstadoDeSiniestro.PAGO_CON_NOVEDAD -> TipoInsignia.ADVERTENCIA
        EstadoDeSiniestro.EN_EVALUACION -> TipoInsignia.INFORMACION
        EstadoDeSiniestro.APROBADO, EstadoDeSiniestro.PAGADO -> TipoInsignia.EXITO
        EstadoDeSiniestro.RADICADO, EstadoDeSiniestro.RECHAZADO, EstadoDeSiniestro.DESCONOCIDO -> TipoInsignia.NEUTRO
    }

    @StringRes
    fun linea(codigo: String?): Int? = when (codigo) {
        "auto" -> R.string.siniestros_seguimiento_linea_autos
        "home" -> R.string.siniestros_seguimiento_linea_hogar
        "travel" -> R.string.siniestros_seguimiento_linea_viaje
        "life" -> R.string.siniestros_seguimiento_linea_vida
        else -> null
    }

    @DrawableRes
    fun icono(codigo: String?): Int = when (codigo) {
        "auto" -> R.drawable.ic_carro
        "home" -> R.drawable.ic_inicio
        "travel" -> R.drawable.ic_avion
        else -> R.drawable.ic_escudo
    }

    @StringRes
    fun incidente(codigo: String): Int? = when (codigo) {
        "collision" -> R.string.siniestros_seguimiento_incidente_colision
        "theft" -> R.string.siniestros_seguimiento_incidente_hurto
        "third_party_damage" -> R.string.siniestros_seguimiento_incidente_terceros
        "glass" -> R.string.siniestros_seguimiento_incidente_vidrios
        "water_damage" -> R.string.siniestros_seguimiento_incidente_agua
        "other" -> R.string.siniestros_seguimiento_incidente_otro
        "flight_delay" -> R.string.siniestros_seguimiento_incidente_retraso_vuelo
        else -> null
    }

    /**
     * El parámetro objetivo de una resolución automática con lo observado:
     * «Retraso del vuelo %1$s» (HU-M2.4-01, criterio 3). `null` para un
     * parámetro que la aplicación todavía no conoce.
     */
    @StringRes
    fun parametro(codigo: String): Int? = when (codigo) {
        "flight_delay" -> R.string.siniestros_seguimiento_parametro_retraso_vuelo
        else -> null
    }

    /** La unidad del disparador: «minutos». `null` si no se conoce: se muestra el código. */
    @StringRes
    fun unidad(codigo: String): Int? = when (codigo) {
        "minutes" -> R.string.siniestros_seguimiento_unidad_minutos
        else -> null
    }

    @StringRes
    fun etapa(etapa: EtapaDeLinea): Int = when (etapa.etapa) {
        Etapa.RADICADO -> R.string.siniestros_seguimiento_etapa_radicado
        Etapa.REVISION -> R.string.siniestros_seguimiento_etapa_revision
        Etapa.EVALUACION -> R.string.siniestros_seguimiento_etapa_evaluacion
        Etapa.DECISION ->
            if (etapa.rechazada) R.string.siniestros_seguimiento_etapa_rechazado else R.string.siniestros_seguimiento_etapa_aprobado
        Etapa.PAGO -> R.string.siniestros_seguimiento_etapa_pagado
        Etapa.EVENTO_DETECTADO -> R.string.siniestros_seguimiento_etapa_evento_detectado
        Etapa.VALIDACION_AUTOMATICA -> R.string.siniestros_seguimiento_etapa_validacion_automatica
        Etapa.PAGO_EJECUTADO -> R.string.siniestros_seguimiento_etapa_pago_ejecutado
    }

    /** Lo que dice una etapa que todavía no llega, como en M10. */
    @StringRes
    fun etapaPendiente(etapa: Etapa): Int? = when (etapa) {
        Etapa.DECISION -> R.string.siniestros_seguimiento_etapa_aprobado_pendiente
        Etapa.PAGO, Etapa.PAGO_EJECUTADO -> R.string.siniestros_seguimiento_etapa_pagado_pendiente
        else -> null
    }

    /** «Qué sigue» de M10, según el estado. */
    @StringRes
    fun queSigue(estado: EstadoDeSiniestro): Int = when (estado) {
        EstadoDeSiniestro.RADICADO -> R.string.siniestros_seguimiento_que_sigue_radicado
        EstadoDeSiniestro.DEVUELTO -> R.string.siniestros_seguimiento_que_sigue_devuelto
        EstadoDeSiniestro.EN_EVALUACION -> R.string.siniestros_seguimiento_que_sigue_en_evaluacion
        EstadoDeSiniestro.APROBADO -> R.string.siniestros_seguimiento_que_sigue_aprobado
        EstadoDeSiniestro.PAGO_CON_NOVEDAD -> R.string.siniestros_seguimiento_que_sigue_pago_con_novedad
        EstadoDeSiniestro.PAGADO -> R.string.siniestros_seguimiento_que_sigue_pagado
        EstadoDeSiniestro.RECHAZADO -> R.string.siniestros_seguimiento_que_sigue_rechazado
        EstadoDeSiniestro.DESCONOCIDO -> R.string.siniestros_seguimiento_que_sigue_desconocido
    }
}
