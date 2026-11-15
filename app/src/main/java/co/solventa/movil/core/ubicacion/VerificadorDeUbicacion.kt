package co.solventa.movil.core.ubicacion

/** Qué permiso de ubicación tiene la aplicación. La aproximada también sirve. */
enum class PermisoDeUbicacion { PRECISO, APROXIMADO, NINGUNO }

/**
 * Si el teléfono está en condiciones de dar una ubicación (HU-M2.3-01,
 * criterio 1): el permiso y el servicio de ubicación del sistema.
 *
 * [Localizador] devuelve `null` sin decir por qué; quien tiene que explicarle
 * al asegurado qué falta —y llevarlo al flujo del sistema que lo arregla—
 * pregunta aquí antes.
 */
interface VerificadorDeUbicacion {

    fun permiso(): PermisoDeUbicacion

    /** Si el servicio de ubicación del sistema está encendido. */
    fun servicioEncendido(): Boolean
}
