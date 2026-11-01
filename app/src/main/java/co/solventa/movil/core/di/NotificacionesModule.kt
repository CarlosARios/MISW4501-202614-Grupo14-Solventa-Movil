package co.solventa.movil.core.di

import co.solventa.movil.BuildConfig
import co.solventa.movil.core.api.notificaciones.NotificacionesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

/**
 * Cliente del servicio de notificaciones (HU-M3.1-01). Usa el mismo [OkHttpClient]
 * general que los demas servicios: con el id de correlacion y el token de
 * identidad del asegurado, que es como el servicio sabe de quien es el centro.
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificacionesModule {

    @Provides
    @Singleton
    fun proveerApiDeNotificaciones(cliente: OkHttpClient): NotificacionesApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(urlBase())
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NotificacionesApi::class.java)
    }

    private fun urlBase(): String = BuildConfig.API_GATEWAY_URL.trimEnd('/') + "/"
}
