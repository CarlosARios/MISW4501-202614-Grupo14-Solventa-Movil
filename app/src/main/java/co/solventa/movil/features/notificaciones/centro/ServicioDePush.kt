package co.solventa.movil.features.notificaciones.centro

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * El token con el que Firebase Cloud Messaging alcanza este teléfono
 * (HU-M3.3-01). `null` cuando la aplicación se construyó sin el
 * `google-services.json` (el CI, o una corrida local contra el simulador de
 * *push*): entonces el dispositivo se registra con el id de la instalación,
 * como en HU-M3.1-01.
 */
interface TokenDePush {
    suspend fun token(): String?

    /** Sin Firebase: lo que usan las pruebas que no lo necesitan. */
    object Ninguno : TokenDePush {
        override suspend fun token(): String? = null
    }
}

/** [TokenDePush] de Firebase; un error al pedirlo se trata como si no lo hubiera. */
@Singleton
class TokenDeFirebase @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : TokenDePush {

    override suspend fun token(): String? {
        if (FirebaseApp.getApps(contexto).isEmpty()) return null
        return suspendCancellableCoroutine { continuacion ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
                continuacion.resume(if (tarea.isSuccessful) tarea.result?.takeIf { it.isNotBlank() } else null)
            }
        }
    }
}

/**
 * Recibe los *push* de Firebase Cloud Messaging (HU-M3.3-01). El servicio de
 * notificaciones manda mensajes de solo datos con prioridad alta, que
 * despiertan la aplicación aunque esté cerrada o el teléfono en reposo: el
 * mensaje solo dice que hay algo nuevo, y la revisión de siempre trae el centro
 * y muestra la notificación del sistema, con los textos, los canales y la
 * apertura de la aplicación. Así el aviso llega en segundos y no a los 15
 * minutos de la revisión periódica, que queda como respaldo.
 */
@AndroidEntryPoint
class ServicioDePush : FirebaseMessagingService() {

    @Inject
    lateinit var programador: ProgramadorDeRevision

    override fun onMessageReceived(mensaje: RemoteMessage) {
        programador.revisarAhora()
    }

    /** Firebase cambió el token: la próxima revisión registra el dispositivo con el nuevo. */
    override fun onNewToken(token: String) {
        programador.revisarAhora()
    }
}
