package co.solventa.movil.core.persistencia

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import co.solventa.movil.core.persistencia.dao.BorradorDeRadicacionDao
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import co.solventa.movil.core.persistencia.dao.RevocacionPendienteDao
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
import co.solventa.movil.core.persistencia.entidades.ConsentimientoEntidad
import co.solventa.movil.core.persistencia.entidades.RevocacionPendienteEntidad
import co.solventa.movil.core.persistencia.entidades.SincronizacionEntidad

/**
 * La base de datos del dispositivo.
 *
 * Guarda lo que tiene que sobrevivir a cerrar la aplicación y **no** es
 * secreto: el borrador del reporte de siniestro. Los tokens de la sesión no
 * viven aquí, sino en el almacenamiento cifrado de `core/auth/sesion`.
 *
 * La version 2 (HU-M1.3-01) agrega la copia sin conexion de los
 * consentimientos de Open Finance y la hora de su ultima sincronizacion. La
 * migracion 1 → 2 es automatica porque solo agrega tablas: los borradores de
 * la radicacion se conservan.
 *
 * La version 3 (HU-M1.3-02) agrega la cola de revocaciones pendientes de envio
 * y, en la copia de cada consentimiento, su revocacion (columna nueva que
 * admite nulos). La migracion 2 → 3 tambien es automatica y conserva todo.
 *
 * El esquema se exporta a `app/schemas`, que es lo que permite revisar en un
 * *pull request* qué cambió una migración.
 */
@Database(
    entities = [
        BorradorDeRadicacion::class,
        ConsentimientoEntidad::class,
        SincronizacionEntidad::class,
        RevocacionPendienteEntidad::class,
    ],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
    exportSchema = true,
)
abstract class BaseDeDatosSolventa : RoomDatabase() {

    abstract fun borradoresDeRadicacion(): BorradorDeRadicacionDao

    abstract fun consentimientos(): ConsentimientoDao

    abstract fun revocacionesPendientes(): RevocacionPendienteDao

    companion object {
        const val NOMBRE = "solventa.db"
    }
}
