package co.solventa.movil.features.perfil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.auth.biometria.AutenticadorBiometrico
import co.solventa.movil.core.auth.biometria.DisponibilidadBiometrica
import co.solventa.movil.core.auth.biometria.ResultadoBiometrico
import co.solventa.movil.core.auth.sesion.ResultadoIngreso
import co.solventa.movil.core.auth.sesion.Sesion
import co.solventa.movil.core.auth.sesion.SesionRepositorio
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaRepositorio
import co.solventa.movil.core.persistencia.BaseDeDatosSolventa
import co.solventa.movil.core.persistencia.entidades.ConsentimientoEntidad
import co.solventa.movil.core.persistencia.entidades.RevocacionPendienteEntidad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

/**
 * El estado de M19 · Perfil (HU-M1.3-01), con la copia local real de
 * los consentimientos (Robolectric) para la insignia «N activos».
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PerfilViewModelTest {

    private lateinit var base: BaseDeDatosSolventa
    private val sesion = SesionDePrueba()
    private val autenticador = AutenticadorDePrueba()
    private val idiomas = IdiomasDePrueba()

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        base = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun despues() {
        base.close()
        Dispatchers.resetMain()
    }

    private fun vm() =
        PerfilViewModel(sesion, autenticador, idiomas, base.consentimientos(), base.revocacionesPendientes(), "1.0", { AHORA })

    /** Espera a que Room entregue lo que observa el ViewModel. */
    private fun esperar(condicion: () -> Boolean) {
        repeat(50) {
            if (condicion()) return
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
    }

    private fun fila(propietario: String, id: String, estado: String = "active", venceEn: Long = AHORA + 1_000) =
        ConsentimientoEntidad(
            propietario, id, estado, estado == "active", "p", "embedded", "v1", AHORA - 1_000, venceEn, null, null,
            "[]", "[]", "[]",
        )

    @Test
    fun `muestra el nombre, el correo y las iniciales de la sesion`() {
        val vm = vm()
        esperar { vm.estado.value.nombre.isNotEmpty() }

        val estado = vm.estado.value
        assertEquals("Carolina Pérez Ruiz", estado.nombre)
        assertEquals("carolina@example.com", estado.correo)
        assertEquals("CP", estado.iniciales)
        assertEquals("1.0", estado.version)
        assertEquals(5, estado.minutosDeCierre)
    }

    @Test
    fun `las iniciales salen de las dos primeras palabras`() {
        assertEquals("AM", PerfilViewModel.iniciales("ana  maría pérez"))
        assertEquals("C", PerfilViewModel.iniciales("Carolina"))
        assertEquals("", PerfilViewModel.iniciales(""))
    }

    @Test
    fun `cuenta los consentimientos activos y vigentes del titular`() {
        runBlocking {
            base.consentimientos().reemplazarDePropietario(
                "sub-a",
                listOf(fila("sub-a", "1"), fila("sub-a", "2"), fila("sub-a", "3", estado = "revoked"), fila("sub-a", "4", venceEn = AHORA)),
                AHORA,
            )
            base.consentimientos().reemplazarDePropietario("sub-b", listOf(fila("sub-b", "9")), AHORA)
        }

        val vm = vm()
        esperar { vm.estado.value.activos == 2 }

        assertEquals(2, vm.estado.value.activos)
    }

    @Test
    fun `la biometria se alterna solo si el dispositivo la tiene`() {
        autenticador.disponibilidad = DisponibilidadBiometrica.DISPONIBLE
        val vm = vm()
        esperar { vm.estado.value.biometriaDisponible }
        assertTrue(vm.estado.value.biometriaDisponible)
        assertFalse(vm.estado.value.biometriaHabilitada)

        vm.alternarBiometria(true)
        esperar { vm.estado.value.biometriaHabilitada }

        assertTrue(vm.estado.value.biometriaHabilitada)
        assertEquals(listOf(true), sesion.cambiosDeBiometria)
    }

    @Test
    fun `sin biometria en el dispositivo el interruptor no hace nada`() {
        autenticador.disponibilidad = DisponibilidadBiometrica.NO_CONFIGURADA
        val vm = vm()
        esperar { vm.estado.value.nombre.isNotEmpty() }

        vm.alternarBiometria(true)

        assertFalse(vm.estado.value.biometriaDisponible)
        assertFalse(vm.estado.value.biometriaHabilitada)
        assertTrue(sesion.cambiosDeBiometria.isEmpty())
    }

    @Test
    fun `el idioma sigue al repositorio y se elige desde el perfil`() {
        val vm = vm()
        assertEquals(Idioma.ESPANOL, vm.estado.value.idioma)

        vm.elegirIdioma(Idioma.INGLES)
        esperar { vm.estado.value.idioma == Idioma.INGLES }

        assertEquals(Idioma.INGLES, vm.estado.value.idioma)
        assertEquals(listOf(Idioma.INGLES), idiomas.elegidos)
    }

    @Test
    fun `cerrar sesion cierra el repositorio y luego avisa`() {
        val vm = vm()
        var avisado = false

        vm.cerrarSesion { avisado = true }
        esperar { avisado }

        assertTrue(sesion.cerrada)
        assertTrue(avisado)
    }

    // --- HU-M1.3-02: aviso al cerrar sesion con revocaciones en cola (D-7 a) ---

    @Test
    fun `con revocaciones pendientes avisa antes de cerrar y no cierra`() {
        runBlocking {
            base.revocacionesPendientes().encolar(
                RevocacionPendienteEntidad("sub-a", "cns-1", "k1", "Banco Aliado", AHORA, "America/Bogota", "Pixel", "biometric"),
            )
        }
        val vm = vm()
        var avisado = false

        vm.cerrarSesion { avisado = true }
        esperar { vm.estado.value.revocacionesPendientesAlCerrar > 0 }

        assertEquals(1, vm.estado.value.revocacionesPendientesAlCerrar)
        assertFalse(sesion.cerrada)
        assertFalse(avisado)

        vm.cancelarCierre()
        assertEquals(0, vm.estado.value.revocacionesPendientesAlCerrar)
        assertFalse(sesion.cerrada)
    }

    @Test
    fun `confirmar el cierre cierra igual y la cola queda en el telefono`() {
        runBlocking {
            base.revocacionesPendientes().encolar(
                RevocacionPendienteEntidad("sub-a", "cns-1", "k1", "Banco Aliado", AHORA, "America/Bogota", "Pixel", "biometric"),
            )
        }
        val vm = vm()
        var avisado = false

        vm.confirmarCierre { avisado = true }
        esperar { avisado }

        assertTrue(sesion.cerrada)
        assertEquals(0, vm.estado.value.revocacionesPendientesAlCerrar)
        assertEquals(1, runBlocking { base.revocacionesPendientes().contar("sub-a") })
    }

    private class SesionDePrueba : SesionRepositorio {
        var cerrada = false
        val cambiosDeBiometria = mutableListOf<Boolean>()
        override suspend fun sesionGuardada(): Sesion = Sesion("carolina@example.com", "Carolina Pérez Ruiz", "sub-a")
        override suspend fun biometriaHabilitada(): Boolean = cambiosDeBiometria.lastOrNull() ?: false
        override suspend fun desbloquear(): Sesion = sesionGuardada()
        override suspend fun ingresarConContrasena(correo: String, contrasena: String): ResultadoIngreso =
            ResultadoIngreso.CredencialesInvalidas
        override suspend fun habilitarBiometria(habilitada: Boolean) {
            cambiosDeBiometria += habilitada
        }
        override suspend fun cerrar() {
            cerrada = true
        }
    }

    private class AutenticadorDePrueba : AutenticadorBiometrico {
        var disponibilidad = DisponibilidadBiometrica.SIN_HARDWARE
        override fun disponibilidad(): DisponibilidadBiometrica = disponibilidad
        override suspend fun autenticar(): ResultadoBiometrico = ResultadoBiometrico.Exito
    }

    private class IdiomasDePrueba : IdiomaRepositorio {
        private val _actual = MutableStateFlow(Idioma.ESPANOL)
        val elegidos = mutableListOf<Idioma>()
        override val actual: StateFlow<Idioma> = _actual
        override val elegidoPorElUsuario: Boolean get() = elegidos.isNotEmpty()
        override fun elegir(idioma: Idioma) {
            elegidos += idioma
            _actual.value = idioma
        }
    }

    private companion object {
        const val AHORA = 1_792_800_000_000L
    }
}
