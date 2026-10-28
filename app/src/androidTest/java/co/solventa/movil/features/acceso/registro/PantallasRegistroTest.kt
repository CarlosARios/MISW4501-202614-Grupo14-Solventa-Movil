package co.solventa.movil.features.acceso.registro

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import co.solventa.movil.R
import co.solventa.movil.shared.ui.camara.camaraSimulada
import co.solventa.movil.ui.theme.SolventaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de interfaz de las pantallas del registro (HU-M1.1-01).
 *
 * Verifican lo que las unitarias no pueden: que el estado del `ViewModel`
 * llegue a la pantalla y que los criterios de aceptación se vean. Se prueban
 * los composables directamente con estado fijo, sin Hilt ni navegación: lo
 * que está bajo prueba es la pantalla, no el cableado.
 */
class PantallasRegistroTest {

    @get:Rule
    val composicion = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    private fun texto(id: Int) = contexto.getString(id)

    private val datos = DatosDocumento(
        nombres = "Carolina",
        apellidos = "Pérez Ruiz",
        tipoDocumento = "CC",
        numeroDocumento = "1.020.345.678",
        fechaNacimiento = "14/05/1991",
        correo = "carolina.perez@correo.com",
        celular = "+57 301 234 5678",
    )

    /** M1c pide la contraseña para habilitar la verificación. */
    private val contrasena = "Solventa2026!"

