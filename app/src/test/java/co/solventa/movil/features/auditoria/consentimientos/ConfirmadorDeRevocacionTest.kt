package co.solventa.movil.features.auditoria.consentimientos

import co.solventa.movil.core.auth.biometria.AutenticadorBiometrico
import co.solventa.movil.core.auth.biometria.DisponibilidadBiometrica
import co.solventa.movil.core.auth.biometria.ResultadoBiometrico
import co.solventa.movil.core.auth.sesion.VerificacionDeContrasena
import co.solventa.movil.core.auth.sesion.VerificadorDeContrasena
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La eleccion y el resultado de la confirmacion (HU-M1.3-02, D-6 a): la
 * huella solo si el telefono la tiene y el titular la habilito en Solventa;
 * si no, la contraseña.
 */
class ConfirmadorDeRevocacionTest {

    private val biometria = BiometriaFalsa()
    private val sesion = SesionFalsa()
    private val contrasena = VerificadorFalso()

    private fun confirmador() = ConfirmadorDeRevocacion(biometria, sesion, contrasena)

    @Test
    fun `la biometria se usa solo si esta disponible y habilitada en Solventa`() = runTest {
        biometria.disponibilidad = DisponibilidadBiometrica.DISPONIBLE
        sesion.biometria = true
        assertEquals(MetodoDeConfirmacion.BIOMETRIA, confirmador().metodo())

        sesion.biometria = false
        assertEquals(MetodoDeConfirmacion.CONTRASENA, confirmador().metodo())

        sesion.biometria = true
        biometria.disponibilidad = DisponibilidadBiometrica.NO_CONFIGURADA
        assertEquals(MetodoDeConfirmacion.CONTRASENA, confirmador().metodo())
        biometria.disponibilidad = DisponibilidadBiometrica.SIN_HARDWARE
        assertEquals(MetodoDeConfirmacion.CONTRASENA, confirmador().metodo())
    }

    @Test
    fun `solo una huella reconocida confirma`() = runTest {
        mapOf(
            ResultadoBiometrico.Exito to ResultadoDeConfirmacion.Confirmada,
            ResultadoBiometrico.NoReconocido to ResultadoDeConfirmacion.HuellaNoReconocida,
            ResultadoBiometrico.Cancelado to ResultadoDeConfirmacion.NoConfirmada,
            ResultadoBiometrico.NoDisponible to ResultadoDeConfirmacion.NoConfirmada,
        ).forEach { (biometrico, esperado) ->
            biometria.resultado = biometrico
            assertEquals(biometrico.toString(), esperado, confirmador().conBiometria())
        }
    }

    @Test
    fun `la contraseña confirma, no confirma o pide red`() = runTest {
        mapOf(
            VerificacionDeContrasena.Correcta to ResultadoDeConfirmacion.Confirmada,
            VerificacionDeContrasena.Incorrecta to ResultadoDeConfirmacion.ContrasenaIncorrecta,
            VerificacionDeContrasena.SinConexion to ResultadoDeConfirmacion.SinConexion,
        ).forEach { (verificacion, esperado) ->
            contrasena.resultado = verificacion
            assertEquals(verificacion.toString(), esperado, confirmador().conContrasena("unaClave"))
        }
        assertEquals(listOf("unaClave", "unaClave", "unaClave"), contrasena.recibidas)
    }
}

/** El sensor biometrico: disponibilidad y respuesta fijadas por la prueba. */
class BiometriaFalsa : AutenticadorBiometrico {
    var disponibilidad = DisponibilidadBiometrica.DISPONIBLE
    var resultado: ResultadoBiometrico = ResultadoBiometrico.Exito
    var llamadas = 0
    override fun disponibilidad(): DisponibilidadBiometrica = disponibilidad
    override suspend fun autenticar(): ResultadoBiometrico {
        llamadas++
        return resultado
    }
}

/** La verificacion de la contraseña: respuesta fijada por la prueba. */
class VerificadorFalso : VerificadorDeContrasena {
    var resultado: VerificacionDeContrasena = VerificacionDeContrasena.Correcta
    val recibidas = mutableListOf<String>()
    override suspend fun verificarContrasena(contrasena: String): VerificacionDeContrasena {
        recibidas += contrasena
        return resultado
    }
}
