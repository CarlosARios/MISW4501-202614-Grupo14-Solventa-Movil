package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.core.api.siniestros.SolicitudDeAsistenciaDto
import co.solventa.movil.core.api.siniestros.UbicacionDeAsistenciaDto
import co.solventa.movil.core.ubicacion.PermisoDeUbicacion
import co.solventa.movil.core.ubicacion.Ubicacion
import java.time.Instant

/** Qué variante de M14b se le muestra al asegurado (criterio 1). */
enum class TipoDeDialogo {
    /** Falta el permiso y el sistema todavía puede mostrar su diálogo. */
    FALTA_PERMISO,

    /** Falta el permiso y el sistema ya no muestra su diálogo: solo queda ir a los ajustes de la aplicación. */
    PERMISO_NEGADO_PARA_SIEMPRE,

    /** Hay permiso, pero el servicio de ubicación está apagado. */
    SERVICIO_APAGADO,
}

/** Lo siguiente después de tocar «Solicitar asistencia y llamar». */
sealed interface SiguientePaso {
    data class Explicar(val dialogo: TipoDeDialogo) : SiguientePaso

    data object ObtenerUbicacion : SiguientePaso
}

/**
 * Las reglas de la solicitud de asistencia (HU-M2.3-01), sin Android: qué se
 * hace con el permiso y el servicio, qué resultado de ubicación se registra,
 * cuándo vence la solicitud en la cola y cómo se arma el cuerpo del contrato.
 */
object ReglasDeAsistencia {

    /** Una solicitud que no sale en este tiempo se descarta (S7). */
    const val VENCIMIENTO_MS = 60 * 60 * 1000L

    /** Una solicitud más reciente que esto se retoma al volver a M14 (D9). */
    const val RETOMABLE_MS = 30 * 60 * 1000L

    /** Las solicitudes ya resueltas se borran del teléfono después de esto (D9). */
    const val CONSERVACION_MS = 24 * 60 * 60 * 1000L

    /**
     * Un *fix* más viejo que esto no se envía: se registra `no_fix` (contrato
     * §5, «hay un *fix* de 60 s o menos»).
     */
    const val ANTIGUEDAD_MAXIMA_MS = 60_000L

    /** Rango que el contrato admite para `accuracy_m`: mayor que 0 y hasta 10 000 m. */
    const val PRECISION_MAXIMA_M = 10_000.0

    /** Largo que el contrato admite para `place`. */
    const val LUGAR_MINIMO = 3
    const val LUGAR_MAXIMO = 160

    /**
     * Criterio 1 y S2: sin permiso se explica y se pide (o se manda a los
     * ajustes si el sistema ya no muestra su diálogo); con el servicio apagado
     * se manda a los ajustes de ubicación; con los dos se lee la ubicación.
     */
    fun siguientePaso(permiso: PermisoDeUbicacion, servicioEncendido: Boolean, permisoBloqueado: Boolean): SiguientePaso =
        when {
            permiso == PermisoDeUbicacion.NINGUNO && permisoBloqueado ->
                SiguientePaso.Explicar(TipoDeDialogo.PERMISO_NEGADO_PARA_SIEMPRE)
            permiso == PermisoDeUbicacion.NINGUNO -> SiguientePaso.Explicar(TipoDeDialogo.FALTA_PERMISO)
            !servicioEncendido -> SiguientePaso.Explicar(TipoDeDialogo.SERVICIO_APAGADO)
            else -> SiguientePaso.ObtenerUbicacion
        }

