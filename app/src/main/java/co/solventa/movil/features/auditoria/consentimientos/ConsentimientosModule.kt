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
 * plano.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConsentimientosModule {

    @Binds
    abstract fun enlazarRepositorio(impl: ConsentimientosRepositorioImpl): ConsentimientosRepositorio

    @Binds
    @IntoSet
    abstract fun enlazarLimpieza(impl: ConsentimientosRepositorioImpl): AlCerrarSesion
}

/** El repositorio para el trabajo que crea WorkManager fuera de Hilt. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PuntoDeEntradaDeConsentimientos {
    fun repositorio(): ConsentimientosRepositorio
}
