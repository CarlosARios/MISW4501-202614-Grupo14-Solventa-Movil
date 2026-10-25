package co.solventa.movil.features.siniestros.evidencia

import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.dao.EvidenciaCapturadaDao
import co.solventa.movil.core.ubicacion.Localizador
import co.solventa.movil.core.ubicacion.LocalizadorDelSistema
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * La captura de evidencia (HU-M2.1-01). El módulo vive junto al flujo porque
 * enlaza interfaces de `features` con sus implementaciones.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EvidenciaModule {

    @Binds
    @Singleton
    abstract fun enlazarRepositorio(local: EvidenciaRepositorioLocal): EvidenciaRepositorio

    @Binds
    @Singleton
    abstract fun enlazarHuella(sha256: HuellaSha256): HuellaDeArchivo

    @Binds
    @Singleton
    abstract fun enlazarEnvio(conWorkManager: EnvioConWorkManager): EnvioDeEvidencia

    @Binds
    @Singleton
    abstract fun enlazarLocalizador(delSistema: LocalizadorDelSistema): Localizador

    companion object {
        @Provides
        fun proveerEvidencias(base: BaseDeDatosSolventa): EvidenciaCapturadaDao =
            base.evidenciasCapturadas()
    }
}
