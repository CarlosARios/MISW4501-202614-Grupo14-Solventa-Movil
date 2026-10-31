package co.solventa.movil.features.siniestros.radicacion

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.solventa.movil.core.navegacion.Ruta

/**
 * Asistente de radicación del siniestro (HU-M2.2-01).
 *
 * Las cuatro etapas viven en un solo destino y el `ViewModel` dice cuál se
 * muestra. El botón atrás del sistema retrocede de etapa, no sale del flujo:
 * salir es cosa de la **×** del encabezado, que además guarda el borrador.
 *
 * El `ViewModel` se obtiene de la entrada del destino, así que sobrevive al
 * paso entre etapas y muere con el flujo.
 */
fun NavGraphBuilder.grafoRadicacion(navegacion: NavController) {

    composable<Ruta.Radicacion> { entrada ->
        val viewModel = viewModelDelAsistente(entrada)
        val estado by viewModel.estado.collectAsStateWithLifecycle()

        LaunchedEffect(estado.envio) {
            if (estado.envio is EstadoDelEnvio.Radicado) {
                navegacion.navigate(Ruta.RadicacionRadicado)
            }
        }

        BackHandler(enabled = !estado.esPrimeraEtapa) { viewModel.volver() }

        // El encabezado del visor nombra el tipo de siniestro en curso.
        val tipoDeSiniestro = estado.radicacion.tipo?.let { stringResource(tituloDe(it)) }.orEmpty()

        val salir = {
            // Guardar y salir: el borrador ya está escrito, así que basta con
            // dejar el flujo.
            navegacion.popBackStack(Ruta.Inicio, inclusive = false)
            Unit
        }

        when (estado.etapa) {
            Etapa.IDENTIFICACION -> PantallaIdentificacion(
                estado = estado,
                onElegirPoliza = viewModel::elegirPoliza,
                onElegirTipo = viewModel::elegirTipo,
                onContinuar = viewModel::continuar,
                onGuardarYSalir = salir,
                onAtras = salir,
            )

            Etapa.HECHOS -> PantallaHechos(
                estado = estado,
                onFechaCambia = viewModel::fijarFecha,
                onLugarCambia = viewModel::fijarLugar,
                onDescripcionCambia = viewModel::fijarDescripcion,
                onMontoCambia = viewModel::fijarMontoEstimado,
                onResponder = viewModel::responder,
                onContinuar = viewModel::continuar,
                onGuardarYSalir = salir,
                onAtras = viewModel::volver,
            )

            Etapa.EVIDENCIAS -> PantallaEvidencias(
                estado = estado,
                onDescartar = viewModel::descartarEvidencia,
                onCapturar = {
                    navegacion.navigate(
                        Ruta.CapturaDeEvidencia(
                            borradorId = estado.borradorId,
                            tipoDeSiniestro = tipoDeSiniestro,
                        ),
                    )
                },
                onContinuar = viewModel::continuar,
                onGuardarYSalir = salir,
                onAtras = viewModel::volver,
            )

            Etapa.DECLARACION -> PantallaDeclaracion(
                estado = estado,
                onDeclarar = viewModel::aceptarDeclaracion,
                onRadicar = viewModel::radicar,
                onGuardarYSalir = salir,
                onAtras = viewModel::volver,
                onDescartarAviso = viewModel::descartarAvisoDeEnvio,
            )
        }

        // Criterio 7: al volver a entrar se ofrece retomar lo que quedó a
        // medias, antes de tocar nada.
        if (estado.retomandoBorrador) {
            HojaDeBorrador(
                estado = estado,
                onContinuar = viewModel::continuarBorrador,
                onDescartar = viewModel::descartarBorrador,
            )
        }
    }

    composable<Ruta.RadicacionRadicado> { entrada ->
        val viewModel = viewModelDelAsistente(entrada, navegacion)
        val estado by viewModel.estado.collectAsStateWithLifecycle()
        val caso = (estado.envio as? EstadoDelEnvio.Radicado)?.caso

        if (caso == null) {
            // Sin caso no hay nada que mostrar: ocurre si se llega aquí por
            // una restauración del proceso.
            LaunchedEffect(Unit) { navegacion.popBackStack(Ruta.Inicio, inclusive = false) }
        } else {
            val alInicio = {
                navegacion.popBackStack(Ruta.Inicio, inclusive = false)
                Unit
            }
            PantallaRadicado(
                estado = estado,
                caso = caso,
                // La lista de siniestros es de HU-M2.4-01; hasta que exista,
                // las dos salidas llevan al inicio.
                onIrAMisSiniestros = alInicio,
                onIrAlInicio = alInicio,
            )
        }
    }
}

/**
 * El `ViewModel` del asistente, compartido entre el destino de las etapas y
 * el del radicado: el resumen del caso sale del mismo estado.
 */
@Composable
private fun viewModelDelAsistente(
    entrada: NavBackStackEntry,
    navegacion: NavController? = null,
): RadicacionViewModel {
    val dueño = remember(entrada) {
        navegacion?.getBackStackEntry(Ruta.Radicacion) ?: entrada
    }
    return hiltViewModel(dueño)
}
