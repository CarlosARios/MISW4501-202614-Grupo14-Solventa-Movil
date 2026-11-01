package co.solventa.movil.features.notificaciones.centro

import co.solventa.movil.core.auth.sesion.AlCerrarSesion
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Enlaces del centro de notificaciones (HU-M3.1-01): el repositorio y su
 * limpieza al cerrar la sesión, la instalación, el permiso, el aviso del
 * sistema y la revisión periódica.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificacionesModule {

    @Binds
    abstract fun enlazarRepositorio(impl: NotificacionesRepositorioImpl): NotificacionesRepositorio

    @Binds
    @IntoSet
    abstract fun enlazarLimpieza(impl: NotificacionesRepositorioImpl): AlCerrarSesion

    @Binds
    abstract fun enlazarInstalacion(impl: IdentidadDeInstalacionEnPreferencias): IdentidadDeInstalacion

    @Binds
    abstract fun enlazarPermiso(impl: PermisoDeNotificacionesDelSistema): PermisoDeNotificaciones

    @Binds
    abstract fun enlazarAvisador(impl: AvisadorDelSistema): AvisadorDeNotificaciones

    @Binds
    abstract fun enlazarProgramador(impl: ProgramadorDeRevisionWorkManager): ProgramadorDeRevision
}

/** El revisor para el trabajo que crea WorkManager fuera de Hilt. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PuntoDeEntradaDeNotificaciones {
    fun revisorDeNotificaciones(): RevisorDeNotificaciones
}
