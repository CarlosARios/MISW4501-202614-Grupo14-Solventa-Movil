package co.solventa.movil.core.di

import co.solventa.movil.BuildConfig
import co.solventa.movil.core.api.originacion.OriginacionApi
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
 * El cliente de originación, el primero que habla con nuestro API Gateway.
 *
 * Usa el `OkHttpClient` general —correlación y autenticación— y no el de
 * Cognito: estas peticiones sí llevan el token cuando hay sesión. El
 * registro es la excepción y no necesita trato aparte, porque sin sesión el
 * interceptor deja la petición como está.
 */
@Module
@InstallIn(SingletonComponent::class)
object OriginacionModule {

    @Provides
    @Singleton
    fun proveerApiDeOriginacion(cliente: OkHttpClient): OriginacionApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(urlBase())
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OriginacionApi::class.java)
    }

    /**
     * Retrofit exige que la base termine en `/` y `URL_API_GATEWAY` no la
     * trae, así que se normaliza aquí en lugar de pedirle al que compila que
     * la escriba de una forma concreta.
     */
    private fun urlBase(): String = BuildConfig.API_GATEWAY_URL.trimEnd('/') + "/"
}
