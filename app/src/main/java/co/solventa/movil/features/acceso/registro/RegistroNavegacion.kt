package co.solventa.movil.features.acceso.registro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import co.solventa.movil.core.navegacion.Ruta

/**
 * Grafo del registro con verificación de identidad (HU-M1.1-01).
 *
 * El `ViewModel` se obtiene de la entrada del **grafo**, no de cada pantalla:
 * el documento capturado y los datos extraídos tienen que sobrevivir el paso
 * de M1a a M1h.
 *
 * La navegación la decide el estado, no el botón. Una pantalla pide una
 * acción al `ViewModel` y el `LaunchedEffect` reacciona a lo que el estado
 * diga; así el recorrido es el mismo que prueban las pruebas unitarias.
 */
fun NavGraphBuilder.grafoRegistro(navegacion: NavController) {
    navigation<Ruta.Registro>(startDestination = Ruta.CapturaFrente) {

        composable<Ruta.CapturaFrente> { entrada ->
            val viewModel = viewModelDelGrafo(entrada, navegacion)
            val estado by viewModel.estado.collectAsStateWithLifecycle()

            LaunchedEffect(estado.frente) {
                if (estado.frente != null) navegacion.navigate(Ruta.CapturaReverso)
            }

            CapturaDocumentoPantalla(
                cara = CaraDocumento.FRENTE,
                estado = estado,
                onCapturar = { ruta ->
                    viewModel.capturar(ImagenDocumento(CaraDocumento.FRENTE, ruta))
                },
                onLuzInsuficiente = viewModel::reportarLuzInsuficiente,
                onDescartarAviso = viewModel::descartarProblemaCalidad,
                onAtras = { navegacion.popBackStack() },
            )
        }

        composable<Ruta.CapturaReverso> { entrada ->
            val viewModel = viewModelDelGrafo(entrada, navegacion)
            val estado by viewModel.estado.collectAsStateWithLifecycle()

            LaunchedEffect(estado.datos) {
                if (estado.datos != null) navegacion.navigate(Ruta.ConfirmacionDatos)
            }

            CapturaDocumentoPantalla(
                cara = CaraDocumento.REVERSO,
                estado = estado,
                onCapturar = { ruta ->
                    viewModel.capturar(ImagenDocumento(CaraDocumento.REVERSO, ruta))
                },
                onLuzInsuficiente = viewModel::reportarLuzInsuficiente,
                onDescartarAviso = viewModel::descartarProblemaCalidad,
                onAtras = { navegacion.popBackStack() },
            )
        }

        composable<Ruta.ConfirmacionDatos> { entrada ->
            val viewModel = viewModelDelGrafo(entrada, navegacion)
            val estado by viewModel.estado.collectAsStateWithLifecycle()

            // Se sale de M1c cuando el registro arranca, no cuando se
            // toca el boton: un dato rechazado o un documento ya registrado
            // tienen que corregirse en esta misma pantalla.
            LaunchedEffect(estado.verificacion) {
                if (estado.verificacion == EstadoVerificacion.EnProceso) {
                    navegacion.navigate(Ruta.Verificando)
                }
            }

            ConfirmacionDatosPantalla(
                estado = estado,
                onDatosCambian = viewModel::actualizarDatos,
                onContrasenaCambia = viewModel::actualizarContrasena,
                onTerminos = viewModel::aceptarTerminos,
                onVerificar = viewModel::registrar,
                onAtras = { navegacion.popBackStack() },
            )
        }

        composable<Ruta.Verificando> { entrada ->
            val viewModel = viewModelDelGrafo(entrada, navegacion)
            val estado by viewModel.estado.collectAsStateWithLifecycle()

            LaunchedEffect(estado.verificacion) {
                when (estado.verificacion) {
                    is EstadoVerificacion.Exitosa ->
                        navegacion.navigate(Ruta.VerificacionExitosa)

                    is EstadoVerificacion.NoSuperada ->
                        navegacion.navigate(Ruta.VerificacionNoSuperada)

                    is EstadoVerificacion.Demorada ->
                        navegacion.navigate(Ruta.VerificacionDemorada)

                    // Un dato rechazado (422) o un documento ya registrado
                    // (409) devuelven el estado a NoIniciada: se corrigen en
                    // M1c, que marca los campos. Sin esto la pantalla se
                    // quedaba esperando un resultado que ya había llegado.
                    is EstadoVerificacion.NoIniciada ->
                        navegacion.popBackStack(Ruta.ConfirmacionDatos, inclusive = false)

                    else -> Unit
                }
            }

            VerificandoPantalla(onAvisarPorNotificacion = viewModel::avisarPorNotificacion)
        }

        composable<Ruta.VerificacionExitosa> {
            VerificacionExitosaPantalla(
                onIrAlInicio = {
                    navegacion.navigate(Ruta.Inicio) {
                        popUpTo(Ruta.Bienvenida) { inclusive = true }
                    }
                },
            )
        }

        composable<Ruta.VerificacionNoSuperada> { entrada ->
            val viewModel = viewModelDelGrafo(entrada, navegacion)
            val estado by viewModel.estado.collectAsStateWithLifecycle()
            val referencia = (estado.verificacion as? EstadoVerificacion.NoSuperada)
                ?.referencia
                .orEmpty()

            VerificacionNoSuperadaPantalla(
                referencia = referencia,
                onIntentarDeNuevo = {
                    viewModel.reiniciar()
                    navegacion.navigate(Ruta.CapturaFrente) {
                        popUpTo(Ruta.Registro) { inclusive = false }
                    }
                },
                onContactarSoporte = { },
            )
        }

        composable<Ruta.VerificacionDemorada> {
            // La cuenta no quedó activa, así que la salida es la bienvenida
            // y no el inicio de la aplicación (criterio 5).
            VerificacionDemoradaPantalla(
                onEntendido = {
                    navegacion.navigate(Ruta.Bienvenida) {
                        popUpTo(Ruta.Bienvenida) { inclusive = true }
                    }
                },
                onContactarSoporte = { },
            )
        }
    }
}

@Composable
private fun viewModelDelGrafo(
    entrada: NavBackStackEntry,
    navegacion: NavController,
): RegistroViewModel {
    val entradaDelGrafo = remember(entrada) { navegacion.getBackStackEntry(Ruta.Registro) }
    return hiltViewModel(entradaDelGrafo)
}
