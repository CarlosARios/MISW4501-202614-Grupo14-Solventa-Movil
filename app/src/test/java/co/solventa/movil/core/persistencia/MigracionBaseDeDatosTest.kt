package co.solventa.movil.core.persistencia

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * La migracion 1 → 2 de la base (HU-M1.3-01) contra los esquemas versionados
 * de `app/schemas`: agrega las tablas de la copia de consentimientos y
 * conserva el borrador de la radicacion que hubiera en el telefono. La 2 → 3
 * (HU-M1.3-02) agrega la cola de revocaciones pendientes y la revocacion de
 * cada consentimiento, y conserva la copia. La 3 → 4 (HU-M2.2-01) agrega el
 * monto estimado al borrador de la radicacion.
 *
 * Usa el ayudante con controlador explicito: el constructor por nombre de
 * archivo falla en Windows al comparar la ruta completa con el nombre.
 */
@RunWith(AndroidJUnit4::class)
class MigracionBaseDeDatosTest {

    private val archivo = ApplicationProvider.getApplicationContext<Context>().getDatabasePath(NOMBRE)

    @get:Rule
    val ayudante = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = archivo,
        driver = AndroidSQLiteDriver(),
        databaseClass = BaseDeDatosSolventa::class,
    )

    @Test
    fun `la migracion 1 a 2 conserva los borradores y agrega la copia`() {
        ayudante.createDatabase(1).apply {
            execSQL(
                "INSERT INTO borrador_de_radicacion (id, etapa, poliza, tipo, ocurrioEn, lugar, descripcion, " +
                    "respuestasJson, declaracionAceptada, actualizadoEn) " +
                    "VALUES ('borrador-1', 'HECHOS', 'AU-2025-118204', 'COLISION', 1792539000000, " +
                    "'Autopista Norte km 12', 'Choque por alcance', '{}', 0, 1792539100000)",
            )
            close()
        }

        // Valida que el resultado sea exactamente el esquema 2 exportado.
        val migrada = ayudante.runMigrationsAndValidate(2)
        try {
            assertEquals(listOf("borrador-1"), textos(migrada, "SELECT id FROM borrador_de_radicacion"))
            assertEquals(listOf("0"), textos(migrada, "SELECT COUNT(*) FROM consentimiento"))
            assertEquals(listOf("0"), textos(migrada, "SELECT COUNT(*) FROM sincronizacion"))
        } finally {
            migrada.close()
        }
    }

    @Test
    fun `la migracion 2 a 3 conserva la copia y agrega la cola de revocaciones`() {
        ayudante.createDatabase(2).apply {
            execSQL(
                "INSERT INTO consentimiento (propietario, id, estado, vigente, finalidad, canal, versionDeTerminos, " +
                    "otorgadoEn, venceEn, finalizadoEn, comprobante, entidades, alcance, historial) " +
                    "VALUES ('sub-a', 'cns-1', 'active', 1, 'risk_profiling_mortgage_life_offer', 'embedded', 'v1', " +
                    "1789573920000, 1821109920000, NULL, NULL, '[]', '[]', '[]')",
            )
            execSQL("INSERT INTO sincronizacion (propietario, recurso, sincronizadoEn) VALUES ('sub-a', 'consentimientos', 1)")
            close()
        }

        // Valida que el resultado sea exactamente el esquema 3 exportado.
        val migrada = ayudante.runMigrationsAndValidate(3)
        try {
            assertEquals(listOf("cns-1"), textos(migrada, "SELECT id FROM consentimiento WHERE propietario = 'sub-a'"))
            assertEquals(listOf("1"), textos(migrada, "SELECT COUNT(*) FROM consentimiento WHERE revocacion IS NULL"))
            assertEquals(listOf("0"), textos(migrada, "SELECT COUNT(*) FROM revocacion_pendiente"))
            assertEquals(listOf("1"), textos(migrada, "SELECT COUNT(*) FROM sincronizacion"))
        } finally {
            migrada.close()
        }
    }

    @Test
    fun `de la version 1 a la 3 en un solo paso`() {
        ayudante.createDatabase(1).close()

        val migrada = ayudante.runMigrationsAndValidate(3)
        try {
            assertEquals(listOf("0"), textos(migrada, "SELECT COUNT(*) FROM revocacion_pendiente"))
        } finally {
            migrada.close()
        }
    }

    /** HU-M2.2-01: el borrador gana el monto estimado, vacío en los que ya estaban. */
    @Test
    fun `la migracion 3 a 4 conserva el borrador con el monto vacio`() {
        ayudante.createDatabase(3).apply {
            execSQL(
                "INSERT INTO borrador_de_radicacion (id, etapa, poliza, tipo, ocurrioEn, lugar, descripcion, " +
                    "respuestasJson, declaracionAceptada, actualizadoEn) " +
                    "VALUES ('borrador-1', 'HECHOS', 'AU-2025-118204', 'COLISION', 1792539000000, " +
                    "'Autopista Norte km 12', 'Choque por alcance', '{}', 0, 1792539100000)",
            )
            close()
        }

        // Valida que el resultado sea exactamente el esquema 4 exportado.
        val migrada = ayudante.runMigrationsAndValidate(4)
        try {
            assertEquals(listOf("borrador-1"), textos(migrada, "SELECT id FROM borrador_de_radicacion"))
            assertEquals(listOf(""), textos(migrada, "SELECT montoEstimado FROM borrador_de_radicacion"))
        } finally {
            migrada.close()
        }
    }

    /** HU-M1.2-01: la versión 5 agrega la copia cifrada de las pólizas y conserva lo demás. */
    @Test
    fun `la migracion 4 a 5 conserva el borrador y agrega la copia de las polizas`() {
        ayudante.createDatabase(4).apply {
            execSQL(
                "INSERT INTO borrador_de_radicacion (id, etapa, poliza, tipo, ocurrioEn, lugar, descripcion, " +
                    "respuestasJson, declaracionAceptada, actualizadoEn, montoEstimado) " +
                    "VALUES ('borrador-1', 'HECHOS', 'AU-2025-118204', 'COLISION', 1792539000000, " +
                    "'Autopista Norte km 12', 'Choque por alcance', '{}', 0, 1792539100000, '4800000')",
            )
            close()
        }

        // Valida que el resultado sea exactamente el esquema 5 exportado.
        val migrada = ayudante.runMigrationsAndValidate(5)
        try {
            assertEquals(listOf("borrador-1"), textos(migrada, "SELECT id FROM borrador_de_radicacion"))
            migrada.execSQL(
                "INSERT INTO poliza (propietario, id, orden, contenidoCifrado) VALUES ('sub-a', 'AU-2025-118204', 0, 'x')",
            )
            assertEquals(listOf("AU-2025-118204"), textos(migrada, "SELECT id FROM poliza"))
        } finally {
            migrada.close()
        }
    }

    private fun textos(conexion: SQLiteConnection, consulta: String): List<String> =
        conexion.prepare(consulta).use { sentencia ->
            buildList { while (sentencia.step()) add(sentencia.getText(0)) }
        }

    private companion object {
        const val NOMBRE = "migracion-de-prueba.db"
    }
}
