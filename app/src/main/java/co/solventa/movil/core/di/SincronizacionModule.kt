package co.solventa.movil.core.di

import co.solventa.movil.core.sincronizacion.ObservadorDeConectividad
import co.solventa.movil.core.sincronizacion.ObservadorDeConectividadAndroid
import co.solventa.movil.core.sincronizacion.ProgramadorDeSincronizacion
import co.solventa.movil.core.sincronizacion.ProgramadorDeSincronizacionWorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Conectividad y sincronizacion en segundo plano (HU-M1.3-01). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SincronizacionModule {

    @Binds
    @Singleton
    abstract fun enlazarObservadorDeConectividad(android: ObservadorDeConectividadAndroid): ObservadorDeConectividad

    @Binds
    @Singleton
    abstract fun enlazarProgramadorDeSincronizacion(
        workManager: ProgramadorDeSincronizacionWorkManager,
    ): ProgramadorDeSincronizacion
}
