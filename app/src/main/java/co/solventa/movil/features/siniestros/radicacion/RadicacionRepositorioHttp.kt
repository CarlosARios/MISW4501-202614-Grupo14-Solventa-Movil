package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.core.api.siniestros.ErrorDeRadicacion
import co.solventa.movil.core.api.siniestros.RespuestaDeRadicacion
import co.solventa.movil.core.api.siniestros.SiniestrosApi
import co.solventa.movil.core.api.siniestros.SolicitudDeRadicacion
import co.solventa.movil.core.errores.Correlacion
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La radicación contra `POST /v1/claims` del API Gateway (HU-M2.2-01).
 *
 * Traduce lo que el asistente reunió a lo que el contrato recibe —el instante
 * en RFC 3339 y el tipo en el vocabulario del contrato— y los códigos de
 * estado a los caminos del flujo.
 *
 * **Un `409` no existe en este contrato.** Repetir la radicación con la misma
 * clave devuelve `200` con el caso que ya se creó, así que el reintento es un
 * éxito y no un conflicto.
 */
@Singleton
class RadicacionRepositorioHttp @Inject constructor(
    private val api: SiniestrosApi,
) : RadicacionRepositorio {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun radicar(borrador: String, radicacion: Radicacion): ResultadoRadicacion {
        val correlacion = Correlacion.nuevo()
        return try {
            interpretar(api.radicar(borrador, correlacion, solicitud(radicacion)), correlacion)
        } catch (sinRed: IOException) {
            ResultadoRadicacion.NoDisponible(correlacion)
        } catch (ilegible: SerializationException) {
            ResultadoRadicacion.NoDisponible(correlacion)
        }
    }

    private fun interpretar(
        respuesta: Response<RespuestaDeRadicacion>,
        correlacion: String,
    ): ResultadoRadicacion {
        val cuerpo = respuesta.body()
        return when {
            respuesta.isSuccessful && cuerpo != null -> ResultadoRadicacion.Exitoso(
                CasoRadicado(
                    casoId = cuerpo.casoId,
                    radicado = cuerpo.radicado,
                    radicadoEn = cuerpo.radicadoEn,
                ),
            )

            respuesta.code() == DATO_INVALIDO -> camposDe(respuesta, correlacion)
            else -> ResultadoRadicacion.NoDisponible(correlacion)
        }
    }

    /**
     * Un `422` trae los campos que el servicio rechazó, salvo cuando la clave
     * de idempotencia se reutilizó con otra radicación: eso no es un dato que
     * el usuario pueda corregir, así que se trata como falla reintentable con
     * otra clave.
     */
    private fun camposDe(respuesta: Response<*>, correlacion: String): ResultadoRadicacion {
        val detalle = respuesta.errorBody()?.string().orEmpty()
        val error = runCatching { json.decodeFromString<ErrorDeRadicacion>(detalle) }.getOrNull()
            ?: return ResultadoRadicacion.DatosInvalidos(CampoDeRadicacion.entries.toSet())

        if (error.error == CLAVE_REUTILIZADA) {
            return ResultadoRadicacion.NoDisponible(correlacion)
        }
        val campos = error.campos.mapNotNull { CAMPOS[it.campo] }.toSet()
        return ResultadoRadicacion.DatosInvalidos(
            campos.ifEmpty { CampoDeRadicacion.entries.toSet() },
        )
    }

    private fun solicitud(r: Radicacion) = SolicitudDeRadicacion(
        poliza = r.poliza.trim(),
        tipoDeIncidente = r.tipo?.valorDelContrato.orEmpty(),
        ocurrioEn = r.ocurrioEn?.let { Instant.ofEpochMilli(it).toString() }.orEmpty(),
        lugar = r.lugar.trim(),
        descripcion = r.descripcion.trim(),
        detalles = r.respuestas,
        declaracionAceptada = r.declaracionAceptada,
    )

    private companion object {
        const val DATO_INVALIDO = 422
        const val CLAVE_REUTILIZADA = "idempotency_key_reused"

        /** Los nombres del contrato, en el orden de las etapas. */
        val CAMPOS = mapOf(
            "policy_id" to CampoDeRadicacion.POLIZA,
            "incident_type" to CampoDeRadicacion.TIPO,
            "occurred_at" to CampoDeRadicacion.FECHA,
            "place" to CampoDeRadicacion.LUGAR,
            "description" to CampoDeRadicacion.DESCRIPCION,
            "details" to CampoDeRadicacion.RESPUESTAS,
            "declaration_accepted" to CampoDeRadicacion.DECLARACION,
        )
    }
}
