package co.solventa.movil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.solventa.movil.core.auth.sesion.SesionViewModel
import co.solventa.movil.core.navegacion.BarraInferior
import co.solventa.movil.core.navegacion.PestanaInferior
import co.solventa.movil.core.navegacion.Ruta
import co.solventa.movil.core.navegacion.pestanaDe
import co.solventa.movil.features.acceso.ingreso.HojaDeSesionExpirada
import co.solventa.movil.features.acceso.ingreso.destinoIngreso
import co.solventa.movil.core.i18n.ConIdioma
import co.solventa.movil.core.i18n.IdiomaViewModel
import co.solventa.movil.features.acceso.bienvenida.PantallaBienvenida
import co.solventa.movil.features.acceso.registro.grafoRegistro
import co.solventa.movil.features.auditoria.consentimientos.grafoConsentimientos
import co.solventa.movil.features.siniestros.evidencia.grafoCapturaDeEvidencia
import co.solventa.movil.features.siniestros.radicacion.grafoRadicacion
import co.solventa.movil.features.inicio.PantallaInicio
import co.solventa.movil.features.perfil.destinoPerfil
import co.solventa.movil.shared.ui.BotonSolventa
import co.solventa.movil.shared.ui.TipoBoton
import co.solventa.movil.ui.theme.Espacio

/**
 * Navegación raíz de la aplicación.
 *
 * Vive en el paquete raíz y no en `core/navegacion` porque es la raíz de
 * composición: tiene que conocer todos los dominios, y `core` no puede
 * importar de `features`. En `core/navegacion` queda el contrato —[Ruta]—,
 * que sí es transversal.
 *
 * Hoy contiene los recorridos de registro (HU-M1.1-01) e ingreso
 * (HU-M1.1-02). M3 corresponde a otra historia y de momento tiene una
 * versión provisional que saluda con la identidad del *user pool* y da
 * acceso a las pantallas entregadas.
 *
 * Aquí vive también la vigilancia de inactividad, porque la sesión puede
 * expirar en cualquier pantalla y la hoja de M2d aparece encima de donde esté
 * el usuario.
 *
 * La barra inferior (HU-M1.3-01, D-14 b) se muestra solo en Inicio, Perfil y
 * Privacidad y consentimientos, como en los frames 55:3017 y 37:1233; no en el
 * detalle de un consentimiento, el acceso, el registro ni la radicación.
 */
@Composable
fun SolventaNavHost(
    modifier: Modifier = Modifier,
    navegacion: NavHostController = rememberNavController(),
    sesion: SesionViewModel = hiltViewModel(),
    idiomas: IdiomaViewModel = hiltViewModel(),
) {
    val expirada by sesion.expirada.collectAsStateWithLifecycle()
    val idioma by idiomas.actual.collectAsStateWithLifecycle()
    val entrada by navegacion.currentBackStackEntryAsState()
    val pestana = pestanaDe(rutaDe(entrada?.destination))

    // Todo lo que se dibuje debajo lee sus textos del idioma elegido. Al
    // cambiarlo, la jerarquia se recompone traducida sin reiniciar nada.
    ConIdioma(idioma) {
    Box(
        modifier = modifier
            // Cualquier toque reinicia la cuenta de los cinco minutos. Se
            // escucha en la fase inicial y sin consumir el evento, así que la
            // pantalla de abajo lo recibe igual.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        sesion.registrarActividad()
                    }
                }
            },
    ) {
        Scaffold(
            // Las pantallas ya atienden sus propios márgenes del sistema: aquí
            // solo se reserva el alto de la barra inferior cuando se muestra.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                pestana?.let { seleccionada ->
                    BarraInferior(seleccionada = seleccionada, onElegir = { irAPestana(navegacion, it) })
                }
            },
        ) { relleno ->
        NavHost(
            navController = navegacion,
            startDestination = Ruta.Bienvenida,
            modifier = Modifier.padding(relleno).consumeWindowInsets(relleno),
        ) {
            composable<Ruta.Bienvenida> {
                PantallaBienvenida(
                    idioma = idioma,
                    onElegirIdioma = idiomas::elegir,
                    onCrearCuenta = { navegacion.navigate(Ruta.Registro) },
                    onYaTengoCuenta = { navegacion.navigate(Ruta.Ingreso) },
                )
            }

            grafoRegistro(navegacion)
            grafoRadicacion(navegacion)
            grafoConsentimientos(navegacion)
            destinoPerfil(
                onConsentimientos = { navegacion.navigate(Ruta.Consentimientos) },
                onSesionCerrada = {
                    // Lo mismo que cerrar la sesión desde Inicio.
                    sesion.salirAIngreso()
                    navegacion.navigate(Ruta.Bienvenida) {
                        popUpTo(Ruta.Bienvenida) { inclusive = true }
                    }
                },
            )
            grafoCapturaDeEvidencia(navegacion)
            destinoIngreso(navegacion)

            composable<Ruta.Inicio> {
                // Entrar al inicio es lo que marca que hay sesión abierta.
                LaunchedEffect(Unit) { sesion.iniciarVigilancia() }
                PantallaInicio(
                    onRegistro = { navegacion.navigate(Ruta.Registro) },
                    onReportarSiniestro = { navegacion.navigate(Ruta.Radicacion) },
                    onVerificacionExitosa = { navegacion.navigate(Ruta.VerificacionExitosa) },
                    onVerificacionNoSuperada = { navegacion.navigate(Ruta.VerificacionNoSuperada) },
                    onVerificacionDemorada = { navegacion.navigate(Ruta.VerificacionDemorada) },
                    onCerrarSesion = {
                        // Se deja de vigilar antes de salir: sin sesión, el
                        // temporizador de inactividad no tiene qué expirar.
                        sesion.salirAIngreso()
                        navegacion.navigate(Ruta.Bienvenida) {
                            popUpTo(Ruta.Bienvenida) { inclusive = true }
                        }
                    },
                )
            }
        }
        }
    }

    if (expirada) {
        HojaDeSesionExpirada(
            onConHuella = sesion::reautenticarConHuella,
            onConContrasena = {
                sesion.salirAIngreso()
                navegacion.navigate(Ruta.Ingreso) {
                    popUpTo(Ruta.Bienvenida) { inclusive = true }
                }
            },
        )
    }
    }
}

/** El destino actual como [Ruta], solo para los que tienen barra inferior. */
private fun rutaDe(destino: NavDestination?): Ruta? = when {
    destino == null -> null
    destino.hasRoute<Ruta.Inicio>() -> Ruta.Inicio
    destino.hasRoute<Ruta.Perfil>() -> Ruta.Perfil
    destino.hasRoute<Ruta.Consentimientos>() -> Ruta.Consentimientos
    else -> null
}

/** Cada pestaña vuelve a su raíz sin apilar copias; atrás desde Perfil lleva a Inicio. */
private fun irAPestana(navegacion: NavHostController, pestana: PestanaInferior) {
    val ruta = pestana.ruta ?: return
    navegacion.navigate(ruta) {
        popUpTo(Ruta.Inicio)
        launchSingleTop = true
    }
}
