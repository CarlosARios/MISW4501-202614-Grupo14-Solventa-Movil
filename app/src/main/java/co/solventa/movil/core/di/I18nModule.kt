package co.solventa.movil.core.di

import co.solventa.movil.core.i18n.AlmacenDeIdioma
import co.solventa.movil.core.i18n.AlmacenDeIdiomaEnPreferencias
import co.solventa.movil.core.i18n.IdiomaRepositorio
import co.solventa.movil.core.i18n.IdiomaRepositorioLocal
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** El idioma de la aplicación y dónde se guarda la elección. */
@Module
@InstallIn(SingletonComponent::class)
abstract class I18nModule {

    @Binds
    @Singleton
    abstract fun enlazarRepositorioDeIdioma(local: IdiomaRepositorioLocal): IdiomaRepositorio

    @Binds
    @Singleton
    abstract fun enlazarAlmacenDeIdioma(
        preferencias: AlmacenDeIdiomaEnPreferencias,
    ): AlmacenDeIdioma
}
