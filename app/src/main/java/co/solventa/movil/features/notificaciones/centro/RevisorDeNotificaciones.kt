package co.solventa.movil.features.notificaciones.centro

import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaRepositorio
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Una revisión del centro (HU-M3.1-01): registra el dispositivo con el permiso
 * y el idioma actuales, trae las notificaciones del servicio y muestra la
 * notificación del sistema de las que todavía no se avisaron. La hacen el
 * sondeo con la aplicación abierta y el trabajo en segundo plano.
 *
 * Una notificación se marca avisada aunque no haya permiso: ya está en el
 * centro (criterio 5), y conceder el permiso después no debe inundar al
 * asegurado con avisos viejos.
 */
@Singleton
class RevisorDeNotificaciones @Inject constructor(
    private val repositorio: NotificacionesRepositorio,
    private val avisador: AvisadorDeNotificaciones,
    private val permiso: PermisoDeNotificaciones,
    private val idiomas: IdiomaRepositorio,
) {

    suspend fun revisar(): ResultadoDeSincronizacion {
        val idioma = idiomas.actual.value
        repositorio.registrarDispositivo(permiso.concedido(), codigo(idioma))
        val resultado = repositorio.sincronizar()
        if (resultado is ResultadoDeSincronizacion.Sincronizado && resultado.nuevas.isNotEmpty()) {
            avisador.avisar(resultado.nuevas, idioma)
            repositorio.marcarAvisadas(resultado.nuevas.map { it.id })
        }
        return resultado
    }

    /** El idioma del *push* para el servicio: `es` o `en`. */
    private fun codigo(idioma: Idioma): String = if (idioma == Idioma.INGLES) "en" else "es"
}
