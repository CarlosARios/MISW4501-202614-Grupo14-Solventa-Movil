package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.features.siniestros.evidencia.EstadoDeEvidencia
import co.solventa.movil.features.siniestros.evidencia.EvidenciaDelReporte
import co.solventa.movil.features.siniestros.evidencia.LimitesDeEvidencia
import co.solventa.movil.features.siniestros.evidencia.TipoDeEvidencia
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * HU-M2.2-01 · asistente guiado de radicación. Cada pantalla se dibuja con un
 * estado fijo: lo que se prueba es lo que el asegurado ve y si puede avanzar.
 */
class PantallasRadicacionTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg argumentos: Any) = contexto.getString(id, *argumentos)

    private val poliza = PolizaElegible(id = "AU-2025-118204", etiqueta = "Autos · AU-2025-118204 · KJT-482")

    private val identificada = Radicacion(poliza = poliza.id, tipo = TipoDeIncidente.COLISION)

    private val hechosCompletos = identificada.copy(
        ocurrioEn = 1_761_000_000_000L,
        lugar = "Calle 80 con carrera 30, Bogotá",
        descripcion = "Al detenerme en el semáforo, un vehículo golpeó la parte trasera.",
        respuestas = mapOf("personas_lesionadas" to "no", "informe_policia" to "no"),
    )

    private fun foto(i: Int, estado: EstadoDeEvidencia = EstadoDeEvidencia.SUBIDA) = EvidenciaDelReporte(
        id = "ev-$i",
        tipo = TipoDeEvidencia.FOTO,
        rutaArchivo = "/tmp/ev-$i.jpg",
        capturadaEn = 0L,
        ubicacion = null,
        duracionS = 0,
        estado = estado,
    )

    private fun identificacion(estado: RadicacionUiState, onContinuar: () -> Unit = {}) {
        composicion.setContent {
            SolventaTheme {
                PantallaIdentificacion(
                    estado = estado,
                    onElegirPoliza = {},
                    onElegirTipo = {},
                    onContinuar = onContinuar,
                    onGuardarYSalir = {},
                    onAtras = {},
                )
            }
        }
    }

    private fun hechos(estado: RadicacionUiState) {
        composicion.setContent {
            SolventaTheme {
                PantallaHechos(
                    estado = estado,
                    onFechaCambia = {},
                    onLugarCambia = {},
                    onDescripcionCambia = {},
                    onMontoCambia = {},
                    onResponder = { _, _ -> },
                    onContinuar = {},
                    onGuardarYSalir = {},
                    onAtras = {},
                )
            }
        }
    }

    private fun declaracion(estado: RadicacionUiState, onRadicar: () -> Unit = {}) {
        composicion.setContent {
            SolventaTheme {
                PantallaDeclaracion(
                    estado = estado,
                    onDeclarar = {},
                    onRadicar = onRadicar,
                    onGuardarYSalir = {},
                    onAtras = {},
                    onDescartarAviso = {},
                )
            }
        }
    }

    // Criterio 1 — las cuatro etapas del proceso, en orden.
    @Test
    fun el_asistente_muestra_las_cuatro_etapas() {
        identificacion(RadicacionUiState(cargando = false, polizas = listOf(poliza)))

        listOf(
            R.string.radicacion_etapa_identificacion,
            R.string.radicacion_etapa_hechos,
            R.string.radicacion_etapa_evidencias,
            R.string.radicacion_etapa_declaracion,
        ).forEach {
            // Cada etapa aparece en su paso y, la actual, también en «Paso 1 de 4 · …».
            composicion.onAllNodesWithText(texto(it), substring = true).onFirst().assertExists()
        }
    }

    // Criterio 2 — las preguntas dependen del tipo de incidente.
    @Test
    fun una_colision_pregunta_por_lesionados_y_no_por_el_hurto() {
        hechos(RadicacionUiState(cargando = false, etapa = Etapa.HECHOS, radicacion = identificada))

        composicion.onNodeWithText(texto(R.string.radicacion_pregunta_lesionados)).assertExists()
        composicion.onNodeWithText(texto(R.string.radicacion_pregunta_alcance_hurto)).assertDoesNotExist()
    }

    @Test
    fun un_hurto_pregunta_por_el_alcance_y_no_por_lesionados() {
        hechos(
            RadicacionUiState(
                cargando = false,
                etapa = Etapa.HECHOS,
                radicacion = identificada.copy(tipo = TipoDeIncidente.HURTO),
            ),
        )

        composicion.onNodeWithText(texto(R.string.radicacion_pregunta_alcance_hurto)).assertExists()
        composicion.onNodeWithText(texto(R.string.radicacion_pregunta_lesionados)).assertDoesNotExist()
    }

    // Criterio 3 — sin lo obligatorio no se avanza, y se dice qué falta.
    @Test
    fun sin_poliza_ni_tipo_no_se_avanza_y_se_dice_que_falta() {
        identificacion(RadicacionUiState(cargando = false, polizas = listOf(poliza)))

        composicion.onNodeWithText(texto(R.string.comun_continuar)).assertIsNotEnabled()
        val faltan = texto(R.string.radicacion_falta_poliza) + " " + texto(R.string.comun_y) + " " +
            texto(R.string.radicacion_falta_tipo)
        composicion.onNodeWithText(texto(R.string.radicacion_faltan, faltan)).assertIsDisplayed()
    }

    // HU-M2.2-01 — el monto estimado es opcional, dice cómo se enviará y solo
    // detiene el paso si lo escrito no es un monto.
    @Test
    fun el_monto_estimado_dice_como_se_enviara_y_deja_avanzar() {
        hechos(
            RadicacionUiState(
                cargando = false,
                etapa = Etapa.HECHOS,
                radicacion = hechosCompletos.copy(montoEstimado = "4800000"),
            ),
        )

        val locale = contexto.resources.configuration.locales[0]
        val vista = texto(R.string.radicacion_monto_vista, MontoEstimado.aTexto(4_800_000, locale))
        composicion.onNodeWithText(vista).assertExists()
        composicion.onNodeWithText(texto(R.string.comun_continuar)).assertIsEnabled()
    }

    @Test
    fun un_monto_invalido_dice_por_que_y_no_deja_avanzar() {
        hechos(
            RadicacionUiState(
                cargando = false,
                etapa = Etapa.HECHOS,
                radicacion = hechosCompletos.copy(montoEstimado = "4,5"),
            ),
        )

        composicion.onNodeWithText(texto(R.string.radicacion_monto_formato)).assertExists()
        composicion.onNodeWithText(texto(R.string.comun_continuar)).assertIsNotEnabled()
        composicion.onNodeWithText(texto(R.string.radicacion_faltan, texto(R.string.radicacion_falta_monto)))
            .assertExists()
    }

    @Test
    fun sin_monto_los_hechos_completos_dejan_avanzar() {
        hechos(RadicacionUiState(cargando = false, etapa = Etapa.HECHOS, radicacion = hechosCompletos))

        composicion.onNodeWithText(texto(R.string.radicacion_monto_ayuda)).assertExists()
        composicion.onNodeWithText(texto(R.string.comun_continuar)).assertIsEnabled()
    }

    @Test
    fun con_poliza_y_tipo_se_puede_avanzar() {
        var avanzo = false
        identificacion(
            RadicacionUiState(cargando = false, polizas = listOf(poliza), radicacion = identificada),
            onContinuar = { avanzo = true },
        )

        composicion.onNodeWithText(texto(R.string.comun_continuar)).assertIsEnabled().performClick()
        assertTrue(avanzo)
    }

    @Test
    fun sin_las_fotos_minimas_no_se_avanza_de_evidencias() {
        val pocas = List(LimitesDeEvidencia.MINIMO_DE_FOTOS - 1) { foto(it) }
        composicion.setContent {
            SolventaTheme {
                PantallaEvidencias(
                    estado = RadicacionUiState(
                        cargando = false,
                        etapa = Etapa.EVIDENCIAS,
                        radicacion = hechosCompletos,
                        evidencias = pocas,
                    ),
                    onCapturar = {},
                    onDescartar = {},
                    onContinuar = {},
                    onGuardarYSalir = {},
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.radicacion_evidencias_faltantes)).assertIsDisplayed()
    }

    // Criterio 4 — sin la declaración bajo juramento no se radica.
    @Test
    fun sin_la_declaracion_no_se_radica() {
        declaracion(
            RadicacionUiState(
                cargando = false,
                etapa = Etapa.DECLARACION,
                radicacion = hechosCompletos,
                evidencias = List(LimitesDeEvidencia.MINIMO_DE_FOTOS) { foto(it) },
            ),
        )

        composicion.onNodeWithText(texto(R.string.radicacion_radicar)).assertIsNotEnabled()
    }

    @Test
    fun con_la_declaracion_y_la_evidencia_enviada_se_radica() {
        var radico = false
        declaracion(
            RadicacionUiState(
                cargando = false,
                etapa = Etapa.DECLARACION,
                radicacion = hechosCompletos.copy(declaracionAceptada = true),
                evidencias = List(LimitesDeEvidencia.MINIMO_DE_FOTOS) { foto(it) },
            ),
            onRadicar = { radico = true },
        )

        composicion.onNodeWithText(texto(R.string.radicacion_radicar)).assertIsEnabled().performClick()
        assertTrue(radico)
    }

    @Test
    fun mientras_la_evidencia_viaja_no_se_radica_y_se_explica() {
        declaracion(
            RadicacionUiState(
                cargando = false,
                etapa = Etapa.DECLARACION,
                radicacion = hechosCompletos.copy(declaracionAceptada = true),
                evidencias = List(LimitesDeEvidencia.MINIMO_DE_FOTOS) { foto(it, EstadoDeEvidencia.PENDIENTE) },
            ),
        )

        composicion.onNodeWithText(texto(R.string.radicacion_radicar)).assertIsNotEnabled()
        composicion.onNodeWithText(texto(R.string.radicacion_evidencias_esperando_envio)).assertIsDisplayed()
    }

    @Test
    fun el_radicado_muestra_el_codigo_unico_del_caso() {
        composicion.setContent {
            SolventaTheme {
                PantallaRadicado(
                    estado = RadicacionUiState(cargando = false, radicacion = hechosCompletos),
                    caso = CasoRadicado(casoId = "c-1", radicado = "SIN-2026-01655", radicadoEn = "2026-10-30T15:00:00Z"),
                    onIrAMisSiniestros = {},
                    onIrAlInicio = {},
                )
            }
        }

        composicion.onNodeWithText("SIN-2026-01655", substring = true).assertIsDisplayed()
    }

    // Criterio 5 — la barra anuncia cuánto se lleva recorrido.
    @Test
    fun la_barra_de_progreso_anuncia_el_avance_de_la_etapa() {
        hechos(RadicacionUiState(cargando = false, etapa = Etapa.HECHOS, radicacion = identificada))

        composicion.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.5f, 0f..1f))).assertExists()
    }
}
