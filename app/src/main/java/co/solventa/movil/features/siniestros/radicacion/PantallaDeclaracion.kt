package co.solventa.movil.features.siniestros.radicacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import co.solventa.movil.R
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.SeparadorTarjeta
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M11d · Radicación · Paso 4 Declaración.
 *
 * El resumen de lo diligenciado y la declaración bajo gravedad de juramento,
 * que es lo que habilita el botón de radicar (estado M11d2).
 */
@Composable
fun PantallaDeclaracion(
    estado: RadicacionUiState,
    onDeclarar: (Boolean) -> Unit,
    onRadicar: () -> Unit,
    onGuardarYSalir: () -> Unit,
    onAtras: () -> Unit,
    onDescartarAviso: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = LocalColoresSolventa.current
    val enviando = estado.envio == EstadoDelEnvio.Enviando

    PantallaRegistro(
        titulo = stringResource(R.string.radicacion_titulo),
        onAtras = onAtras,
        onCerrar = onGuardarYSalir,
        modifier = modifier,
        acciones = {
            BotonSolventa(
                texto = stringResource(
                    if (enviando) R.string.radicacion_radicando else R.string.radicacion_radicar,
                ),
                onClick = onRadicar,
                habilitado = estado.puedeAvanzar,
            )
            TextoDeFaltantes(estado)
            BotonSolventa(
                texto = stringResource(R.string.radicacion_volver),
                onClick = onAtras,
                tipo = TipoBoton.SECUNDARIO,
            )
        },
    ) {
        PasosDelAsistente(etapa = estado.etapa, avance = estado.avance)

        Text(
            text = stringResource(R.string.radicacion_declaracion_titulo),
            style = MaterialTheme.typography.titleMedium,
        )

        // El servicio no respondio: lo diligenciado sigue guardado y el
        // reintento usa la misma clave, asi que no se radica dos veces.
        val envio = estado.envio
        if (envio is EstadoDelEnvio.NoDisponible) {
            AvisoSolventa(
                tipo = TipoAviso.ERROR,
                titulo = stringResource(R.string.radicacion_envio_fallido_titulo),
                texto = stringResource(R.string.radicacion_envio_fallido, envio.referencia),
            )
            BotonSolventa(
                texto = stringResource(R.string.comun_entendido),
                onClick = onDescartarAviso,
                tipo = TipoBoton.TERCIARIO,
            )
        }

        TarjetaSolventa {
            FilaDeResumen(
                etiqueta = stringResource(R.string.radicacion_poliza_afectada),
                valor = estado.polizaElegida?.etiqueta ?: estado.radicacion.poliza,
            )
            SeparadorTarjeta()
            FilaDeResumen(
                etiqueta = stringResource(R.string.radicacion_tipo_de_incidente),
                valor = estado.radicacion.tipo?.let { stringResource(tituloDe(it)) }.orEmpty(),
            )
            SeparadorTarjeta()
            FilaDeResumen(
                etiqueta = stringResource(R.string.radicacion_fecha_y_lugar),
                valor = listOf(
                    FechaDelHecho.aTexto(estado.radicacion.ocurrioEn),
                    estado.radicacion.lugar,
                ).filter { it.isNotBlank() }.joinToString(" · "),
            )
            if (estado.radicacion.respuestas.isNotEmpty()) {
                SeparadorTarjeta()
                FilaDeResumen(
                    etiqueta = stringResource(R.string.radicacion_respuestas),
                    valor = resumenDeRespuestas(estado),
                )
            }
            SeparadorTarjeta()
            FilaDeResumen(
                etiqueta = stringResource(R.string.radicacion_etapa_evidencias),
                valor = stringResource(R.string.radicacion_evidencias_pendientes),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.x8),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = estado.radicacion.declaracionAceptada,
                onCheckedChange = onDeclarar,
                enabled = !enviando,
            )
            Text(
                text = stringResource(R.string.radicacion_declaracion_casilla),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            text = stringResource(R.string.radicacion_declaracion_nota),
            style = MaterialTheme.typography.bodySmall,
            color = colores.textoSecundario,
        )
    }
}

@Composable
private fun FilaDeResumen(etiqueta: String, valor: String) {
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

/** «No / Sí» en el orden en que se preguntaron. */
@Composable
private fun resumenDeRespuestas(estado: RadicacionUiState): String =
    preguntasDe(estado.radicacion.tipo).mapNotNull { pregunta ->
        val respuesta = estado.radicacion.respuestas[pregunta.id] ?: return@mapNotNull null
        pregunta.opciones.firstOrNull { it.valor == respuesta }
            ?.let { stringResource(it.etiqueta) }
            ?: respuesta
    }.joinToString(" / ")
