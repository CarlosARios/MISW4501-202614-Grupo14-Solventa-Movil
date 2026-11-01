package co.solventa.movil.core.api.notificaciones

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Cliente del servicio de notificaciones (HU-M3.1-01): el centro de
 * notificaciones del asegurado y el registro del dispositivo. Contrato:
 * `docs/contratos/HU-M3.1-01-api.md` del repositorio de servicios.
 *
 * El titular nunca viaja como parámetro: el servicio lo deduce del token que
 * adjunta el interceptor de `core/auth`. Devuelven [Response] porque el código
 * decide el estado de la pantalla: 401 (sesión vencida), 404 (la notificación
 * no es de esta cuenta) y 503 (el servicio no respondió).
 */
interface NotificacionesApi {

    /** Las notificaciones del asegurado, la más reciente primero. */
    @GET("v1/notifications")
    suspend fun notificaciones(@Query("limit") limite: Int = LIMITE): Response<ListaDeNotificacionesDto>

    /** Una notificación: lo que abre el push (M10b). */
    @GET("v1/notifications/{notification_id}")
    suspend fun notificacion(@Path("notification_id") id: String): Response<NotificacionDto>

    @POST("v1/notifications/{notification_id}/read")
    suspend fun marcarLeida(@Path("notification_id") id: String): Response<Unit>

    /** «Marcar todas como leídas» de M15. */
    @POST("v1/notifications/read-all")
    suspend fun marcarTodasLeidas(): Response<Unit>

    /** Registra la instalación: su token, su idioma y si dio el permiso de notificaciones. */
    @PUT("v1/devices/{installation_id}")
    suspend fun registrarDispositivo(
        @Path("installation_id") instalacion: String,
        @Body dispositivo: DispositivoDto,
    ): Response<Unit>

    companion object {
        /** Cuántas notificaciones trae el centro; el servicio admite hasta 100. */
        const val LIMITE = 100
    }
}
