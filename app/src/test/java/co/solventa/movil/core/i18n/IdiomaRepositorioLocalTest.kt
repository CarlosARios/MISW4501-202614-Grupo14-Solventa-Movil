package co.solventa.movil.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * El idioma de la aplicación: de dónde sale el primero y qué pasa al
 * cambiarlo.
 */
class IdiomaRepositorioLocalTest {

    private val almacen = AlmacenEnMemoria()

    @Test
    fun `sin eleccion previa se usa el del dispositivo`() {
        almacen.locale = Locale.forLanguageTag("en-US")

        val repositorio = IdiomaRepositorioLocal(almacen)

        assertEquals(Idioma.INGLES, repositorio.actual.value)
        assertFalse(repositorio.elegidoPorElUsuario)
    }

    @Test
    fun `lo elegido se guarda y se anuncia`() {
        val repositorio = IdiomaRepositorioLocal(almacen)

        repositorio.elegir(Idioma.INGLES)

        assertEquals(Idioma.INGLES, repositorio.actual.value)
        assertTrue(repositorio.elegidoPorElUsuario)
        assertEquals("INGLES", almacen.guardado)
    }

    @Test
    fun `al volver a abrir la aplicacion manda lo elegido`() {
        almacen.guardado = "INGLES"

        val repositorio = IdiomaRepositorioLocal(almacen)

        assertEquals(Idioma.INGLES, repositorio.actual.value)
        assertTrue(repositorio.elegidoPorElUsuario)
    }

    /** Lo guardado puede venir de una versión con otros idiomas. */
    @Test
    fun `un idioma desconocido se ignora y manda el del dispositivo`() {
        almacen.guardado = "PORTUGUES"
        almacen.locale = Locale.forLanguageTag("es-CO")

        val repositorio = IdiomaRepositorioLocal(almacen)

        assertEquals(Idioma.ESPANOL, repositorio.actual.value)
        assertFalse(repositorio.elegidoPorElUsuario)
    }

    // El dispositivo decide el primero, y cualquier variante del español es
    // español: es-CO, es-MX o es a secas.
    @Test
    fun `cualquier variante del espanol es espanol`() {
        assertEquals(Idioma.ESPANOL, Idioma.delSistema(Locale.forLanguageTag("es-CO")))
        assertEquals(Idioma.ESPANOL, Idioma.delSistema(Locale.forLanguageTag("es-MX")))
        assertEquals(Idioma.ESPANOL, Idioma.delSistema(Locale.forLanguageTag("es")))
    }

    @Test
    fun `lo que no es espanol cae en ingles`() {
        assertEquals(Idioma.INGLES, Idioma.delSistema(Locale.forLanguageTag("en-US")))
        assertEquals(Idioma.INGLES, Idioma.delSistema(Locale.forLanguageTag("pt-BR")))
    }

    /** El nombre del idioma no se traduce: cada uno se lee en el suyo. */
    @Test
    fun `cada idioma se anuncia en su propio idioma`() {
        assertEquals("Español (Colombia)", Idioma.ESPANOL.etiqueta)
        assertEquals("English", Idioma.INGLES.etiqueta)
    }

    @Test
    fun `el locale corresponde a las traducciones del proyecto`() {
        assertEquals("es", Idioma.ESPANOL.aLocale().language)
        assertEquals("CO", Idioma.ESPANOL.aLocale().country)
        assertEquals("en", Idioma.INGLES.aLocale().language)
    }

    @Test
    fun `un nombre desconocido no corresponde a ningun idioma`() {
        assertNull(Idioma.deNombre("PORTUGUES"))
        assertNull(Idioma.deNombre(null))
        assertEquals(Idioma.ESPANOL, Idioma.deNombre("ESPANOL"))
    }
}

private class AlmacenEnMemoria : AlmacenDeIdioma {
    var guardado: String? = null
    var locale: Locale = Locale.forLanguageTag("es-CO")

    override fun leer(): String? = guardado

    override fun guardar(valor: String) {
        guardado = valor
    }

    override fun localeDelSistema(): Locale = locale
}
