package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.core.api.auditoria.ConsentimientoDto
import co.solventa.movil.core.api.auditoria.RevocacionDto
import co.solventa.movil.core.persistencia.entidades.ConsentimientoEntidad
import co.solventa.movil.core.persistencia.entidades.RevocacionPendienteEntidad
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Traduce entre la red, la copia local y el dominio (HU-M1.3-01). La entidad
 * de Room guarda entidades, alcance e historial como JSON en texto: este
 * mapeador es el unico que escribe y lee ese texto.
 */
object MapeadorDeConsentimientos {

    private val json = Json { ignoreUnknownKeys = true }

    fun desdeDto(dto: ConsentimientoDto): Consentimiento = Consentimiento(
        id = dto.id,
        estado = dto.estado,
        vigente = dto.vigente,
        entidades = dto.entidades.map { Entidad(it.codigo, it.nombre) },
        finalidad = dto.finalidad,
        canal = dto.canal,
        versionDeTerminos = dto.versionDeTerminos,
        alcance = dto.alcance.map { Fuente(it.codigo, it.categorias, it.vigencia) },
        otorgadoEn = instante(dto.otorgadoEn),
        venceEn = instante(dto.venceEn),
        finalizadoEn = dto.finalizadoEn?.let(::instante),
        comprobante = dto.comprobanteDeRevocacion,
        historial = dto.historial.map {
            CambioDeEstado(it.estado, instante(it.ocurrioEn), it.comprobante, it.canal, it.entidad)
        },
        revocable = dto.revocable,
        revocacion = dto.revocacion?.let(::revocacionDesdeDto),
    )

    /** La revocacion que entrego el servicio (HU-M1.3-02). */
    fun revocacionDesdeDto(dto: RevocacionDto): Revocacion = Revocacion(
        comprobante = dto.comprobante,
        estado = dto.estado,
        solicitadaEn = instante(dto.solicitadaEn),
        efectivaEn = instante(dto.efectivaEn),
        canal = dto.canal,
        dispositivo = dto.dispositivo,
        estadoDelAviso = dto.estadoDelAviso,
        entidades = dto.entidades.map {
            EntidadDeRevocacion(it.codigo, it.nombre, it.estado, it.confirmadaEn?.let(::instante), it.plazo?.let(::instante))
        },
    )

    /**
     * El consentimiento tal como queda al registrarse su revocacion: deja de
     * estar vigente desde la hora efectiva y su historial suma la solicitud y
     * la revocacion (sin repetirlas si la copia ya las traia). Asi el titular
     * lo ve revocado en seguida, sin esperar la siguiente sincronizacion.
     */
    fun conRevocacion(c: Consentimiento, revocacion: Revocacion): Consentimiento {
        val nuevos = listOf(
            CambioDeEstado(CambiosDeEstado.REVOCACION_SOLICITADA, revocacion.solicitadaEn, null, revocacion.canal, null),
            CambioDeEstado(CambiosDeEstado.REVOCADO, revocacion.efectivaEn, revocacion.comprobante, revocacion.canal, null),
        ).filterNot { nuevo -> c.historial.any { it.estado == nuevo.estado && it.entidad == null } }
        return c.copy(
            estado = revocacion.estado,
            vigente = false,
            revocable = false,
            finalizadoEn = revocacion.efectivaEn,
            comprobante = revocacion.comprobante,
            revocacion = revocacion,
            historial = (c.historial + nuevos).sortedBy { it.ocurrioEn },
        )
    }

    /** Una fila de la cola local como la ve el dominio. */
    fun pendienteDesdeEntidad(e: RevocacionPendienteEntidad): RevocacionPendiente =
        RevocacionPendiente(e.consentimientoId, e.entidad, e.solicitadaEn, e.intentos)

    fun aEntidad(c: Consentimiento, propietario: String): ConsentimientoEntidad = ConsentimientoEntidad(
        propietario = propietario,
        id = c.id,
        estado = c.estado,
        vigente = c.vigente,
        finalidad = c.finalidad,
        canal = c.canal,
        versionDeTerminos = c.versionDeTerminos,
        otorgadoEn = c.otorgadoEn,
        venceEn = c.venceEn,
        finalizadoEn = c.finalizadoEn,
        comprobante = c.comprobante,
        entidades = json.encodeToString(ListSerializer(EntidadGuardada.serializer()), c.entidades.map(::EntidadGuardada)),
        alcance = json.encodeToString(ListSerializer(FuenteGuardada.serializer()), c.alcance.map(::FuenteGuardada)),
        historial = json.encodeToString(ListSerializer(CambioGuardado.serializer()), c.historial.map(::CambioGuardado)),
        revocacion = c.revocacion?.let { json.encodeToString(RevocacionGuardada.serializer(), RevocacionGuardada(it)) },
    )

