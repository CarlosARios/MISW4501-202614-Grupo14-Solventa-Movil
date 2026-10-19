package co.solventa.movil.core.auth.cognito

import co.solventa.movil.core.auth.sesion.AlmacenDeSesion
import co.solventa.movil.core.auth.sesion.TokensDeSesion

/**
 * El almacén de la sesión sin el Keystore, para poder probar el repositorio
 * en la JVM. El cifrado real se verifica en el dispositivo, que es donde el
 * Keystore existe.
 */
class AlmacenDeSesionEnMemoria(private var guardados: TokensDeSesion? = null) : AlmacenDeSesion {

    private var biometria = true
    var borrados = 0
        private set

    override fun guardarTokens(tokens: TokensDeSesion) {
        guardados = tokens
    }

    override fun tokens(): TokensDeSesion? = guardados

    override fun borrarTokens() {
        guardados = null
        borrados++
    }

    override fun biometriaHabilitada(): Boolean = biometria

    override fun habilitarBiometria(habilitada: Boolean) {
        biometria = habilitada
    }
}
