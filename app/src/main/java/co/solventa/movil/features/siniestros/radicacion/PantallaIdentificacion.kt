package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import co.solventa.movil.R
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M11a · Radicación · Paso 1 Identificación.
 *
 * La póliza y el tipo de incidente, que es lo que decide las preguntas de la
 * etapa siguiente (criterio 2).
 */
@Composable
fun PantallaIdentificacion(
    estado: RadicacionUiState,
    onElegirPoliza: (String) -> Unit,
    onElegirTipo: (TipoDeIncidente) -> Unit,
    onContinuar: () -> Unit,
    onGuardarYSalir: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    var eligiendoPoliza by remember { mutableStateOf(false) }

    PantallaRegistro(
        titulo = stringResource(R.string.radicacion_titulo),
        onAtras = onAtras,
        onCerrar = onGuardarYSalir,
        modifier = modifier,
        acciones = {
            BotonSolventa(
                texto = stringResource(R.string.comun_continuar),
                onClick = onContinuar,
                habilitado = estado.puedeAvanzar,
                icono = painterResource(R.drawable.ic_flecha_derecha),
            )
            TextoDeFaltantes(estado)
            BotonSolventa(
                texto = stringResource(R.string.radicacion_guardar_y_salir),
                onClick = onGuardarYSalir,
                tipo = TipoBoton.SECUNDARIO,
            )
        },
    ) {
        PasosDelAsistente(etapa = estado.etapa, avance = estado.avance)

        Text(
            text = stringResource(R.string.radicacion_identificacion_pregunta),
            style = MaterialTheme.typography.titleMedium,
        )

        CampoSelector(
            etiqueta = stringResource(R.string.radicacion_poliza_afectada),
            valor = estado.polizaElegida?.etiqueta
                ?: stringResource(R.string.radicacion_poliza_sin_elegir),
            onAbrir = { eligiendoPoliza = !eligiendoPoliza },
            ayuda = stringResource(R.string.radicacion_solo_polizas_activas),
            error = estado.camposRechazados.contains(CampoDeRadicacion.POLIZA),
        )

        // La lista se despliega bajo el selector: una sola póliza vigente no
        // justifica una hoja modal, y con varias se comporta igual.
        if (eligiendoPoliza) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Espacio.x8),
                modifier = Modifier.fillMaxWidth(),
            ) {
                estado.polizas.forEach { poliza ->
                    TarjetaDeOpcion(
                        titulo = poliza.etiqueta,
                        detalle = stringResource(R.string.radicacion_poliza_vigente),
                        icono = painterResource(R.drawable.ic_carro),
                        elegida = poliza.id == estado.radicacion.poliza,
                        onElegir = {
                            onElegirPoliza(poliza.id)
                            eligiendoPoliza = false
                        },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.radicacion_tipo_de_incidente),
            style = MaterialTheme.typography.labelSmall,
            color = colores.textoSecundario,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x8),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TipoDeIncidente.entries.forEach { tipo ->
                TarjetaDeOpcion(
                    titulo = stringResource(tituloDe(tipo)),
                    detalle = stringResource(detalleDe(tipo)),
                    icono = painterResource(iconoDe(tipo)),
                    elegida = estado.radicacion.tipo == tipo,
                    onElegir = { onElegirTipo(tipo) },
                )
            }
        }

        Text(
            text = stringResource(R.string.radicacion_identificacion_nota),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )
    }
}

internal fun tituloDe(tipo: TipoDeIncidente) = when (tipo) {
    TipoDeIncidente.COLISION -> R.string.radicacion_tipo_colision
    TipoDeIncidente.HURTO -> R.string.radicacion_tipo_hurto
    TipoDeIncidente.DANOS_POR_TERCEROS -> R.string.radicacion_tipo_danos
    TipoDeIncidente.OTRO -> R.string.radicacion_tipo_otro
}

internal fun detalleDe(tipo: TipoDeIncidente) = when (tipo) {
    TipoDeIncidente.COLISION -> R.string.radicacion_tipo_colision_detalle
    TipoDeIncidente.HURTO -> R.string.radicacion_tipo_hurto_detalle
    TipoDeIncidente.DANOS_POR_TERCEROS -> R.string.radicacion_tipo_danos_detalle
    TipoDeIncidente.OTRO -> R.string.radicacion_tipo_otro_detalle
}

internal fun iconoDe(tipo: TipoDeIncidente) = when (tipo) {
    TipoDeIncidente.COLISION -> R.drawable.ic_carro
    TipoDeIncidente.HURTO -> R.drawable.ic_prohibido
    TipoDeIncidente.DANOS_POR_TERCEROS -> R.drawable.ic_triangulo_alerta
    TipoDeIncidente.OTRO -> R.drawable.ic_mas_opciones
}
