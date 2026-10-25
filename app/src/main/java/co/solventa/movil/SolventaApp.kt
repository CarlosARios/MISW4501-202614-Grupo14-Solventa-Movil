package co.solventa.movil

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Punto de entrada de la aplicación. La anotación le indica a Hilt que genere
 * aquí el contenedor raíz de dependencias, del que cuelgan los demás.
 *
 * Además configura WorkManager con la fábrica de Hilt, que es lo que permite
 * que un trabajador reciba sus dependencias por constructor: el envío de la
 * evidencia del siniestro necesita el API y la base de datos (HU-M2.1-01).
 */
@HiltAndroidApp
class SolventaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var fabricaDeTrabajadores: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(fabricaDeTrabajadores)
            .build()
}
