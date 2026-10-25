package co.solventa.movil.core.sincronizacion

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Si el dispositivo tiene conexion a internet (HU-M1.3-01, criterio 5). Lo que
 * debe funcionar sin red lo observa para avisar que esta mostrando la copia
 * local y para actualizarse solo cuando la red vuelve.
 */
interface ObservadorDeConectividad {
    val conectado: StateFlow<Boolean>
}

/**
 * [ObservadorDeConectividad] sobre la red por defecto del sistema. Una red
 * cuenta como conectada cuando ofrece internet y el sistema la valido (un
 * portal cautivo sin validar no cuenta).
 */
@Singleton
class ObservadorDeConectividadAndroid @Inject constructor(
    @ApplicationContext contexto: Context,
) : ObservadorDeConectividad {

    private val gestor = contexto.getSystemService(ConnectivityManager::class.java)

    private val _conectado = MutableStateFlow(estadoActual())
    override val conectado: StateFlow<Boolean> = _conectado.asStateFlow()

    init {
        gestor.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                    _conectado.value = conInternet(capacidades)
                }

                override fun onLost(network: Network) {
                    _conectado.value = false
                }
            },
        )
    }

    private fun estadoActual(): Boolean =
        gestor.getNetworkCapabilities(gestor.activeNetwork)?.let(::conInternet) ?: false

    private fun conInternet(capacidades: NetworkCapabilities): Boolean =
        capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
