package co.solventa.movil.core.di

import co.solventa.movil.core.auth.interceptores.InterceptorDeAutenticacion
import co.solventa.movil.core.errores.InterceptorDeCorrelacion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * El cliente HTTP que usarán todos los servicios de `core/api`.
 *
 * **El orden de los interceptores importa y es el mismo que el del cliente
 * web:** correlación → autenticación → errores. La correlación va primero
 * para que el identificador exista aunque la autenticación rechace la
 * petición antes de enviarla; los errores van al final para ver la respuesta
 * ya resuelta.
 *
 * El de errores entra con el primer contrato de API.
 *
 * **Sin `HttpLoggingInterceptor`, a propósito.** Lo que hace falta registrar
 * —método, ruta, código, duración y correlación— ya lo deja
 * [InterceptorDeCorrelacion] en todas las variantes, y sin arriesgarse a
 * volcar encabezados: por ahí viaja el token. Además, el de OkHttp solo se
 * declara en `debugImplementation`, así que referenciarlo tras un
 * `if (BuildConfig.DEBUG)` no compila en `release`: la comprobación es en
 * ejecución, pero la clase tiene que existir al compilar.
 */
@Module
@InstallIn(SingletonComponent::class)
object RedModule {

    @Provides
    @Singleton
    fun proveerClienteHttp(autenticacion: InterceptorDeAutenticacion): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(InterceptorDeCorrelacion())
        .addInterceptor(autenticacion)
        .connectTimeout(TIEMPO_DE_CONEXION_S, TimeUnit.SECONDS)
        .readTimeout(TIEMPO_DE_LECTURA_S, TimeUnit.SECONDS)
        .build()

    /**
     * Tiempos de espera del lado del cliente. El presupuesto del backend para
     * el recorrido de cotización es de 700 ms sobre el conjunto de fuentes
     * (HU-W1.2-02), así que estos márgenes son holgados a propósito: cubren
     * una red móvil mala, no una falla del servicio.
     */
    private const val TIEMPO_DE_CONEXION_S = 10L
    private const val TIEMPO_DE_LECTURA_S = 30L
}
