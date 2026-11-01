package co.solventa.movil.features.notificaciones.centro

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El identificador de esta instalación de la aplicación (HU-M3.1-01): con él
 * el servicio sabe a qué dispositivo va cada *push*. Se crea una vez y
 * sobrevive a cerrar la sesión; reinstalar la aplicación crea otro.
 */
interface IdentidadDeInstalacion {
    fun id(): String
}

/** [IdentidadDeInstalacion] en las preferencias de la aplicación. */
@Singleton
class IdentidadDeInstalacionEnPreferencias @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : IdentidadDeInstalacion {

    private val preferencias by lazy { contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE) }

    @Synchronized
    override fun id(): String = preferencias.getString(CLAVE, null) ?: ("inst-" + UUID.randomUUID()).also {
        preferencias.edit { putString(CLAVE, it) }
    }

    private companion object {
        const val ARCHIVO = "instalacion-solventa"
        const val CLAVE = "id"
    }
}

/**
 * Si el usuario permite que la aplicación muestre notificaciones (HU-M3.1-01,
 * criterios 3 y 5): el permiso de Android 13 en adelante y el interruptor de
 * los ajustes del sistema en cualquier versión.
 */
interface PermisoDeNotificaciones {
    fun concedido(): Boolean
}

@Singleton
class PermisoDeNotificacionesDelSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : PermisoDeNotificaciones {
    override fun concedido(): Boolean = NotificationManagerCompat.from(contexto).areNotificationsEnabled()
}
