package co.solventa.movil.core.api.auditoria

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

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
 */
interface AuditoriaApi {

    /** Todos los consentimientos del titular, con detalle e historial. */
    @GET("v1/consents")
    suspend fun consentimientos(): Response<ListaDeConsentimientosDto>

    /** Un consentimiento del titular; uno ajeno responde 404. */
    @GET("v1/consents/{consent_id}")
    suspend fun consentimiento(@Path("consent_id") id: String): Response<ConsentimientoDto>

    companion object {
        /** Codigo de error de 403 cuando la cuenta todavia no tiene su vinculo. */
        const val ERROR_CUENTA_SIN_VINCULAR = "holder_not_linked"
    }
}
