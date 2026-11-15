package co.solventa.movil.features.siniestros.asistencias

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import co.solventa.movil.R
import co.solventa.movil.core.i18n.IdiomaRepositorio
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Avisa que una solicitud de asistencia no se pudo registrar cuando M14 ya no
 * está a la vista (HU-M2.3-01, S6). La atención por voz ya ocurrió: el aviso
 * solo dice que la ubicación no llegó a la bandeja.
 */
fun interface AvisadorDeAsistencia {
    fun noRegistrada(motivo: MotivoDeDescarte)
}

/** [AvisadorDeAsistencia] con una notificación del sistema en el canal de D13, si hay permiso. */
@Singleton
class AvisadorDeAsistenciaDelSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val idiomas: IdiomaRepositorio,
) : AvisadorDeAsistencia {

    @SuppressLint("MissingPermission") // Se comprueba antes en puedeNotificar().
    override fun noRegistrada(motivo: MotivoDeDescarte) {
        if (!puedeNotificar()) return
        val textos = contexto.createConfigurationContext(
            Configuration(contexto.resources.configuration).apply { setLocale(idiomas.actual.value.aLocale()) },
        )
        EnvioDeAsistenciasWorker.crearCanal(textos)
        val cuerpo = textos.getString(TextosDeAsistencia.descarte(motivo))
        val aviso = NotificationCompat.Builder(contexto, EnvioDeAsistenciasWorker.CANAL)
            .setSmallIcon(R.drawable.ic_triangulo_alerta)
            .setContentTitle(textos.getString(R.string.siniestros_asistencia_no_registrada_titulo))
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(abrirAplicacion())
            .build()
        NotificationManagerCompat.from(contexto).notify(ID, aviso)
    }

    private fun puedeNotificar(): Boolean {
        val permiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permiso && NotificationManagerCompat.from(contexto).areNotificationsEnabled()
    }

    private fun abrirAplicacion(): PendingIntent {
        val intent = (contexto.packageManager.getLaunchIntentForPackage(contexto.packageName) ?: Intent())
            .setPackage(contexto.packageName)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(contexto, ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val ID = 2302
    }
}
