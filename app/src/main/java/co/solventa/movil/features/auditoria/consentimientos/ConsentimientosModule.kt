package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Enlaces de la consulta de consentimientos (HU-M1.3-01): el repositorio, su
 * limpieza al cerrar la sesion y el punto de entrada del trabajo en segundo
 * plano. HU-M1.3-02 agrega la revocacion: su repositorio, el envio de la cola
 * y su trabajo urgente. La cola **no** se registra como limpieza al cerrar la
 * sesion: queda del titular y se envia cuando vuelva a entrar (D-7 a).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConsentimientosModule {

    @Binds
    abstract fun enlazarRepositorio(impl: ConsentimientosRepositorioImpl): ConsentimientosRepositorio

    @Binds
    @IntoSet
    abstract fun enlazarLimpieza(impl: ConsentimientosRepositorioImpl): AlCerrarSesion

    @Binds
    abstract fun enlazarRevocaciones(impl: RevocacionRepositorioImpl): RevocacionRepositorio

    @Binds
    abstract fun enlazarEnviador(impl: RevocacionRepositorioImpl): EnviadorDeRevocaciones

    @Binds
    abstract fun enlazarProgramadorDeEnvio(impl: ProgramadorDeEnvioDeRevocacionesWorkManager): ProgramadorDeEnvioDeRevocaciones

    @Binds
    abstract fun enlazarGuardadorDeComprobante(impl: GuardadorDeComprobanteAndroid): GuardadorDeComprobante
}

/** El repositorio para el trabajo que crea WorkManager fuera de Hilt. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PuntoDeEntradaDeConsentimientos {
    fun repositorio(): ConsentimientosRepositorio
}

/** El repositorio de la revocacion para su trabajo urgente (HU-M1.3-02). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PuntoDeEntradaDeRevocaciones {
    fun revocaciones(): RevocacionRepositorio
}
