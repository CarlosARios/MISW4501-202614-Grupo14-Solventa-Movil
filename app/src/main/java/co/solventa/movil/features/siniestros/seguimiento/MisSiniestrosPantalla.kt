package co.solventa.movil.features.siniestros.seguimiento

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.solventa.movil.R
import co.solventa.movil.core.i18n.FormatoDeFecha
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaViewModel
import co.solventa.movil.shared.ui.AvisoSolventa
import co.solventa.movil.shared.ui.BarraProgreso
import co.solventa.movil.shared.ui.PantallaRegistro
import co.solventa.movil.shared.ui.TarjetaSolventa
import co.solventa.movil.shared.ui.TipoAviso
import co.solventa.movil.ui.theme.Espacio
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * M9 · Mis siniestros (HU-M2.4-01), pestaña de la barra inferior, con el
 * estado degradado de M9c (HA-L02): cuando el servicio responde desde su
 * caché, el aviso con la hora de esa versión, *Reintentar ahora*, la sección
 * marcada «versión en caché» y «Caché 14:30» en cada tarjeta.
 */
@Composable
fun MisSiniestrosPantalla(
    onAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: MisSiniestrosViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    ContenidoMisSiniestros(
        estado = estado,
        idioma = idioma,
        ahora = System.currentTimeMillis(),
        onAbrir = onAbrir,
        onReintentar = vm::reintentar,
        modifier = modifier,
    )
}

/** La pantalla sin estado propio: lo que prueban las pruebas de interfaz. */
@Composable
fun ContenidoMisSiniestros(
    estado: MisSiniestrosUiState,
    idioma: Idioma,
    ahora: Long,
    onAbrir: (String) -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PantallaRegistro(titulo = stringResource(R.string.siniestros_seguimiento_titulo), modifier = modifier) {
        val frescura = estado.frescura
        if (frescura != null && frescura.desdeCache) {
            AvisoDeVersionEnCache(frescura.leidoEn, ahora, idioma, estado.recargando, onReintentar)
        }
        val fallo = estado.fallo
        when {
            estado.cargando -> CargandoSiniestros()
            fallo != null && !estado.hayContenido -> FalloConReintento(fallo, onReintentar)
            estado.vacio -> Text(
                text = stringResource(R.string.siniestros_seguimiento_vacio),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> {
                if (fallo != null) {
                    AvisoSolventa(tipo = TipoAviso.ERROR, texto = stringResource(R.string.siniestros_seguimiento_error_con_lista))
                }
                val cache = frescura?.takeIf { it.desdeCache }
                Seccion(R.string.siniestros_seguimiento_en_curso, estado.enCurso, cache, idioma, onAbrir, conProgreso = true)
                Seccion(R.string.siniestros_seguimiento_cerrados, estado.cerrados, cache, idioma, onAbrir, conProgreso = false)
            }
        }
    }
}

@Composable
private fun Seccion(
    rotulo: Int,
    siniestros: List<Siniestro>,
    cache: Frescura?,
    idioma: Idioma,
    onAbrir: (String) -> Unit,
    conProgreso: Boolean,
) {
    if (siniestros.isEmpty()) return
    val base = stringResource(rotulo, siniestros.size)
    RotuloDeSeccion(if (cache != null) stringResource(R.string.siniestros_seguimiento_seccion_en_cache, base) else base)
    siniestros.forEach { TarjetaDeSiniestro(it, cache, idioma, onAbrir, conProgreso) }
}

/** Una tarjeta de M9: tipo, radicado, fecha del hecho, estado y, en curso, cuánto lleva. */
@Composable
private fun TarjetaDeSiniestro(
    s: Siniestro,
    cache: Frescura?,
    idioma: Idioma,
    onAbrir: (String) -> Unit,
    conProgreso: Boolean,
) {
    val colores = LocalColoresSolventa.current
    TarjetaSolventa(
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.siniestros_seguimiento_ver_detalle, s.radicado)) {
            onAbrir(s.id)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.x12), modifier = Modifier.padding(vertical = Espacio.x16)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.x12), verticalAlignment = Alignment.Top) {
                IconoDeSiniestro(s.linea)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.x4)) {
                    Text(text = tituloDe(s), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.siniestros_seguimiento_subtitulo, s.radicado, FormatoDeFecha.fecha(s.ocurrioEn, idioma)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.textoSecundario,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_derecha),
                    contentDescription = null,
                    tint = colores.textoSecundario,
                    modifier = Modifier.size(20.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { InsigniaDeSiniestro(s) }
                if (cache != null) {
                    Text(
                        text = stringResource(R.string.siniestros_seguimiento_cache_hora, FormatoDeFecha.hora(cache.leidoEn, idioma)),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.textoSecundario,
                    )
                }
            }
            if (conProgreso) {
                BarraProgreso(avance = LineaDeTiempo.progreso(s.estado), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
