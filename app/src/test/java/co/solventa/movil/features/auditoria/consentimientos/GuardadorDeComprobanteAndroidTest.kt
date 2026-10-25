package co.solventa.movil.features.auditoria.consentimientos

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * El comprobante se escribe en el destino que entrego el selector del
 * sistema (Robolectric, HU-M1.3-02); un destino que no se puede abrir no
 * revienta la pantalla.
 */
@RunWith(AndroidJUnit4::class)
class GuardadorDeComprobanteAndroidTest {

    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val guardador = GuardadorDeComprobanteAndroid(contexto)

    @Test
    fun `escribe los bytes del PDF en el destino elegido`() = runTest {
        val archivo = File(contexto.cacheDir, "REV-2026-0088.pdf").apply { delete() }

        val guardado = guardador.guardar(Uri.fromFile(archivo).toString(), "%PDF-1.3 comprobante".toByteArray())

        assertTrue(guardado)
        assertArrayEquals("%PDF-1.3 comprobante".toByteArray(), archivo.readBytes())
    }

    @Test
    fun `un destino que no se puede abrir no se guarda`() = runTest {
        val carpetaInexistente = File(contexto.cacheDir, "no-existe/REV.pdf")

        assertFalse(guardador.guardar(Uri.fromFile(carpetaInexistente).toString(), "%PDF".toByteArray()))
    }
}
