package co.solventa.movil.core.api.siniestros

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * La solicitud de asistencia en sitio (HU-M2.3-01). El contrato está en
 * `docs/contratos/HU-M2.3-01-api.md` del repositorio de servicios.
 *
 * Los campos opcionales tienen `null` por omisión y el `Json` del cliente no
 * escribe los valores por omisión: así una ubicación `unavailable` viaja sin
 * coordenadas y una `sent` sin motivo, que es lo que el contrato exige.
 */

/** Cuerpo de `POST /v1/claims/assistance-requests`. */
@Serializable
data class SolicitudDeAsistenciaDto(
    @SerialName("policy_id") val poliza: String,
    @SerialName("location") val ubicacion: UbicacionDeAsistenciaDto,
    /** La dirección legible del geocodificador del teléfono, de 3 a 160 caracteres. */
    @SerialName("place") val lugar: String? = null,
)

/** El resultado de la ubicación: `sent` con coordenadas o `unavailable` con su motivo. */
@Serializable
data class UbicacionDeAsistenciaDto(
    @SerialName("status") val estado: String,
    @SerialName("latitude") val latitud: Double? = null,
    @SerialName("longitude") val longitud: Double? = null,
    @SerialName("accuracy_m") val precisionM: Double? = null,
    /** Instante del *fix*, en RFC 3339 y en UTC. */
    @SerialName("captured_at") val capturadaEn: String? = null,
    @SerialName("unavailable_reason") val motivo: String? = null,
)

/** Respuesta `201`/`200` del `POST` y `200` del `GET`. */
@Serializable
data class RespuestaDeAsistenciaDto(
    @SerialName("request_id") val id: String,
    /** El número que se le muestra al asegurado: `AST-<año>-<cuatro dígitos o más>`. */
    @SerialName("request_number") val numero: String,
    @SerialName("status") val estado: String,
    @SerialName("policy") val poliza: PolizaDeAsistenciaDto? = null,
    @SerialName("location") val ubicacion: UbicacionDeAsistenciaDto? = null,
    @SerialName("place") val lugar: String? = null,
    /** Cuándo la registró el servidor, en RFC 3339 y en UTC. */
    @SerialName("requested_at") val registradaEn: String,
)

/** La póliza tal como la describió el núcleo de pólizas al registrar. */
@Serializable
data class PolizaDeAsistenciaDto(
    @SerialName("policy_id") val id: String,
    @SerialName("line") val linea: String? = null,
    @SerialName("product") val producto: String? = null,
    @SerialName("insured_object") val objetoAsegurado: String? = null,
)

/** El cuerpo de un error: `error` y, en `policy_not_eligible`, su `reason`. */
@Serializable
data class ErrorDeAsistenciaDto(
    @SerialName("error") val error: String,
    @SerialName("reason") val motivo: String? = null,
)
