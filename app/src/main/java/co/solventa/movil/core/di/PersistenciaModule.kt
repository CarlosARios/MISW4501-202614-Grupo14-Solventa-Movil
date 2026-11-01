package co.solventa.movil.core.di

import android.content.Context
import androidx.room.Room
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.dao.BorradorDeRadicacionDao
import co.solventa.movil.core.persistencia.cifrado.CifradorAesGcm
import co.solventa.movil.core.persistencia.cifrado.CifradorDeDatos
import co.solventa.movil.core.persistencia.cifrado.ClaveDelKeystore
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import co.solventa.movil.core.persistencia.dao.PolizaDao
import co.solventa.movil.core.persistencia.dao.NotificacionDao
import co.solventa.movil.core.persistencia.dao.RevocacionPendienteDao
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

    @Provides
    fun proveerConsentimientos(base: BaseDeDatosSolventa): ConsentimientoDao = base.consentimientos()

    @Provides
    fun proveerRevocacionesPendientes(base: BaseDeDatosSolventa): RevocacionPendienteDao = base.revocacionesPendientes()

    @Provides
    fun proveerPolizas(base: BaseDeDatosSolventa): PolizaDao = base.polizas()

    /** El cifrado de lo que se guarda con contenido sensible (HU-M1.2-01). */
    @Provides
    @Singleton
    fun proveerCifradorDeDatos(): CifradorDeDatos = CifradorAesGcm(ClaveDelKeystore::obtener)

    @Provides
    fun proveerNotificaciones(base: BaseDeDatosSolventa): NotificacionDao = base.notificaciones()
}
