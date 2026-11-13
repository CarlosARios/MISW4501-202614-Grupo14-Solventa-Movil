package co.solventa.movil.features.siniestros.seguimiento

import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Enlaces de la consulta de siniestros (HU-M2.4-01, HA-L02): el repositorio,
 * la copia sin conexión y su limpieza al cerrar la sesión.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConsultaDeSiniestrosModule {

    @Binds
    abstract fun enlazarRepositorio(impl: ConsultaDeSiniestrosRepositorioHttp): ConsultaDeSiniestrosRepositorio

    @Binds
    abstract fun enlazarCopia(impl: CopiaLocalDeSiniestrosRoom): CopiaLocalDeSiniestros

    @Binds
    @IntoSet
    abstract fun enlazarLimpieza(impl: CopiaLocalDeSiniestrosRoom): AlCerrarSesion
}
