package co.solventa.movil.features.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.shared.ui.HojaDeIdioma
import co.solventa.movil.shared.ui.InsigniaEstado
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.SeparadorTarjeta
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoInsignia
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M19 · Perfil (HU-M1.3-01). Es una pestaña de la barra inferior, asi que no
 * tiene flecha de volver.
 */
@Composable
fun PerfilPantalla(
    onConsentimientos: () -> Unit,
    onSesionCerrada: () -> Unit,
    modifier: Modifier = Modifier,
    vm: PerfilViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    ContenidoPerfil(
        estado = estado,
        onAlternarBiometria = vm::alternarBiometria,
        onElegirIdioma = vm::elegirIdioma,
        onConsentimientos = onConsentimientos,
        onCerrarSesion = { vm.cerrarSesion(onSesionCerrada) },
        modifier = modifier,
        onConfirmarCierre = { vm.confirmarCierre(onSesionCerrada) },
        onCancelarCierre = vm::cancelarCierre,
    )
}

/** El perfil sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoPerfil(
    estado: PerfilUiState,
    onAlternarBiometria: (Boolean) -> Unit,
    onElegirIdioma: (Idioma) -> Unit,
    onConsentimientos: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    onConfirmarCierre: () -> Unit = {},
    onCancelarCierre: () -> Unit = {},
) {
    var hojaDeIdioma by rememberSaveable { mutableStateOf(false) }

    PantallaRegistro(titulo = stringResource(R.string.perfil_titulo), modifier = modifier) {
        Identidad(estado)

        Seccion(stringResource(R.string.perfil_seccion_cuenta)) {
            FilaDePerfil(R.drawable.ic_perfil, stringResource(R.string.perfil_datos_personales), null, alTocar = null)
            SeparadorTarjeta()
            FilaDePerfil(R.drawable.ic_tarjeta, stringResource(R.string.perfil_medios_de_pago), null, alTocar = null)
        }

        Seccion(stringResource(R.string.perfil_seccion_seguridad)) {
            FilaConInterruptor(
                icono = R.drawable.ic_huella,
                titulo = stringResource(R.string.perfil_biometria),
                activado = estado.biometriaHabilitada,
                habilitado = estado.biometriaDisponible,
                onCambio = onAlternarBiometria,
                detalle = if (estado.biometriaDisponible) null else stringResource(R.string.perfil_biometria_no_disponible),
            )
            SeparadorTarjeta()
            FilaDePerfil(R.drawable.ic_candado, stringResource(R.string.perfil_cambiar_contrasena), null, alTocar = null)
            SeparadorTarjeta()
            FilaInformativaDePerfil(
                icono = R.drawable.ic_reloj,
                titulo = stringResource(R.string.perfil_cierre_automatico),
                valor = stringResource(R.string.perfil_cierre_minutos, estado.minutosDeCierre),
            )
        }

        Seccion(stringResource(R.string.perfil_seccion_privacidad)) {
            FilaDePerfil(
                icono = R.drawable.ic_base_de_datos,
                titulo = stringResource(R.string.perfil_privacidad_consentimientos),
                valor = null,
                alTocar = onConsentimientos,
                adorno = {
                    InsigniaEstado(
                        texto = pluralStringResource(R.plurals.perfil_consentimientos_activos, estado.activos, estado.activos),
                        tipo = TipoInsignia.EXITO,
                    )
                },
            )
            SeparadorTarjeta()
            FilaDePerfil(R.drawable.ic_descargar, stringResource(R.string.perfil_descargar_datos), null, alTocar = null)
        }

        Seccion(stringResource(R.string.perfil_seccion_preferencias)) {
            FilaDePerfil(
                icono = R.drawable.ic_globo,
                titulo = stringResource(R.string.perfil_idioma),
                valor = estado.idioma.etiqueta,
                alTocar = { hojaDeIdioma = true },
            )
            SeparadorTarjeta()
            FilaDePerfil(R.drawable.ic_campana, stringResource(R.string.perfil_notificaciones), null, alTocar = null)
            SeparadorTarjeta()
            FilaDePerfil(R.drawable.ic_sincronizar, stringResource(R.string.perfil_sincronizacion), null, alTocar = null)
        }

        TarjetaSolventa {
            FilaDePerfil(
                icono = R.drawable.ic_cerrar_sesion,
                titulo = stringResource(R.string.perfil_cerrar_sesion),
                valor = null,
                alTocar = onCerrarSesion,
                color = MaterialTheme.colorScheme.error,
                // Sin flecha: cierra la sesion, no abre otra pantalla.
                conFlecha = false,
            )
        }

        Text(
            text = stringResource(R.string.perfil_version, estado.version),
            style = MaterialTheme.typography.bodySmall,
            color = LocalColoresSolventa.current.textoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (estado.revocacionesPendientesAlCerrar > 0) {
        AvisoDeRevocacionesSinEnviar(estado.revocacionesPendientesAlCerrar, onConfirmarCierre, onCancelarCierre)
    }

    if (hojaDeIdioma) {
        HojaDeIdioma(
            actual = estado.idioma,
            onElegir = {
                onElegirIdioma(it)
                hojaDeIdioma = false
            },
            onCerrar = { hojaDeIdioma = false },
        )
    }
}

/**
 * Antes de cerrar la sesion con revocaciones en cola (HU-M1.3-02, D-7 a): no
 * se pierden, se envian cuando el titular vuelva a entrar con su cuenta.
 */
@Composable
private fun AvisoDeRevocacionesSinEnviar(cuantas: Int, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(text = stringResource(R.string.perfil_cerrar_sesion_pendientes_titulo)) },
        text = { Text(text = pluralStringResource(R.plurals.perfil_cerrar_sesion_pendientes, cuantas, cuantas)) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    text = stringResource(R.string.perfil_cerrar_sesion_pendientes_confirmar),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text(text = stringResource(R.string.perfil_cerrar_sesion_pendientes_cancelar))
            }
        },
    )
}

@Composable
private fun Identidad(estado: PerfilUiState) {
    TarjetaSolventa {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espacio.x16),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Espacio.x16),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            ) {
                Text(
                    text = estado.iniciales,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column {
                Text(text = estado.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = estado.correo,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalColoresSolventa.current.textoSecundario,
                )
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, filas: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x8)) {
        Text(
            text = titulo.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LocalColoresSolventa.current.textoSecundario,
        )
        TarjetaSolventa { filas() }
    }
}
