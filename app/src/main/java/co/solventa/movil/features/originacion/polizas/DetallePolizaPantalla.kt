package co.solventa.movil.features.originacion.polizas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaViewModel
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.SeparadorTarjeta
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M5 · Detalle de la póliza (HU-M1.2-01, criterio 2): número de póliza, tipo
 * de seguro, vigencia con inicio y fin, coberturas con sus montos asegurados y
 * asegurado principal. Se lee de la copia local, así que se ve igual sin
 * conexión; en ese caso lo dice, con la hora de la copia.
 */
@Composable
fun DetallePolizaPantalla(
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DetallePolizaViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    ContenidoDetallePoliza(estado = estado, idioma = idioma, onAtras = onAtras, modifier = modifier)
}

/** La pantalla sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoDetallePoliza(
    estado: DetallePolizaUiState,
    idioma: Idioma,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaRegistro(
        titulo = stringResource(R.string.originacion_poliza_titulo),
        onAtras = onAtras,
        modifier = modifier,
        indicador = if (estado.sinConexion) {
            { IconoSinConexion() }
        } else {
            null
        },
    ) {
        val sincronizado = estado.sincronizadoEn
        if (estado.sinConexion && sincronizado != null) {
            AvisoSolventa(
                tipo = TipoAviso.ADVERTENCIA,
                texto = stringResource(
                    R.string.originacion_poliza_sin_conexion,
                    FormatoDeFecha.fecha(sincronizado, idioma),
                    FormatoDeFecha.hora(sincronizado, idioma),
                ),
            )
        }
        estado.cambio?.let { AvisoSolventa(tipo = TipoAviso.EXITO, texto = textoDeCambio(it, idioma)) }

        val p = estado.poliza
        when {
            estado.cargando -> Unit
            p == null -> Text(text = stringResource(R.string.originacion_poliza_no_disponible), style = MaterialTheme.typography.bodyLarge)
            else -> Detalle(p, idioma)
        }
    }
}

@Composable
private fun Detalle(p: PolizaEnLista, idioma: Idioma) {
    val poliza = p.poliza
    val locale = idioma.aLocale()
    val sinCobertura = PresentacionDePolizas.sinCobertura(p)

    // Encabezado: tipo, número y situación, como la tarjeta de M4.
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.CenterVertically) {
        IconoDeLinea(poliza.linea, atenuado = sinCobertura)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
            Text(
                text = nombreDe(poliza),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            InsigniaDeSituacion(p)
        }
    }

    TarjetaSolventa {
        Campo(stringResource(R.string.originacion_poliza_numero), poliza.id)
        SeparadorTarjeta()
        Campo(stringResource(R.string.originacion_poliza_tipo), textoDeLinea(poliza.linea))
        poliza.objetoAsegurado?.let {
            SeparadorTarjeta()
            Campo(stringResource(R.string.originacion_poliza_objeto), it)
        }
        SeparadorTarjeta()
        Campo(
            stringResource(R.string.originacion_poliza_vigencia),
            stringResource(
                R.string.originacion_poliza_vigencia_valor,
                FormatoDeFecha.fecha(poliza.inicio, idioma),
                FormatoDeFecha.fecha(poliza.fin, idioma),
            ),
        )
        SeparadorTarjeta()
        Campo(stringResource(R.string.originacion_polizas_valor_asegurado), EtiquetasDePoliza.monto(poliza.valorAsegurado, poliza.moneda, locale))
        poliza.prima?.let {
            SeparadorTarjeta()
            Campo(stringResource(R.string.originacion_poliza_prima), EtiquetasDePoliza.monto(it, poliza.moneda, locale))
        }
    }

    Rotulo(stringResource(R.string.originacion_poliza_coberturas))
    TarjetaSolventa {
        if (poliza.coberturas.isEmpty()) {
            Text(
                text = stringResource(R.string.originacion_poliza_sin_coberturas),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = Espacio.x12),
            )
        }
        poliza.coberturas.forEachIndexed { i, cobertura ->
            if (i > 0) SeparadorTarjeta()
            FilaDeCobertura(
                nombre = EtiquetasDePoliza.cobertura(cobertura.codigo)?.let { stringResource(it) } ?: cobertura.codigo,
                monto = EtiquetasDePoliza.monto(cobertura.monto, poliza.moneda, locale),
            )
        }
    }

    Rotulo(stringResource(R.string.originacion_poliza_asegurado))
    TarjetaSolventa {
        val documento = listOfNotNull(poliza.asegurado.tipoDeDocumento, poliza.asegurado.documento).joinToString(" ")
        Column(
            verticalArrangement = Arrangement.spacedBy(Espacio.x4),
            modifier = Modifier
                .padding(vertical = Espacio.x12)
                .clearAndSetSemantics { contentDescription = listOf(poliza.asegurado.nombre, documento).joinToString(", ") },
        ) {
            Text(text = poliza.asegurado.nombre, style = MaterialTheme.typography.titleMedium)
            if (documento.isNotEmpty()) {
                Text(text = documento, style = MaterialTheme.typography.bodyMedium, color = LocalColoresSolventa.current.textoSecundario)
            }
        }
    }
}

/** Un dato de la póliza: su rótulo y su valor, leídos juntos por el lector de pantalla. */
@Composable
private fun Campo(rotulo: String, valor: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Espacio.x4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Espacio.x12)
            .clearAndSetSemantics { contentDescription = "$rotulo: $valor" },
    ) {
        Text(text = rotulo, style = MaterialTheme.typography.labelMedium, color = LocalColoresSolventa.current.textoSecundario)
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FilaDeCobertura(nombre: String, monto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Espacio.x12)
            .clearAndSetSemantics { contentDescription = "$nombre: $monto" },
    ) {
        Text(text = nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = monto, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
    }
}

@Composable
private fun Rotulo(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = LocalColoresSolventa.current.textoSecundario,
        modifier = Modifier.semantics { heading() },
    )
}
