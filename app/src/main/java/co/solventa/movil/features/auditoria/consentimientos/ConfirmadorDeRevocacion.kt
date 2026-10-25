package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.core.auth.biometria.AutenticadorBiometrico
import co.solventa.movil.core.auth.biometria.DisponibilidadBiometrica
import co.solventa.movil.core.auth.biometria.ResultadoBiometrico
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.auth.sesion.VerificacionDeContrasena
import co.solventa.movil.core.auth.sesion.VerificadorDeContrasena
import javax.inject.Inject

/** Resultado de pedirle al titular que confirme la revocacion (criterios 1 y 2). */
sealed interface ResultadoDeConfirmacion {
    data object Confirmada : ResultadoDeConfirmacion

    /** Cancelo, no se reconocio o la contraseña no es la suya: nada cambia (criterio 2). */
    data object NoConfirmada : ResultadoDeConfirmacion

    /** La contraseña se verifica con el proveedor de identidad y no hay red. */
    data object SinConexion : ResultadoDeConfirmacion
}

/**
 * Elige y ejecuta la confirmacion de la revocacion (HU-M1.3-02, decision
 * D-6 a): la biometria si el telefono la tiene y el titular la habilito en
 * Solventa; si no, la contraseña de Solventa verificada con Cognito.
 *
 * Usa el [AutenticadorBiometrico] de HU-M1.1-02 sin cambios: el dialogo del
 * sistema pide la huella y el efecto de la revocacion lo explica la hoja.
 */
class ConfirmadorDeRevocacion @Inject constructor(
    private val biometria: AutenticadorBiometrico,
    private val sesion: SesionRepositorio,
    private val contrasena: VerificadorDeContrasena,
) {

    suspend fun metodo(): MetodoDeConfirmacion =
        if (biometria.disponibilidad() == DisponibilidadBiometrica.DISPONIBLE && sesion.biometriaHabilitada()) {
            MetodoDeConfirmacion.BIOMETRIA
        } else {
            MetodoDeConfirmacion.CONTRASENA
        }

    suspend fun conBiometria(): ResultadoDeConfirmacion = when (biometria.autenticar()) {
        ResultadoBiometrico.Exito -> ResultadoDeConfirmacion.Confirmada
        ResultadoBiometrico.NoReconocido, ResultadoBiometrico.Cancelado, ResultadoBiometrico.NoDisponible ->
            ResultadoDeConfirmacion.NoConfirmada
    }

    suspend fun conContrasena(valor: String): ResultadoDeConfirmacion = when (contrasena.verificarContrasena(valor)) {
        VerificacionDeContrasena.Correcta -> ResultadoDeConfirmacion.Confirmada
        VerificacionDeContrasena.Incorrecta -> ResultadoDeConfirmacion.NoConfirmada
        VerificacionDeContrasena.SinConexion -> ResultadoDeConfirmacion.SinConexion
    }
}
