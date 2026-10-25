package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.core.api.auditoria.AuditoriaApi
import co.solventa.movil.core.api.auditoria.ErrorDeAuditoriaDto
import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import co.solventa.movil.core.sincronizacion.ProgramadorDeSincronizacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ConsentimientosRepositorio] sobre el servicio de auditoria y la copia local
 * en Room (HU-M1.3-01).
 *
 * - Cada lectura y escritura va por el `sub` de la sesion: un telefono que
 *   usaron dos cuentas nunca mezcla sus consentimientos, y al actualizar se
 *   borra lo que hubiera dejado otra cuenta (criterio 6).
 * - Sin red se conserva la copia y se programa la sincronizacion en segundo
 *   plano, que corre cuando la red vuelve aunque la aplicacion este cerrada
 *   (criterio 5).
 * - Al cerrar la sesion se borra toda la copia ([AlCerrarSesion], D-6 a).
 * - Antes de pedir la lista se envia la cola de revocaciones pendientes
 *   (HU-M1.3-02): una revocacion en cola sale primero, con prioridad alta.
 */
@Singleton
class ConsentimientosRepositorioImpl internal constructor(
    private val api: AuditoriaApi,
    private val dao: ConsentimientoDao,
    private val sesion: SesionRepositorio,
    private val conectividad: ObservadorDeConectividad,
    private val programador: ProgramadorDeSincronizacion,
    private val enviador: EnviadorDeRevocaciones = EnviadorDeRevocaciones { ResultadoDeEnvio.Enviadas(0) },
    private val reloj: () -> Long,
) : ConsentimientosRepositorio, AlCerrarSesion {

    @Inject
    constructor(
        api: AuditoriaApi,
        dao: ConsentimientoDao,
        sesion: SesionRepositorio,
        conectividad: ObservadorDeConectividad,
        programador: ProgramadorDeSincronizacion,
        enviador: EnviadorDeRevocaciones,
    ) : this(api, dao, sesion, conectividad, programador, enviador, System::currentTimeMillis)

    private val json = Json { ignoreUnknownKeys = true }

    override val conectado: StateFlow<Boolean> = conectividad.conectado

    override fun observar(): Flow<CopiaDeConsentimientos> = flow {
        val sub = sub()
        if (sub.isEmpty()) {
            emit(CopiaDeConsentimientos(emptyList(), sincronizadoEn = null, sinConexion = !conectado.value))
            return@flow
        }
        emitAll(
            combine(
                dao.observarDePropietario(sub),
                dao.observarSincronizacion(sub, ConsentimientoDao.RECURSO_CONSENTIMIENTOS),
                conectividad.conectado,
            ) { filas, sincronizadoEn, conectado ->
                CopiaDeConsentimientos(filas.map(MapeadorDeConsentimientos::desdeEntidad), sincronizadoEn, !conectado)
            },
        )
    }

    override fun observarUno(id: String): Flow<Consentimiento?> = flow {
        val sub = sub()
        if (sub.isEmpty()) {
            emit(null)
            return@flow
        }
        emitAll(dao.observarUno(sub, id).map { fila -> fila?.let(MapeadorDeConsentimientos::desdeEntidad) })
    }

    override suspend fun actualizar(): ResultadoDeActualizacion {
        val sub = sub()
        if (sub.isEmpty()) return ResultadoDeActualizacion.SinSesion
        return try {
            // La cola de revocaciones va primero: si sale ahora, la lista que
            // llega despues ya trae esos consentimientos revocados.
            enviador.enviarPendientes()
            val respuesta = api.consentimientos()
            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> {
                    val filas = cuerpo.elementos.map { MapeadorDeConsentimientos.aEntidad(MapeadorDeConsentimientos.desdeDto(it), sub) }
                    dao.reemplazarDePropietario(sub, filas, reloj())
                    dao.borrarDeOtrosPropietarios(sub)
                    ResultadoDeActualizacion.Actualizado
                }
                respuesta.code() == CODIGO_PROHIBIDO && codigoDeError(respuesta.errorBody()?.string()) ==
                    AuditoriaApi.ERROR_CUENTA_SIN_VINCULAR -> ResultadoDeActualizacion.CuentaSinVincular
                else -> ResultadoDeActualizacion.Error
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            programador.programar(TRABAJO, SincronizacionDeConsentimientosWorker::class.java)
            ResultadoDeActualizacion.SinConexion
        } catch (_: RuntimeException) {
            // Un cuerpo que no se puede leer no borra la copia que habia.
            ResultadoDeActualizacion.Error
        }
    }

    override suspend fun limpiar() {
        dao.borrarTodo()
    }

    private suspend fun sub(): String = sesion.sesionGuardada()?.sub.orEmpty()

    private fun codigoDeError(cuerpo: String?): String? =
        cuerpo?.let { runCatching { json.decodeFromString<ErrorDeAuditoriaDto>(it).error }.getOrNull() }

    companion object {
        /** Nombre unico del trabajo de sincronizacion en segundo plano. */
        const val TRABAJO = "sincronizar-consentimientos"

        private const val CODIGO_PROHIBIDO = 403
    }
}
