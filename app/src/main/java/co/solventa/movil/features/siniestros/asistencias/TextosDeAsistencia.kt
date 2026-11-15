package co.solventa.movil.features.siniestros.asistencias

import androidx.annotation.StringRes
import co.solventa.movil.R

/**
 * El texto de cada estado de la solicitud de asistencia (HU-M2.3-01). Vive
 * fuera del archivo de interfaz para que la correspondencia se pruebe en la
 * JVM: la pantalla y la notificación del sistema leen de aquí.
 */
object TextosDeAsistencia {

    @StringRes
    fun dialogo(tipo: TipoDeDialogo): Int = when (tipo) {
        TipoDeDialogo.FALTA_PERMISO -> R.string.siniestros_asistencia_dialogo_permiso_titulo
        TipoDeDialogo.PERMISO_NEGADO_PARA_SIEMPRE -> R.string.siniestros_asistencia_dialogo_bloqueado_titulo
        TipoDeDialogo.SERVICIO_APAGADO -> R.string.siniestros_asistencia_dialogo_servicio_titulo
    }

    /** El aviso amarillo de M14c. */
    @StringRes
    fun sinUbicacion(motivo: MotivoSinUbicacion): Int = when (motivo) {
        MotivoSinUbicacion.PERMISO_NEGADO -> R.string.siniestros_asistencia_sin_ubicacion_permiso
        MotivoSinUbicacion.UBICACION_APAGADA -> R.string.siniestros_asistencia_sin_ubicacion_apagada
        MotivoSinUbicacion.SIN_LECTURA -> R.string.siniestros_asistencia_sin_ubicacion_senal
    }

    /** El mismo texto en M14 y en la notificación de S6. */
    @StringRes
    fun descarte(motivo: MotivoDeDescarte): Int = when (motivo) {
        MotivoDeDescarte.POLIZA_NO_ACTIVA -> R.string.siniestros_asistencia_descarte_poliza
        MotivoDeDescarte.VENCIDA -> R.string.siniestros_asistencia_descarte_vencida
        MotivoDeDescarte.RECHAZADA -> R.string.siniestros_asistencia_descarte_rechazada
    }

    @StringRes
    fun estado(paso: PasoDelEstado): Int = when (paso) {
        PasoDelEstado.UBICACION_RECIBIDA -> R.string.siniestros_asistencia_estado_ubicacion_recibida
        PasoDelEstado.SOLICITUD_RECIBIDA -> R.string.siniestros_asistencia_estado_solicitud_recibida
        PasoDelEstado.ENVIANDO -> R.string.siniestros_asistencia_enviando
        PasoDelEstado.ENVIANDO_SOLICITUD -> R.string.siniestros_asistencia_enviando_solicitud
        PasoDelEstado.SIN_CONEXION -> R.string.siniestros_asistencia_sin_conexion
        PasoDelEstado.NO_REGISTRADA -> R.string.siniestros_asistencia_no_registrada_titulo
        PasoDelEstado.LLAMADA_EN_CURSO -> R.string.siniestros_asistencia_estado_llamada
        PasoDelEstado.POR_DESPACHAR -> R.string.siniestros_asistencia_estado_por_despachar
        PasoDelEstado.EN_ATENCION -> R.string.siniestros_asistencia_estado_en_atencion
        PasoDelEstado.DESPACHADA -> R.string.siniestros_asistencia_estado_despachada
        PasoDelEstado.CERRADA -> R.string.siniestros_asistencia_estado_cerrada
        PasoDelEstado.DESCONOCIDO -> R.string.siniestros_asistencia_estado_desconocido
    }

    /**
     * «Datos que enviamos» solo cuando el servicio ya los recibió; en la cola o
     * descartada son los datos de la solicitud, que aún no se enviaron.
     */
    @StringRes
    fun datosTitulo(envio: EstadoDeEnvio): Int = when (envio) {
        EstadoDeEnvio.Confirmada -> R.string.siniestros_asistencia_datos_titulo
        EstadoDeEnvio.EnCola, is EstadoDeEnvio.Descartada -> R.string.siniestros_asistencia_datos_titulo_solicitud
    }

    @StringRes
    fun hemisferio(hemisferio: Hemisferio): Int = when (hemisferio) {
        Hemisferio.NORTE -> R.string.siniestros_asistencia_norte
        Hemisferio.SUR -> R.string.siniestros_asistencia_sur
        Hemisferio.ESTE -> R.string.siniestros_asistencia_este
        Hemisferio.OESTE -> R.string.siniestros_asistencia_oeste
    }
}
