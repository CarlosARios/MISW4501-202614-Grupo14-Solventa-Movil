package co.solventa.movil.features.perfil

import co.solventa.movil.core.i18n.Idioma

/**
 * Estado de M19 · Perfil (HU-M1.3-01, D-7 e). Solo lo que tiene un servicio
 * detras; las filas sin servicio se muestran en espera y no estan aqui.
 */
data class PerfilUiState(
    val nombre: String = "",
    val correo: String = "",
    val iniciales: String = "",
    val biometriaHabilitada: Boolean = false,
    /** Si el dispositivo tiene huella o rostro configurado. */
    val biometriaDisponible: Boolean = false,
    val idioma: Idioma = Idioma.ESPANOL,
    /** Consentimientos activos de la copia local, para la insignia «N activos». */
    val activos: Int = 0,
    val version: String = "",
    /** Minutos de inactividad tras los que se cierra la sesion. */
    val minutosDeCierre: Int = 0,
)
