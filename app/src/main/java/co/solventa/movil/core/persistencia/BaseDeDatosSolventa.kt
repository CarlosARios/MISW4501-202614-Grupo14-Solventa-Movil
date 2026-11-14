package co.solventa.movil.core.persistencia

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import co.solventa.movil.core.persistencia.dao.BorradorDeRadicacionDao
import co.solventa.movil.core.persistencia.dao.ConsentimientoDao
import co.solventa.movil.core.persistencia.dao.EvidenciaCapturadaDao
import co.solventa.movil.core.persistencia.dao.PolizaDao
import co.solventa.movil.core.persistencia.dao.NotificacionDao
import co.solventa.movil.core.persistencia.dao.RevocacionPendienteDao
import co.solventa.movil.core.persistencia.dao.SiniestroGuardadoDao
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
import co.solventa.movil.core.persistencia.entidades.ConsentimientoEntidad
import co.solventa.movil.core.persistencia.entidades.EvidenciaCapturada
import co.solventa.movil.core.persistencia.entidades.PolizaEntidad
import co.solventa.movil.core.persistencia.entidades.NotificacionEntidad
import co.solventa.movil.core.persistencia.entidades.RevocacionPendienteEntidad
import co.solventa.movil.core.persistencia.entidades.SiniestroGuardadoEntidad
import co.solventa.movil.core.persistencia.entidades.SincronizacionEntidad

/**
 * La base de datos del dispositivo.
 *
 * Guarda lo que tiene que sobrevivir a cerrar la aplicación y **no** es
 * secreto: el borrador del reporte de siniestro y las evidencias que se
 * capturaron para él, con el estado de su envío. Los tokens de la sesión no
 * viven aquí, sino en el almacenamiento cifrado de `core/auth/sesion`.
 *
 * La version 2 (HU-M1.3-01) agrega la copia sin conexion de los
 * consentimientos de Open Finance y la hora de su ultima sincronizacion. La
 * migracion 1 → 2 es automatica porque solo agrega tablas: los borradores y
 * las evidencias de la radicacion se conservan.
 *
 * La version 3 (HU-M1.3-02) agrega la cola de revocaciones pendientes de envio
 * y, en la copia de cada consentimiento, su revocacion (columna nueva que
 * admite nulos). La migracion 2 → 3 tambien es automatica y conserva todo.
 * La 3 → 4 (HU-M2.2-01) agrega al borrador de la radicacion el monto
 * estimado, vacio en los borradores que ya estaban.
 *
 * La version 5 (HU-M1.2-01) agrega la copia sin conexion de las polizas. Es la
 * primera tabla con contenido sensible —el documento del asegurado y los
 * montos asegurados—, asi que no guarda nada en claro: cada fila es el texto
 * cifrado con la clave del Keystore (`cifrado/`). La migracion 4 → 5 tambien
 * es automatica y conserva todo.
 *
 * La version 6 (HU-M3.1-01) agrega la copia del centro de notificaciones del
 * asegurado, con la marca de cuales ya se avisaron con una notificacion del
 * sistema. La migracion 5 → 6 tambien es automatica y conserva todo.
 *
 * La version 7 (HU-M2.2-02) agrega al borrador de la radicacion su dueño y el
 * estado de su envio en segundo plano, y a cada evidencia su dueño. La
 * migracion 6 → 7 es automatica; lo que ya estaba queda sin dueño y no se
 * muestra a nadie.
 *
 * La version 8 (HU-M2.4-01) agrega la copia sin conexion de *Mis siniestros*
 * y del detalle de cada caso, cifrada como la de las polizas. La migracion
 * 7 → 8 es automatica porque solo agrega una tabla.
 *
 * La version 9 (HU-M3.3-01) agrega a la copia de cada notificacion su cambio
 * de etapa del siniestro (columna nueva que admite nulos). La migracion 8 → 9
 * es automatica y conserva todo.
 *
 * La version 10 (HU-M3.2-01) agrega a la copia de cada notificacion su alerta
 * de vencimiento de poliza (columna nueva que admite nulos). La migracion
 * 9 → 10 es automatica y conserva todo.
 *
 * El esquema se exporta a `app/schemas`, que es lo que permite revisar en un
 * *pull request* qué cambió una migración.
 */
@Database(
    entities = [
        BorradorDeRadicacion::class,
        EvidenciaCapturada::class,
        ConsentimientoEntidad::class,
        SincronizacionEntidad::class,
        RevocacionPendienteEntidad::class,
        PolizaEntidad::class,
        NotificacionEntidad::class,
        SiniestroGuardadoEntidad::class,
    ],
    version = 10,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
    ],
    exportSchema = true,
)
abstract class BaseDeDatosSolventa : RoomDatabase() {

    abstract fun borradoresDeRadicacion(): BorradorDeRadicacionDao

    abstract fun evidenciasCapturadas(): EvidenciaCapturadaDao

    abstract fun consentimientos(): ConsentimientoDao

    abstract fun revocacionesPendientes(): RevocacionPendienteDao

    abstract fun polizas(): PolizaDao

    abstract fun notificaciones(): NotificacionDao

    abstract fun siniestrosGuardados(): SiniestroGuardadoDao

    companion object {
        const val NOMBRE = "solventa.db"
    }
}
