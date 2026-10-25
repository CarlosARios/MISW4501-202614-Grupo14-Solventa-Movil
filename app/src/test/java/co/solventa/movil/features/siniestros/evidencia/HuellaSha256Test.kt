package co.solventa.movil.features.siniestros.evidencia

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

/**
 * El resumen de integridad de la evidencia (HU-M2.1-01, criterio 4).
 *
 * No es un detalle interno: es lo que viaja en la firma de la subida y lo que
 * el almacenamiento compara al recibir el archivo. Un resumen distinto al del
 * cuerpo hace que la subida se rechace, de modo que aquí se comprueba contra
 * valores calculados aparte.
 */
class HuellaSha256Test {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val huellas = HuellaSha256()

    @Test
    fun `el resumen y el tamano son los del archivo`() = runTest {
        val archivo = conContenido("solventa".toByteArray())

        val huella = huellas.de(archivo)

        // El contrato lo nombra en hexadecimal minúsculo: 64 caracteres.
        assertEquals(64, huella?.sha256?.length)
        assertEquals(sha256De("solventa".toByteArray()), huella?.sha256)
        assertEquals(8L, huella?.tamanoBytes)
    }

    /** Un video no cabe en memoria: se lee por bloques y el resumen es el mismo. */
    @Test
    fun `un archivo mayor que el bloque de lectura se resume completo`() = runTest {
        val contenido = ByteArray(200_000) { (it % 251).toByte() }
        val archivo = conContenido(contenido)

        val huella = huellas.de(archivo)

        assertEquals(sha256De(contenido), huella?.sha256)
        assertEquals(200_000L, huella?.tamanoBytes)
    }

    @Test
    fun `un archivo vacio tiene el resumen del vacio`() = runTest {
        val huella = huellas.de(conContenido(ByteArray(0)))

        assertEquals(sha256De(ByteArray(0)), huella?.sha256)
        assertEquals(0L, huella?.tamanoBytes)
    }

    /** La captura pudo borrarse antes de que el trabajador llegue a subirla. */
    @Test
    fun `un archivo que no esta no tiene huella`() = runTest {
        assertNull(huellas.de(File(carpeta.root, "no-esta.jpg")))
    }

    private fun conContenido(bytes: ByteArray): File =
        carpeta.newFile().apply { writeBytes(bytes) }

    private fun sha256De(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
