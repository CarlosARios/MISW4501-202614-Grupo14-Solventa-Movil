package co.solventa.movil.features.siniestros.radicacion

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
    abstract fun enlazarRepositorioDePolizas(
        demostracion: PolizasDeDemostracion,
    ): PolizasRepositorio
}
