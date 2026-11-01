package co.solventa.movil.core.i18n

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Hace que todo lo que esté dentro se dibuje en el idioma indicado.
 *
 * Provee una configuración y un contexto con ese idioma, que es de donde
 * `stringResource` saca los textos. Cambiar el idioma es cambiar este valor:
 * la jerarquía se recompone y los textos aparecen traducidos **sin reiniciar
 * la aplicación ni recrear la actividad**, que es lo que permite enseñarlo en
 * vivo.
 */
@Composable
fun ConIdioma(idioma: Idioma, contenido: @Composable () -> Unit) {
    val original = LocalContext.current
    val delDispositivo = LocalConfiguration.current

    // Se rehace cuando cambia el idioma elegido y cuando cambia la
    // configuración del dispositivo, para no perder una rotación ni un
    // cambio de tamaño de letra.
    val configuracion = remember(idioma, delDispositivo) {
        Configuration(delDispositivo).apply { setLocale(idioma.aLocale()) }
    }
    val contexto = remember(original, configuracion) {
        ContextoConIdioma(original, configuracion)
    }

    CompositionLocalProvider(
        LocalContext provides contexto,
        LocalConfiguration provides configuracion,
        content = contenido,
    )
}

/**
 * El idioma de la pantalla, para llevarlo a una ventana propia.
 *
 * Una hoja inferior (`ModalBottomSheet`) o un diálogo se dibujan en su propia
 * ventana, y esa ventana vuelve a proveer el contexto y la configuración **del
 * dispositivo**: lo que [ConIdioma] puso arriba no llega, y la hoja sale en el
 * idioma del teléfono y no en el de la aplicación. Se lee el idioma **antes**
 * de abrir la ventana y se vuelve a aplicar dentro:
 *
 * ```
 * val idioma = recordarIdiomaDeLaPantalla()
 * ModalBottomSheet(onDismissRequest = …) {
 *     idioma.Aplicar { ContenidoDeLaHoja() }
 * }
 * ```
 */
class IdiomaDeLaPantalla internal constructor(
    private val contexto: Context,
    private val configuracion: Configuration,
) {
    @Composable
    fun Aplicar(contenido: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalContext provides contexto,
            LocalConfiguration provides configuracion,
            content = contenido,
        )
    }
}

/** Lee el idioma con el que se está dibujando la pantalla. Ver [IdiomaDeLaPantalla]. */
@Composable
fun recordarIdiomaDeLaPantalla(): IdiomaDeLaPantalla {
    val contexto = LocalContext.current
    val configuracion = LocalConfiguration.current
    return remember(contexto, configuracion) { IdiomaDeLaPantalla(contexto, configuracion) }
}

/**
 * El contexto de siempre, con los recursos en otro idioma.
 *
 * **Envuelve en lugar de reemplazar, y eso no es un detalle.**
 * `createConfigurationContext` devuelve un contexto suelto, que ya no es la
 * actividad: con él, `hiltViewModel()` falla al crear cualquier `ViewModel`
 * —«Expected an activity context»— y la aplicación se cae al entrar a la
 * primera pantalla que tenga uno. Envolviendo, la actividad sigue en la
 * cadena y quien la busque la encuentra; lo único que cambia son los
 * recursos.
 */
private class ContextoConIdioma(
    base: Context,
    configuracion: Configuration,
) : ContextWrapper(base) {

    private val recursos: Resources =
        base.createConfigurationContext(configuracion).resources

    override fun getResources(): Resources = recursos
}
