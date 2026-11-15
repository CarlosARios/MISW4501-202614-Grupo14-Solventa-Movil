package co.solventa.movil.features.siniestros.asistencias

import co.solventa.movil.core.ubicacion.PermisoDeUbicacion
import co.solventa.movil.core.ubicacion.Ubicacion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las reglas de la solicitud de asistencia (HU-M2.3-01) con tabla y valores
 * límite: el paso siguiente (criterio 1), el resultado de la ubicación (S1,
 * criterios 2 y 4), el vencimiento (S7), la solicitud que se retoma (D9) y el
 * cuerpo del contrato.
 */
class ReglasDeAsistenciaTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun lectura(
        latitud: Double = 4.7712,
        longitud: Double = -74.0452,
        precision: Float = 8f,
        leidaEn: Long = AHORA - 5_000,
    ) = Ubicacion(latitud, longitud, altitud = 2_600.0, precisionM = precision, leidaEn = leidaEn)

    // --- Criterio 1 y S2: paso siguiente --------------------------------------------------------

    @Test
    fun `permiso por servicio da el paso siguiente`() {
        val casos = listOf(
            Triple(PermisoDeUbicacion.PRECISO, true, SiguientePaso.ObtenerUbicacion),
            Triple(PermisoDeUbicacion.APROXIMADO, true, SiguientePaso.ObtenerUbicacion),
            Triple(PermisoDeUbicacion.PRECISO, false, SiguientePaso.Explicar(TipoDeDialogo.SERVICIO_APAGADO)),
            Triple(PermisoDeUbicacion.APROXIMADO, false, SiguientePaso.Explicar(TipoDeDialogo.SERVICIO_APAGADO)),
            Triple(PermisoDeUbicacion.NINGUNO, true, SiguientePaso.Explicar(TipoDeDialogo.FALTA_PERMISO)),
            Triple(PermisoDeUbicacion.NINGUNO, false, SiguientePaso.Explicar(TipoDeDialogo.FALTA_PERMISO)),
        )
        casos.forEach { (permiso, servicio, esperado) ->
            assertEquals("$permiso/$servicio", esperado, ReglasDeAsistencia.siguientePaso(permiso, servicio, permisoBloqueado = false))
        }
    }

    @Test
    fun `sin permiso y con el dialogo del sistema bloqueado se manda a los ajustes de la app`() {
        assertEquals(
            SiguientePaso.Explicar(TipoDeDialogo.PERMISO_NEGADO_PARA_SIEMPRE),
            ReglasDeAsistencia.siguientePaso(PermisoDeUbicacion.NINGUNO, servicioEncendido = false, permisoBloqueado = true),
        )
        // Con permiso, el bloqueo anterior ya no importa.
        assertEquals(
            SiguientePaso.ObtenerUbicacion,
            ReglasDeAsistencia.siguientePaso(PermisoDeUbicacion.PRECISO, servicioEncendido = true, permisoBloqueado = true),
        )
    }

    // --- S1: permiso x servicio x lectura -------------------------------------------------------

    @Test
    fun `las ocho combinaciones de permiso servicio y lectura dan su resultado`() {
        val conLectura = lectura()
        val casos = listOf(
            Triple(PermisoDeUbicacion.PRECISO to true, conLectura, ResultadoDeUbicacion.Compartida(conLectura)),
            Triple(PermisoDeUbicacion.PRECISO to true, null, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA)),
            Triple(PermisoDeUbicacion.PRECISO to false, conLectura, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.UBICACION_APAGADA)),
            Triple(PermisoDeUbicacion.PRECISO to false, null, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.UBICACION_APAGADA)),
            Triple(PermisoDeUbicacion.NINGUNO to true, conLectura, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO)),
            Triple(PermisoDeUbicacion.NINGUNO to true, null, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO)),
            Triple(PermisoDeUbicacion.NINGUNO to false, conLectura, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO)),
            Triple(PermisoDeUbicacion.NINGUNO to false, null, ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.PERMISO_NEGADO)),
        )
        casos.forEach { (condicion, ubicacion, esperado) ->
            val (permiso, servicio) = condicion
            assertEquals(
                "$permiso/$servicio/${ubicacion != null}",
                esperado,
                ReglasDeAsistencia.resultadoDe(ubicacion, permiso, servicio, AHORA),
            )
        }
    }

    @Test
    fun `con solo la aproximada la ubicacion se envia con su precision`() {
        val gruesa = lectura(precision = 1_500f)

        assertEquals(
            ResultadoDeUbicacion.Compartida(gruesa),
            ReglasDeAsistencia.resultadoDe(gruesa, PermisoDeUbicacion.APROXIMADO, true, AHORA),
        )
    }

    @Test
    fun `un fix de 60 s se envia y uno de 60 s y 1 ms es no_fix`() {
        val justo = lectura(leidaEn = AHORA - 60_000)
        val viejo = lectura(leidaEn = AHORA - 60_001)

        assertEquals(ResultadoDeUbicacion.Compartida(justo), ReglasDeAsistencia.resultadoDe(justo, PermisoDeUbicacion.PRECISO, true, AHORA))
        assertEquals(
            ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA),
            ReglasDeAsistencia.resultadoDe(viejo, PermisoDeUbicacion.PRECISO, true, AHORA),
        )
    }

    @Test
    fun `una precision fuera del rango del contrato es no_fix`() {
        listOf(0f, -1f, 10_000.5f).forEach { precision ->
            assertEquals(
                "precision $precision",
                ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA),
                ReglasDeAsistencia.resultadoDe(lectura(precision = precision), PermisoDeUbicacion.PRECISO, true, AHORA),
            )
        }
        val limite = lectura(precision = 10_000f)
        assertEquals(ResultadoDeUbicacion.Compartida(limite), ReglasDeAsistencia.resultadoDe(limite, PermisoDeUbicacion.PRECISO, true, AHORA))
    }

    @Test
    fun `llamar sin ubicacion registra el motivo de lo que falta`() {
        assertEquals(MotivoSinUbicacion.PERMISO_NEGADO, ReglasDeAsistencia.motivoSinUbicacion(PermisoDeUbicacion.NINGUNO, false))
        assertEquals(MotivoSinUbicacion.UBICACION_APAGADA, ReglasDeAsistencia.motivoSinUbicacion(PermisoDeUbicacion.APROXIMADO, false))
        assertEquals(MotivoSinUbicacion.SIN_LECTURA, ReglasDeAsistencia.motivoSinUbicacion(PermisoDeUbicacion.PRECISO, true))
    }

    // --- S7 y D9: vencimiento y solicitud que se retoma -----------------------------------------

    @Test
    fun `vence a los 60 minutos exactos`() {
        assertFalse(ReglasDeAsistencia.vencida(creadaEn = 0, ahora = MINUTO * 60 - 1_000))
        assertTrue(ReglasDeAsistencia.vencida(creadaEn = 0, ahora = MINUTO * 60))
        assertTrue(ReglasDeAsistencia.vencida(creadaEn = 0, ahora = MINUTO * 60 + 1_000))
    }

    @Test
    fun `se retoma hasta los 29 59 y no a los 30 00`() {
        assertTrue(ReglasDeAsistencia.retomable(creadaEn = 0, ahora = MINUTO * 30 - 1_000))
        assertFalse(ReglasDeAsistencia.retomable(creadaEn = 0, ahora = MINUTO * 30))
    }

    // --- Cuerpo del contrato --------------------------------------------------------------------

    @Test
    fun `el cuerpo sent lleva coordenadas precision e instante en UTC`() {
        val cuerpo = ReglasDeAsistencia.aCuerpo(
            "AU-2025-118204",
            ResultadoDeUbicacion.Compartida(lectura(precision = 4.7f, leidaEn = 1_794_710_331_000)),
            "  Autopista Norte km 12, Bogotá ",
        )

        assertEquals(
            """{"policy_id":"AU-2025-118204","location":{"status":"sent","latitude":4.7712,"longitude":-74.0452,""" +
                """"accuracy_m":4.7,"captured_at":"2026-11-15T02:38:51Z"},"place":"Autopista Norte km 12, Bogotá"}""",
            json.encodeToString(cuerpo),
        )
    }

    @Test
    fun `cada motivo de unavailable viaja sin coordenadas ni lugar`() {
        MotivoSinUbicacion.entries.forEach { motivo ->
            val cuerpo = ReglasDeAsistencia.aCuerpo("AU-2025-118204", ResultadoDeUbicacion.NoDisponible(motivo), "Calle 80")

            assertEquals(
                """{"policy_id":"AU-2025-118204","location":{"status":"unavailable","unavailable_reason":"${motivo.codigo}"}}""",
                json.encodeToString(cuerpo),
            )
        }
    }

    @Test
    fun `las coordenadas en los bordes y en cero no se pierden`() {
        listOf(90.0 to 180.0, -90.0 to -180.0, 0.0 to 0.0).forEach { (latitud, longitud) ->
            val cuerpo = ReglasDeAsistencia.aCuerpo(
                "AU-2025-118204",
                ResultadoDeUbicacion.Compartida(lectura(latitud = latitud, longitud = longitud)),
                null,
            )
            assertEquals(latitud, cuerpo.ubicacion.latitud!!, 0.0)
            assertEquals(longitud, cuerpo.ubicacion.longitud!!, 0.0)
            assertTrue(json.encodeToString(cuerpo).contains("\"latitude\":$latitud"))
            assertNull(cuerpo.lugar)
        }
        assertEquals(
            ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA),
            ReglasDeAsistencia.resultadoDe(lectura(latitud = 90.1), PermisoDeUbicacion.PRECISO, true, AHORA),
        )
    }

    @Test
    fun `el lugar se limpia se recorta a 160 y se omite si es corto`() {
        assertEquals("Calle 80 # 30-12", ReglasDeAsistencia.lugarValido("  Calle 80\n# 30-12 "))
        assertEquals(160, ReglasDeAsistencia.lugarValido("a".repeat(200))!!.length)
        assertEquals("abc", ReglasDeAsistencia.lugarValido("abc"))
        assertNull(ReglasDeAsistencia.lugarValido(" ab "))
        assertNull(ReglasDeAsistencia.lugarValido(null))
    }

    // --- Estados del servicio -------------------------------------------------------------------

    @Test
    fun `los cuatro estados del servicio y uno desconocido`() {
        assertEquals(EstadoDeLaSolicitud.NUEVA, EstadoDeLaSolicitud.desdeCodigo("new"))
        assertEquals(EstadoDeLaSolicitud.EN_LLAMADA, EstadoDeLaSolicitud.desdeCodigo("in_call"))
        assertEquals(EstadoDeLaSolicitud.DESPACHADA, EstadoDeLaSolicitud.desdeCodigo("dispatched"))
        assertEquals(EstadoDeLaSolicitud.CERRADA, EstadoDeLaSolicitud.desdeCodigo("closed"))
        assertEquals(EstadoDeLaSolicitud.DESCONOCIDO, EstadoDeLaSolicitud.desdeCodigo("escalated"))
        assertEquals(EstadoDeLaSolicitud.DESCONOCIDO, EstadoDeLaSolicitud.desdeCodigo(null))
    }

    @Test
    fun `los codigos guardados vuelven a su motivo`() {
        MotivoSinUbicacion.entries.forEach { assertEquals(it, MotivoSinUbicacion.desdeCodigo(it.codigo)) }
        assertNull(MotivoSinUbicacion.desdeCodigo("otro"))
        MotivoDeDescarte.entries.forEach { assertEquals(it, MotivoDeDescarte.desdeCodigo(it.codigo)) }
        assertEquals(MotivoDeDescarte.RECHAZADA, MotivoDeDescarte.desdeCodigo(null))
    }

    private companion object {
        const val AHORA = 1_794_710_400_000L
        const val MINUTO = 60_000L
    }
}
