package co.solventa.movil.features.siniestros.radicacion

import co.solventa.movil.features.siniestros.UsuarioActual
import co.solventa.movil.features.siniestros.UsuarioDeLaSesion
import co.solventa.movil.features.siniestros.radicacion.envio.AvisadorDeRadicacion
import co.solventa.movil.features.siniestros.radicacion.envio.AvisadorDeRadicacionDelSistema
import co.solventa.movil.features.siniestros.radicacion.envio.ColaDeRadicacion
import co.solventa.movil.features.siniestros.radicacion.envio.ColaDeRadicacionConWorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * El módulo vive junto al flujo y no en `core/di`, porque `core` no puede
 * importar de `features`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RadicacionModule {

    @Binds
    @Singleton
    abstract fun enlazarRepositorioDeRadicacion(
        http: RadicacionRepositorioHttp,
    ): RadicacionRepositorio

    @Binds
    @Singleton
    abstract fun enlazarRepositorioDeBorradores(
        local: BorradorRepositorioLocal,
    ): BorradorRepositorio

    @Binds
    @Singleton
    abstract fun enlazarUsuarioActual(sesion: UsuarioDeLaSesion): UsuarioActual

    @Binds
    @Singleton
    abstract fun enlazarColaDeRadicacion(
        conWorkManager: ColaDeRadicacionConWorkManager,
    ): ColaDeRadicacion

    @Binds
    @Singleton
    abstract fun enlazarAvisadorDeRadicacion(
        delSistema: AvisadorDeRadicacionDelSistema,
    ): AvisadorDeRadicacion

    @Binds
    @Singleton
    abstract fun enlazarRepositorioDePolizas(
        billetera: PolizasDeLaBilletera,
    ): PolizasRepositorio
}
