package co.solventa.movil.features.siniestros.asistencias

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowGeocoder
import java.util.Locale

/**
 * La dirección legible con el `Geocoder` del sistema (Robolectric,
 * HU-M2.3-01, D7): ausente, sin resultados, con error y con una dirección de
 * más de 160 caracteres, por la vía de Android 13 o superior y por la anterior.
 */
@RunWith(AndroidJUnit4::class)
class TraductorConGeocoderTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val geocoder = Geocoder(contexto, Locale.forLanguageTag("es-CO"))

    private fun traductor(presente: Boolean = true) = TraductorConGeocoder(crear = { geocoder }, presente = { presente })

    private fun direccion(linea: String?, calle: String? = null, ciudad: String? = null) =
        Address(Locale.forLanguageTag("es-CO")).apply {
            linea?.let { setAddressLine(0, it) }
            thoroughfare = calle
            locality = ciudad
        }

    @After
    fun despues() {
        ShadowGeocoder.reset()
    }

    @Test
    fun `sin geocodificador no hay lugar`() = runTest {
        assertNull(traductor(presente = false).traducir(4.7712, -74.0452))
    }

    @Test
    fun `con una direccion devuelve su primera linea`() = runTest {
        shadowOf(geocoder).setFromLocation(listOf(direccion("Autopista Norte km 12, Bogotá")))

        assertEquals("Autopista Norte km 12, Bogotá", traductor().traducir(4.7712, -74.0452))
    }

    @Test
    fun `sin linea arma calle y ciudad`() = runTest {
        shadowOf(geocoder).setFromLocation(listOf(direccion(null, "Autopista Norte", "Bogotá")))

        assertEquals("Autopista Norte, Bogotá", traductor().traducir(4.7712, -74.0452))
    }

    @Test
    fun `sin resultados o sin datos no hay lugar`() = runTest {
        shadowOf(geocoder).setFromLocation(emptyList())
        assertNull(traductor().traducir(4.7712, -74.0452))

        shadowOf(geocoder).setFromLocation(listOf(direccion(null)))
        assertNull(traductor().traducir(4.7712, -74.0452))
    }

    @Test
    fun `con error no hay lugar`() = runTest {
        shadowOf(geocoder).setErrorMessage("servicio no disponible")

        assertNull(traductor().traducir(4.7712, -74.0452))
    }

    @Test
    fun `una direccion de mas de 160 caracteres se recorta`() = runTest {
        shadowOf(geocoder).setFromLocation(listOf(direccion("Calle ".repeat(40))))

        assertEquals(ReglasDeAsistencia.LUGAR_MAXIMO, traductor().traducir(4.7712, -74.0452)!!.length)
    }

    @Test
    fun `coordenadas fuera de rango no revientan`() = runTest {
        assertNull(traductor().traducir(91.0, -74.0452))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `antes de Android 13 usa la lectura sincrona`() = runBlocking {
        // En otro hilo y con tiempo real: con el tiempo virtual de runTest el
        // corte de 3 s se cumpliría antes de que el hilo conteste.
        shadowOf(geocoder).setFromLocation(listOf(direccion("Autopista Norte km 12, Bogotá")))
        assertEquals("Autopista Norte km 12, Bogotá", traductor().traducir(4.7712, -74.0452))

        // Una lectura que el geocodificador rechaza no revienta.
        assertNull(traductor().traducir(91.0, -74.0452))
    }
}
