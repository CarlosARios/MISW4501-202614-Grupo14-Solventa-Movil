package co.solventa.movil.features.notificaciones.centro

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
import co.solventa.movil.core.i18n.Idioma
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Muestra la notificación del sistema de cada notificación nueva del centro
 * (HU-M3.1-01, M15b): con la aplicación en segundo plano o cerrada el sistema
 * la muestra igual, respetando el permiso (criterio 3), y tocarla abre el
 * detalle del pago (criterio 4).
 */
interface AvisadorDeNotificaciones {
    /** Muestra un aviso por cada una; devuelve false si no hay permiso. */
    fun avisar(notificaciones: List<Notificacion>, idioma: Idioma): Boolean
}

/**
 * [AvisadorDeNotificaciones] con `NotificationManagerCompat`. Cada pago es su
 * propia notificación, con su propio id, dentro del grupo de los pagos
 * automáticos, más un resumen del grupo: varios pagos el mismo día se apilan y
 * ninguno reemplaza a otro (criterio 6).
 */
@Singleton
class AvisadorDelSistema @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val permiso: PermisoDeNotificaciones,
) : AvisadorDeNotificaciones {

    @SuppressLint("MissingPermission") // El permiso se comprueba antes con PermisoDeNotificaciones.
    override fun avisar(notificaciones: List<Notificacion>, idioma: Idioma): Boolean {
        if (!permiso.concedido()) return false
        val textos = contextoEn(idioma)
        val gestor = NotificationManagerCompat.from(contexto)
        crearCanal(textos)
        val pagos = notificaciones.filter { it.pago != null }
        pagos.forEach { n -> gestor.notify(n.id, ID_PAGO, aviso(textos, n, idioma)) }
        if (pagos.isNotEmpty()) {
            gestor.notify(ID_RESUMEN, resumen(textos))
        }
        return true
    }

    private fun aviso(textos: Context, n: Notificacion, idioma: Idioma): android.app.Notification {
        val pago = requireNotNull(n.pago)
        val locale = idioma.aLocale()
        val titulo = textos.getString(R.string.notificaciones_aviso_titulo, TextosDeNotificacion.monto(pago.monto, pago.moneda, locale))
        val cuerpo = textos.getString(
            TextosDeNotificacion.cuerpoDelAviso(pago.disparador.parametro),
            pago.disparador.evento,
            TextosDeNotificacion.numero(pago.disparador.observado, locale),
        )
        return NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_zap)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setWhen(n.ocurrioEn)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setGroup(GRUPO)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrir(n.id))
            .addAction(0, textos.getString(R.string.notificaciones_aviso_ver_detalle), abrir(n.id))
            .build()
    }

    private fun resumen(textos: Context) = NotificationCompat.Builder(contexto, CANAL)
        .setSmallIcon(R.drawable.ic_zap)
        .setContentTitle(textos.getString(R.string.notificaciones_canal_pagos))
        .setGroup(GRUPO)
        .setGroupSummary(true)
        .setAutoCancel(true)
        .build()

    /** Abre la aplicación directamente en el detalle de esta notificación. */
    private fun abrir(id: String): PendingIntent {
        // La actividad de entrada del paquete, sin nombrarla: un dominio no
        // conoce la raíz de composición de la aplicación.
        val intent = (contexto.packageManager.getLaunchIntentForPackage(contexto.packageName) ?: Intent())
            .setPackage(contexto.packageName)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION, id)
        return PendingIntent.getActivity(
            contexto,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun crearCanal(textos: Context) {
        val canal = NotificationChannel(CANAL, textos.getString(R.string.notificaciones_canal_pagos), NotificationManager.IMPORTANCE_HIGH)
        canal.description = textos.getString(R.string.notificaciones_canal_pagos_descripcion)
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** Los textos en el idioma elegido en la aplicación, no en el del sistema. */
    private fun contextoEn(idioma: Idioma): Context {
        val configuracion = Configuration(contexto.resources.configuration).apply { setLocale(idioma.aLocale()) }
        return contexto.createConfigurationContext(configuracion)
    }

    companion object {
        const val CANAL = "pagos-parametricos"
        const val GRUPO = "co.solventa.movil.pagos-parametricos"
        private const val ID_PAGO = 3101
        private const val ID_RESUMEN = 3100
    }
}
