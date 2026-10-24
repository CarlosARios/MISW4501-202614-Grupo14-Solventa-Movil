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
 * conserva el borrador de la radicacion que hubiera en el telefono.
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

    private fun textos(conexion: SQLiteConnection, consulta: String): List<String> =
        conexion.prepare(consulta).use { sentencia ->
            buildList { while (sentencia.step()) add(sentencia.getText(0)) }
        }

    private companion object {
        const val NOMBRE = "migracion-de-prueba.db"
    }
}
