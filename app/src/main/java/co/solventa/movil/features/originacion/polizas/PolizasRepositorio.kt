package co.solventa.movil.features.originacion.polizas

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Resultado de pedirle las pólizas al servicio de originación (HU-M1.2-01). */
sealed interface ResultadoDeSincronizacion {
    /** La copia quedó igual a la del servidor; [cambios] es lo que se detectó. */
    data class Sincronizado(val cambios: List<CambioDePoliza>) : ResultadoDeSincronizacion

    /** Sin red: la copia se conserva y se programó la sincronización. */
    data object SinConexion : ResultadoDeSincronizacion

    /**
     * El servicio respondió sin todas las pólizas (`partial`), falló o la
     * sesión venció: la copia se conserva tal cual.
     */
    data object Error : ResultadoDeSincronizacion

    /** No hay sesión con `sub`: no se lee ni se escribe nada. */
    data object SinSesion : ResultadoDeSincronizacion
}

/** Lo que el asegurado ve de su copia local. */
data class CopiaDePolizas(
    val polizas: List<Poliza>,
    /** Cuándo se sincronizó por última vez, o `null` si nunca. */
    val sincronizadoEn: Long?,
    val sinConexion: Boolean,
)

/**
 * Las pólizas del asegurado de la sesión (HU-M1.2-01). Room es la fuente de
 * la verdad: las pantallas observan la copia cifrada, que sirve sin conexión
 * (criterios 1 y 2), y [sincronizar] la reemplaza con la del servidor.
 */
interface PolizasRepositorio {

    /** Si hay red; al volver, quien observa pide una sincronización (criterio 3). */
    val conectado: StateFlow<Boolean>

    /**
     * Los cambios que detectó la última sincronización y el asegurado todavía
     * no descartó (criterio 4). Sobrevive a cambiar de pantalla: la
     * sincronización en segundo plano también los deja aquí.
     */
    val cambios: StateFlow<List<CambioDePoliza>>

    fun observar(): Flow<CopiaDePolizas>

    fun observarUna(id: String): Flow<Poliza?>

    suspend fun sincronizar(): ResultadoDeSincronizacion

    fun descartarCambios()
}
