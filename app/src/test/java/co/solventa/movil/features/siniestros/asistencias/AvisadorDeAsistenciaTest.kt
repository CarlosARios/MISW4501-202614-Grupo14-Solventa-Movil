package co.solventa.movil.features.siniestros.asistencias

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import co.solventa.movil.R
import co.solventa.movil.core.i18n.Idioma
import co.solventa.movil.core.i18n.IdiomaRepositorio
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * El aviso de S6 (Robolectric, HU-M2.3-01): una notificación del sistema en el
 * canal de D13, con el texto del motivo en el idioma elegido, y nada sin
 * permiso de notificaciones.
 */
@RunWith(AndroidJUnit4::class)
class AvisadorDeAsistenciaTest {

    private val aplicacion = ApplicationProvider.getApplicationContext<Application>()
    private val gestor = aplicacion.getSystemService(NotificationManager::class.java)
    private val idiomas = mockk<IdiomaRepositorio> { every { actual } returns MutableStateFlow(Idioma.ESPANOL) }
    private val avisador = AvisadorDeAsistenciaDelSistema(aplicacion, idiomas)

    @Test
    fun `con permiso publica la notificacion del motivo`() {
        shadowOf(aplicacion).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        avisador.noRegistrada(MotivoDeDescarte.POLIZA_NO_ACTIVA)

        // En el idioma elegido (español), aunque el dispositivo esté en otro.
        val enEspanol = aplicacion.createConfigurationContext(
            Configuration(aplicacion.resources.configuration).apply { setLocale(Idioma.ESPANOL.aLocale()) },
        )
        val publicada = shadowOf(gestor).allNotifications.single()
        assertEquals(EnvioDeAsistenciasWorker.CANAL, publicada.channelId)
        assertEquals(
            enEspanol.getString(R.string.siniestros_asistencia_no_registrada_titulo),
            shadowOf(publicada).contentTitle.toString(),
        )
        assertEquals(enEspanol.getString(R.string.siniestros_asistencia_descarte_poliza), shadowOf(publicada).contentText.toString())
    }

    @Test
    fun `sin permiso no publica nada`() {
        shadowOf(aplicacion).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        avisador.noRegistrada(MotivoDeDescarte.VENCIDA)

        assertTrue(shadowOf(gestor).allNotifications.isEmpty())
    }
}