    fun desdeEntidad(e: ConsentimientoEntidad): Consentimiento = Consentimiento(
        id = e.id,
        estado = e.estado,
        vigente = e.vigente,
        entidades = lista(e.entidades, EntidadGuardada.serializer()).map { Entidad(it.codigo, it.nombre) },
        finalidad = e.finalidad,
        canal = e.canal,
        versionDeTerminos = e.versionDeTerminos,
        alcance = lista(e.alcance, FuenteGuardada.serializer()).map { Fuente(it.codigo, it.categorias, it.vigencia) },
        otorgadoEn = e.otorgadoEn,
        venceEn = e.venceEn,
        finalizadoEn = e.finalizadoEn,
        comprobante = e.comprobante,
        historial = lista(e.historial, CambioGuardado.serializer()).map {
            CambioDeEstado(it.estado, it.ocurrioEn, it.comprobante, it.canal, it.entidad)
        },
        // La copia no guarda el indicador: el servidor solo deja revocar un activo.
        revocable = e.estado == EstadosDeConsentimiento.ACTIVO,
        revocacion = e.revocacion?.let { texto ->
            runCatching { json.decodeFromString(RevocacionGuardada.serializer(), texto).aDominio() }.getOrNull()
        },
    )

    /** RFC 3339 a milisegundos; una fecha mal formada no tumba la pantalla. */
    private fun instante(texto: String): Long = runCatching { Instant.parse(texto).toEpochMilli() }.getOrDefault(0L)

    private fun <T> lista(texto: String, serializador: kotlinx.serialization.KSerializer<T>): List<T> =
        runCatching { json.decodeFromString(ListSerializer(serializador), texto) }.getOrDefault(emptyList())

    @Serializable
    private data class EntidadGuardada(
        @SerialName("codigo") val codigo: String,
        @SerialName("nombre") val nombre: String,
    ) {
        constructor(e: Entidad) : this(e.codigo, e.nombre)
    }

    @Serializable
    private data class FuenteGuardada(
        @SerialName("codigo") val codigo: String,
        @SerialName("categorias") val categorias: List<String>,
        @SerialName("vigencia") val vigencia: String,
    ) {
        constructor(f: Fuente) : this(f.codigo, f.categorias, f.vigencia)
    }

    @Serializable
    private data class EntidadDeRevocacionGuardada(
        @SerialName("codigo") val codigo: String,
        @SerialName("nombre") val nombre: String,
        @SerialName("estado") val estado: String,
        @SerialName("confirmada_en") val confirmadaEn: Long? = null,
        @SerialName("plazo") val plazo: Long? = null,
    )

    @Serializable
    private data class RevocacionGuardada(
        @SerialName("comprobante") val comprobante: String,
        @SerialName("estado") val estado: String,
        @SerialName("solicitada_en") val solicitadaEn: Long,
        @SerialName("efectiva_en") val efectivaEn: Long,
        @SerialName("canal") val canal: String,
        @SerialName("dispositivo") val dispositivo: String? = null,
        @SerialName("estado_del_aviso") val estadoDelAviso: String,
        @SerialName("entidades") val entidades: List<EntidadDeRevocacionGuardada> = emptyList(),
    ) {
        constructor(r: Revocacion) : this(
            r.comprobante, r.estado, r.solicitadaEn, r.efectivaEn, r.canal, r.dispositivo, r.estadoDelAviso,
            r.entidades.map { EntidadDeRevocacionGuardada(it.codigo, it.nombre, it.estado, it.confirmadaEn, it.plazo) },
        )

        fun aDominio() = Revocacion(
            comprobante, estado, solicitadaEn, efectivaEn, canal, dispositivo, estadoDelAviso,
            entidades.map { EntidadDeRevocacion(it.codigo, it.nombre, it.estado, it.confirmadaEn, it.plazo) },
        )
    }

    @Serializable
    private data class CambioGuardado(
        @SerialName("estado") val estado: String,
        @SerialName("ocurrio_en") val ocurrioEn: Long,
        @SerialName("comprobante") val comprobante: String? = null,
        @SerialName("canal") val canal: String? = null,
        @SerialName("entidad") val entidad: String? = null,
    ) {
        constructor(c: CambioDeEstado) : this(c.estado, c.ocurrioEn, c.comprobante, c.canal, c.entidad)
    }
}
