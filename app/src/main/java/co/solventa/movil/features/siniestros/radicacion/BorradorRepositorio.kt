package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.core.persistencia.dao.BorradorDeRadicacionDao
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
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
)

interface BorradorRepositorio {
    /** El borrador que se retoma al volver a entrar, si lo hay. */
    suspend fun enCurso(): BorradorEnCurso?

    suspend fun guardar(borrador: BorradorEnCurso)

    /** Se descarta cuando el siniestro quedó radicado o el usuario lo abandona. */
    suspend fun descartar(id: String)

    /** Un identificador nuevo para empezar un reporte. */
    fun nuevoIdentificador(): String
}

@Singleton
class BorradorRepositorioLocal @Inject constructor(
    private val dao: BorradorDeRadicacionDao,
) : BorradorRepositorio {

    private val json = Json

    /** Solo ordena los borradores; no hace falta fijarlo en las pruebas. */
    private val reloj: () -> Long = System::currentTimeMillis

    override suspend fun enCurso(): BorradorEnCurso? = dao.enCurso()?.let(::aBorrador)

    override suspend fun guardar(borrador: BorradorEnCurso) {
        dao.guardar(aEntidad(borrador))
    }

    override suspend fun descartar(id: String) {
        dao.borrar(id)
    }

    override fun nuevoIdentificador(): String = UUID.randomUUID().toString()

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
    )

    private fun aEntidad(b: BorradorEnCurso) = BorradorDeRadicacion(
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
}
