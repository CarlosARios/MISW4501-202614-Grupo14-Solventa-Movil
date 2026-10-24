package co.solventa.movil.core.i18n

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Lo poco que el idioma necesita del dispositivo. */
interface AlmacenDeIdioma {
    fun leer(): String?
    fun guardar(valor: String)

    /**
     * El idioma en el que el sistema esta corriendo la aplicacion. Se lee de
     * la configuracion y no de `Locale.getDefault()`: el idioma efectivo de
     * una aplicacion Android es el que negocian la lista del sistema y las
     * traducciones que la aplicacion trae, y eso vive en la configuracion.
     */
    fun localeDelSistema(): Locale
}

/**
 * El idioma en el que corre la aplicación.
 *
 * Es un flujo porque el cambio se ve **sin reiniciar**: la pantalla observa
 * este valor y la jerarquía entera se recompone con los textos del idioma
 * nuevo.
 */
interface IdiomaRepositorio {
    val actual: StateFlow<Idioma>

    /** Si el usuario eligió uno, o sigue el del dispositivo. */
    val elegidoPorElUsuario: Boolean

    fun elegir(idioma: Idioma)
}

/**
 * Guarda la elección en las preferencias de la aplicación.
 *
 * **Por qué no `AppCompatDelegate.setApplicationLocales`.** Esa es la vía de
 * la plataforma, pero en Android 12 y anteriores —el Moto G20 de las pruebas
 * corre Android 11— necesita el respaldo de `androidx.appcompat`, y con él
 * una `AppCompatActivity` y un tema de AppCompat. Esta aplicación es Compose
 * sobre `FragmentActivity` con tema propio, así que eso serían tres cambios
 * estructurales y una dependencia nueva para lo mismo. Además recrea la
 * actividad: el evaluador vería un parpadeo en lugar de un cambio.
 *
 * Sobre un idioma por aplicación del sistema (Android 13+) se puede migrar
 * después sin tocar la interfaz: lo que esta clase expone es el idioma, no
 * cómo se guarda.
 */
@Singleton
class IdiomaRepositorioLocal @Inject constructor(
    private val almacen: AlmacenDeIdioma,
) : IdiomaRepositorio {

    private val guardado = Idioma.deNombre(almacen.leer())

    private val _actual = MutableStateFlow(guardado ?: Idioma.delSistema(almacen.localeDelSistema()))
    override val actual: StateFlow<Idioma> = _actual.asStateFlow()

    override var elegidoPorElUsuario: Boolean = guardado != null
        private set

    override fun elegir(idioma: Idioma) {
        almacen.guardar(idioma.name)
        elegidoPorElUsuario = true
        _actual.value = idioma
    }
}

/**
 * El idioma vive en unas preferencias en claro: no es un secreto, es una
 * preferencia de interfaz.
 */
@Singleton
class AlmacenDeIdiomaEnPreferencias @Inject constructor(
    @ApplicationContext private val contexto: Context,
) : AlmacenDeIdioma {

    private val preferencias by lazy {
        contexto.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    }

    override fun leer(): String? = preferencias.getString(CLAVE, null)

    override fun guardar(valor: String) {
        preferencias.edit().putString(CLAVE, valor).apply()
    }

    override fun localeDelSistema(): Locale =
        contexto.resources.configuration.locales[0]

    private companion object {
        const val ARCHIVO = "preferencias_de_interfaz"
        const val CLAVE = "idioma"
    }
}
