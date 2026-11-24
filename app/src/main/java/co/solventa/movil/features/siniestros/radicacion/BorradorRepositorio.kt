package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.core.persistencia.dao.BorradorDeRadicacionDao
import co.solventa.movil.core.persistencia.dao.EvidenciaCapturadaDao
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
import co.solventa.movil.features.siniestros.UsuarioActual
import co.solventa.movil.features.siniestros.evidencia.TipoDeEvidencia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El reporte a medio diligenciar, con la etapa en la que quedó.
 *
 * El identificador es además la clave con la que se radica, de modo que un
 * reintento desde el mismo borrador no crea dos casos.
 */
data class BorradorEnCurso(
    val id: String,
    val etapa: Etapa,
    val radicacion: Radicacion,
    val actualizadoEn: Long = 0L,
    /** Intentos de envío fallidos desde que se encoló (HU-M2.2-02). */
    val intentos: Int = 0,
)

/** En qué va el envío de un borrador (HU-M2.2-02). */
enum class EnvioDelBorrador(val guardado: String) {
    /** El asegurado lo sigue diligenciando. */
    EDICION("edicion"),

    /** Completo, esperando red para enviarse en segundo plano. */
    EN_COLA("en_cola"),
    ENVIANDO("enviando"),

    /** Se agotaron los reintentos automáticos; queda el reintento manual. */
    FALLIDO("fallido"),

    /** El servicio confirmó el radicado; falta mostrar el aviso. */
    RADICADO("radicado"),
    ;

    companion object {
        fun de(valor: String) = entries.firstOrNull { it.guardado == valor } ?: EDICION
    }
}

/** Un borrador como se ve en *Mis siniestros* (M13b–M13d). */
data class BorradorResumen(
    val id: String,
    val etapa: Etapa,
    val tipo: TipoDeIncidente?,
    val poliza: String,
    val actualizadoEn: Long,
    val envio: EnvioDelBorrador,
    val intentos: Int = 0,
    val ultimoIntentoEn: Long? = null,
    val radicado: String? = null,
    val radicadoEn: Long? = null,
    val casoId: String? = null,
    val fotos: Int = 0,
    val videos: Int = 0,
)

interface BorradorRepositorio {
    /** El borrador en edición que se retoma al volver a entrar, si lo hay. */
    suspend fun enCurso(): BorradorEnCurso?

    suspend fun guardar(borrador: BorradorEnCurso)

    /** Se descarta cuando el siniestro quedó radicado o el usuario lo abandona. */
    suspend fun descartar(id: String)

    /** Un identificador nuevo para empezar un reporte. */
    fun nuevoIdentificador(): String

    /** Los borradores del usuario de la sesión, con lo que lleva capturado. */
    fun delUsuario(): Flow<List<BorradorResumen>>

    /** Lo vuelve a poner en edición y de primero, para continuarlo. */
    suspend fun retomar(id: String)

    /** Pasa a la cola de envío en segundo plano, con los intentos en cero. */
    suspend fun encolar(id: String)

    /**
     * El borrador completo para enviarlo, solo si es del usuario de la
     * sesión: el envío va firmado con su token.
     */
    suspend fun paraEnviar(id: String): BorradorEnCurso?

    suspend fun marcarEnviando(id: String)

    /** Cuenta un intento fallido y deja el borrador en [envio]. */
    suspend fun registrarFallo(id: String, envio: EnvioDelBorrador)

    /** El servicio confirmó: se guarda el radicado hasta mostrar el aviso. */
    suspend fun marcarRadicado(id: String, caso: CasoRadicado)
}

