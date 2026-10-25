package co.solventa.movil.core.navegacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las pestañas de la barra inferior y donde se marca cada una (HU-M1.3-01). */
class PestanaInferiorTest {

    @Test
    fun `cuatro pestañas en el orden del frame`() {
        assertEquals(
            listOf(PestanaInferior.INICIO, PestanaInferior.POLIZAS, PestanaInferior.SINIESTROS, PestanaInferior.PERFIL),
            PestanaInferior.entries.toList(),
        )
    }

    @Test
    fun `inicio y perfil navegan y las demas quedan en espera con su descripcion`() {
        assertTrue(PestanaInferior.INICIO.habilitada)
        assertEquals(Ruta.Inicio, PestanaInferior.INICIO.ruta)
        assertTrue(PestanaInferior.PERFIL.habilitada)
        assertEquals(Ruta.Perfil, PestanaInferior.PERFIL.ruta)
        assertNull(PestanaInferior.INICIO.descripcionEnEspera)

        listOf(PestanaInferior.POLIZAS, PestanaInferior.SINIESTROS).forEach {
            assertFalse(it.habilitada)
            assertNull(it.ruta)
            assertNotNull(it.descripcionEnEspera)
        }
    }

    @Test
    fun `cada destino marca su pestaña y consentimientos deja perfil seleccionada`() {
        assertEquals(PestanaInferior.INICIO, pestanaDe(Ruta.Inicio))
        assertEquals(PestanaInferior.PERFIL, pestanaDe(Ruta.Perfil))
        assertEquals(PestanaInferior.PERFIL, pestanaDe(Ruta.Consentimientos))
    }

    @Test
    fun `fuera de las pestañas no hay barra`() {
        assertNull(pestanaDe(Ruta.DetalleConsentimiento("cns-1")))
        assertNull(pestanaDe(Ruta.Bienvenida))
        assertNull(pestanaDe(Ruta.Radicacion))
        assertNull(pestanaDe(null))
    }
}
