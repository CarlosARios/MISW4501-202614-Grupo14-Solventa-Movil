package co.solventa.movil.core.navegacion

import kotlinx.serialization.Serializable

/**
 * Destinos de navegación. Una ruta por pantalla del prototipo, **no** por
 * variante: M1f (iluminación insuficiente) es un estado de M1a, y M2b y M2c
 * son estados de M2a. Las variantes se resuelven dentro del mismo composable.
 *
 * Ninguna pantalla escribe una cadena de ruta: se navega con estos objetos.
 */
sealed interface Ruta {

    /** M0 · Bienvenida. Punto de inicio de la aplicación sin sesión. */
    @Serializable
    data object Bienvenida : Ruta

    /** Grafo del registro con verificación de identidad (HU-M1.1-01). */
    @Serializable
    data object Registro : Ruta

    /** M1a · Captura del frente. Incluye el estado M1f, poca iluminación. */
    @Serializable
    data object CapturaFrente : Ruta

    /** M1b · Captura del reverso. */
    @Serializable
    data object CapturaReverso : Ruta

    /** M1c · Confirmación de los datos extraídos del documento. */
    @Serializable
    data object ConfirmacionDatos : Ruta

    /** M1g · Verificando identidad. */
    @Serializable
    data object Verificando : Ruta

    /** M1d · Verificación exitosa, cuenta activa. */
    @Serializable
    data object VerificacionExitosa : Ruta

    /** M1e · Verificación no superada. */
    @Serializable
    data object VerificacionNoSuperada : Ruta

    /** M1h · La verificación tarda; el resultado llega por notificación. */
    @Serializable
    data object VerificacionDemorada : Ruta

    /** M2a · Ingreso. Incluye M2b (contraseña) y M2c (tres intentos). */
    @Serializable
    data object Ingreso : Ruta

    /** M3 · Inicio. Primera pantalla con sesión activa. */
    @Serializable
    data object Inicio : Ruta

    /**
     * M11 · Asistente de radicación del siniestro (HU-M2.2-01).
     *
     * Las cuatro etapas comparten un destino: cuál se muestra lo decide el
     * estado del asistente, que es el mismo que se guarda en el borrador. Así
     * el indicador de pasos, el botón de avanzar y lo que se retoma al volver
     * a entrar salen de una sola fuente.
     */
    @Serializable
    data object Radicacion : Ruta

    /** M11e · Radicado generado. Cierra el flujo de la radicación. */
    @Serializable
    data object RadicacionRadicado : Ruta

    /** M4 · Mis pólizas, pestaña de la barra inferior (HU-M1.2-01). */
    @Serializable
    data object Polizas : Ruta

    /** M5 · Detalle de una póliza (HU-M1.2-01). */
    @Serializable
    data class DetallePoliza(val id: String) : Ruta

    /** M15 · Centro de notificaciones (HU-M3.1-01). */
    @Serializable
    data object Notificaciones : Ruta

    /**
     * M10b · Siniestro paramétrico resuelto automáticamente (HU-M3.1-01): el
     * detalle del pago que abre una notificación.
     */
    @Serializable
    data class DetallePagoParametrico(val id: String) : Ruta

    /** M19 · Perfil, pestaña de la barra inferior (HU-M1.3-01). */
    @Serializable
    data object Perfil : Ruta

    /** M8 · Privacidad y consentimientos (HU-M1.3-01). */
    @Serializable
    data object Consentimientos : Ruta

    /** M8b · Detalle de un consentimiento (HU-M1.3-01). */
    @Serializable
    data class DetalleConsentimiento(val id: String) : Ruta

    /** Consentimiento revocado y comprobante (HU-M1.3-02, frame 38:1414). */
    @Serializable
    data class ConsentimientoRevocado(val id: String) : Ruta

    /**
     * M12 · Cámara · captura de evidencia con GPS y hora (HU-M2.1-01).
     *
     * Es un destino aparte del asistente porque ocupa la pantalla completa y
     * porque se entra y se sale de él varias veces: una por cada evidencia.
     * Lleva el borrador al que pertenece lo capturado.
     */
    @Serializable
    data class CapturaDeEvidencia(val borradorId: String, val tipoDeSiniestro: String) : Ruta
}
