package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.core.ubicacion.VerificadorDeUbicacion
import co.solventa.movil.core.ubicacion.VerificadorDeUbicacionDelSistema
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * La solicitud de asistencia en sitio (HU-M2.3-01). El módulo vive junto al
 * flujo y no en `core/di`, porque `core` no puede importar de `features`.
 * Las pólizas y el usuario actual ya los enlaza `RadicacionModule`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AsistenciasModule {

    @Binds
    @Singleton
    abstract fun enlazarRepositorio(impl: AsistenciasRepositorioImpl): AsistenciasRepositorio

    @Binds
    @Singleton
    abstract fun enlazarProgramador(impl: ProgramadorDeEnvioDeAsistenciasWorkManager): ProgramadorDeEnvioDeAsistencias

    @Binds
    @Singleton
    abstract fun enlazarTraductor(impl: TraductorConGeocoder): TraductorDeCoordenadas

    @Binds
    @Singleton
    abstract fun enlazarAvisador(impl: AvisadorDeAsistenciaDelSistema): AvisadorDeAsistencia

    @Binds
    @Singleton
    abstract fun enlazarVerificador(impl: VerificadorDeUbicacionDelSistema): VerificadorDeUbicacion
}

/** Para que [EnvioDeAsistenciasWorker], que crea WorkManager, tome el repositorio de Hilt. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PuntoDeEntradaDeAsistencias {
    fun asistencias(): AsistenciasRepositorio
}
