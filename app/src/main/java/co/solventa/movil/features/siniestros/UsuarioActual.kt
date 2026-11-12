package co.solventa.movil.features.siniestros

import co.solventa.movil.core.auth.sesion.SesionRepositorio
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quién tiene la sesión abierta, por su `sub`. Los borradores y las
 * evidencias del teléfono son de quien los creó (HU-M2.2-02, criterio 6).
 * Vacío cuando no hay sesión: entonces no se ve ni se envía nada.
 */
fun interface UsuarioActual {
    suspend fun sub(): String
}

@Singleton
class UsuarioDeLaSesion @Inject constructor(
    private val sesion: SesionRepositorio,
) : UsuarioActual {
    override suspend fun sub(): String = sesion.sesionGuardada()?.sub.orEmpty()
}
