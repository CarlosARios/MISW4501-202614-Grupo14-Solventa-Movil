package co.solventa.movil.core.auth.cognito

import co.solventa.movil.BuildConfig
import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import co.solventa.movil.core.auth.sesion.AlmacenDeSesion
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.auth.sesion.TokensDeSesion
import co.solventa.movil.core.auth.sesion.VerificacionDeContrasena
import co.solventa.movil.core.auth.sesion.VerificadorDeContrasena
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * La sesión del asegurado contra el *user pool* de Cognito (HA-S01).
 *
 * Reemplaza a `SesionRepositorioSimulado`, que existía mientras el *pool* no
 * estaba creado. El puerto [SesionRepositorio] no cambió.
 *
 * **El desbloqueo con biometría sigue siendo local**, como pide el criterio 2
 * de HU-M1.1-02: no hay petición al proveedor de identidad, los tokens ya
 * estaban emitidos en el dispositivo y la huella solo los libera.
 *
 * **La renovación silenciosa es lo que hace usable la sesión.** Los tokens del
 * *pool* duran cinco minutos y una sesión de trabajo dura mucho más, así que
 * sin renovar, la aplicación quedaría inservible a los cinco minutos de
 * entrar. [tokenVigente] la resuelve antes de cada petición, y el *refresh
 * token* del cliente móvil dura 30 días.
 */
