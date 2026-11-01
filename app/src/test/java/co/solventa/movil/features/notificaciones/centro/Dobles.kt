package co.solventa.movil.features.notificaciones.centro

import co.solventa.movil.core.api.notificaciones.DisparadorDto
import co.solventa.movil.core.api.notificaciones.DispositivoDto
import co.solventa.movil.core.api.notificaciones.ListaDeNotificacionesDto
import co.solventa.movil.core.api.notificaciones.MedioDePagoDto
import co.solventa.movil.core.api.notificaciones.NotificacionDto
import co.solventa.movil.core.api.notificaciones.NotificacionesApi
import co.solventa.movil.core.api.notificaciones.PagoParametricoDto
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaRepositorio
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

/**
 * Dobles de prueba del centro de notificaciones (HU-M3.1-01). El pago es el
 * del prototipo (M15 y M15b): $ 180.000 COP por el vuelo AV-8421, 215 min de
 * retraso, a la Visa •••• 1111, póliza VJ-2026-000931.
 */

const val PAGADO = 1_789_162_800_000L // 2026-09-11T21:40:00Z
const val DIA = 86_400_000L

fun notificacionDto(
    id: String = "ntf-ppo-1",
    ocurrio: String = "2026-09-11T21:40:00Z",
    leida: String? = null,
    tipo: String = "parametric_payout",
    evento: String = "AV-8421",
) = NotificacionDto(
    id = id,
    tipo = tipo,
    ocurrioEn = ocurrio,
    creadaEn = ocurrio,
    leidaEn = leida,
    pago = PagoParametricoDto(
        id = id.removePrefix("ntf-"), siniestro = "SIN-2026-01702", poliza = "VJ-2026-000931", monto = 180_000.0,
        moneda = "COP",
        disparador = DisparadorDto("flight_delay", evento, 180.0, 215.0, "minutes", "2026-09-11T21:39:00Z"),
        medioDePago = MedioDePagoDto("visa", "1111"),
        claveDeIdempotencia = "pay-" + id.removePrefix("ntf-"),
        pagadoEn = ocurrio,
    ),
)

fun notificacion(id: String = "ntf-ppo-1", ocurrioEn: Long = PAGADO, leidaEn: Long? = null, evento: String = "AV-8421") =
    Notificacion(
        id = id,
        tipo = TiposDeNotificacion.PAGO_PARAMETRICO,
        ocurrioEn = ocurrioEn,
        leidaEn = leidaEn,
        pago = PagoParametrico(
            id = id.removePrefix("ntf-"), siniestro = "SIN-2026-01702", poliza = "VJ-2026-000931", monto = 180_000.0,
            moneda = "COP", disparador = Disparador("flight_delay", evento, 180.0, 215.0, "minutes", ocurrioEn - 60_000),
            medioDePago = MedioDePago("visa", "1111"), claveDeIdempotencia = "pay-" + id.removePrefix("ntf-"),
            pagadoEn = ocurrioEn,
        ),
    )

/** El servicio: responde la lista que se le fije, o falla sin red. */
class NotificacionesFalsas : NotificacionesApi {
    var lista: List<NotificacionDto> = emptyList()
    var codigo = 200
    var sinRed = false
    var llamadas = 0
    val leidas = mutableListOf<String>()
    var todasLeidas = 0
    val dispositivos = mutableListOf<Pair<String, DispositivoDto>>()
    var codigoDispositivo = 200

    private fun <T> error(): Response<T> = Response.error(codigo, "{}".toResponseBody("application/json".toMediaType()))

    override suspend fun notificaciones(limite: Int): Response<ListaDeNotificacionesDto> {
        llamadas++
        if (sinRed) throw IOException("sin red")
        return if (codigo in 200..299) Response.success(ListaDeNotificacionesDto(lista, lista.count { it.leidaEn == null })) else error()
    }

    override suspend fun notificacion(id: String): Response<NotificacionDto> = Response.success(lista.first { it.id == id })

    override suspend fun marcarLeida(id: String): Response<Unit> {
        if (sinRed) throw IOException("sin red")
        leidas += id
        return Response.success(204, Unit)
    }

