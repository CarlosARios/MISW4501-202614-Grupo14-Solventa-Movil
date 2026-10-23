package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.MedallonEstado
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.SeparadorTarjeta
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa
import co.solventa.movil.R

/**
 * M11e · Radicado generado. Criterio 4: el código único y el resumen del
 * caso al confirmar la declaración.
 */
@Composable
fun PantallaRadicado(
    estado: RadicacionUiState,
    caso: CasoRadicado,
    onIrAMisSiniestros: () -> Unit,
    onIrAlInicio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current

    PantallaRegistro(
        titulo = stringResource(R.string.radicacion_radicado_titulo),
        modifier = modifier,
        acciones = {
            BotonSolventa(
                texto = stringResource(R.string.radicacion_ver_mis_siniestros),
                onClick = onIrAMisSiniestros,
            )
            BotonSolventa(
                texto = stringResource(R.string.radicacion_ir_al_inicio),
                onClick = onIrAlInicio,
                tipo = TipoBoton.SECUNDARIO,
            )
        },
    ) {
        MedallonEstado(
            icono = painterResource(R.drawable.ic_circulo_check),
            tinte = colores.exitoFuerte,
            fondo = colores.exitoSuave,
            descripcion = stringResource(R.string.radicacion_radicado_encabezado),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Text(
            text = stringResource(R.string.radicacion_radicado_encabezado),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.radicacion_codigo_unico),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )
        Text(
            text = caso.radicado,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(
                R.string.radicacion_radicado_detalle,
                FechaDelHecho.diaDeRfc3339(caso.radicadoEn),
                FechaDelHecho.horaDeRfc3339(caso.radicadoEn),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )

        TarjetaSolventa {
            FilaDelCaso(
                etiqueta = stringResource(R.string.radicacion_poliza_afectada),
                valor = estado.polizaElegida?.etiqueta ?: estado.radicacion.poliza,
            )
            SeparadorTarjeta()
            FilaDelCaso(
                etiqueta = stringResource(R.string.radicacion_tipo),
                valor = tipoConLesionados(estado),
            )
            SeparadorTarjeta()
            FilaDelCaso(
                etiqueta = stringResource(R.string.radicacion_etapa_evidencias),
                valor = stringResource(R.string.radicacion_evidencias_pendientes),
            )
            SeparadorTarjeta()
            FilaDelCaso(
                etiqueta = stringResource(R.string.radicacion_estado),
                valor = stringResource(R.string.radicacion_estado_activo),
            )
        }
    }
}

/** «Colisión · sin lesionados», como en el frame. */
@Composable
private fun tipoConLesionados(estado: RadicacionUiState): String {
    val tipo = estado.radicacion.tipo?.let { stringResource(tituloDe(it)) }.orEmpty()
    val lesionados = when (estado.radicacion.respuestas["personas_lesionadas"]) {
        "no" -> stringResource(R.string.radicacion_sin_lesionados)
        "si" -> stringResource(R.string.radicacion_con_lesionados)
        else -> null
    }
    return listOfNotNull(tipo.ifBlank { null }, lesionados).joinToString(" · ")
}

@Composable
private fun FilaDelCaso(etiqueta: String, valor: String) {
    val colores = LocalColoresSolventa.current
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = etiqueta.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colores.textoSecundario,
        )
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}
