package co.solventa.movil.core.navegacion

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import co.solventa.movil.R
import co.solventa.movil.ui.theme.LocalColoresSolventa

/**
 * Pestañas de la barra inferior del frame 55:3017 (HU-M1.3-01, D-14 b). Las que
 * todavia no tienen pantalla quedan **en espera**: visibles, atenuadas y sin
 * navegar, con una descripcion que lo dice (Mis siniestros es de HU-M2.4-01; la
 * radicacion sigue entrando por Inicio). Pólizas lleva a M4 desde HU-M1.2-01.
 */
enum class PestanaInferior(
    @param:StringRes val etiqueta: Int,
    @param:DrawableRes val icono: Int,
    /** A donde lleva; `null` mientras la pestaña esta en espera. */
    val ruta: Ruta?,
    /** Lo que lee el lector de pantalla de una pestaña en espera. */
    @param:StringRes val descripcionEnEspera: Int?,
) {
    INICIO(R.string.navegacion_pestana_inicio, R.drawable.ic_inicio, Ruta.Inicio, null),
    POLIZAS(R.string.navegacion_pestana_polizas, R.drawable.ic_polizas, Ruta.Polizas, null),
    SINIESTROS(
        R.string.navegacion_pestana_siniestros,
        R.drawable.ic_siniestros,
        null,
        R.string.navegacion_pestana_siniestros_en_espera,
    ),
    PERFIL(R.string.navegacion_pestana_perfil, R.drawable.ic_perfil, Ruta.Perfil, null),
    ;

    val habilitada: Boolean get() = ruta != null
}

/**
 * La pestaña que se marca en cada destino, o `null` donde la barra no se
 * muestra. Privacidad y consentimientos se abre desde Perfil, asi que deja
 * Perfil seleccionada, como en el frame 37:1233. Mis pólizas (HU-M1.2-01) es
 * su propia pestaña; su detalle no lleva barra, como el de un consentimiento.
 */
fun pestanaDe(ruta: Ruta?): PestanaInferior? = when (ruta) {
    Ruta.Inicio -> PestanaInferior.INICIO
    Ruta.Polizas -> PestanaInferior.POLIZAS
    Ruta.Perfil, Ruta.Consentimientos -> PestanaInferior.PERFIL
    else -> null
}

/** La barra inferior de cuatro pestañas (frame 55:3017). */
@Composable
fun BarraInferior(
    seleccionada: PestanaInferior,
    onElegir: (PestanaInferior) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primario = MaterialTheme.colorScheme.primary
    val secundario = LocalColoresSolventa.current.textoSecundario
    NavigationBar(modifier = modifier, containerColor = MaterialTheme.colorScheme.surface) {
        PestanaInferior.entries.forEach { pestana ->
            val enEspera = pestana.descripcionEnEspera?.let { stringResource(it) }
            NavigationBarItem(
                selected = pestana == seleccionada,
                onClick = { if (pestana.habilitada) onElegir(pestana) },
                enabled = pestana.habilitada,
                icon = { Icon(painter = painterResource(pestana.icono), contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(pestana.etiqueta).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primario,
                    selectedTextColor = primario,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = secundario,
                    unselectedTextColor = secundario,
                ),
                modifier = if (enEspera != null) Modifier.semantics { contentDescription = enEspera } else Modifier,
            )
        }
    }
}
