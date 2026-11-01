package co.solventa.movil.features.originacion.polizas

import co.solventa.movil.core.api.originacion.ListaDePolizasDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La traducción de las pólizas entre la red, el dominio y la copia (HU-M1.2-01). */
class MapeadorDePolizasTest {

    @Test
    fun `la poliza del servicio trae todo lo que pide el criterio 2`() {
        val p = MapeadorDePolizas.desdeDto(polizaDto(prima = 142_300.0))

        assertEquals("AU-2025-118204", p.id)
        assertEquals(LineasDePoliza.AUTOS, p.linea)
        assertEquals(1_759_363_200_000L, p.inicio) // 2025-10-02T00:00:00Z
        assertEquals(1_790_899_200_000L, p.fin) // 2026-10-02T00:00:00Z
        assertEquals(listOf(Cobertura("collision", 72_500_000.0), Cobertura("theft", 72_500_000.0)), p.coberturas)
        assertEquals(Asegurado("Carolina Pérez Ruiz", "CC", "1020304050"), p.asegurado)
        assertEquals(142_300.0, p.prima)
        assertEquals("KJT-482", p.objetoAsegurado)
    }

    @Test
    fun `los textos vacios del servicio quedan nulos`() {
        val p = MapeadorDePolizas.desdeDto(polizaDto().copy(producto = " ", objetoAsegurado = "", renovacionDe = ""))

        assertNull(p.producto)
        assertNull(p.objetoAsegurado)
        assertNull(p.renovacionDe)
    }

    @Test(expected = RuntimeException::class)
    fun `una fecha que no se entiende hace fallar la lista`() {
        MapeadorDePolizas.desdeDto(polizaDto(fin = "mañana"))
    }

    @Test
    fun `lo que se guarda vuelve igual`() {
        val p = poliza(prima = 68_500.0, renovacionDe = "AU-2024-093377")

        assertEquals(p, MapeadorDePolizas.desdeJson(MapeadorDePolizas.aJson(p)))
    }

    @Test
    fun `una copia ilegible no es una poliza`() {
        assertNull(MapeadorDePolizas.desdeJson("{no es json"))
    }

    @Test
    fun `lee la respuesta del contrato con campos que no usa`() {
        val json = Json { ignoreUnknownKeys = true }
        val lista = json.decodeFromString(
            ListaDePolizasDto.serializer(),
            """{"items":[{"policy_id":"pol-1","source":"origination","line":"life","product":null,"insured_object":null,
              "status":"active","holder_name":"Carolina Pérez","holder":{"full_name":"Carolina Pérez","document_type":null,
              "document_id":"1020304050"},"premium":68500,"currency":"COP","insured_amount":250000000,
              "coverages":[{"code":"death","insured_amount":250000000}],"coverage_start":"2026-09-16T00:00:00Z",
              "coverage_end":"2046-09-16T00:00:00Z","renewal_of":null,"renewal_window":{"opens_at":"2046-08-17T00:00:00Z",
              "closes_at":"2046-09-16T00:00:00Z"},"can_renew":false,"renewal":null}],"partial":true}""",
        )

        assertTrue(lista.parcial)
        val p = MapeadorDePolizas.desdeDto(lista.elementos.single())
        assertEquals(68_500.0, p.prima)
        assertNull(p.asegurado.tipoDeDocumento)
        assertFalse(p.coberturas.isEmpty())
    }
}
