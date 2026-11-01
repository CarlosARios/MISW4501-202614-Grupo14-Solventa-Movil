package co.solventa.movil.features.notificaciones.centro

import co.solventa.movil.core.api.notificaciones.DispositivoDto
import co.solventa.movil.core.api.notificaciones.NotificacionesApi
import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.persistencia.dao.NotificacionDao
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NotificacionesRepositorio] sobre el servicio de notificaciones y la copia
 * local en Room (HU-M3.1-01).
 *
 * - Cada lectura y escritura va por el `sub` de la sesión; al sincronizar se
 *   borra lo que hubiera dejado otra cuenta.
 * - Leer una notificación se ve en seguida en la copia y se le avisa al
 *   servicio sin bloquear: si no hay red, la siguiente sincronización trae lo
 *   que el servicio tenga.
 * - Al cerrar la sesión se borra toda la copia ([AlCerrarSesion]).
 */
@Singleton
class NotificacionesRepositorioImpl internal constructor(
    private val api: NotificacionesApi,
    private val dao: NotificacionDao,
    private val sesion: SesionRepositorio,
    private val conectividad: ObservadorDeConectividad,
    private val instalacion: IdentidadDeInstalacion,
    private val reloj: () -> Long,
) : NotificacionesRepositorio, AlCerrarSesion {

    @Inject
    constructor(
        api: NotificacionesApi,
        dao: NotificacionDao,
        sesion: SesionRepositorio,
        conectividad: ObservadorDeConectividad,
        instalacion: IdentidadDeInstalacion,
    ) : this(api, dao, sesion, conectividad, instalacion, System::currentTimeMillis)

    override val conectado: StateFlow<Boolean> = conectividad.conectado

    /** Lo último que se registró con éxito, por cuenta: no se repite sin cambios. */
    @Volatile
    private var registrado: Triple<String, Boolean, String>? = null

    override fun observar(): Flow<CopiaDeNotificaciones> = flow {
        val sub = sub()
        if (sub.isEmpty()) {
            emit(CopiaDeNotificaciones(emptyList(), null, !conectado.value))
            return@flow
        }
        emitAll(
            combine(dao.observarDePropietario(sub), dao.observarSincronizacion(sub), conectividad.conectado) { filas, en, red ->
                CopiaDeNotificaciones(filas.map(MapeadorDeNotificaciones::desdeEntidad).filter(::conocida), en, !red)
            },
        )
    }

    override fun observarUna(id: String): Flow<Notificacion?> = flow {
        val sub = sub()
        if (sub.isEmpty()) {
            emit(null)
            return@flow
        }
        emitAll(dao.observarUna(sub, id).map { fila -> fila?.let(MapeadorDeNotificaciones::desdeEntidad)?.takeIf(::conocida) })
    }

    override suspend fun sincronizar(): ResultadoDeSincronizacion {
        val sub = sub()
        if (sub.isEmpty()) return ResultadoDeSincronizacion.SinSesion
        return try {
            val respuesta = api.notificaciones()
            val cuerpo = respuesta.body()
            if (!respuesta.isSuccessful || cuerpo == null) return ResultadoDeSincronizacion.Error
            val filas = cuerpo.elementos.map { MapeadorDeNotificaciones.aEntidad(MapeadorDeNotificaciones.desdeDto(it), sub) }
            dao.guardarSincronizadas(sub, filas, reloj())
            dao.borrarDeOtrosPropietarios(sub)
            ResultadoDeSincronizacion.Sincronizado(
                dao.sinAvisar(sub).map(MapeadorDeNotificaciones::desdeEntidad).filter(::conocida),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            ResultadoDeSincronizacion.SinConexion
        } catch (_: RuntimeException) {
            // Un cuerpo o una fecha que no se pueden leer no borran la copia.
            ResultadoDeSincronizacion.Error
        }
    }

    override suspend fun marcarAvisadas(ids: List<String>) {
        val sub = sub()
        if (sub.isNotEmpty() && ids.isNotEmpty()) dao.marcarAvisadas(sub, ids, reloj())
    }

    override suspend fun marcarLeida(id: String) {
        val sub = sub()
        if (sub.isEmpty()) return
        dao.marcarLeida(sub, id, reloj())
        alServicio { api.marcarLeida(id) }
    }

    override suspend fun marcarTodasLeidas() {
        val sub = sub()
        if (sub.isEmpty()) return
        dao.marcarTodasLeidas(sub, reloj())
        alServicio { api.marcarTodasLeidas() }
    }

    override suspend fun registrarDispositivo(permisoConcedido: Boolean, idioma: String) {
        val sub = sub()
        if (sub.isEmpty()) return
        val estado = Triple(sub, permisoConcedido, idioma)
        if (registrado == estado) return
        val id = instalacion.id()
        // Sin proveedor de push real (decisión de HU-M3.1-01) el token es el
        // id de la instalación; con FCM será el token que entregue Firebase.
        val dto = DispositivoDto(token = id.takeIf { permisoConcedido }, pushHabilitado = permisoConcedido, idioma = idioma)
        val ok = alServicio { api.registrarDispositivo(id, dto) }
        if (ok) registrado = estado
    }

    override suspend fun limpiar() {
        registrado = null
        dao.borrarTodo()
    }

    /** Una llamada que no debe interrumpir a nadie: si falla, no pasa nada. */
    private suspend fun alServicio(llamada: suspend () -> retrofit2.Response<Unit>): Boolean = try {
        llamada().isSuccessful
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        false
    } catch (_: RuntimeException) {
        false
    }

    /** Solo los tipos que la aplicación sabe mostrar. */
    private fun conocida(n: Notificacion): Boolean = n.tipo == TiposDeNotificacion.PAGO_PARAMETRICO && n.pago != null

    private suspend fun sub(): String = sesion.sesionGuardada()?.sub.orEmpty()
}
