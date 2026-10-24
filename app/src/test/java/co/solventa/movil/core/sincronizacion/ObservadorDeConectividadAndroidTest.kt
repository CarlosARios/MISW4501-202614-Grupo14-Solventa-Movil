package co.solventa.movil.core.sincronizacion

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * La conectividad sobre la red por defecto del sistema (Robolectric,
 * HU-M1.3-01): solo una red con internet validado cuenta como conectada.
 */
@RunWith(AndroidJUnit4::class)
class ObservadorDeConectividadAndroidTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val gestor = contexto.getSystemService(ConnectivityManager::class.java)
    private val red = ShadowNetwork.newInstance(7)

    private fun capacidades(vararg cuales: Int): NetworkCapabilities =
        ShadowNetworkCapabilities.newInstance().also { c -> cuales.forEach { shadowOf(c).addCapability(it) } }

    private fun avisar(accion: (ConnectivityManager.NetworkCallback) -> Unit) =
        shadowOf(gestor).networkCallbacks.forEach(accion)

    @Test
    fun `una red con internet validado esta conectada y al perderla deja de estarlo`() {
        val observador = ObservadorDeConectividadAndroid(contexto)

        avisar {
            it.onCapabilitiesChanged(
                red,
                capacidades(NetworkCapabilities.NET_CAPABILITY_INTERNET, NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            )
        }
        assertTrue(observador.conectado.value)

        avisar { it.onLost(red) }
        assertFalse(observador.conectado.value)
    }

    @Test
    fun `una red sin validar no cuenta como conectada`() {
        val observador = ObservadorDeConectividadAndroid(contexto)

        avisar { it.onCapabilitiesChanged(red, capacidades(NetworkCapabilities.NET_CAPABILITY_INTERNET)) }

        assertFalse(observador.conectado.value)
    }

    @Test
    fun `el estado inicial sale de la red activa`() {
        shadowOf(gestor).setNetworkCapabilities(
            gestor.activeNetwork,
            capacidades(NetworkCapabilities.NET_CAPABILITY_INTERNET, NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        )

        assertTrue(ObservadorDeConectividadAndroid(contexto).conectado.value)
    }
}
