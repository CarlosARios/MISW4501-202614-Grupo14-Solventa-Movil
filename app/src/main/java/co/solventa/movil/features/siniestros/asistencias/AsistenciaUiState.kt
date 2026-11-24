package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.features.siniestros.radicacion.PolizaElegible

/** Las pólizas sobre las que se puede pedir asistencia (D6). */
sealed interface PolizasDeAsistencia {
    data object Cargando : PolizasDeAsistencia

    /** Sin pólizas activas no hay solicitud, pero sí el botón de llamar (criterio 4). */
    data object SinPolizas : PolizasDeAsistencia

    /** Una sola queda elegida; con varias, [elegida] es `null` hasta que el asegurado escoja. */
    data class Lista(val elegibles: List<PolizaElegible>, val elegida: PolizaElegible?) : PolizasDeAsistencia
}

/** Dónde está M14. Las variantes del prototipo son estados, no rutas. */
sealed interface PasoDeAsistencia {
    /** La pantalla inicial: póliza, línea de atención y «Solicitar asistencia y llamar» (S8). */
    data object Inicial : PasoDeAsistencia

    /** M14b, con la variante que corresponda (criterio 1). */
    data class Dialogo(val tipo: TipoDeDialogo) : PasoDeAsistencia

    /** «Obteniendo tu ubicación…», a lo sumo 8 s. */
    data object ObteniendoUbicacion : PasoDeAsistencia

    /** M14 enviada o M14c: la solicitud quedó en la cola y el marcador se abrió. */
    data object Solicitada : PasoDeAsistencia
}

/**
 * Lo que la pantalla tiene que hacer con el sistema operativo una sola vez. El
 * `ViewModel` decide; la pantalla solo ejecuta y avisa que lo hizo.
 */
sealed interface EventoDeAsistencia {
    /** El diálogo de permisos del sistema (precisa y aproximada). */
    data object PedirPermiso : EventoDeAsistencia

    data object AbrirAjustesDeUbicacion : EventoDeAsistencia

    data object AbrirAjustesDeLaApp : EventoDeAsistencia

    /** El marcador con la línea de atención (`ACTION_DIAL`, D4). */
    data class AbrirMarcador(val numero: String) : EventoDeAsistencia
}

/** El encabezado de M14 una vez solicitada (D10, S6). */
enum class EncabezadoDeAsistencia {
    /** Con ubicación y confirmada: «Ubicación enviada a Solventa · hh:mm:ss». */
    ENVIADA,

    /** Sin ubicación y confirmada. */
    REGISTRADA,

    /** En la cola con red: «Enviando tu ubicación…». */
    ENVIANDO,

    /** En la cola con red y sin ubicación que enviar: «Enviando tu solicitud…». */
    ENVIANDO_SOLICITUD,

    /** En la cola sin red: «Sin conexión de datos: la enviaremos en cuanto haya señal». */
    SIN_CONEXION,

    /** Descartada (S6). */
    NO_REGISTRADA,
}

/** Cada fila de «Estado de la solicitud» (D11). Siempre con ícono y texto (S10). */
enum class PasoDelEstado {
    UBICACION_RECIBIDA,
    SOLICITUD_RECIBIDA,
    ENVIANDO,
    ENVIANDO_SOLICITUD,
    SIN_CONEXION,
    NO_REGISTRADA,

    /** Estado local: se abrió el marcador. No es del servicio. */
    LLAMADA_EN_CURSO,
    POR_DESPACHAR,
    EN_ATENCION,
    DESPACHADA,
    CERRADA,
    DESCONOCIDO,
}

