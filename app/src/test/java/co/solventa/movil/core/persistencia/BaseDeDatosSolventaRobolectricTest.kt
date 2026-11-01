package co.solventa.movil.core.persistencia

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.core.persistencia.entidades.BorradorDeRadicacion
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de humo de Robolectric (HU-M1.3-01, D-12 b): la base de datos real
 * de la aplicacion abre en la JVM, sin emulador, y guarda y lee un borrador
 * de la radicacion. Si esto corre, Room y el resto del dispositivo que
 * necesitan las pruebas de la copia sin conexion tambien corren.
 */
@RunWith(AndroidJUnit4::class)
class BaseDeDatosSolventaRobolectricTest {

    private lateinit var base: BaseDeDatosSolventa

    @Before
    fun antes() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDeDatosSolventa::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun despues() {
        base.close()
    }

    @Test
    fun `la base abre en la JVM y guarda y lee un borrador`() = runTest {
        val borrador = BorradorDeRadicacion(
            id = "borrador-1",
            etapa = "HECHOS",
            poliza = "AU-2025-118204",
            tipo = "COLISION",
            ocurrioEn = 1_792_539_000_000L,
            lugar = "Autopista Norte km 12, Bogotá",
            descripcion = "Choque por alcance en el semáforo.",
            respuestasJson = "{}",
            declaracionAceptada = false,
            actualizadoEn = 1_792_539_100_000L,
        )

        base.borradoresDeRadicacion().guardar(borrador)

        assertEquals(borrador, base.borradoresDeRadicacion().enCurso())
    }
}
