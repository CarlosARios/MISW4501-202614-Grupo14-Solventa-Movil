package co.solventa.movil.core.di

import co.solventa.movil.BuildConfig
import co.solventa.movil.core.api.auditoria.AuditoriaApi
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
 * Cliente del servicio de auditoria (HU-M1.3-01). Usa el mismo [OkHttpClient]
 * general que los demas servicios: con el id de correlacion y el token de
 * identidad del asegurado, que es como el servicio sabe quien es el titular.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuditoriaModule {

    @Provides
    @Singleton
    fun proveerApiDeAuditoria(cliente: OkHttpClient): AuditoriaApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(urlBase())
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuditoriaApi::class.java)
    }

    private fun urlBase(): String = BuildConfig.API_GATEWAY_URL.trimEnd('/') + "/"
}
