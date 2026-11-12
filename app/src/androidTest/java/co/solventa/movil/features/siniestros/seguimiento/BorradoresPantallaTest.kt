package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.features.siniestros.radicacion.BorradorResumen
import co.solventa.movil.features.siniestros.radicacion.EnvioDelBorrador
import co.solventa.movil.features.siniestros.radicacion.Etapa
import co.solventa.movil.features.siniestros.radicacion.TipoDeIncidente
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Los borradores en *Mis siniestros* (HU-M2.2-02): la tarjeta *Sin enviar*
 * (M13b), el aviso del envío completado con su radicado (M13c) y el del envío
 * fallido con *Reintentar ahora* (M13d). Se ven aunque el servicio no
 * responda: sin señal es justo lo que hay que mostrar.
 */
class BorradoresPantallaTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int, vararg args: Any) = contexto.getString(id, *args)

    private var abierto: String? = null
    private var reintentos = 0
    private var reportes = 0

    private fun borrador(envio: EnvioDelBorrador, intentos: Int = 0) = BorradorResumen(
        id = "b1",
        etapa = Etapa.EVIDENCIAS,
        tipo = TipoDeIncidente.COLISION,
        poliza = "AU-2025-118204",
        actualizadoEn = DatosDeConsulta.AHORA,
        envio = envio,
        intentos = intentos,
        ultimoIntentoEn = DatosDeConsulta.AHORA,
        fotos = 4,
        videos = 1,
    )

    private fun montar(borradores: BorradoresUiState, consulta: MisSiniestrosUiState = MisSiniestrosUiState(cargando = false)) {
        composicion.setContent {
            SolventaTheme {
                ContenidoMisSiniestros(
                    estado = consulta,
                    idioma = Idioma.ESPANOL,
                    ahora = DatosDeConsulta.AHORA,
                    onAbrir = {},
                    onReintentar = {},
                    borradores = borradores,
                    onReportar = { reportes++ },
                    onAbrirBorrador = { abierto = it },
                    onReintentarEnvios = { reintentos++ },
                )
            }
        }
    }

    private fun aviso(id: Int) =
        composicion.onAllNodesWithContentDescription(texto(id), substring = true).onFirst()

    @Test
    fun la_tarjeta_del_borrador_sale_sin_enviar_aun_sin_respuesta_del_servicio() {
        montar(
            BorradoresUiState(borradores = listOf(borrador(EnvioDelBorrador.EN_COLA))),
            consulta = MisSiniestrosUiState(cargando = false, fallo = FalloDeConsulta.SIN_CONEXION),
        )

        composicion.onNodeWithText(texto(R.string.siniestros_borradores_seccion, 1).uppercase()).assertIsDisplayed()
        composicion.onNodeWithText(
            texto(R.string.siniestros_borradores_titulo, texto(R.string.radicacion_tipo_colision)),
        ).assertIsDisplayed().performClick()
        composicion.onNodeWithText(texto(R.string.siniestros_borradores_sin_enviar)).assertIsDisplayed()
        assertEquals("b1", abierto)
    }

    @Test
    fun el_envio_completado_muestra_el_radicado() {
        montar(
            BorradoresUiState(
                radicado = borrador(EnvioDelBorrador.RADICADO, intentos = 2)
                    .copy(radicado = "SIN-2026-01877", radicadoEn = DatosDeConsulta.AHORA),
            ),
        )

        aviso(R.string.siniestros_borradores_registrado_titulo).assertIsDisplayed()
        composicion.onAllNodesWithContentDescription("SIN-2026-01877", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun el_envio_fallido_ofrece_reintentar_ahora() {
        montar(BorradoresUiState(borradores = listOf(borrador(EnvioDelBorrador.FALLIDO, intentos = 3))))

        aviso(R.string.siniestros_borradores_fallido_titulo).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_borradores_envio_fallido)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.siniestros_seguimiento_reintentar_ahora)).performClick()

        assertEquals(1, reintentos)
    }

    @Test
    fun reportar_un_siniestro_abre_el_asistente() {
        montar(BorradoresUiState())

        composicion.onNodeWithText(texto(R.string.siniestros_borradores_reportar)).performClick()

        assertEquals(1, reportes)
    }
}
