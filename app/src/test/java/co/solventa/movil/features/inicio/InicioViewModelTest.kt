package co.solventa.movil.features.inicio

import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * M3 · Inicio, versión provisional: a quién saluda y qué deja al salir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    private val despachador = StandardTestDispatcher()

    @Before
    fun antes() {
        Dispatchers.setMain(despachador)
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    /**
     * El nombre viene del *id token* del *user pool*, así que ver el nombre
     * correcto en pantalla es la señal de que el ingreso fue real.
     */
    @Test
    fun `saluda con la identidad que dejo el ingreso`() = runTest(despachador) {
        val repositorio = RepositorioFalso(Sesion(correo = "asegurado@example.com", nombre = "Carolina"))

        val vm = InicioViewModel(repositorio)
        testScheduler.advanceUntilIdle()

        assertEquals("Carolina", vm.sesion.value?.nombre)
        assertEquals("asegurado@example.com", vm.sesion.value?.correo)
    }

    @Test
    fun `sin sesion guardada no hay a quien saludar`() = runTest(despachador) {
        val vm = InicioViewModel(RepositorioFalso(null))
        testScheduler.advanceUntilIdle()

        assertNull(vm.sesion.value)
    }

    @Test
    fun `cerrar sesion borra los tokens y avisa a quien navega`() = runTest(despachador) {
        val repositorio = RepositorioFalso(Sesion(correo = "asegurado@example.com", nombre = "Carolina"))
        val vm = InicioViewModel(repositorio)
        testScheduler.advanceUntilIdle()
        var navego = false

        vm.cerrarSesion { navego = true }
        testScheduler.advanceUntilIdle()

        assertTrue("el repositorio debió cerrar la sesión", repositorio.cerrada)
        assertNull(vm.sesion.value)
        assertTrue("debió avisar para navegar fuera", navego)
    }

    private class RepositorioFalso(private var sesion: Sesion?) : SesionRepositorio {
        var cerrada = false
            private set

        override suspend fun sesionGuardada(): Sesion? = sesion

        override suspend fun biometriaHabilitada(): Boolean = true

        override suspend fun desbloquear(): Sesion? = sesion

        override suspend fun ingresarConContrasena(correo: String, contrasena: String): ResultadoIngreso =
            ResultadoIngreso.CredencialesInvalidas

        override suspend fun habilitarBiometria(habilitada: Boolean) = Unit

        override suspend fun cerrar() {
            sesion = null
            cerrada = true
        }
    }
}
