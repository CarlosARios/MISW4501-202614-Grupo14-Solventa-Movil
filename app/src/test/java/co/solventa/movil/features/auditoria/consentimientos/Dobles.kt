package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.core.api.auditoria.AuditoriaApi
import co.solventa.movil.core.api.auditoria.CambioDeEstadoDto
import co.solventa.movil.core.api.auditoria.ConsentimientoDto
import co.solventa.movil.core.api.auditoria.EntidadDto
import co.solventa.movil.core.api.auditoria.FuenteDto
import co.solventa.movil.core.api.auditoria.EntidadDeRevocacionDto
import co.solventa.movil.core.api.auditoria.ListaDeConsentimientosDto
import co.solventa.movil.core.api.auditoria.RevocacionDto
import co.solventa.movil.core.api.auditoria.SolicitudDeRevocacionDto
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import co.solventa.movil.core.sincronizacion.ProgramadorDeSincronizacion
import androidx.work.ListenableWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

/** Dobles de prueba de la consulta de consentimientos (HU-M1.3-01). */

const val OTORGADO = 1_789_573_920_000L // 2026-09-16T15:52:00Z
const val VENCE = 1_821_109_920_000L // 2027-09-16T15:52:00Z
const val AHORA = 1_792_800_000_000L // 2026-10-24T00:00:00Z

fun dto(
    id: String,
    estado: String = "active",
    otorgado: String = "2026-09-16T15:52:00Z",
    vence: String = "2027-09-16T15:52:00Z",
    finalizado: String? = null,
    comprobante: String? = null,
) = ConsentimientoDto(
    id = id,
    estado = estado,
    vigente = estado == "active",
    entidades = listOf(EntidadDto("aliado-banco-1", "Banco Aliado")),
    finalidad = "risk_profiling_mortgage_life_offer",
    canal = "embedded",
    versionDeTerminos = "v1",
    alcance = listOf(FuenteDto("open_finance", listOf("credit_products", "balances"), "12_months")),
    otorgadoEn = otorgado,
    venceEn = vence,
    finalizadoEn = finalizado,
    comprobanteDeRevocacion = comprobante,
    historial = listOfNotNull(
        CambioDeEstadoDto("granted", otorgado, canal = "embedded"),
        if (estado == "revoked") CambioDeEstadoDto("revoked", finalizado!!, comprobante = comprobante, canal = "mobile") else null,
    ),
)

fun consentimiento(
    id: String,
    estado: String = EstadosDeConsentimiento.ACTIVO,
    otorgadoEn: Long = OTORGADO,
    venceEn: Long = VENCE,
    finalizadoEn: Long? = null,
    comprobante: String? = null,
) = Consentimiento(
    id = id,
    estado = estado,
    vigente = estado == EstadosDeConsentimiento.ACTIVO,
    entidades = listOf(Entidad("aliado-banco-1", "Banco Aliado")),
    finalidad = "risk_profiling_mortgage_life_offer",
    canal = "embedded",
    versionDeTerminos = "v1",
    alcance = listOf(Fuente("open_finance", listOf("credit_products"), "12_months")),
    otorgadoEn = otorgadoEn,
    venceEn = venceEn,
    finalizadoEn = finalizadoEn,
    comprobante = comprobante,
    historial = listOf(CambioDeEstado(CambiosDeEstado.OTORGADO, otorgadoEn, null, "embedded", null)),
)

/** El servicio de auditoria: responde lo que se le fije, o falla sin red. */
class AuditoriaFalsa : AuditoriaApi {
    var lista: List<ConsentimientoDto> = emptyList()
    var codigo: Int = 200
    var cuerpoDeError: String = ""
    var sinRed = false
    var llamadas = 0

    override suspend fun consentimientos(): Response<ListaDeConsentimientosDto> {
        llamadas++
        if (sinRed) throw IOException("sin red")
        return if (codigo in 200..299) {
            Response.success(ListaDeConsentimientosDto(lista))
        } else {
            Response.error(codigo, cuerpoDeError.toResponseBody("application/json".toMediaType()))
        }
    }

    override suspend fun consentimiento(id: String): Response<ConsentimientoDto> {
        llamadas++
        if (sinRed) throw IOException("sin red")
        return if (codigo in 200..299) {
            Response.success(lista.first { it.id == id })
        } else {
            Response.error(codigo, cuerpoDeError.toResponseBody("application/json".toMediaType()))
        }
    }

    // --- HU-M1.3-02 ---