    /**
     * S1: el desenlace de la lectura. Sin permiso o sin servicio no hay
     * lectura que valga; con los dos, una lectura ausente, vieja o con una
     * precisión que el contrato no admite es `no_fix`. Con solo la aproximada
     * la ubicación se envía igual, con su precisión (D5).
     */
    fun resultadoDe(
        ubicacion: Ubicacion?,
        permiso: PermisoDeUbicacion,
        servicioEncendido: Boolean,
        ahora: Long,
    ): ResultadoDeUbicacion = when {
        permiso == PermisoDeUbicacion.NINGUNO -> ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO)
        !servicioEncendido -> ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.UBICACION_APAGADA)
        ubicacion == null || !utilizable(ubicacion, ahora) ->
            ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA)
        else -> ResultadoDeUbicacion.Compartida(ubicacion)
    }

    /** «Llamar sin ubicación» (M14b): el motivo que corresponde a lo que falta. */
    fun motivoSinUbicacion(permiso: PermisoDeUbicacion, servicioEncendido: Boolean): MotivoSinUbicacion = when {
        permiso == PermisoDeUbicacion.NINGUNO -> MotivoSinUbicacion.PERMISO_NEGADO
        !servicioEncendido -> MotivoSinUbicacion.UBICACION_APAGADA
        else -> MotivoSinUbicacion.SIN_LECTURA
    }

    /** S7: a los 60 minutos del toque la solicitud ya no se envía. */
    fun vencida(creadaEn: Long, ahora: Long): Boolean = ahora - creadaEn >= VENCIMIENTO_MS

    /** D9: una solicitud de hace menos de 30 minutos se retoma al abrir M14. */
    fun retomable(creadaEn: Long, ahora: Long): Boolean = ahora - creadaEn < RETOMABLE_MS

    /**
     * La dirección que da el geocodificador, como el contrato la recibe: sin
     * espacios en los extremos, recortada a 160 caracteres y omitida si queda
     * con menos de 3.
     */
    fun lugarValido(texto: String?): String? {
        val limpio = texto?.replace(ESPACIOS, " ")?.trim() ?: return null
        val recortado = limpio.take(LUGAR_MAXIMO).trimEnd()
        return recortado.takeIf { it.length >= LUGAR_MINIMO }
    }

    /**
     * El cuerpo de `POST /v1/claims/assistance-requests`. Con `unavailable` no
     * viaja ninguna coordenada (el servicio las rechaza como `not_applicable`),
     * y `0, 0` es una ubicación como cualquier otra.
     */
    fun aCuerpo(poliza: String, resultado: ResultadoDeUbicacion, lugar: String?): SolicitudDeAsistenciaDto =
        SolicitudDeAsistenciaDto(
            poliza = poliza,
            ubicacion = when (resultado) {
                is ResultadoDeUbicacion.Compartida -> UbicacionDeAsistenciaDto(
                    estado = ESTADO_ENVIADA,
                    latitud = resultado.ubicacion.latitud,
                    longitud = resultado.ubicacion.longitud,
                    precisionM = precisionDe(resultado.ubicacion),
                    capturadaEn = Instant.ofEpochMilli(resultado.ubicacion.leidaEn).toString(),
                )
                is ResultadoDeUbicacion.NoDisponible -> UbicacionDeAsistenciaDto(
                    estado = ESTADO_NO_DISPONIBLE,
                    motivo = resultado.motivo.codigo,
                )
            },
            lugar = if (resultado is ResultadoDeUbicacion.Compartida) lugarValido(lugar) else null,
        )

    /**
     * La precisión en metros como la escribe el sistema: un `Float` pasado a
     * `Double` sin el ruido binario (`4.7f` viaja como `4.7`, no como
     * `4.699999809265137`).
     */
    fun precisionDe(ubicacion: Ubicacion): Double = ubicacion.precisionM.toString().toDouble()

    private fun utilizable(ubicacion: Ubicacion, ahora: Long): Boolean {
        val precision = precisionDe(ubicacion)
        return ahora - ubicacion.leidaEn <= ANTIGUEDAD_MAXIMA_MS &&
            precision > 0 && precision <= PRECISION_MAXIMA_M &&
            ubicacion.latitud in -90.0..90.0 && ubicacion.longitud in -180.0..180.0
    }

    const val ESTADO_ENVIADA = "sent"
    const val ESTADO_NO_DISPONIBLE = "unavailable"

    private val ESPACIOS = Regex("\\s+")
}
