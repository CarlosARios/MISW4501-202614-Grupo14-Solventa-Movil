package co.solventa.movil.features.acceso.registro

import co.solventa.movil.core.imagen.LectorDeLuminancia
import co.solventa.movil.core.imagen.LectorDeLuminanciaJpeg
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
abstract class RegistroModule {

    @Binds
    @Singleton
    abstract fun enlazarRepositorioKyc(
        analizador: AnalizadorDeNitidez,
    ): RegistroKycRepositorio

    @Binds
    abstract fun enlazarLectorDeLuminancia(
        lector: LectorDeLuminanciaJpeg,
    ): LectorDeLuminancia

    @Binds
    @Singleton
    abstract fun enlazarRepositorioDeRegistro(
        http: RegistroRepositorioHttp,
    ): RegistroRepositorio
}
