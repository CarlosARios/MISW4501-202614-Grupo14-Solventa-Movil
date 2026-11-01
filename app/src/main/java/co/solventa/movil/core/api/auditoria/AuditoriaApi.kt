package co.solventa.movil.core.api.auditoria

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Cliente del servicio de auditoria (`audit`) para los consentimientos del
 * titular (HU-M1.3-01). Contrato: `docs/contratos/HU-M1.3-01-api.md` del
 * repositorio de servicios.
 *
 * El titular nunca viaja como parametro: el servicio lo deduce del token de
 * identidad que adjunta el interceptor de `core/auth`. Por eso las dos rutas
 * son de solo lectura y no reciben documento ni identificador de cliente.
 *
 * Devuelven [Response] y no el cuerpo directamente porque cada codigo lleva a
 * un estado distinto de la pantalla: 401 (sesion vencida), 403
 * `holder_not_linked` (la cuenta todavia no esta vinculada a sus
 * consentimientos) y 503 (el servicio no respondio).
 *
 * HU-M1.3-02 agrega la revocacion y su comprobante (contrato
 * `docs/contratos/HU-M1.3-02-api.md`): la revocacion lleva la clave de
 * idempotencia que la aplicacion genera al confirmar y conserva en su cola, asi
 * reenviarla nunca revoca dos veces.
 */
interface AuditoriaApi {

    /** Todos los consentimientos del titular, con detalle e historial. */
    @GET("v1/consents")
    suspend fun consentimientos(): Response<ListaDeConsentimientosDto>

    /** Un consentimiento del titular; uno ajeno responde 404. */
    @GET("v1/consents/{consent_id}")
    suspend fun consentimiento(@Path("consent_id") id: String): Response<ConsentimientoDto>

    /**
     * Revoca un consentimiento activo del titular (HU-M1.3-02). 201 la primera
     * vez, 200 con la misma clave; 409 `consent_already_revoked` o
     * `consent_not_active` cuando ya no se puede revocar.
     */
    @POST("v1/consents/{consent_id}/revocations")
    suspend fun revocar(
        @Path("consent_id") id: String,
        @Header("Idempotency-Key") clave: String,
        @Body solicitud: SolicitudDeRevocacionDto,
    ): Response<RevocacionDto>

    /** El comprobante de la revocacion en PDF, en el idioma pedido. */
    @Streaming
    @GET("v1/consents/{consent_id}/revocation-receipt")
    suspend fun comprobanteDeRevocacion(
        @Path("consent_id") id: String,
        @Header("Accept-Language") idioma: String,
    ): Response<ResponseBody>

    companion object {
        /** Codigo de error de 403 cuando la cuenta todavia no tiene su vinculo. */
        const val ERROR_CUENTA_SIN_VINCULAR = "holder_not_linked"

        /** Codigo de error de 409 cuando otra solicitud ya revoco el consentimiento. */
        const val ERROR_YA_REVOCADO = "consent_already_revoked"

        /** Codigo de error de 409 cuando el consentimiento vencio. */
        const val ERROR_NO_ACTIVO = "consent_not_active"
    }
}
