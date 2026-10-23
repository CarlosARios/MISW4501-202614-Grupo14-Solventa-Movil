package co.solventa.movil.core.di

import co.solventa.movil.BuildConfig
import co.solventa.movil.core.api.siniestros.SiniestrosApi
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
 * El cliente del servicio de siniestros.
 *
 * Usa el `OkHttpClient` general, que lleva la correlación y la autenticación:
 * la radicación va con la sesión del asegurado.
 */
@Module
@InstallIn(SingletonComponent::class)
object SiniestrosModule {

    @Provides
    @Singleton
    fun proveerApiDeSiniestros(cliente: OkHttpClient): SiniestrosApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(urlBase())
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SiniestrosApi::class.java)
    }

    /**
     * Retrofit exige que la base termine en `/` y `URL_API_GATEWAY` no la
     * trae, así que se normaliza aquí.
     */
    private fun urlBase(): String = BuildConfig.API_GATEWAY_URL.trimEnd('/') + "/"
}
