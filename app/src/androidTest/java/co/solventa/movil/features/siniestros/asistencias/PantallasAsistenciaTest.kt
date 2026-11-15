package co.solventa.movil.features.siniestros.asistencias

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.ubicacion.Ubicacion
import co.solventa.movil.features.siniestros.radicacion.PolizaElegible
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * HU-M2.3-01 · M14, M14b y M14c con estado inyectado: lo que el asegurado ve
 * en cada estado y que cada botón llama a lo que corresponde. El GPS y el
 * marcador del emulador no intervienen (se prueban a mano, Fase D del plan).
 */
class PantallasAsistenciaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg argumentos: Any) = contexto.getString(id, *argumentos)

    private val idioma: Locale get() = contexto.resources.configuration.locales[0]

    private val poliza = PolizaElegible(id = "AU-2025-118204", etiqueta = "Autos · AU-2025-118204 · KJT-482")
    private val otra = PolizaElegible(id = "HO-2026-000412", etiqueta = "Hogar · HO-2026-000412")
    private val lectura = Ubicacion(latitud = 4.7712, longitud = -74.0452, altitud = null, precisionM = 8f, leidaEn = 1_794_710_331_000)

    private val enviada = SolicitudDeAsistencia(
        id = 1,
        poliza = poliza.id,
        etiquetaDePoliza = poliza.etiqueta,
        resultado = ResultadoDeUbicacion.Compartida(lectura),
        envio = EstadoDeEnvio.Confirmada,
        creadaEn = 1_794_710_331_000,
        lugar = "Autopista Norte km 12, Bogotá",
        requestId = "5004f477-d43b-4b8a-80b0-b045207c5d11",
        numero = "AST-2026-0001",
        registradaEn = 1_794_710_337_043,
        estado = EstadoDeLaSolicitud.NUEVA,
    )

    private val llamadas = mutableListOf<String>()

    /** El estado que se dibuja; cambiarlo recompone sin volver a llamar a `setContent`. */
    private val actual = mutableStateOf(AsistenciaUiState())
    private var dibujada = false

    private fun mostrar(estado: AsistenciaUiState) {
        actual.value = estado
        if (dibujada) {
            composicion.waitForIdle()
            return
        }
        dibujada = true
        composicion.setContent {
            SolventaTheme {
                ContenidoAsistencia(
                    estado = actual.value,
                    onAtras = { llamadas += "atras" },
                    onElegirPoliza = { llamadas += "poliza:$it" },
                    onSolicitar = { llamadas += "solicitar" },
                    onLlamarSinSolicitud = { llamadas += "llamar-sin-solicitud" },
                    onActivarUbicacion = { llamadas += "activar" },
                    onLlamarSinUbicacion = { llamadas += "sin-ubicacion" },
                    onCancelar = { llamadas += "cancelar" },
                    onVolverALlamar = { llamadas += "volver-a-llamar" },
                    onVerEstado = { llamadas += "ver-estado" },
                    onSolicitarOtra = { llamadas += "otra" },
                    onCopiarNumero = { llamadas += "copiar" },
                )
            }
        }
    }

    private fun inicial(polizas: PolizasDeAsistencia = PolizasDeAsistencia.Lista(listOf(poliza), poliza)) =
        AsistenciaUiState(polizas = polizas)

    @Test
    fun m14_inicial_muestra_la_poliza_la_linea_y_solicita() {
        mostrar(inicial())

        composicion.onNodeWithText(poliza.etiqueta).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_linea_texto, LineaDeAtencion.VISIBLE)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_solicitar)).assertIsEnabled().performClick()
        assertEquals(listOf("solicitar"), llamadas)
    }

    @Test
    fun m14_con_varias_polizas_hay_que_elegir() {
        mostrar(inicial(PolizasDeAsistencia.Lista(listOf(poliza, otra), elegida = null)))

        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_poliza_elegir)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_solicitar)).assertIsNotEnabled()
        composicion.onNodeWithText(otra.etiqueta).performClick()
        assertEquals(listOf("poliza:${otra.id}"), llamadas)
    }

    @Test
    fun m14_sin_polizas_ofrece_llamar() {
        mostrar(inicial(PolizasDeAsistencia.SinPolizas))

        composicion.onNodeWithContentDescription(texto(R.string.siniestros_asistencia_sin_polizas)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_llamar_linea)).performClick()
        assertEquals(listOf("llamar-sin-solicitud"), llamadas)
    }

    @Test
    fun m14b_las_tres_variantes_explican_y_ofrecen_tres_salidas() {
        TipoDeDialogo.entries.forEach { tipo ->
            llamadas.clear()
            mostrar(inicial().copy(paso = PasoDeAsistencia.Dialogo(tipo)))

            composicion.onNodeWithText(texto(TextosDeAsistencia.dialogo(tipo))).assertIsDisplayed()
            composicion.onNodeWithText(texto(R.string.siniestros_asistencia_dialogo_texto)).assertIsDisplayed()
            composicion.onNodeWithContentDescription(texto(R.string.siniestros_asistencia_icono_alerta)).assertIsDisplayed()
            composicion.onNodeWithText(texto(R.string.siniestros_asistencia_dialogo_activar)).performClick()
            composicion.onNodeWithText(texto(R.string.siniestros_asistencia_dialogo_sin_ubicacion)).performClick()
            composicion.onNodeWithText(texto(R.string.siniestros_asistencia_dialogo_cancelar)).performClick()
            assertEquals(tipo.name, listOf("activar", "sin-ubicacion", "cancelar"), llamadas)
        }
    }

    @Test
    fun m14_obteniendo_la_ubicacion() {
        mostrar(inicial().copy(paso = PasoDeAsistencia.ObteniendoUbicacion))

        composicion.onNodeWithContentDescription(texto(R.string.siniestros_asistencia_obteniendo)).assertIsDisplayed()
    }

    @Test
    fun m14_enviada_muestra_los_datos_del_prototipo() {
        mostrar(inicial().copy(paso = PasoDeAsistencia.Solicitada, solicitud = enviada, marcadorAbierto = true))

        val hora = PresentacionDeAsistencia.horaConSegundos(enviada.registradaEn!!, idioma)
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_asistencia_enviada, hora)).assertIsDisplayed()
        val coordenadas = PresentacionDeAsistencia.coordenadas(lectura, idioma)
        val ubicacion = texto(
            R.string.siniestros_asistencia_dato_coordenadas,
            coordenadas.latitud,
            texto(R.string.siniestros_asistencia_norte),
            coordenadas.longitud,
            texto(R.string.siniestros_asistencia_oeste),
        )
        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_asistencia_dato_ubicacion)}: $ubicacion",
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_asistencia_dato_precision)}: ${texto(R.string.siniestros_asistencia_dato_precision_valor, "8")}",
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.siniestros_asistencia_dato_poliza)}: ${poliza.etiqueta}")
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription("${texto(R.string.siniestros_asistencia_dato_numero)}: AST-2026-0001")
            .performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_asistencia_estado_llamada)).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.siniestros_asistencia_estado_por_despachar)).performScrollTo().assertIsDisplayed()

        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_volver_a_llamar)).performClick()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_ver_estado)).performClick()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_otra)).performClick()
        assertEquals(listOf("volver-a-llamar", "ver-estado", "otra"), llamadas)
    }

    @Test
    fun m14c_sin_ubicacion_avisa_en_amarillo_y_la_llamada_sigue() {
        mostrar(
            inicial().copy(
                paso = PasoDeAsistencia.Solicitada,
                solicitud = enviada.copy(resultado = ResultadoDeUbicacion.NoDisponible(MotivoSinUbicacion.SIN_LECTURA), lugar = null),
                marcadorAbierto = true,
            ),
        )

        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_asistencia_sin_ubicacion_titulo)}. ${texto(R.string.siniestros_asistencia_sin_ubicacion_senal)}",
        ).assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_asistencia_dato_ubicacion)}: ${texto(R.string.siniestros_asistencia_dato_no_disponible)}",
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_volver_a_llamar)).assertIsDisplayed()
    }

    @Test
    fun m14_en_cola_sin_datos_dice_que_se_enviara() {
        mostrar(
            inicial().copy(
                paso = PasoDeAsistencia.Solicitada,
                solicitud = enviada.copy(envio = EstadoDeEnvio.EnCola, requestId = null, numero = null, registradaEn = null, estado = null),
                conectado = false,
            ),
        )

        // El aviso de arriba y la fila de «Estado de la solicitud» dicen lo mismo.
        composicion.onAllNodesWithContentDescription(texto(R.string.siniestros_asistencia_sin_conexion)).assertCountEquals(2)
        composicion.onAllNodesWithContentDescription(texto(R.string.siniestros_asistencia_sin_conexion)).onFirst().assertIsDisplayed()
        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_asistencia_dato_hora)}: ${texto(R.string.siniestros_asistencia_dato_hora_pendiente)}",
        ).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_ver_estado)).assertDoesNotExist()
    }

    @Test
    fun m14_descartada_avisa_que_no_se_registro() {
        mostrar(
            inicial().copy(
                paso = PasoDeAsistencia.Solicitada,
                solicitud = enviada.copy(envio = EstadoDeEnvio.Descartada(MotivoDeDescarte.POLIZA_NO_ACTIVA), requestId = null),
            ),
        )

        composicion.onNodeWithContentDescription(
            "${texto(R.string.siniestros_asistencia_no_registrada_titulo)}. ${texto(R.string.siniestros_asistencia_descarte_poliza)}",
        ).assertIsDisplayed()
    }

    @Test
    fun m14_sin_marcador_muestra_el_numero_para_copiarlo() {
        mostrar(inicial().copy(paso = PasoDeAsistencia.Solicitada, solicitud = enviada, sinMarcador = true))

        composicion.onNodeWithText(LineaDeAtencion.VISIBLE).performScrollTo().assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_asistencia_copiar)).performScrollTo().performClick()
        assertEquals(listOf("copiar"), llamadas)
    }
}