    override suspend fun marcarTodasLeidas(): Response<Unit> {
        if (sinRed) throw IOException("sin red")
        todasLeidas++
        return Response.success(204, Unit)
    }

    override suspend fun registrarDispositivo(instalacion: String, dispositivo: DispositivoDto): Response<Unit> {
        if (sinRed) throw IOException("sin red")
        dispositivos += instalacion to dispositivo
        return if (codigoDispositivo in 200..299) {
            Response.success(Unit)
        } else {
            Response.error(codigoDispositivo, "{}".toResponseBody("application/json".toMediaType()))
        }
    }
}

class SesionFalsa(var sub: String = "sub-a") : SesionRepositorio {
    override suspend fun sesionGuardada(): Sesion? = Sesion("asegurado@example.com", "Carolina", sub)
    override suspend fun biometriaHabilitada(): Boolean = false
    override suspend fun desbloquear(): Sesion? = sesionGuardada()
    override suspend fun ingresarConContrasena(correo: String, contrasena: String): ResultadoIngreso =
        ResultadoIngreso.CredencialesInvalidas
    override suspend fun habilitarBiometria(habilitada: Boolean) = Unit
    override suspend fun cerrar() = Unit
}

class ConectividadFalsa(inicial: Boolean = true) : ObservadorDeConectividad {
    val estado = MutableStateFlow(inicial)
    override val conectado: StateFlow<Boolean> = estado
}

class InstalacionFalsa(private val id: String = "inst-1") : IdentidadDeInstalacion {
    override fun id(): String = id
}

class PermisoFalso(var concedido: Boolean = true) : PermisoDeNotificaciones {
    override fun concedido(): Boolean = concedido
}

class AvisadorFalso : AvisadorDeNotificaciones {
    val avisadas = mutableListOf<Pair<List<String>, Idioma>>()
    override fun avisar(notificaciones: List<Notificacion>, idioma: Idioma): Boolean {
        avisadas += notificaciones.map { it.id } to idioma
        return true
    }
}

class IdiomasFalsos(inicial: Idioma = Idioma.ESPANOL) : IdiomaRepositorio {
    override val actual = MutableStateFlow(inicial)
    override val elegidoPorElUsuario: Boolean = true
    override fun elegir(idioma: Idioma) {
        actual.value = idioma
    }
}

class ProgramadorFalso : ProgramadorDeRevision {
    var programado = 0
    override fun programar() {
        programado++
    }
}

/** El repositorio para los ViewModel y el revisor: copia y resultados fijados por la prueba. */
class RepositorioFalso(copiaInicial: CopiaDeNotificaciones = CopiaDeNotificaciones(emptyList(), null, false)) :
    NotificacionesRepositorio {
    val red = MutableStateFlow(true)
    val copia = MutableStateFlow(copiaInicial)
    var resultado: ResultadoDeSincronizacion = ResultadoDeSincronizacion.Sincronizado(emptyList())
    var sincronizaciones = 0
    var alSincronizar: (() -> Unit)? = null
    val avisadas = mutableListOf<String>()
    val leidas = mutableListOf<String>()
    var todasLeidas = 0
    val registros = mutableListOf<Pair<Boolean, String>>()

    override val conectado: StateFlow<Boolean> = red
    override fun observar(): Flow<CopiaDeNotificaciones> = copia
    override fun observarUna(id: String): Flow<Notificacion?> = copia.map { c -> c.notificaciones.firstOrNull { it.id == id } }

    override suspend fun sincronizar(): ResultadoDeSincronizacion {
        sincronizaciones++
        alSincronizar?.invoke()
        return resultado
    }

    override suspend fun marcarAvisadas(ids: List<String>) {
        avisadas += ids
    }

    override suspend fun marcarLeida(id: String) {
        leidas += id
    }

    override suspend fun marcarTodasLeidas() {
        todasLeidas++
    }

    override suspend fun registrarDispositivo(permisoConcedido: Boolean, idioma: String) {
        registros += permisoConcedido to idioma
    }
}
