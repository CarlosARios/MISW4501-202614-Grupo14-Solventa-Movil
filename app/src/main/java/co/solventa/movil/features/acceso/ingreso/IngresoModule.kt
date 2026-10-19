package co.solventa.movil.features.acceso.ingreso

import co.solventa.movil.core.auth.biometria.AutenticadorBiometrico
import co.solventa.movil.core.auth.biometria.AutenticadorBiometricoAndroid
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Vive junto al flujo y no en `core/di`, porque `core` no importa de
 * `features`.
 *
 * El repositorio de sesión salió de aquí: desde que autentica contra el
 * *user pool* de Cognito, enlaza una interfaz de `core` con una
 * implementación de `core`, así que vive en `core/di/AutenticacionModule`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class IngresoModule {

    /**
     * Desde el paso 3 de la historia se usa el sensor real. El doble
     * desapareció: en un dispositivo sin huella registrada, el propio
     * `BiometricManager` responde `NO_CONFIGURADA` y la aplicación va directo
     * al formulario de contraseña, que es el criterio 4.
     */
    @Binds
    @Singleton
    abstract fun enlazarAutenticadorBiometrico(
        android: AutenticadorBiometricoAndroid,
    ): AutenticadorBiometrico
}
