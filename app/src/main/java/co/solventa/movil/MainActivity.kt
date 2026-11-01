package co.solventa.movil

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import co.solventa.movil.core.auth.biometria.ProveedorDeActividad
import co.solventa.movil.features.notificaciones.centro.AperturaDesdeNotificacion
import co.solventa.movil.ui.theme.SolventaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Extiende `FragmentActivity` y no `ComponentActivity` porque
 * `BiometricPrompt` se monta como un fragmento y lo exige. `FragmentActivity`
 * es una `ComponentActivity`, así que Compose funciona igual.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var actividades: ProveedorDeActividad

    /** La notificación que el usuario tocó para abrir la aplicación (HU-M3.1-01, criterio 4). */
    @Inject
    lateinit var apertura: AperturaDesdeNotificacion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actividades.registrar(this)
        if (savedInstanceState == null) apertura.recibir(intent)
        enableEdgeToEdge()
        setContent {
            SolventaTheme {
                SolventaNavHost()
            }
        }
    }

    /** Con la aplicación ya abierta, tocar una notificación llega por aquí. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        apertura.recibir(intent)
    }

    override fun onDestroy() {
        actividades.olvidar()
        super.onDestroy()
    }
}
