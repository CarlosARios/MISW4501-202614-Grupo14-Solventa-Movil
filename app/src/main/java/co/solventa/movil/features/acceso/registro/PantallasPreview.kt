package co.solventa.movil.features.acceso.registro

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.solventa.movil.shared.ui.camara.camaraSimulada
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * Vistas previas de las ocho pantallas del registro, al tamaño del lienzo del
 * prototipo (390 × 844). Sirven para comparar contra los frames de Figma sin
 * levantar el emulador.
 *
 * Quedan fuera de la medición de cobertura: la anotación `@Preview` está en la
 * lista de exclusiones de Kover.
 */
private const val ANCHO = 390
private const val ALTO = 844

private val DATOS_DE_MUESTRA = DatosDocumento(
    nombres = "Carolina",
    apellidos = "Pérez Ruiz",
    tipoDocumento = "CC",
    numeroDocumento = "1.020.345.678",
    fechaNacimiento = "14/05/1991",
    correo = "carolina.perez@correo.com",
    celular = "+57 301 234 5678",
)

@Preview(name = "M1a · Captura del frente", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaCapturaFrente() {
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

@Preview(name = "M1b · Captura del reverso", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaCapturaReverso() {
    SolventaTheme {
        CapturaDocumentoPantalla(
            cara = CaraDocumento.REVERSO,
            estado = RegistroUiState(frente = ImagenDocumento(CaraDocumento.FRENTE, "")),
            onCapturar = {},
            onLuzInsuficiente = {},
            camara = camaraSimulada(),
            onDescartarAviso = {},
            onAtras = {},
        )
    }
}

@Preview(name = "M1c · Confirmación de datos", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaConfirmacionDatos() {
    SolventaTheme {
        ConfirmacionDatosPantalla(
            estado = RegistroUiState(
                datos = DATOS_DE_MUESTRA,
                contrasena = "Solventa.2026.4821",
                terminosAceptados = true,
            ),
            onDatosCambian = {},
            onContrasenaCambia = {},
            onTerminos = {},
            onVerificar = {},
            onAtras = {},
        )
    }
}

@Preview(name = "M1d · Verificación exitosa", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaVerificacionExitosa() {
    SolventaTheme { VerificacionExitosaPantalla(onIrAlInicio = {}) }
}

@Preview(name = "M1e · Verificación no superada", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaVerificacionNoSuperada() {
    SolventaTheme {
        VerificacionNoSuperadaPantalla(
            referencia = "KYC-2026-77A2 · 16/09/2026 15:20",
            onIntentarDeNuevo = {},
            onContactarSoporte = {},
        )
    }
}

@Preview(name = "M1g · Verificando identidad", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaVerificando() {
    SolventaTheme { VerificandoPantalla(onAvisarPorNotificacion = {}) }
}

@Preview(name = "M1h · La verificación tarda", widthDp = ANCHO, heightDp = ALTO)
@Composable
private fun VistaPreviaVerificacionDemorada() {
    SolventaTheme {
        VerificacionDemoradaPantalla(onEntendido = {}, onContactarSoporte = {})
    }
}

@Preview(name = "M1a en inglés", widthDp = ANCHO, heightDp = ALTO, locale = "en")
@Composable
private fun VistaPreviaCapturaFrenteIngles() {
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