@Singleton
class CognitoSesionRepositorio @Inject constructor(
    private val api: CognitoApi,
    private val almacen: AlmacenDeSesion,
    /**
     * Lo que cada dominio borra del dispositivo al cerrar la sesion
     * (HU-M1.3-01). Va detras de un `Provider` porque esas limpiezas dependen
     * a su vez de la sesion: asi se rompe el ciclo sin que `core` las conozca.
     */
    private val limpiezas: Provider<Set<@JvmSuppressWildcards AlCerrarSesion>>,
) : SesionRepositorio, VerificadorDeContrasena {

    /** Una renovación a la vez: varias peticiones en paralelo comparten la misma. */
    private val cerrojo = Mutex()

    override suspend fun sesionGuardada(): Sesion? = almacen.tokens()?.let {
        // El `sub` se deriva del token guardado: el almacen cifrado no cambia.
        Sesion(it.correo, it.nombre, TokenDeIdentidad.afirmaciones(it.idToken)?.sub.orEmpty())
    }

    override suspend fun biometriaHabilitada(): Boolean = almacen.biometriaHabilitada()

    override suspend fun desbloquear(): Sesion? = sesionGuardada()

    override suspend fun ingresarConContrasena(correo: String, contrasena: String): ResultadoIngreso = try {
        val respuesta = api.iniciarAutenticacion(
            PeticionDeAutenticacion.conContrasena(BuildConfig.COGNITO_CLIENTE_ID, correo.trim(), contrasena),
        )
        val resultado = respuesta.resultado
        when {
            resultado == null -> ResultadoIngreso.CredencialesInvalidas
            else -> {
                val sesion = guardar(resultado, anterior = null)
                ResultadoIngreso.Exitoso(sesion)
            }
        }
    } catch (e: HttpException) {
        // Cognito responde 400 tanto para una contraseña equivocada como para
        // un usuario que no existe, y es deliberado: distinguirlos le diría a
        // un atacante qué correos están registrados.
        if (e.code() in CODIGOS_DE_RECHAZO) ResultadoIngreso.CredencialesInvalidas else ResultadoIngreso.SinConexion
    } catch (_: IOException) {
        ResultadoIngreso.SinConexion
    }

    /**
     * Pide a Cognito que autentique el correo de la sesion con la contraseña
     * dada, como en el ingreso, pero **descarta los tokens que devuelve**: la
     * sesion guardada no cambia (HU-M1.3-02, D-6 a). Sin sesion guardada la
     * contraseña no puede ser correcta.
     */
    override suspend fun verificarContrasena(contrasena: String): VerificacionDeContrasena {
        val correo = almacen.tokens()?.correo?.takeIf { it.isNotBlank() } ?: return VerificacionDeContrasena.Incorrecta
        return try {
            val respuesta = api.iniciarAutenticacion(
                PeticionDeAutenticacion.conContrasena(BuildConfig.COGNITO_CLIENTE_ID, correo, contrasena),
            )
            if (respuesta.resultado != null) VerificacionDeContrasena.Correcta else VerificacionDeContrasena.Incorrecta
        } catch (e: HttpException) {
            if (e.code() in CODIGOS_DE_RECHAZO) VerificacionDeContrasena.Incorrecta else VerificacionDeContrasena.SinConexion
        } catch (_: IOException) {
            VerificacionDeContrasena.SinConexion
        }
    }

    override suspend fun habilitarBiometria(habilitada: Boolean) {
        almacen.habilitarBiometria(habilitada)
    }

    /**
     * Primero cada dominio borra lo suyo (la copia de los consentimientos,
     * HU-M1.3-01) y despues los tokens. Una limpieza que falle no deja la
     * sesion abierta.
     */
    override suspend fun cerrar() {
        limpiezas.get().forEach { limpieza -> runCatching { limpieza.limpiar() } }
        almacen.borrarTokens()
    }

    /**
     * El *id token* que va en `Authorization`, renovado si está por vencer.
     *
     * Devuelve `null` cuando no hay sesión o cuando la renovación falla, y en
     * ese caso borra lo guardado: un *refresh token* que Cognito rechazó
     * —revocado, vencido a los 30 días o de un *pool* que ya no existe— no va
     * a servir en el intento siguiente.
     */
    suspend fun tokenVigente(ahora: Long = System.currentTimeMillis()): String? = cerrojo.withLock {
        val guardados = almacen.tokens() ?: return null
        if (ahora < guardados.venceEn - MARGEN_MS) {
            return guardados.idToken
        }
        try {
            val respuesta = api.iniciarAutenticacion(
                PeticionDeAutenticacion.conRenovacion(BuildConfig.COGNITO_CLIENTE_ID, guardados.tokenDeRenovacion),
            )
            val resultado = respuesta.resultado ?: run {
                almacen.borrarTokens()
                return null
            }
            guardar(resultado, anterior = guardados)
            almacen.tokens()?.idToken
        } catch (e: HttpException) {
            if (e.code() in CODIGOS_DE_RECHAZO) almacen.borrarTokens()
            null
        } catch (_: IOException) {
            // Sin red no se descarta la sesión: el token sigue siendo válido
            // cuando la conexión vuelva, si todavía no ha vencido.
            null
        }
    }

    private fun guardar(resultado: ResultadoDeAutenticacion, anterior: TokensDeSesion?): Sesion {
        val afirmaciones = TokenDeIdentidad.afirmaciones(resultado.idToken)
        val tokens = TokensDeSesion(
            idToken = resultado.idToken,
            accessToken = resultado.accessToken,
            // Al renovar, Cognito no reenvía el de renovación: se conserva el
            // que el dispositivo ya tenía.
            tokenDeRenovacion = resultado.tokenDeRenovacion ?: anterior?.tokenDeRenovacion.orEmpty(),
            // La hora de vencimiento sale del token y no del reloj local, que
            // puede estar desfasado; `ExpiresIn` queda de respaldo.
            venceEn = afirmaciones?.venceEn?.takeIf { it > 0 }
                ?: (System.currentTimeMillis() + resultado.vigenciaEnSegundos * 1_000L),
            correo = afirmaciones?.correo.orEmpty(),
            nombre = afirmaciones?.nombre.orEmpty(),
        )
        almacen.guardarTokens(tokens)
        return Sesion(correo = tokens.correo, nombre = tokens.nombre, sub = afirmaciones?.sub.orEmpty())
    }

    private companion object {
        /**
         * Se renueva un minuto antes de vencer. Con tokens de cinco minutos,
         * esperar al vencimiento dejaría que una petición saliera con un token
         * que caduca mientras viaja.
         */
        const val MARGEN_MS = 60_000L

        val CODIGOS_DE_RECHAZO = setOf(400, 401, 403)
    }
}
