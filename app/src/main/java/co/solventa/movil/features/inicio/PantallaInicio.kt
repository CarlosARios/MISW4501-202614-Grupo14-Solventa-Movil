package co.solventa.movil.features.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.SolventaTheme

/**
 * M3 · Inicio, en versión provisional.
 *
 * La pantalla definitiva pertenece a otra historia y todavía no está
 * construida. Mientras tanto, esta hace dos cosas que sí hacen falta hoy:
 * saluda con la identidad real que devolvió el *user pool* —es la prueba
 * visible de que el ingreso contra Cognito funcionó— y da acceso a las
 * pantallas que sí están entregadas, para poder recorrerlas en una
 * demostración sin reinstalar la aplicación.
 *
 * **Está rotulada como provisional en la propia pantalla**, a propósito: no
 * se presenta como M3, sino como lo que es.
 */
@Composable
fun PantallaInicio(
    onRegistro: () -> Unit,
    onReportarSiniestro: () -> Unit,
    onVerificacionExitosa: () -> Unit,
    onVerificacionNoSuperada: () -> Unit,
    onVerificacionDemorada: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificaciones: () -> Unit = {},
    vm: InicioViewModel = hiltViewModel(),
) {
    val sesion by vm.sesion.collectAsStateWithLifecycle()

    ContenidoInicio(
        nombre = sesion?.nombre.orEmpty(),
        correo = sesion?.correo.orEmpty(),
        onRegistro = onRegistro,
        onReportarSiniestro = onReportarSiniestro,
        onVerificacionExitosa = onVerificacionExitosa,
        onVerificacionNoSuperada = onVerificacionNoSuperada,
        onVerificacionDemorada = onVerificacionDemorada,
        onCerrarSesion = { vm.cerrarSesion(onCerrarSesion) },
        modifier = modifier,
        onNotificaciones = onNotificaciones,
    )
}

@Composable
private fun ContenidoInicio(
    nombre: String,
    correo: String,
    onRegistro: () -> Unit,
    onReportarSiniestro: () -> Unit,
    onVerificacionExitosa: () -> Unit,
    onVerificacionNoSuperada: () -> Unit,
    onVerificacionDemorada: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificaciones: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x12),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espacio.x24),
    ) {
        Text(
            text = stringResource(R.string.inicio_saludo, nombre),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(text = correo, style = MaterialTheme.typography.bodyMedium)

        Text(
            text = stringResource(R.string.inicio_provisional),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = Espacio.x12),
        )

        // La radicacion del siniestro (HU-M2.2-01) es lo primero que se hace
        // desde el inicio, no una pantalla mas de la lista de entregadas.
        BotonSolventa(
            texto = stringResource(R.string.inicio_reportar_siniestro),
            onClick = onReportarSiniestro,
            modifier = Modifier.padding(top = Espacio.x12),
        )
        // El centro de notificaciones (HU-M3.1-01): la campana de M3, mientras
        // Inicio siga siendo provisional.
        BotonSolventa(
            texto = stringResource(R.string.inicio_ir_notificaciones),
            onClick = onNotificaciones,
            tipo = TipoBoton.SECUNDARIO,
        )

        Text(
            text = stringResource(R.string.inicio_pantallas_titulo),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Espacio.x12),
        )
        BotonSolventa(texto = stringResource(R.string.inicio_ir_registro), onClick = onRegistro)
        BotonSolventa(
            texto = stringResource(R.string.inicio_ir_verificacion_exitosa),
            onClick = onVerificacionExitosa,
            tipo = TipoBoton.SECUNDARIO,
        )
        BotonSolventa(
            texto = stringResource(R.string.inicio_ir_verificacion_no_superada),
            onClick = onVerificacionNoSuperada,
            tipo = TipoBoton.SECUNDARIO,
        )
        BotonSolventa(
            texto = stringResource(R.string.inicio_ir_verificacion_demorada),
            onClick = onVerificacionDemorada,
            tipo = TipoBoton.SECUNDARIO,
        )
        BotonSolventa(
            texto = stringResource(R.string.inicio_cerrar_sesion),
            onClick = onCerrarSesion,
            tipo = TipoBoton.TERCIARIO,
            modifier = Modifier.padding(top = Espacio.x12),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VistaPreviaInicio() {
    SolventaTheme {
        ContenidoInicio(
            nombre = "Carolina",
            correo = "asegurado@example.com",
            onRegistro = {},
            onReportarSiniestro = {},
            onVerificacionExitosa = {},
            onVerificacionNoSuperada = {},
            onVerificacionDemorada = {},
            onCerrarSesion = {},
        )
    }
}
