package co.solventa.movil.core.di

import co.solventa.movil.core.imagen.PreparadorDeCaptura
import co.solventa.movil.core.imagen.PreparadorDeCapturaJpeg
import co.solventa.movil.core.imagen.ReductorDeFoto
import co.solventa.movil.core.imagen.ReductorDeFotoJpeg
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Las capturas de la cámara, listas para enviarse (HU-M1.1-01 y HU-M2.1-01). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ImagenModule {

    @Binds
    @Singleton
    abstract fun enlazarPreparadorDeCaptura(jpeg: PreparadorDeCapturaJpeg): PreparadorDeCaptura

    @Binds
    @Singleton
    abstract fun enlazarReductorDeFoto(jpeg: ReductorDeFotoJpeg): ReductorDeFoto
}