@Singleton
class BorradorRepositorioLocal internal constructor(
    private val dao: BorradorDeRadicacionDao,
    private val evidencias: EvidenciaCapturadaDao,
    private val usuario: UsuarioActual,
    private val reloj: () -> Long,
) : BorradorRepositorio {

    @Inject
    constructor(
        dao: BorradorDeRadicacionDao,
        evidencias: EvidenciaCapturadaDao,
        usuario: UsuarioActual,
    ) : this(dao, evidencias, usuario, System::currentTimeMillis)

    private val json = Json

    override suspend fun enCurso(): BorradorEnCurso? {
        val sub = usuario.sub().ifEmpty { return null }
        return dao.enCurso(sub)?.let(::aBorrador)
    }

    override suspend fun guardar(borrador: BorradorEnCurso) {
        val sub = usuario.sub().ifEmpty { return }
        dao.guardar(aEntidad(borrador, sub))
    }

    override suspend fun descartar(id: String) {
        dao.borrar(id)
    }

    override fun nuevoIdentificador(): String = UUID.randomUUID().toString()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun delUsuario(): Flow<List<BorradorResumen>> =
        flow { emit(usuario.sub()) }.flatMapLatest { sub ->
            if (sub.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(dao.deUsuario(sub), evidencias.deUsuario(sub)) { borradores, capturadas ->
                    val porBorrador = capturadas.groupBy { it.borradorId }
                    borradores.map { b ->
                        val suyas = porBorrador[b.id].orEmpty()
                        aResumen(b).copy(
                            fotos = suyas.count { it.tipo == TipoDeEvidencia.FOTO.delContrato },
                            videos = suyas.count { it.tipo == TipoDeEvidencia.VIDEO.delContrato },
                        )
                    }
                }
            }
        }

    override suspend fun retomar(id: String) {
        val sub = usuario.sub().ifEmpty { return }
        dao.retomar(id, sub, reloj())
    }

    override suspend fun encolar(id: String) {
        dao.encolar(id)
    }

    override suspend fun paraEnviar(id: String): BorradorEnCurso? {
        val sub = usuario.sub().ifEmpty { return null }
        // Uno que se volvió a editar, o ya radicado, no se envía.
        return dao.porId(id, sub)
            ?.takeIf { EnvioDelBorrador.de(it.envio) in ENVIABLES }
            ?.let(::aBorrador)
    }

    override suspend fun marcarEnviando(id: String) {
        dao.marcarEnvio(id, EnvioDelBorrador.ENVIANDO.guardado)
    }

    override suspend fun registrarFallo(id: String, envio: EnvioDelBorrador) {
        dao.registrarIntentoFallido(id, envio.guardado, reloj())
    }

    override suspend fun marcarRadicado(id: String, caso: CasoRadicado) {
        dao.marcarRadicado(id, caso.radicado, caso.casoId, reloj())
    }

    private fun aBorrador(e: BorradorDeRadicacion) = BorradorEnCurso(
        id = e.id,
        etapa = etapaDe(e.etapa),
        radicacion = Radicacion(
            poliza = e.poliza,
            tipo = e.tipo?.let { tipoDe(it) },
            ocurrioEn = e.ocurrioEn,
            lugar = e.lugar,
            descripcion = e.descripcion,
            respuestas = respuestasDe(e.respuestasJson),
            declaracionAceptada = e.declaracionAceptada,
            montoEstimado = e.montoEstimado,
        ),
        actualizadoEn = e.actualizadoEn,
        intentos = e.intentos,
    )

    private fun aResumen(e: BorradorDeRadicacion) = BorradorResumen(
        id = e.id,
        etapa = etapaDe(e.etapa),
        tipo = e.tipo?.let { tipoDe(it) },
        poliza = e.poliza,
        actualizadoEn = e.actualizadoEn,
        envio = EnvioDelBorrador.de(e.envio),
        intentos = e.intentos,
        ultimoIntentoEn = e.ultimoIntentoEn,
        radicado = e.radicado,
        radicadoEn = e.radicadoEn,
        casoId = e.casoId,
    )

    private fun aEntidad(b: BorradorEnCurso, propietario: String) = BorradorDeRadicacion(
        id = b.id,
        etapa = b.etapa.name,
        poliza = b.radicacion.poliza,
        tipo = b.radicacion.tipo?.name,
        ocurrioEn = b.radicacion.ocurrioEn,
        lugar = b.radicacion.lugar,
        descripcion = b.radicacion.descripcion,
        respuestasJson = json.encodeToString(b.radicacion.respuestas),
        declaracionAceptada = b.radicacion.declaracionAceptada,
        montoEstimado = b.radicacion.montoEstimado,
        actualizadoEn = reloj(),
        propietario = propietario,
    )

    /**
     * Lo guardado puede ser de una versión anterior de la aplicación. Un
     * valor que ya no existe no vale perder el borrador entero: se vuelve a
     * la primera etapa y al tipo sin elegir.
     */
    private fun etapaDe(valor: String) =
        runCatching { Etapa.valueOf(valor) }.getOrDefault(Etapa.IDENTIFICACION)

    private fun tipoDe(valor: String) =
        runCatching { TipoDeIncidente.valueOf(valor) }.getOrNull()

    private fun respuestasDe(valor: String): Map<String, String> =
        runCatching { json.decodeFromString<Map<String, String>>(valor) }.getOrDefault(emptyMap())

    private companion object {
        val ENVIABLES = setOf(EnvioDelBorrador.EN_COLA, EnvioDelBorrador.ENVIANDO)
    }
}
