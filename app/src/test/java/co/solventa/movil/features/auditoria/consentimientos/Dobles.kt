package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.core.api.auditoria.AuditoriaApi
import co.solventa.movil.core.api.auditoria.CambioDeEstadoDto
import co.solventa.movil.core.api.auditoria.ConsentimientoDto
import co.solventa.movil.core.api.auditoria.EntidadDto
import co.solventa.movil.core.api.auditoria.FuenteDto
import co.solventa.movil.core.api.auditoria.ListaDeConsentimientosDto
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

    override suspend fun consentimiento(id: String): Response<ConsentimientoDto> =
        Response.success(lista.first { it.id == id })
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

class ProgramadorFalso : ProgramadorDeSincronizacion {
    val programados = mutableListOf<Pair<String, Class<out ListenableWorker>>>()
    override fun programar(nombreUnico: String, trabajador: Class<out ListenableWorker>) {
        programados += nombreUnico to trabajador
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
