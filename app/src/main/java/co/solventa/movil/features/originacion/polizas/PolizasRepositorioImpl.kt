package co.solventa.movil.features.originacion.polizas

import co.solventa.movil.core.api.originacion.OriginacionApi
import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.persistencia.cifrado.CifradorDeDatos
import co.solventa.movil.core.persistencia.dao.PolizaDao
import co.solventa.movil.core.persistencia.entidades.PolizaEntidad
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import co.solventa.movil.core.sincronizacion.ProgramadorDeSincronizacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PolizasRepositorio] sobre el servicio de originación y la copia cifrada en
 * Room (HU-M1.2-01).
 *
 * - Cada póliza se guarda cifrada con [CifradorDeDatos] (criterio 6). Una fila
 *   que no se puede descifrar —se perdió la clave del Keystore— no se
 *   muestra; la siguiente sincronización la repone.
 * - Cada lectura y escritura va por el `sub` de la sesión: un teléfono que
 *   usaron dos cuentas nunca mezcla sus pólizas, y al sincronizar se borra lo
 *   que hubiera dejado otra cuenta.
 * - Antes de reemplazar la copia se compara con la lista nueva y los cambios
 *   quedan en [cambios] hasta que el asegurado los descarta (criterio 4).
 * - Sin red se conserva la copia y se programa la sincronización en segundo
 *   plano, que corre cuando la red vuelve (criterio 3).
 * - Una lista parcial (`partial`) **no** reemplaza la copia: las pólizas que
 *   faltan no están canceladas, solo no llegaron.
 * - Al cerrar la sesión se borra toda la copia ([AlCerrarSesion]).
 */
@Singleton
class PolizasRepositorioImpl internal constructor(
    private val api: OriginacionApi,
    private val dao: PolizaDao,
    private val cifrador: CifradorDeDatos,
    private val sesion: SesionRepositorio,
    private val conectividad: ObservadorDeConectividad,
    private val programador: ProgramadorDeSincronizacion,
    private val reloj: () -> Long,
) : PolizasRepositorio, AlCerrarSesion {

    @Inject
    constructor(
        api: OriginacionApi,
        dao: PolizaDao,
        cifrador: CifradorDeDatos,
        sesion: SesionRepositorio,
        conectividad: ObservadorDeConectividad,
        programador: ProgramadorDeSincronizacion,
    ) : this(api, dao, cifrador, sesion, conectividad, programador, System::currentTimeMillis)

    override val conectado: StateFlow<Boolean> = conectividad.conectado

    private val _cambios = MutableStateFlow<List<CambioDePoliza>>(emptyList())
    override val cambios: StateFlow<List<CambioDePoliza>> = _cambios.asStateFlow()

    override fun observar(): Flow<CopiaDePolizas> = flow {
        val sub = sub()
        if (sub.isEmpty()) {
            emit(CopiaDePolizas(emptyList(), sincronizadoEn = null, sinConexion = !conectado.value))
            return@flow
        }
        emitAll(
            combine(dao.observarDePropietario(sub), dao.observarSincronizacion(sub), conectividad.conectado) {
                    filas, sincronizadoEn, conectado ->
                CopiaDePolizas(filas.mapNotNull(::leer), sincronizadoEn, !conectado)
            },
        )
    }

    override fun observarUna(id: String): Flow<Poliza?> = flow {
        val sub = sub()
        if (sub.isEmpty()) {
            emit(null)
            return@flow
        }
        emitAll(dao.observarUna(sub, id).map { fila -> fila?.let(::leer) })
    }

    override suspend fun sincronizar(): ResultadoDeSincronizacion {
        val sub = sub()
        if (sub.isEmpty()) return ResultadoDeSincronizacion.SinSesion
        return try {
            val respuesta = api.polizas()
            val cuerpo = respuesta.body()
            if (!respuesta.isSuccessful || cuerpo == null || cuerpo.parcial) return ResultadoDeSincronizacion.Error
            val nuevas = cuerpo.elementos.map(MapeadorDePolizas::desdeDto)
            val anteriores = dao.dePropietario(sub).mapNotNull(::leer)
            val filas = nuevas.mapIndexed { orden, poliza ->
                PolizaEntidad(sub, poliza.id, orden, cifrador.cifrar(MapeadorDePolizas.aJson(poliza)))
            }
            dao.reemplazarDePropietario(sub, filas, reloj())
            dao.borrarDeOtrosPropietarios(sub)
            val cambios = DetectorDeCambios.comparar(anteriores, nuevas)
            if (cambios.isNotEmpty()) _cambios.value = cambios
            ResultadoDeSincronizacion.Sincronizado(cambios)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            programador.programar(TRABAJO, SincronizacionDePolizasWorker::class.java)
            ResultadoDeSincronizacion.SinConexion
        } catch (_: RuntimeException) {
            // Un cuerpo o una fecha que no se pueden leer no borran la copia.
            ResultadoDeSincronizacion.Error
        }
    }

    override fun descartarCambios() {
        _cambios.value = emptyList()
    }

    override suspend fun limpiar() {
        _cambios.value = emptyList()
        dao.borrarTodo()
    }

    private fun leer(fila: PolizaEntidad): Poliza? =
        cifrador.descifrar(fila.contenidoCifrado)?.let(MapeadorDePolizas::desdeJson)

    private suspend fun sub(): String = sesion.sesionGuardada()?.sub.orEmpty()

    companion object {
        /** Nombre único del trabajo de sincronización en segundo plano. */
        const val TRABAJO = "sincronizar-polizas"
    }
}
