package co.solventa.movil.features.acceso.ingreso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.solventa.movil.R
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.CirculoConIcono
import co.solventa.movil.shared.ui.LogotipoSolventa
import co.solventa.movil.shared.ui.SeparadorHorizontal
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import kotlinx.coroutines.launch

/**
 * M2a y M2c · ingreso con la biometría del dispositivo.
 *
 * Son la misma pantalla: M2c es el estado con intentos fallidos. El diálogo
 * que pide la huella lo dibuja el sistema operativo, no esta pantalla; aquí
 * queda el contexto que se ve detrás y la salida hacia la contraseña.
 */
@Composable
fun IngresoBiometricoPantalla(
    estado: IngresoUiState,
    onReintentar: () -> Unit,
    onUsarContrasena: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val fallo = estado.intentosFallidos > 0

    ArmazonDeAcceso(
        modifier = modifier,
        centrado = true,
        acciones = {
            if (estado.biometriaPausada) {
                BotonSolventa(
                    texto = stringResource(R.string.ingreso_biometria_usar_contrasena),
                    onClick = onUsarContrasena,
                    icono = painterResource(R.drawable.ic_candado),
                )
            } else {
                if (fallo) {
                    BotonSolventa(
                        texto = stringResource(R.string.ingreso_biometria_reintentar),
                        onClick = onReintentar,
                        icono = painterResource(R.drawable.ic_huella),
                        habilitado = !estado.solicitandoHuella,
                    )
                }
                BotonSolventa(
                    texto = stringResource(R.string.ingreso_biometria_usar_contrasena),
                    onClick = onUsarContrasena,
                    tipo = TipoBoton.TERCIARIO,
                )
            }
        },
    ) {
        LogotipoSolventa(modifier = Modifier.align(Alignment.CenterHorizontally))

        Text(
            text = stringResource(R.string.ingreso_saludo, estado.nombre),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!fallo) {
            Text(
                text = stringResource(R.string.ingreso_biometria_instruccion),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.textoSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        CirculoConIcono(
            icono = R.drawable.ic_huella,
            tinte = if (fallo) MaterialTheme.colorScheme.error else colores.informacionFuerte,
            fondo = if (fallo) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                colores.informacionSuave
            },
            descripcion = stringResource(R.string.ingreso_biometria_descripcion),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Text(
            text = stringResource(
                if (fallo) {
                    R.string.ingreso_biometria_no_reconocida
                } else {
                    R.string.ingreso_biometria_toca
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (fallo) {
            AvisoSolventa(
                tipo = TipoAviso.ERROR,
                titulo = stringResource(
                    R.string.ingreso_biometria_intento,
                    estado.intentosFallidos,
                    IngresoUiState.MAXIMO_DE_INTENTOS,
                ),
                texto = stringResource(
                    if (estado.biometriaPausada) {
                        R.string.ingreso_biometria_pausada
                    } else {
                        R.string.ingreso_biometria_intenta_de_nuevo
                    },
                ),
            )
        }

        if (estado.biometriaPausada) {
            Text(
                text = stringResource(R.string.ingreso_biometria_proximo),
                style = MaterialTheme.typography.bodySmall,
                color = colores.textoSecundario,
            )
        }
    }
}

/** M2b · ingreso con correo y contraseña. */
@Composable
fun IngresoContrasenaPantalla(
    estado: IngresoUiState,
    onCorreo: (String) -> Unit,
    onContrasena: (String) -> Unit,
    onHabilitarBiometria: (Boolean) -> Unit,
    onEntrar: () -> Unit,
    onOlvideContrasena: () -> Unit,
    onCrearCuenta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current

    ArmazonDeAcceso(
        modifier = modifier,
        acciones = {
            BotonSolventa(
                texto = stringResource(R.string.ingreso_contrasena_crear_cuenta),
                onClick = onCrearCuenta,
                tipo = TipoBoton.TERCIARIO,
            )
        },
    ) {
        LogotipoSolventa(modifier = Modifier.align(Alignment.CenterHorizontally))

        Text(
            text = stringResource(R.string.ingreso_contrasena_titulo),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            // A esta pantalla se llega por tres caminos y cada uno necesita
            // su explicación: la biometría quedó pausada, el dispositivo no
            // la tiene habilitada, o el usuario la prefirió.
            text = stringResource(
                when {
                    estado.biometriaPausada -> R.string.ingreso_biometria_pausada
                    !estado.biometriaDisponible -> R.string.ingreso_contrasena_introduccion
                    else -> R.string.ingreso_contrasena_por_eleccion
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.textoSecundario,
        )

        CampoConEtiqueta(
            etiqueta = stringResource(R.string.ingreso_contrasena_correo),
            valor = estado.correo,
            onCambio = onCorreo,
            tipoDeTeclado = KeyboardType.Email,
        )

        CampoConEtiqueta(
            etiqueta = stringResource(R.string.ingreso_contrasena_clave),
            valor = estado.contrasena,
            onCambio = onContrasena,
            tipoDeTeclado = KeyboardType.Password,
            oculto = true,
            ayuda = stringResource(
                R.string.ingreso_contrasena_minimo,
                IngresoUiState.MINIMO_DE_CONTRASENA,
            ),
        )

        val error = estado.error
        if (error != null) {
            AvisoSolventa(
                tipo = TipoAviso.ERROR,
                texto = stringResource(
                    when (error) {
                        ErrorDeIngreso.CredencialesInvalidas -> R.string.ingreso_error_credenciales
                        ErrorDeIngreso.SinConexion -> R.string.ingreso_error_sin_conexion
                        ErrorDeIngreso.BiometriaNoDisponible ->
                            R.string.ingreso_biometria_no_disponible
                    },
                ),
            )
        }

        BotonSolventa(
            texto = stringResource(R.string.ingreso_contrasena_entrar),
            onClick = onEntrar,
            icono = painterResource(R.drawable.ic_iniciar_sesion),
            habilitado = estado.puedeEntrar,
        )

        BotonSolventa(
            texto = stringResource(R.string.ingreso_contrasena_olvide),
            onClick = onOlvideContrasena,
            tipo = TipoBoton.TERCIARIO,
        )

        SeparadorHorizontal()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.x12),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Switch(
                checked = estado.habilitarBiometriaAlEntrar,
                onCheckedChange = onHabilitarBiometria,
            )
            Text(
                text = stringResource(R.string.ingreso_contrasena_habilitar_biometria),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            text = stringResource(R.string.ingreso_contrasena_cierre_automatico),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )

        SeparadorHorizontal()
    }
}

/**
 * M2d · la sesión expiró por inactividad.
 *
 * Es una hoja inferior y no una pantalla: aparece encima de donde estaba el
 * usuario, porque el mensaje promete devolverlo ahí mismo.
 *
 * La hoja no se puede ignorar, y conseguirlo pide más que dejar vacío
 * `onDismissRequest`: tocar el velo oculta la hoja igual y Material3 deja su
 * ventana encima tragándose todos los toques, con la aplicación bloqueada
 * —así se encontró en el Moto G20—. Por eso se desactivan los gestos y el
 * botón atrás, y si aun así llega la petición de cierre, se vuelve a mostrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDeSesionExpirada(
    onConHuella: () -> Unit,
    onConContrasena: () -> Unit,
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val alcance = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { alcance.launch { estado.show() } },
        sheetState = estado,
        sheetGesturesEnabled = false,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        dragHandle = null,
    ) {
        ContenidoSesionExpirada(
            onConHuella = onConHuella,
            onConContrasena = onConContrasena,
        )
    }
}

/** El cuerpo de M2d, separado para poder dibujarlo en las vistas previas. */
@Composable
fun ContenidoSesionExpirada(
    onConHuella: () -> Unit,
    onConContrasena: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x16),
        // La hoja va sin asa de arrastre —no se puede cerrar arrastrándola—,
        // así que el margen superior lo pone el contenido.
        modifier = modifier.padding(
            start = Espacio.x16,
            top = Espacio.x24,
            end = Espacio.x16,
            bottom = Espacio.x32,
        ),
    ) {
        Text(
            text = stringResource(R.string.sesion_expirada_titulo),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12)) {
            Icon(
                painter = painterResource(R.drawable.ic_reloj),
                contentDescription = null,
                tint = colores.textoSecundario,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.sesion_expirada_detalle),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.textoSecundario,
            )
        }
        BotonSolventa(
            texto = stringResource(R.string.sesion_expirada_con_huella),
            onClick = onConHuella,
            icono = painterResource(R.drawable.ic_huella),
        )
        BotonSolventa(
            texto = stringResource(R.string.sesion_expirada_con_contrasena),
            onClick = onConContrasena,
            tipo = TipoBoton.SECUNDARIO,
        )
    }
}

/**
 * Armazón de las pantallas de acceso: sin barra superior, porque el logotipo
 * hace de encabezado y no hay a dónde volver.
 */
@Composable
private fun ArmazonDeAcceso(
    modifier: Modifier = Modifier,
    centrado: Boolean = false,
    acciones: @Composable ColumnScope.() -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Espacio.x8),
                modifier = Modifier
                    .fillMaxWidth()
                    // Igual que en el contenedor de registro: con el dibujo
                    // de borde a borde, apartarse del teclado es cosa de la
                    // barra de acciones.
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = Espacio.x16, vertical = Espacio.x16),
                content = acciones,
            )
        },
    ) { relleno ->
        Column(
            verticalArrangement = if (centrado) {
                Arrangement.spacedBy(Espacio.x16, Alignment.CenterVertically)
            } else {
                Arrangement.spacedBy(Espacio.x16)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espacio.x16, vertical = Espacio.x24),
            content = contenido,
        )
    }
}

/**
 * Campo con la etiqueta encima en versales, como en M2b. No se usa la
 * etiqueta flotante de Material porque el diseño muestra las dos cosas a la
 * vez: el rótulo arriba y el valor dentro.
 */
@Composable
private fun CampoConEtiqueta(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    tipoDeTeclado: KeyboardType = KeyboardType.Text,
    oculto: Boolean = false,
    ayuda: String? = null,
) {
    val colores = LocalColoresSolventa.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = colores.textoSecundario,
            letterSpacing = 1.sp,
        )
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = true,
            visualTransformation = if (oculto) {
                PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = tipoDeTeclado),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        if (ayuda != null) {
            Text(
                text = ayuda,
                style = MaterialTheme.typography.bodySmall,
                color = colores.textoSecundario,
            )
        }
    }
}
