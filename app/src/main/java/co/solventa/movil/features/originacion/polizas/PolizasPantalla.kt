package co.solventa.movil.features.originacion.polizas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaViewModel
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M4 · Mis pólizas (HU-M1.2-01): la billetera, que se lee de la copia local
 * cifrada. Es pestaña de la barra inferior, así que no lleva flecha de volver.
 *
 * Los estados del prototipo viven en la misma pantalla: con conexión, el
 * indicador «Última sincronización… · disponible sin conexión»; sin conexión,
 * el aviso con la fecha y hora de la copia y el ícono de sin red (criterio 1);
 * tras sincronizar con cambios, el aviso «Sincronización completada · N
 * cambios» (criterio 4). Además, la copia de más de 30 días se advierte sin
 * esconder nada (criterio 5).
 */
@Composable
fun PolizasPantalla(
    onAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: PolizasViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    ContenidoPolizas(
        estado = estado,
        idioma = idioma,
        onAbrir = onAbrir,
        onReintentar = vm::reintentar,
        onCerrarCambios = vm::descartarCambios,
        modifier = modifier,
    )
}

/** La pantalla sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoPolizas(
    estado: PolizasUiState,
    idioma: Idioma,
    onAbrir: (String) -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    onCerrarCambios: () -> Unit = {},
) {
    PantallaRegistro(
        titulo = stringResource(R.string.originacion_polizas_titulo),
        modifier = modifier,
        indicador = if (estado.sinConexion) {
            { IconoSinConexion() }
        } else {
            null
        },
    ) {
        val sincronizado = estado.sincronizadoEn
        if (estado.sinConexion) {
            AvisoSolventa(
                tipo = TipoAviso.ERROR,
                titulo = stringResource(R.string.originacion_polizas_sin_conexion_titulo),
                texto = if (sincronizado != null) {
                    stringResource(
                        R.string.originacion_polizas_sin_conexion,
                        FormatoDeFecha.fecha(sincronizado, idioma),
                        FormatoDeFecha.hora(sincronizado, idioma),
                    )
                } else {
                    stringResource(R.string.originacion_polizas_sin_conexion_sin_copia)
                },
            )
        } else {
            IndicadorDeSincronizacion(sincronizado, estado.sincronizando, idioma)
        }

        if (estado.cambios.isNotEmpty()) {
            AvisoQueSeCierra(
                tipo = TipoAviso.EXITO,
                titulo = pluralStringResource(R.plurals.originacion_polizas_cambios_titulo, estado.cambios.size, estado.cambios.size),
                texto = (estado.cambios.map { textoDeCambio(it, idioma) } +
                    stringResource(R.string.originacion_polizas_cambios_ver_detalle)).joinToString(" "),
                descripcionCerrar = stringResource(R.string.originacion_polizas_cambios_cerrar),
                onCerrar = onCerrarCambios,
            )
        }

        if (estado.desactualizada) {
            AvisoSolventa(
                tipo = TipoAviso.ADVERTENCIA,
                titulo = stringResource(R.string.originacion_polizas_desactualizada_titulo),
                texto = pluralStringResource(
                    R.plurals.originacion_polizas_desactualizada,
                    PolizasViewModel.DIAS_SIN_SINCRONIZAR.toInt(),
                    PolizasViewModel.DIAS_SIN_SINCRONIZAR.toInt(),
                ),
            )
        }

        when {
            estado.cargando -> Cargando()
            estado.error && !estado.hayContenido -> MensajeConReintento(
                texto = stringResource(R.string.originacion_polizas_error_sin_copia),
                onReintentar = onReintentar,
            )
            estado.vacio -> Text(
                text = stringResource(R.string.originacion_polizas_vacio),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> {
                if (estado.error) {
                    AvisoSolventa(tipo = TipoAviso.ERROR, texto = stringResource(R.string.originacion_polizas_error))
                }
                estado.polizas.forEach { TarjetaDePoliza(it, idioma, onAbrir) }
            }
        }
    }
}

/** «Última sincronización: 16/09/2026 a las 14:30 · disponible sin conexión» (criterios 1 y 3). */
@Composable
private fun IndicadorDeSincronizacion(sincronizadoEn: Long?, sincronizando: Boolean, idioma: Idioma) {
    val texto = when {
        sincronizadoEn != null -> stringResource(
            R.string.originacion_polizas_ultima_sincronizacion,
            FormatoDeFecha.fecha(sincronizadoEn, idioma),
            FormatoDeFecha.hora(sincronizadoEn, idioma),
        )
        sincronizando -> stringResource(R.string.originacion_polizas_sincronizando)
        else -> return
    }
    val colores = LocalColoresSolventa.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = texto },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sincronizar),
            contentDescription = null,
            tint = colores.textoSecundario,
            modifier = Modifier.size(16.dp),
        )
        Text(text = texto, style = MaterialTheme.typography.bodySmall, color = colores.textoSecundario)
    }
}

@Composable
private fun Cargando() {
    val descripcion = stringResource(R.string.originacion_polizas_cargando)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.x48)) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = descripcion })
    }
}

@Composable
private fun MensajeConReintento(texto: String, onReintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.x16)) {
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
        BotonSolventa(
            texto = stringResource(R.string.originacion_polizas_reintentar),
            onClick = onReintentar,
            tipo = TipoBoton.SECUNDARIO,
        )
    }
}
