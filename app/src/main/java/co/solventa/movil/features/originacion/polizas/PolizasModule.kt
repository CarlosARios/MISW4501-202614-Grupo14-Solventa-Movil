package co.solventa.movil.features.originacion.polizas

import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Enlaces de la billetera de pólizas (HU-M1.2-01): el repositorio, su limpieza
 * al cerrar la sesión y el punto de entrada del trabajo en segundo plano.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PolizasModule {

    @Binds
    abstract fun enlazarRepositorio(impl: PolizasRepositorioImpl): PolizasRepositorio

    @Binds
    @IntoSet
    abstract fun enlazarLimpieza(impl: PolizasRepositorioImpl): AlCerrarSesion
}

/** El repositorio para el trabajo que crea WorkManager fuera de Hilt. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PuntoDeEntradaDePolizas {
    fun polizas(): PolizasRepositorio
}
