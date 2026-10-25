package co.solventa.movil.features.siniestros.evidencia

import co.solventa.movil.core.api.siniestros.EvidenciaRadicada
import co.solventa.movil.core.persistencia.dao.EvidenciaCapturadaDao
import co.solventa.movil.core.persistencia.entidades.EvidenciaCapturada
import co.solventa.movil.core.ubicacion.Ubicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Las evidencias de un reporte: se guardan en el teléfono al capturarlas y se
 * suben en segundo plano (HU-M2.1-01, criterio 5).
 *
 * El repositorio no sube nada: deja la evidencia pendiente y pide el envío.
 * Así el asegurado sigue capturando mientras la anterior viaja.
 */
interface EvidenciaRepositorio {

    /** Lo capturado para ese borrador, en orden y con su estado de envío. */
    fun observar(borradorId: String): Flow<List<EvidenciaDelReporte>>

    /**
     * Registra una captura y pide su envío. Devuelve `false` cuando el
     * archivo no se puede leer.
     */
    suspend fun capturar(
        borradorId: String,
        archivo: File,
        tipo: TipoDeEvidencia,
        ubicacion: Ubicacion?,
        duracionS: Int = 0,
    ): Boolean

    /** Descarta una captura y borra su archivo del teléfono. */
    suspend fun descartar(id: String)

    /**
     * Vuelve a intentar el envío de lo que quedó pendiente, sin esperar a que
     * se cumpla la espera creciente de los reintentos.
     */
    suspend fun reintentarEnvios(borradorId: String)

    /**
     * Lo que puede viajar con la radicación: solo lo que ya está en el
     * repositorio de evidencia, porque el servicio comprueba cada llave.
     */
    suspend fun paraRadicar(borradorId: String): List<EvidenciaRadicada>

    /** Si todo lo capturado ya quedó subido. */
    suspend fun todoSubido(borradorId: String): Boolean

    /** Al quedar radicado el siniestro, lo del teléfono deja de hacer falta. */
    suspend fun limpiar(borradorId: String)
}

@Singleton
class EvidenciaRepositorioLocal @Inject constructor(
    private val dao: EvidenciaCapturadaDao,
    private val huellas: HuellaDeArchivo,
    private val envios: EnvioDeEvidencia,
) : EvidenciaRepositorio {

    override fun observar(borradorId: String): Flow<List<EvidenciaDelReporte>> =
        dao.deBorrador(borradorId).map { filas -> filas.map(::aEvidencia) }

    override suspend fun capturar(
        borradorId: String,
        archivo: File,
        tipo: TipoDeEvidencia,
        ubicacion: Ubicacion?,
        duracionS: Int,
    ): Boolean {
        val huella = huellas.de(archivo) ?: return false
        val posicion = dao.listarDeBorrador(borradorId).size
        dao.guardar(
            EvidenciaCapturada(
                id = UUID.randomUUID().toString(),
                borradorId = borradorId,
                posicion = posicion,
                tipo = tipo.delContrato,
                rutaArchivo = archivo.absolutePath,
                capturadaEn = System.currentTimeMillis(),
                latitud = ubicacion?.latitud ?: 0.0,
                longitud = ubicacion?.longitud ?: 0.0,
                altitud = ubicacion?.altitud,
                precisionM = ubicacion?.precisionM ?: 0f,
                ubicacionConfiable = ubicacion?.esConfiable == true,
                sha256 = huella.sha256,
                tamanoBytes = huella.tamanoBytes,
                duracionS = duracionS,
                llaveDelObjeto = null,
                estado = EstadoDeEvidencia.PENDIENTE.guardado,
            ),
        )
        envios.pedir()
        return true
    }

    override suspend fun descartar(id: String) {
        dao.borrar(id)
    }

    override suspend fun reintentarEnvios(borradorId: String) {
        val pendientes = dao.listarDeBorrador(borradorId)
            .any { it.estado != EstadoDeEvidencia.SUBIDA.guardado }
        if (pendientes) envios.reintentar()
    }

    override suspend fun paraRadicar(borradorId: String): List<EvidenciaRadicada> =
        dao.listarDeBorrador(borradorId)
            .filter { it.estado == EstadoDeEvidencia.SUBIDA.guardado && it.llaveDelObjeto != null }
            .map { fila ->
                EvidenciaRadicada(
                    llaveDelObjeto = requireNotNull(fila.llaveDelObjeto),
                    tipo = fila.tipo,
                    capturadaEn = enRfc3339(fila.capturadaEn),
                    latitud = fila.latitud,
                    longitud = fila.longitud,
                    altitud = fila.altitud,
                    precisionM = fila.precisionM.toDouble(),
                    ubicacionConfiable = fila.ubicacionConfiable,
                    sha256 = fila.sha256,
                    tamanoBytes = fila.tamanoBytes,
                    duracionS = fila.duracionS,
                )
            }

    override suspend fun todoSubido(borradorId: String): Boolean =
        dao.listarDeBorrador(borradorId).all { it.estado == EstadoDeEvidencia.SUBIDA.guardado }

    override suspend fun limpiar(borradorId: String) {
        dao.listarDeBorrador(borradorId).forEach { runCatching { File(it.rutaArchivo).delete() } }
        dao.borrarDeBorrador(borradorId)
    }

    private fun aEvidencia(fila: EvidenciaCapturada) = EvidenciaDelReporte(
        id = fila.id,
        tipo = if (fila.tipo == TipoDeEvidencia.VIDEO.delContrato) {
            TipoDeEvidencia.VIDEO
        } else {
            TipoDeEvidencia.FOTO
        },
        rutaArchivo = fila.rutaArchivo,
        capturadaEn = fila.capturadaEn,
        ubicacion = fila.ubicacion(),
        duracionS = fila.duracionS,
        estado = EstadoDeEvidencia.de(fila.estado),
    )

    private fun EvidenciaCapturada.ubicacion(): Ubicacion? =
        if (precisionM <= 0f) {
            null
        } else {
            Ubicacion(latitud, longitud, altitud, precisionM, capturadaEn)
        }

    private fun enRfc3339(instante: Long): String =
        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(instante))
}
