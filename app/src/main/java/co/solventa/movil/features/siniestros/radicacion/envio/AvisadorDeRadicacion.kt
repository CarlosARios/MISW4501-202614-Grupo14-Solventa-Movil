package co.solventa.movil.features.siniestros.radicacion.envio

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import co.solventa.movil.R
import co.solventa.movil.core.i18n.IdiomaRepositorio
import co.solventa.movil.features.notificaciones.centro.PermisoDeNotificaciones
import co.solventa.movil.features.siniestros.radicacion.CasoRadicado
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avisa que un reporte enviado en segundo plano quedó radicado (HU-M2.2-02):
 * el asegurado puede tener la aplicación cerrada cuando vuelve la señal.
 */
fun interface AvisadorDeRadicacion {
    fun radicado(caso: CasoRadicado)
}

/** [AvisadorDeRadicacion] con una notificación del sistema, si hay permiso. */
@Singleton
class AvisadorDeRadicacionDelSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val permiso: PermisoDeNotificaciones,
    private val idiomas: IdiomaRepositorio,
) : AvisadorDeRadicacion {

    @SuppressLint("MissingPermission") // Se comprueba antes con PermisoDeNotificaciones.
    override fun radicado(caso: CasoRadicado) {
        if (!permiso.concedido()) return
        val textos = contexto.createConfigurationContext(
            Configuration(contexto.resources.configuration).apply { setLocale(idiomas.actual.value.aLocale()) },
        )
        val canal = NotificationChannel(
            CANAL,
            textos.getString(R.string.radicacion_offline_canal),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)

        val cuerpo = textos.getString(R.string.radicacion_offline_notificacion, caso.radicado)
        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_circulo_check)
            .setContentTitle(textos.getString(R.string.radicacion_offline_registrado_titulo))
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(abrirAplicacion())
            .build()
        NotificationManagerCompat.from(contexto).notify(caso.casoId, ID, aviso)
    }

    private fun abrirAplicacion(): PendingIntent {
        val intent = (contexto.packageManager.getLaunchIntentForPackage(contexto.packageName) ?: Intent())
            .setPackage(contexto.packageName)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(contexto, ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val CANAL = "radicacion-offline"
        const val ID = 2202
    }
}
