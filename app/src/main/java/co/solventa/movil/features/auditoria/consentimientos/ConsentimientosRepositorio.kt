package co.solventa.movil.features.auditoria.consentimientos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Resultado de pedir la lista al servicio de auditoria (HU-M1.3-01). */
sealed interface ResultadoDeActualizacion {
    /** La copia local quedo igual a la del servidor. */
    data object Actualizado : ResultadoDeActualizacion

    /** Sin red: la copia se conserva y se programo la sincronizacion. */
    data object SinConexion : ResultadoDeActualizacion

    /** 403 `holder_not_linked`: la cuenta todavia no esta vinculada. */
    data object CuentaSinVincular : ResultadoDeActualizacion

    /** Sesion vencida, servicio caido u otra respuesta: la copia se conserva. */
    data object Error : ResultadoDeActualizacion

    /** No hay sesion con `sub`: no se lee ni se escribe nada. */
    data object SinSesion : ResultadoDeActualizacion
}

/** Lo que el titular ve de su copia local. */
data class CopiaDeConsentimientos(
    val consentimientos: List<Consentimiento>,
    /** Cuando se sincronizo por ultima vez, o `null` si nunca. */
    val sincronizadoEn: Long?,
    val sinConexion: Boolean,
)

/**
 * Los consentimientos del titular de la sesion (HU-M1.3-01). Room es la
 * fuente de la verdad: las pantallas observan la copia local, que sirve sin
 * conexion (criterio 5), y [actualizar] la reemplaza con la del servidor.
 */
interface ConsentimientosRepositorio {

    /** Si hay red; al volver, quien observa puede pedir una actualizacion. */
    val conectado: StateFlow<Boolean>

    fun observar(): Flow<CopiaDeConsentimientos>

    fun observarUno(id: String): Flow<Consentimiento?>

    suspend fun actualizar(): ResultadoDeActualizacion
}
