package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.core.persistencia.cifrado.CifradorAesGcm
import co.solventa.movil.core.persistencia.dao.SolicitudDeAsistenciaDao
import co.solventa.movil.core.persistencia.entidades.SolicitudDeAsistenciaEntidad
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.features.siniestros.radicacion.PolizaElegible
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.crypto.KeyGenerator

/** Dobles de la solicitud de asistencia (HU-M2.3-01), compartidos por sus pruebas. */
internal object DatosDeAsistencia {
    const val AHORA = 1_794_710_400_000L
    val poliza = PolizaElegible(id = "AU-2025-118204", etiqueta = "Autos · AU-2025-118204 · KJT-482")
    val otraPoliza = PolizaElegible(id = "HO-2026-000412", etiqueta = "Hogar · HO-2026-000412")
    val lectura = Ubicacion(latitud = 4.7712, longitud = -74.0452, altitud = null, precisionM = 8f, leidaEn = AHORA - 9_000)
    val compartida = ResultadoDeUbicacion.Compartida(lectura)

    fun cifrador() = CifradorAesGcm(KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().let { clave -> { clave } })
}

/** La cola en memoria, con la misma semántica que las consultas del DAO real. */
internal class ColaEnMemoria : SolicitudDeAsistenciaDao {
    val filas = MutableStateFlow<List<SolicitudDeAsistenciaEntidad>>(emptyList())
    private var siguiente = 1L

    override suspend fun insertar(fila: SolicitudDeAsistenciaEntidad): Long {
        val id = siguiente++
        filas.value = filas.value + fila.copy(id = id)
        return id
    }

    override suspend fun actualizar(fila: SolicitudDeAsistenciaEntidad) {
        filas.value = filas.value.map { if (it.id == fila.id) fila else it }
    }

    override suspend fun pendientes(propietario: String) = filas.value
        .filter { it.propietario == propietario && it.estadoDeEnvio == SolicitudDeAsistenciaEntidad.EN_COLA }
        .sortedWith(compareBy({ it.creadaEn }, { it.id }))

    override fun ultima(propietario: String): Flow<SolicitudDeAsistenciaEntidad?> = filas.map { lista ->
        lista.filter { it.propietario == propietario }.maxWithOrNull(compareBy({ it.creadaEn }, { it.id }))
    }

    override suspend fun porRequestId(propietario: String, requestId: String) =
        filas.value.firstOrNull { it.propietario == propietario && it.requestId == requestId }

    override suspend fun borrarResueltasAntesDe(instante: Long) {
        filas.value = filas.value.filterNot { it.estadoDeEnvio != SolicitudDeAsistenciaEntidad.EN_COLA && it.creadaEn < instante }
    }

    fun una(id: Long) = filas.value.single { it.id == id }
}

internal class ConectividadFija(valor: Boolean = true) : ObservadorDeConectividad {
    override val conectado = MutableStateFlow(valor)
}

internal class ProgramadorContador : ProgramadorDeEnvioDeAsistencias {
    var veces = 0
    override fun programar() {
        veces++
    }
}

internal class TraductorFijo(private val lugar: String? = "Autopista Norte km 12, Bogotá") : TraductorDeCoordenadas {
    var llamadas = 0
    var falla: Exception? = null
    override suspend fun traducir(latitud: Double, longitud: Double): String? {
        llamadas++
        falla?.let { throw it }
        return lugar
    }
}

internal class AvisadorRegistro : AvisadorDeAsistencia {
    val avisos = mutableListOf<MotivoDeDescarte>()
    override fun noRegistrada(motivo: MotivoDeDescarte) {
        avisos += motivo
    }
}

/** Un repositorio de mentira para el `ViewModel` y el trabajador. */
internal class RepositorioDeAsistenciaFalso(conectado: Boolean = true) : AsistenciasRepositorio {
    val ultima = MutableStateFlow<SolicitudDeAsistencia?>(null)
    val solicitadas = mutableListOf<Pair<PolizaElegible, ResultadoDeUbicacion>>()
    val consultas = mutableListOf<String>()
    var estadoRemoto: EstadoDeLaSolicitud? = EstadoDeLaSolicitud.NUEVA
    var resultadoDelEnvio = ResultadoDelEnvio.COMPLETO
    var envios = 0
    var reanudaciones = 0
    var visible: Boolean? = null
    var sinSesion = false

    override val conectado: StateFlow<Boolean> = MutableStateFlow(conectado)

    override suspend fun solicitar(poliza: PolizaElegible, resultado: ResultadoDeUbicacion): Long? {
        if (sinSesion) return null
        solicitadas += poliza to resultado
        return solicitadas.size.toLong()
    }

    override fun observarUltima(): Flow<SolicitudDeAsistencia?> = ultima

    override suspend fun reanudarPendientes() {
        reanudaciones++
    }

    override suspend fun enviarPendientes(): ResultadoDelEnvio {
        envios++
        return resultadoDelEnvio
    }

    override suspend fun consultarEstado(requestId: String): EstadoDeLaSolicitud? {
        consultas += requestId
        return estadoRemoto
    }

    override fun pantallaVisible(visible: Boolean) {
        this.visible = visible
    }
}