    /** Lo que responde la revocacion: codigo, cuerpo o falla, fijados por la prueba. */
    var codigoDeRevocacion: Int = 201
    var revocacion: RevocacionDto? = null
    var errorDeRevocacion: String = ""
    var revocacionSinRed = false

    /** Cuanto tarda la revocacion, para probar el corte de 4 s. */
    var demoraDeRevocacion: Long = 0
    val revocaciones = mutableListOf<Triple<String, String, SolicitudDeRevocacionDto>>()

    /** Se llama al recibir la revocacion, antes de responder. */
    var alRevocar: (() -> Unit)? = null
    var comprobante: ByteArray? = "%PDF-1.3".toByteArray()
    var codigoDeComprobante: Int = 200
    var idiomaDelComprobante: String? = null

    override suspend fun revocar(id: String, clave: String, solicitud: SolicitudDeRevocacionDto): Response<RevocacionDto> {
        revocaciones += Triple(id, clave, solicitud)
        alRevocar?.invoke()
        if (demoraDeRevocacion > 0) kotlinx.coroutines.delay(demoraDeRevocacion)
        if (revocacionSinRed) throw IOException("sin red")
        return if (codigoDeRevocacion in 200..299) {
            Response.success(codigoDeRevocacion, revocacion ?: revocacionDto(id))
        } else {
            Response.error(codigoDeRevocacion, errorDeRevocacion.toResponseBody("application/json".toMediaType()))
        }
    }

    override suspend fun comprobanteDeRevocacion(id: String, idioma: String): Response<ResponseBody> {
        idiomaDelComprobante = idioma
        if (sinRed) throw IOException("sin red")
        val bytes = comprobante
        return if (codigoDeComprobante in 200..299 && bytes != null) {
            Response.success(bytes.toResponseBody("application/pdf".toMediaType()))
        } else {
            Response.error(codigoDeComprobante, "{}".toResponseBody("application/json".toMediaType()))
        }
    }
}

/** La revocacion que responde el servicio por omision. */
fun revocacionDto(
    id: String,
    solicitada: String = "2026-10-24T19:59:58Z",
    efectiva: String = "2026-10-24T20:00:00Z",
    estado: String = "revoked",
    aviso: String = "in_progress",
    entidad: String = "pending",
) = RevocacionDto(
    consentimientoId = id, comprobante = "REV-2026-0088", estado = estado, solicitadaEn = solicitada, efectivaEn = efectiva,
    canal = "mobile_app", dispositivo = "Google Pixel 8", estadoDelAviso = aviso,
    entidades = listOf(EntidadDeRevocacionDto("aliado-banco-1", "Banco Aliado", entidad, null, "2026-10-24T20:05:00Z")),
)

class SesionFalsa(var sub: String = "sub-a", var biometria: Boolean = false) : SesionRepositorio {
    override suspend fun sesionGuardada(): Sesion? = Sesion("asegurado@example.com", "Carolina", sub)
    override suspend fun biometriaHabilitada(): Boolean = biometria
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

class ProgramadorFalso : ProgramadorDeSincronizacion {
    val programados = mutableListOf<Pair<String, Class<out ListenableWorker>>>()
    override fun programar(nombreUnico: String, trabajador: Class<out ListenableWorker>) {
        programados += nombreUnico to trabajador
    }
}

/** Cuenta los envios urgentes programados (HU-M1.3-02). */
class ProgramadorDeEnvioFalso : ProgramadorDeEnvioDeRevocaciones {
    var programados = 0
    override fun programar() {
        programados++
    }
}

/** El repositorio de la pantalla: copia y resultado fijados por la prueba. */
class RepositorioFalso(inicial: CopiaDeConsentimientos = CopiaDeConsentimientos(emptyList(), null, false)) :
    ConsentimientosRepositorio {
    val copia = MutableStateFlow(inicial)
    val red = MutableStateFlow(true)
    var resultado: ResultadoDeActualizacion = ResultadoDeActualizacion.Actualizado
    var actualizaciones = 0
    var alActualizar: (() -> Unit)? = null

    override val conectado: StateFlow<Boolean> = red
    override fun observar(): Flow<CopiaDeConsentimientos> = copia
    override fun observarUno(id: String): Flow<Consentimiento?> =
        kotlinx.coroutines.flow.flow { copia.collect { c -> emit(c.consentimientos.firstOrNull { it.id == id }) } }

    override suspend fun actualizar(): ResultadoDeActualizacion {
        actualizaciones++
        alActualizar?.invoke()
        return resultado
    }
}
