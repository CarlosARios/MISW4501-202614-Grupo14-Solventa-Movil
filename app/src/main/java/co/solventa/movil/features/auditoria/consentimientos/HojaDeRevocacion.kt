package co.solventa.movil.features.auditoria.consentimientos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.solventa.movil.R
import co.solventa.movil.core.i18n.ConIdioma
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/** Minimo de la contraseña de Solventa, el mismo del ingreso (HU-M1.1-02). */
internal const val MINIMO_DE_CONTRASENA = 10

/**
 * La hoja inferior que confirma la revocacion (HU-M1.3-02, criterio 1):
 * frame `37:1519`, con la huella o con la contraseña. Explica el
 * efecto y pide la confirmacion; tocar fuera de la hoja o «Cancelar» la cierra
 * sin cambiar nada (criterio 2). Mientras se verifica no se puede cerrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDeRevocacion(
    entidad: String,
    hoja: PasoDeRevocacion.Hoja,
    idioma: Idioma,
    onConfirmarConBiometria: () -> Unit,
    onConfirmarConContrasena: (String) -> Unit,
    onCancelar: () -> Unit,
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onCancelar, sheetState = estado) {
        // La hoja se dibuja en su propia ventana, que vuelve a proveer el
        // contexto y la configuracion del dispositivo: sin esto saldria en el
        // idioma del telefono y no en el de la aplicacion (como HojaDeIdioma).
        ConIdioma(idioma) {
            ContenidoHojaDeRevocacion(
                entidad = entidad,
                hoja = hoja,
                onConfirmarConBiometria = onConfirmarConBiometria,
                onConfirmarConContrasena = onConfirmarConContrasena,
                onCancelar = onCancelar,
            )
        }
    }
}

/** El cuerpo de la hoja, sin estado de la hoja misma: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoHojaDeRevocacion(
    entidad: String,
    hoja: PasoDeRevocacion.Hoja,
    onConfirmarConBiometria: () -> Unit,
    onConfirmarConContrasena: (String) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    var contrasena by rememberSaveable { mutableStateOf("") }
    val conContrasena = hoja.metodo == MetodoDeConfirmacion.CONTRASENA
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x16),
        modifier = modifier.padding(start = Espacio.x16, end = Espacio.x16, bottom = Espacio.x32),
    ) {
        Text(text = stringResource(R.string.auditoria_revocacion_hoja_titulo, entidad), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.auditoria_revocacion_hoja_efecto) + " " + stringResource(
                if (conContrasena) R.string.auditoria_revocacion_hoja_con_contrasena else R.string.auditoria_revocacion_hoja_con_biometria,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.textoSecundario,
        )
        if (conContrasena) {
            CampoDeContrasena(valor = contrasena, onCambio = { contrasena = it }, habilitado = !hoja.verificando)
        } else {
            val descripcion = stringResource(R.string.auditoria_revocacion_hoja_icono_biometria)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    painter = painterResource(R.drawable.ic_huella),
                    contentDescription = descripcion,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        if (hoja.sinConexion) {
            AvisoSolventa(tipo = TipoAviso.ADVERTENCIA, texto = stringResource(R.string.auditoria_revocacion_hoja_sin_conexion))
        }
        if (hoja.verificando) {
            val verificando = stringResource(R.string.auditoria_revocacion_hoja_verificando)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = verificando })
            }
        }
        BotonDeRevocar(
            texto = stringResource(
                if (conContrasena) {
                    R.string.auditoria_revocacion_hoja_confirmar_contrasena
                } else {
                    R.string.auditoria_revocacion_hoja_confirmar_biometria
                },
            ),
            conHuella = !conContrasena,
            habilitado = !hoja.verificando && (!conContrasena || contrasena.length >= MINIMO_DE_CONTRASENA),
            onClick = { if (conContrasena) onConfirmarConContrasena(contrasena) else onConfirmarConBiometria() },
        )
        BotonSolventa(
            texto = stringResource(R.string.comun_cancelar),
            onClick = onCancelar,
            tipo = TipoBoton.SECUNDARIO,
            habilitado = !hoja.verificando,
        )
    }
}

/** El campo «Contraseña» del inicio de sesion (`23:519`), con su ayuda. */
@Composable
private fun CampoDeContrasena(valor: String, onCambio: (String) -> Unit, habilitado: Boolean) {
    val colores = LocalColoresSolventa.current
    val etiqueta = stringResource(R.string.auditoria_revocacion_hoja_contrasena)
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x4), modifier = Modifier.fillMaxWidth()) {
        Text(text = etiqueta, style = MaterialTheme.typography.labelMedium, color = colores.textoSecundario, letterSpacing = 1.sp)
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = true,
            enabled = habilitado,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = etiqueta },
        )
        Text(
            text = stringResource(R.string.auditoria_revocacion_hoja_contrasena_minimo, MINIMO_DE_CONTRASENA),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )
    }
}