    // Criterio 1 — la captura guía al usuario paso a paso.
    @Test
    fun m1a_guia_la_captura_del_frente() {
        composicion.setContent {
            SolventaTheme {
                CapturaDocumentoPantalla(
                    cara = CaraDocumento.FRENTE,
                    estado = RegistroUiState(),
                    onCapturar = {},
                    onLuzInsuficiente = {},
                    camara = camaraSimulada(),
                    onDescartarAviso = {},
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.registro_captura_paso)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_captura_frente_instruccion))
            .assertIsDisplayed()
        // El marco y los consejos se anuncian como una sola unidad, así que
        // se preguntan por su descripción y no por su texto: es lo que oye
        // quien usa un lector de pantalla.
        composicion.onNodeWithContentDescription(
            texto(R.string.registro_captura_marco_descripcion),
        ).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.registro_captura_consejo_luz))
            .assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_captura_accion)).assertHasClickAction()
    }

    @Test
    fun m1b_pide_el_reverso_y_no_el_frente() {
        composicion.setContent {
            SolventaTheme {
                CapturaDocumentoPantalla(
                    cara = CaraDocumento.REVERSO,
                    estado = RegistroUiState(),
                    onCapturar = {},
                    onLuzInsuficiente = {},
                    camara = camaraSimulada(),
                    onDescartarAviso = {},
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.registro_captura_reverso_instruccion))
            .assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_captura_frente_instruccion))
            .assertDoesNotExist()
    }

    // Criterio 4 — la calidad insuficiente se informa y se explica cómo
    // corregirla, sin dejar continuar con esa imagen.
    @Test
    fun m1f_explica_la_poca_luz_cuando_el_estado_lo_reporta() {
        composicion.setContent {
            SolventaTheme {
                CapturaDocumentoPantalla(
                    cara = CaraDocumento.FRENTE,
                    estado = RegistroUiState(
                        problemaCalidad = ProblemaCalidad.ILUMINACION_INSUFICIENTE,
                    ),
                    onCapturar = {},
                    onLuzInsuficiente = {},
                    camara = camaraSimulada(),
                    onDescartarAviso = {},
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.registro_calidad_oscura_titulo))
            .assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_calidad_oscura_detalle))
            .assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_calidad_reintentar))
            .assertHasClickAction()
    }

    // Criterio 1 — confirmar los datos antes de enviarlos a verificación.
    @Test
    fun m1c_muestra_los_datos_extraidos_del_documento() {
        composicion.setContent {
            SolventaTheme {
                ConfirmacionDatosPantalla(
                    estado = RegistroUiState(datos = datos),
                    onDatosCambian = {},
                    onContrasenaCambia = {},
                    onTerminos = {},
                    onVerificar = {},
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithText("Carolina").assertIsDisplayed()
        composicion.onNodeWithText("CC 1.020.345.678").assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_datos_documento_ayuda))
            .assertIsDisplayed()
    }

    @Test
    fun m1c_no_deja_verificar_sin_aceptar_los_terminos() {
        composicion.setContent {
            SolventaTheme {
                ConfirmacionDatosPantalla(
                    estado = RegistroUiState(
                        datos = datos,
                        contrasena = contrasena,
                        terminosAceptados = false,
                    ),
                    onDatosCambian = {},
                    onContrasenaCambia = {},
                    onTerminos = {},
                    onVerificar = {},
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.registro_datos_accion)).assertIsNotEnabled()
    }

    @Test
    fun m1c_habilita_la_verificacion_con_los_terminos_aceptados() {
        var verificado = false
        composicion.setContent {
            SolventaTheme {
                ConfirmacionDatosPantalla(
                    estado = RegistroUiState(
                        datos = datos,
                        contrasena = contrasena,
                        terminosAceptados = true,
                    ),
                    onDatosCambian = {},
                    onContrasenaCambia = {},
                    onTerminos = {},
                    onVerificar = { verificado = true },
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.registro_datos_accion))
            .assertIsEnabled()
            .performClick()

        assertTrue(verificado)
    }

    // Criterio 6 — mientras verifica, el usuario ve el avance y tiene salida.
    @Test
    fun m1g_muestra_el_avance_y_ofrece_salir_de_la_espera() {
        var aviso = false
        composicion.setContent {
            SolventaTheme { VerificandoPantalla(onAvisarPorNotificacion = { aviso = true }) }
        }

        composicion.onNodeWithText(texto(R.string.registro_verificando_encabezado))
            .assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.registro_verificando_prueba_vida))
            .assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_verificando_avisarme)).performClick()

        assertTrue(aviso)
    }

    // Criterio 2 — la verificación exitosa informa que la cuenta quedó activa.
    @Test
    fun m1d_confirma_que_la_cuenta_quedo_activa() {
        composicion.setContent {
            SolventaTheme { VerificacionExitosaPantalla(onIrAlInicio = {}) }
        }

        composicion.onNodeWithText(texto(R.string.registro_exito_encabezado)).assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_exito_detalle)).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.registro_exito_listas))
            .assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.registro_exito_accion)).assertHasClickAction()
    }

    /**
     * Criterio 3 — el fallo **no** revela el motivo técnico.
     *
     * No basta con ver el mensaje correcto: la prueba afirma que ninguna de
     * las tres causas reales aparece en pantalla. Si alguien agrega el motivo
     * «para que el usuario entienda mejor», esta prueba lo detiene.
     */
    @Test
    fun m1e_no_revela_por_que_fallo_la_verificacion() {
        composicion.setContent {
            SolventaTheme {
                VerificacionNoSuperadaPantalla(
                    referencia = "KYC-2026-77A2",
                    onIntentarDeNuevo = {},
                    onContactarSoporte = {},
                )
            }
        }

        composicion.onNodeWithText(texto(R.string.registro_fallo_encabezado)).assertIsDisplayed()
        composicion.onNodeWithText("KYC-2026-77A2", substring = true).assertIsDisplayed()

        listOf("restrictiv", "restricted", "ilegible", "illegible", "no verificable")
            .forEach { causa ->
                composicion.onNodeWithText(causa, substring = true, ignoreCase = true)
                    .assertDoesNotExist()
            }
    }

    // Criterios 5 y 6 — se avisa por notificación y se dice, sin rodeos, que
    // la cuenta todavía no está activa.
    @Test
    fun m1h_avisa_que_la_cuenta_no_esta_activa() {
        composicion.setContent {
            SolventaTheme {
                VerificacionDemoradaPantalla(onEntendido = {}, onContactarSoporte = {})
            }
        }

        composicion.onNodeWithText(texto(R.string.registro_demora_encabezado)).assertIsDisplayed()
        composicion.onNodeWithContentDescription(texto(R.string.registro_demora_cuenta_inactiva))
            .assertIsDisplayed()
        composicion.onNodeWithText(texto(R.string.comun_entendido)).assertHasClickAction()
    }

    // OBJ 13 — todo elemento no textual tiene descripción para el lector de
    // pantalla.
    @Test
    fun el_boton_de_atras_es_accesible() {
        composicion.setContent {
            SolventaTheme {
                CapturaDocumentoPantalla(
                    cara = CaraDocumento.FRENTE,
                    estado = RegistroUiState(),
                    onCapturar = {},
                    onLuzInsuficiente = {},
                    camara = camaraSimulada(),
                    onDescartarAviso = {},
                    onAtras = {},
                )
            }
        }

        composicion.onNodeWithContentDescription(texto(R.string.comun_atras))
            .assertHasClickAction()
        composicion.onNodeWithContentDescription(
            texto(R.string.registro_captura_marco_descripcion),
        ).assertIsDisplayed()
    }
}
