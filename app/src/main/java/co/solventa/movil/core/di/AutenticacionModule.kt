package co.solventa.movil.core.di

import co.solventa.movil.BuildConfig
import co.solventa.movil.core.auth.cognito.CognitoApi
import co.solventa.movil.core.auth.cognito.CognitoSesionRepositorio
import co.solventa.movil.core.auth.interceptores.ProveedorDeToken
import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import co.solventa.movil.core.auth.sesion.AlmacenDeSesion
import co.solventa.movil.core.auth.sesion.AlmacenDeSesionCifrado
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * La sesión contra el *user pool* de Cognito (HA-S01).
 *
 * Vive en `core/di` y no junto al flujo de ingreso porque ahora enlaza una
 * interfaz de `core` con una implementación de `core`: ya no hay nada de
 * `features` de por medio.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutenticacionModule {

    @Binds
    @Singleton
    abstract fun enlazarRepositorioDeSesion(cognito: CognitoSesionRepositorio): SesionRepositorio

    @Binds
    @Singleton
    abstract fun enlazarAlmacenDeSesion(cifrado: AlmacenDeSesionCifrado): AlmacenDeSesion

    /**
     * Conjunto de limpiezas al cerrar la sesion (HU-M1.3-01). Declararlo aqui
     * permite que este vacio si ningun dominio aporta una.
     */
    @Multibinds
    abstract fun limpiezasAlCerrarSesion(): Set<AlCerrarSesion>

    companion object {

        /**
         * El cliente HTTP con el que se habla con Cognito.
         *
         * **Es uno aparte, y tiene que serlo.** El cliente general lleva el
         * interceptor de autenticación, que para cada petición pide un token
         * vigente; si la llamada que renueva el token pasara por él, pediría
         * un token para poder pedir un token. Como la renovación se serializa
         * con un cerrojo, eso no daría un bucle sino un bloqueo: la
         * aplicación se quedaría esperándose a sí misma.
         *
         * Tampoco lleva el de correlación: ese identificador es del contrato
         * con nuestro backend y no significa nada para AWS.
         */
        @Provides
        @Singleton
        @ClienteDeCognito
        fun proveerClienteDeCognito(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIEMPO_DE_CONEXION_S, TimeUnit.SECONDS)
            .readTimeout(TIEMPO_DE_LECTURA_S, TimeUnit.SECONDS)
            .build()

        @Provides
        @Singleton
        fun proveerApiDeCognito(@ClienteDeCognito cliente: OkHttpClient): CognitoApi {
            val json = Json { ignoreUnknownKeys = true }
            return Retrofit.Builder()
                .baseUrl("https://cognito-idp.${BuildConfig.COGNITO_REGION}.amazonaws.com/")
                .client(cliente)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(CognitoApi::class.java)
        }

        /**
         * Lo que el interceptor necesita de la sesión, sin que dependa del
         * repositorio entero.
         */
        @Provides
        @Singleton
        fun proveerProveedorDeToken(repositorio: CognitoSesionRepositorio): ProveedorDeToken =
            ProveedorDeToken { repositorio.tokenVigente() }

        private const val TIEMPO_DE_CONEXION_S = 10L
        private const val TIEMPO_DE_LECTURA_S = 30L
    }
}

/** Distingue el cliente HTTP de Cognito del que usa el API Gateway. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ClienteDeCognito
