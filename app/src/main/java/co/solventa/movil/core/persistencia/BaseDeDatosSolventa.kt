package co.solventa.movil.core.persistencia

import androidx.room.Database
import androidx.room.RoomDatabase
import co.solventa.movil.core.persistencia.dao.BorradorDeRadicacionDao
import co.solventa.movil.core.persistencia.dao.EvidenciaCapturadaDao
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
import co.solventa.movil.core.persistencia.entidades.EvidenciaCapturada

/**
 * La base de datos del dispositivo.
 *
 * Guarda lo que tiene que sobrevivir a cerrar la aplicación y **no** es
 * secreto: el borrador del reporte de siniestro y las evidencias que se
 * capturaron para él, con el estado de su envío. Los tokens de la sesión no
 * viven aquí, sino en el almacenamiento cifrado de `core/auth/sesion`.
 *
 * El esquema se exporta a `app/schemas`, que es lo que permite revisar en un
 * *pull request* qué cambió una migración.
 */
@Database(
    entities = [BorradorDeRadicacion::class, EvidenciaCapturada::class],
    version = 1,
    exportSchema = true,
)
abstract class BaseDeDatosSolventa : RoomDatabase() {

    abstract fun borradoresDeRadicacion(): BorradorDeRadicacionDao

    abstract fun evidenciasCapturadas(): EvidenciaCapturadaDao

    companion object {
        const val NOMBRE = "solventa.db"
    }
}
