package co.solventa.movil.core.auth.sesion

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El almacén cifrado de la sesión, en el dispositivo.
 *
 * Es una prueba instrumentada y no puede ser otra cosa: la clave vive en el
 * Keystore de Android, que no existe en la JVM. Lo que se comprueba aquí es
 * justamente lo que el doble en memoria no puede comprobar: que lo guardado
 * en disco no está en claro y que un contenido corrupto no deja la sesión en
 * un estado a medias.
 */
@RunWith(AndroidJUnit4::class)
class AlmacenDeSesionCifradoTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var almacen: AlmacenDeSesionCifrado

    @Before
    fun antes() {
        almacen = AlmacenDeSesionCifrado(contexto)
        almacen.borrarTokens()
    }

    private fun tokens() = TokensDeSesion(
        idToken = "id-de-prueba",
        accessToken = "acceso-de-prueba",
        tokenDeRenovacion = "renovacion-de-prueba",
        venceEn = 1_760_000_300_000L,
        correo = "asegurado@example.com",
        nombre = "Carolina",
    )

    @Test
    fun guardaYDevuelveLaSesionIntacta() {
        almacen.guardarTokens(tokens())

        assertEquals(tokens(), almacen.tokens())
    }

    @Test
    fun sinSesionGuardadaDevuelveNulo() {
        assertNull(almacen.tokens())
    }

    /**
     * El *refresh token* dura 30 días: en claro, cualquiera con acceso a los
     * archivos de la aplicación podría pedir sesiones nuevas durante un mes.
     */
    @Test
    fun loQueQuedaEnDiscoNoEstaEnClaro() {
        almacen.guardarTokens(tokens())

        val crudo = contexto.getSharedPreferences("sesion-solventa", android.content.Context.MODE_PRIVATE)
            .getString("tokens", null)

        assertNotEquals(null, crudo)
        for (secreto in listOf("renovacion-de-prueba", "id-de-prueba", "asegurado@example.com")) {
            assert(!crudo!!.contains(secreto)) { "el almacen dejo $secreto legible en disco" }
        }
    }

    /**
     * Ocurre al restaurar una copia en otro dispositivo: el texto cifrado
     * llega, pero la clave del Keystore no. No hay forma de recuperarlo, así
     * que se descarta y se pide iniciar sesión otra vez.
     */
    @Test
    fun unContenidoQueNoSePuedeDescifrarSeDescarta() {
        contexto.getSharedPreferences("sesion-solventa", android.content.Context.MODE_PRIVATE)
            .edit().putString("tokens", "basura:tampoco-es-base64").commit()

        assertNull(almacen.tokens())
        assertNull(almacen.tokens())
    }

    @Test
    fun borrarDejaElDispositivoSinSesion() {
        almacen.guardarTokens(tokens())

        almacen.borrarTokens()

        assertNull(almacen.tokens())
    }

    @Test
    fun laPreferenciaDeBiometriaSobrevive() {
        almacen.habilitarBiometria(false)

        assertEquals(false, almacen.biometriaHabilitada())
    }
}