/** Estado de M14, M14b y M14c (HU-M2.3-01). */
data class AsistenciaUiState(
    val polizas: PolizasDeAsistencia = PolizasDeAsistencia.Cargando,
    val paso: PasoDeAsistencia = PasoDeAsistencia.Inicial,
    /** La solicitud que se muestra; `null` en la pantalla inicial o sin sesión. */
    val solicitud: SolicitudDeAsistencia? = null,
    val conectado: Boolean = true,
    val evento: EventoDeAsistencia? = null,
    /** El marcador se abrió: «Llamada en curso con el operador» (estado local). */
    val marcadorAbierto: Boolean = false,
    /** El dispositivo no tiene marcador: se muestra el número para copiarlo (S4). */
    val sinMarcador: Boolean = false,
    val consultandoEstado: Boolean = false,
    /** La última consulta del estado falló; lo mostrado no se borra. */
    val errorAlConsultar: Boolean = false,
) {
    val polizaElegida: PolizaElegible?
        get() = (polizas as? PolizasDeAsistencia.Lista)?.elegida

    /** «Solicitar asistencia y llamar» se habilita con una póliza elegida. */
    val puedeSolicitar: Boolean
        get() = paso == PasoDeAsistencia.Inicial && polizaElegida != null

    /** M14c: el aviso amarillo de que no se pudo obtener la ubicación. */
    val motivoSinUbicacion: MotivoSinUbicacion?
        get() = (solicitud?.resultado as? ResultadoDeUbicacion.NoDisponible)?.motivo

    val encabezado: EncabezadoDeAsistencia?
        get() {
            val actual = solicitud ?: return null
            return when (actual.envio) {
                EstadoDeEnvio.Confirmada ->
                    if (actual.resultado is ResultadoDeUbicacion.Compartida) EncabezadoDeAsistencia.ENVIADA else EncabezadoDeAsistencia.REGISTRADA
                EstadoDeEnvio.EnCola -> when {
                    !conectado -> EncabezadoDeAsistencia.SIN_CONEXION
                    actual.resultado is ResultadoDeUbicacion.Compartida -> EncabezadoDeAsistencia.ENVIANDO
                    else -> EncabezadoDeAsistencia.ENVIANDO_SOLICITUD
                }
                is EstadoDeEnvio.Descartada -> EncabezadoDeAsistencia.NO_REGISTRADA
            }
        }

    /** El motivo del descarte, para el aviso de S6 en la pantalla. */
    val motivoDeDescarte: MotivoDeDescarte?
        get() = (solicitud?.envio as? EstadoDeEnvio.Descartada)?.motivo

    /** «Ver estado de la solicitud» existe cuando el servicio ya dio su identificador. */
    val puedeVerEstado: Boolean
        get() = solicitud?.requestId != null

    /** Las filas de «Estado de la solicitud» (D11). */
    val pasosDelEstado: List<PasoDelEstado>
        get() {
            val actual = solicitud ?: return if (marcadorAbierto) listOf(PasoDelEstado.LLAMADA_EN_CURSO) else emptyList()
            return buildList {
                add(
                    when (actual.envio) {
                        EstadoDeEnvio.Confirmada ->
                            if (actual.resultado is ResultadoDeUbicacion.Compartida) PasoDelEstado.UBICACION_RECIBIDA else PasoDelEstado.SOLICITUD_RECIBIDA
                        EstadoDeEnvio.EnCola -> when {
                            !conectado -> PasoDelEstado.SIN_CONEXION
                            actual.resultado is ResultadoDeUbicacion.Compartida -> PasoDelEstado.ENVIANDO
                            else -> PasoDelEstado.ENVIANDO_SOLICITUD
                        }
                        is EstadoDeEnvio.Descartada -> PasoDelEstado.NO_REGISTRADA
                    },
                )
                if (marcadorAbierto) add(PasoDelEstado.LLAMADA_EN_CURSO)
                if (actual.envio == EstadoDeEnvio.Confirmada) {
                    add(
                        when (actual.estado ?: EstadoDeLaSolicitud.NUEVA) {
                            EstadoDeLaSolicitud.NUEVA -> PasoDelEstado.POR_DESPACHAR
                            EstadoDeLaSolicitud.EN_LLAMADA -> PasoDelEstado.EN_ATENCION
                            EstadoDeLaSolicitud.DESPACHADA -> PasoDelEstado.DESPACHADA
                            EstadoDeLaSolicitud.CERRADA -> PasoDelEstado.CERRADA
                            EstadoDeLaSolicitud.DESCONOCIDO -> PasoDelEstado.DESCONOCIDO
                        },
                    )
                }
            }
        }
}
