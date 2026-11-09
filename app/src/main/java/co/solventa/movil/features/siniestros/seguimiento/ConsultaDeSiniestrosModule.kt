package co.solventa.movil.features.siniestros.seguimiento

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Enlace de la consulta de siniestros (HU-M2.4-01, HA-L02). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConsultaDeSiniestrosModule {

    @Binds
    abstract fun enlazarRepositorio(impl: ConsultaDeSiniestrosRepositorioHttp): ConsultaDeSiniestrosRepositorio
}
