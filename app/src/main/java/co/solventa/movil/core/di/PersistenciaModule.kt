package co.solventa.movil.core.di

import android.content.Context
import androidx.room.Room
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.dao.BorradorDeRadicacionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * La base de datos del dispositivo y sus DAO.
 *
 * Vive en `core/di` porque la base es de `core`: cualquier flujo puede
 * necesitarla y ninguno es su dueño.
 */
@Module
@InstallIn(SingletonComponent::class)
object PersistenciaModule {

    @Provides
    @Singleton
    fun proveerBaseDeDatos(@ApplicationContext contexto: Context): BaseDeDatosSolventa =
        Room.databaseBuilder(contexto, BaseDeDatosSolventa::class.java, BaseDeDatosSolventa.NOMBRE)
            .build()

    @Provides
    fun proveerBorradoresDeRadicacion(base: BaseDeDatosSolventa): BorradorDeRadicacionDao =
        base.borradoresDeRadicacion()
}
