package co.solventa.movil.features.originacion.polizas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los cambios que se avisan tras sincronizar (HU-M1.2-01, criterio 4). */
class DetectorDeCambiosTest {

    private val autos = poliza()
    private val hogar = poliza(id = "HG-2025-880231", linea = LineasDePoliza.HOGAR, valorAsegurado = 320_000_000.0)

    @Test
    fun `la primera sincronizacion no avisa nada`() {
        assertTrue(DetectorDeCambios.comparar(emptyList(), listOf(autos, hogar)).isEmpty())
    }

    @Test
    fun `sin diferencias no hay cambios`() {
        assertTrue(DetectorDeCambios.comparar(listOf(autos, hogar), listOf(autos, hogar)).isEmpty())
    }

    @Test
    fun `la renovacion de autos y la cancelacion de hogar del prototipo son dos cambios`() {
        val renovada = autos.copy(fin = autos.fin + 365 * DIA)
        val cancelada = hogar.copy(estado = EstadosDePoliza.CANCELADA)

        val cambios = DetectorDeCambios.comparar(listOf(autos, hogar), listOf(renovada, cancelada))

        assertEquals(
            listOf(CambioDePoliza(renovada, TipoDeCambio.RENOVADA), CambioDePoliza(cancelada, TipoDeCambio.CANCELADA)),
            cambios,
        )
    }

    @Test
    fun `una poliza nueva que renueva una de la copia es una renovacion`() {
        val nueva = poliza(id = "pol-2", renovacionDe = autos.id, inicio = autos.fin, fin = autos.fin + 365 * DIA)

        assertEquals(
            listOf(CambioDePoliza(nueva, TipoDeCambio.RENOVADA)),
            DetectorDeCambios.comparar(listOf(autos), listOf(autos, nueva)),
        )
    }

    @Test
    fun `una poliza nueva sin antecedente no se avisa`() {
        val nueva = poliza(id = "VJ-2026-000931", linea = LineasDePoliza.VIAJE)

        assertTrue(DetectorDeCambios.comparar(listOf(autos), listOf(autos, nueva)).isEmpty())
    }

    @Test
    fun `otro dato distinto es una modificacion`() {
        val modificada = autos.copy(valorAsegurado = 80_000_000.0)

        assertEquals(
            listOf(CambioDePoliza(modificada, TipoDeCambio.MODIFICADA)),
            DetectorDeCambios.comparar(listOf(autos), listOf(modificada)),
        )
    }

    @Test
    fun `una que ya estaba cancelada y cambia otro dato es una modificacion`() {
        val cancelada = hogar.copy(estado = EstadosDePoliza.CANCELADA)
        val corregida = cancelada.copy(objetoAsegurado = "Calle 93 # 14-20, Bogotá")

        assertEquals(TipoDeCambio.MODIFICADA, DetectorDeCambios.comparar(listOf(cancelada), listOf(corregida)).single().tipo)
    }
}
