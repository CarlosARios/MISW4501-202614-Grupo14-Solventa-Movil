package co.solventa.movil.features.originacion.polizas

import androidx.work.ListenableWorker
import co.solventa.movil.core.api.originacion.AseguradoDto
import co.solventa.movil.core.api.originacion.CoberturaDto
import co.solventa.movil.core.api.originacion.LecturaDeDocumento
import co.solventa.movil.core.api.originacion.ListaDePolizasDto
import co.solventa.movil.core.api.originacion.OriginacionApi
import co.solventa.movil.core.api.originacion.PolizaDto
import co.solventa.movil.core.api.originacion.RespuestaDeRegistro
import co.solventa.movil.core.api.originacion.SolicitudDeLectura
import co.solventa.movil.core.api.originacion.SolicitudDeRegistro
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.persistencia.cifrado.CifradorAesGcm
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import co.solventa.movil.core.sincronizacion.ProgramadorDeSincronizacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException
import javax.crypto.KeyGenerator

/**
 * Dobles de prueba de la billetera de pólizas (HU-M1.2-01). Los datos son los
 * del prototipo (`docs/pantallas.md`): AU-2025-118204 con placa KJT-482 y
 * $ 72.500.000 asegurados, la de vida VH-2026-004512 y la de hogar
 * HG-2025-880231.
 */

const val AHORA = 1_789_569_000_000L // 2026-09-16T14:30:00Z
const val DIA = 86_400_000L

/** Una clave AES de software: el Keystore de Android no existe en la JVM. */
fun cifradorDePrueba() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().let { clave ->
    CifradorAesGcm { clave }
}

fun polizaDto(
    id: String = "AU-2025-118204",
    linea: String = "auto",
    estado: String = "active",
    inicio: String = "2025-10-02T00:00:00Z",
    fin: String = "2026-10-02T00:00:00Z",
    prima: Double? = null,
    renovacionDe: String? = null,
) = PolizaDto(
    id = id,
    origen = "policy_core",
    linea = linea,
    producto = "Autos",
    objetoAsegurado = "KJT-482",
    estado = estado,
    asegurado = AseguradoDto("Carolina Pérez Ruiz", "CC", "1020304050"),
    prima = prima,
    moneda = "COP",
    valorAsegurado = 72_500_000.0,
    coberturas = listOf(CoberturaDto("collision", 72_500_000.0), CoberturaDto("theft", 72_500_000.0)),
    inicio = inicio,
    fin = fin,
    renovacionDe = renovacionDe,
)

fun poliza(
    id: String = "AU-2025-118204",
    linea: String = LineasDePoliza.AUTOS,
    estado: String = EstadosDePoliza.ACTIVA,
    inicio: Long = AHORA - 300 * DIA,
    fin: Long = AHORA + 200 * DIA,
    prima: Double? = null,
    renovacionDe: String? = null,
    valorAsegurado: Double = 72_500_000.0,
) = Poliza(
    id = id,
    origen = "policy_core",
    linea = linea,
    producto = "Autos",
    objetoAsegurado = "KJT-482",
    estado = estado,
    asegurado = Asegurado("Carolina Pérez Ruiz", "CC", "1020304050"),
    prima = prima,
    moneda = "COP",
    valorAsegurado = valorAsegurado,
    coberturas = listOf(Cobertura("collision", valorAsegurado)),
    inicio = inicio,
    fin = fin,
    renovacionDe = renovacionDe,
)

/** El servicio de originación: responde la lista que se le fije, o falla sin red. */
class OriginacionFalsa : OriginacionApi {
    var lista: List<PolizaDto> = emptyList()
    var parcial = false
    var codigo = 200
    var sinRed = false
    var llamadas = 0

    override suspend fun polizas(): Response<ListaDePolizasDto> {
        llamadas++
        if (sinRed) throw IOException("sin red")
        return if (codigo in 200..299) {
            Response.success(ListaDePolizasDto(lista, parcial))
        } else {
            Response.error(codigo, "{}".toResponseBody("application/json".toMediaType()))
        }
    }

    override suspend fun leerDocumento(correlacion: String, peticion: SolicitudDeLectura): Response<LecturaDeDocumento> =
        error("la billetera no lee documentos")

    override suspend fun registrar(correlacion: String, peticion: SolicitudDeRegistro): Response<RespuestaDeRegistro> =
        error("la billetera no registra")
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

/** El repositorio para los ViewModel y el trabajo: copia y resultado fijados por la prueba. */
class RepositorioFalso(
    copiaInicial: CopiaDePolizas = CopiaDePolizas(emptyList(), null, sinConexion = false),
) : PolizasRepositorio {
    val red = MutableStateFlow(true)
    val copia = MutableStateFlow(copiaInicial)
    val cambiosFijados = MutableStateFlow<List<CambioDePoliza>>(emptyList())
    var resultado: ResultadoDeSincronizacion = ResultadoDeSincronizacion.Sincronizado(emptyList())
    var sincronizaciones = 0

    override val conectado: StateFlow<Boolean> = red
    override val cambios: StateFlow<List<CambioDePoliza>> = cambiosFijados

    override fun observar(): Flow<CopiaDePolizas> = copia

    override fun observarUna(id: String): Flow<Poliza?> = copia.map { c -> c.polizas.firstOrNull { it.id == id } }

    override suspend fun sincronizar(): ResultadoDeSincronizacion {
        sincronizaciones++
        return resultado
    }

    override fun descartarCambios() {
        cambiosFijados.value = emptyList()
    }
}
