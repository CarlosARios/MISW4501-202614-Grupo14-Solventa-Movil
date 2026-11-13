package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.persistencia.cifrado.CifradorDeDatos
import co.solventa.movil.core.persistencia.dao.SiniestroGuardadoDao
import co.solventa.movil.core.persistencia.entidades.SiniestroGuardadoEntidad
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Una versión guardada en el teléfono y la hora de sus datos. */
data class CopiaGuardada<T>(val valor: T, val guardadoEn: Long)

/**
 * La última versión de *Mis siniestros* y del detalle de cada caso que
 * entregó el servicio, guardada en el teléfono para mostrarla sin conexión
 * (HU-M2.4-01, criterio 5).
 */
interface CopiaLocalDeSiniestros {

    suspend fun guardarLista(lista: List<Siniestro>, guardadoEn: Long)

    suspend fun lista(): CopiaGuardada<List<Siniestro>>?

    suspend fun guardarDetalle(detalle: DetalleDeSiniestro, guardadoEn: Long)

    suspend fun detalle(id: String): CopiaGuardada<DetalleDeSiniestro>?

    /** Sin copia: lo que usan las pruebas que no la necesitan. */
    object Ninguna : CopiaLocalDeSiniestros {
        override suspend fun guardarLista(lista: List<Siniestro>, guardadoEn: Long) = Unit
        override suspend fun lista(): CopiaGuardada<List<Siniestro>>? = null
        override suspend fun guardarDetalle(detalle: DetalleDeSiniestro, guardadoEn: Long) = Unit
        override suspend fun detalle(id: String): CopiaGuardada<DetalleDeSiniestro>? = null
    }
}

/**
 * [CopiaLocalDeSiniestros] en Room, cifrada con [CifradorDeDatos] como la
 * copia de las pólizas (HU-M1.2-01):
 *
 * - Cada lectura y escritura va por el `sub` de la sesión, y al guardar se
 *   borra lo que hubiera dejado otra cuenta. Sin sesión no se guarda ni se lee
 *   nada.
 * - Una fila que no se puede descifrar o leer (se perdió la clave del
 *   Keystore, o cambió el modelo) es como si no hubiera copia: la siguiente
 *   consulta con red la repone.
 * - Al cerrar la sesión se borra toda la copia ([AlCerrarSesion]).
 */
@Singleton
class CopiaLocalDeSiniestrosRoom @Inject constructor(
    private val dao: SiniestroGuardadoDao,
    private val cifrador: CifradorDeDatos,
    private val sesion: SesionRepositorio,
) : CopiaLocalDeSiniestros, AlCerrarSesion {

    override suspend fun guardarLista(lista: List<Siniestro>, guardadoEn: Long) =
        guardar(CLAVE_LISTA, ListSerializer(Siniestro.serializer()), lista, guardadoEn)

    override suspend fun lista(): CopiaGuardada<List<Siniestro>>? = leer(CLAVE_LISTA, ListSerializer(Siniestro.serializer()))

    override suspend fun guardarDetalle(detalle: DetalleDeSiniestro, guardadoEn: Long) =
        guardar(claveDeDetalle(detalle.siniestro.id), DetalleDeSiniestro.serializer(), detalle, guardadoEn)

    override suspend fun detalle(id: String): CopiaGuardada<DetalleDeSiniestro>? =
        leer(claveDeDetalle(id), DetalleDeSiniestro.serializer())

    override suspend fun limpiar() = dao.borrarTodo()

    private suspend fun <T> guardar(clave: String, serializador: KSerializer<T>, valor: T, guardadoEn: Long) {
        val sub = sub()
        if (sub.isEmpty()) return
        dao.guardar(SiniestroGuardadoEntidad(sub, clave, cifrador.cifrar(JSON.encodeToString(serializador, valor)), guardadoEn))
        dao.borrarDeOtrosPropietarios(sub)
    }

    private suspend fun <T> leer(clave: String, serializador: KSerializer<T>): CopiaGuardada<T>? {
        val sub = sub()
        if (sub.isEmpty()) return null
        val fila = dao.leer(sub, clave) ?: return null
        val claro = cifrador.descifrar(fila.contenidoCifrado) ?: return null
        val valor = runCatching { JSON.decodeFromString(serializador, claro) }.getOrNull() ?: return null
        return CopiaGuardada(valor, fila.guardadoEn)
    }

    private suspend fun sub(): String = sesion.sesionGuardada()?.sub.orEmpty()

    internal companion object {
        const val CLAVE_LISTA = "lista"

        fun claveDeDetalle(id: String) = "detalle:$id"

        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
