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
import co.solventa.movil.core.i18n.FormatoDeFecha
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
        // HU-M3.3-01: cada cambio de etapa es su propia notificación, en su
        // propio canal y grupo, y abre la línea de tiempo del siniestro.
        val etapas = notificaciones.filter { it.etapa != null }
        if (etapas.isNotEmpty()) {
            crearCanalDeEtapas(textos)
            etapas.forEach { n -> gestor.notify(n.id, ID_ETAPA, avisoDeEtapa(textos, n, idioma)) }
            gestor.notify(ID_RESUMEN_ETAPAS, resumenDeEtapas(textos))
        }
        // HU-M3.2-01: cada alerta de vencimiento es su propia notificación, en
        // su propio canal y grupo, y abre el detalle de la póliza.
        val vencimientos = notificaciones.filter { it.vencimiento != null }
        if (vencimientos.isNotEmpty()) {
            crearCanalDeVencimientos(textos)
            vencimientos.forEach { n -> gestor.notify(n.id, ID_VENCIMIENTO, avisoDeVencimiento(textos, n, idioma)) }
            gestor.notify(ID_RESUMEN_VENCIMIENTOS, resumenDeVencimientos(textos))
        }
        return true
    }

    private fun avisoDeVencimiento(textos: Context, n: Notificacion, idioma: Idioma): android.app.Notification {
        val a = requireNotNull(n.vencimiento)
        val titulo = if (TextosDeNotificacion.venceHoy(a)) {
            textos.getString(R.string.notificaciones_vencimiento_titulo_hoy)
        } else {
            textos.resources.getQuantityString(R.plurals.notificaciones_vencimiento_titulo, a.diasRestantes, a.diasRestantes)
        }
        val cuerpo = textos.getString(
            R.string.notificaciones_vencimiento_cuerpo,
            a.poliza,
            TextosDeNotificacion.descripcionDePoliza(a, textos.getString(TextosDeNotificacion.lineaDeVencimiento(a.linea))),
            FormatoDeFecha.fecha(a.venceEn, idioma),
        )
        val abrirPoliza = abrir(n.id, poliza = a.poliza)
        return NotificationCompat.Builder(contexto, CANAL_VENCIMIENTOS)
            .setSmallIcon(R.drawable.ic_polizas)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setWhen(n.ocurrioEn)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setGroup(GRUPO_VENCIMIENTOS)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirPoliza)
            .addAction(0, textos.getString(R.string.notificaciones_vencimiento_ver_poliza), abrirPoliza)
            .build()
    }

    private fun resumenDeVencimientos(textos: Context) = NotificationCompat.Builder(contexto, CANAL_VENCIMIENTOS)
        .setSmallIcon(R.drawable.ic_polizas)
        .setContentTitle(textos.getString(R.string.notificaciones_canal_vencimientos))
        .setGroup(GRUPO_VENCIMIENTOS)
        .setGroupSummary(true)
        .setAutoCancel(true)
        .build()

    private fun crearCanalDeVencimientos(textos: Context) {
        val canal = NotificationChannel(
            CANAL_VENCIMIENTOS,
            textos.getString(R.string.notificaciones_canal_vencimientos),
            NotificationManager.IMPORTANCE_HIGH,
        )
        canal.description = textos.getString(R.string.notificaciones_canal_vencimientos_descripcion)
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun avisoDeEtapa(textos: Context, n: Notificacion, idioma: Idioma): android.app.Notification {
        val e = requireNotNull(n.etapa)
        val monto = e.monto
        val moneda = e.moneda
        val titulo = if (monto != null && moneda != null) {
            textos.getString(TextosDeNotificacion.tituloDeEtapa(e.etapa, conMonto = true), TextosDeNotificacion.monto(monto, moneda, idioma.aLocale()))
        } else {
            textos.getString(TextosDeNotificacion.tituloDeEtapa(e.etapa, conMonto = false))
        }
        val instante = TextosDeNotificacion.instanteDeEtapa(e)
        val cuerpo = textos.getString(
            TextosDeNotificacion.cuerpoDeEtapa(e.etapa),
            e.radicado,
            FormatoDeFecha.fecha(instante, idioma),
            FormatoDeFecha.hora(instante, idioma),
        )
        val abrirLinea = abrir(n.id, e.siniestroId)
        return NotificationCompat.Builder(contexto, CANAL_ETAPAS)
            .setSmallIcon(R.drawable.ic_campana)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setWhen(e.cambioEn)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setGroup(GRUPO_ETAPAS)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirLinea)
            .addAction(0, textos.getString(R.string.notificaciones_etapa_ver_linea_de_tiempo), abrirLinea)
            .build()
    }

    private fun resumenDeEtapas(textos: Context) = NotificationCompat.Builder(contexto, CANAL_ETAPAS)
        .setSmallIcon(R.drawable.ic_campana)
        .setContentTitle(textos.getString(R.string.notificaciones_canal_etapas))
        .setGroup(GRUPO_ETAPAS)
        .setGroupSummary(true)
        .setAutoCancel(true)
        .build()

    private fun crearCanalDeEtapas(textos: Context) {
        val canal = NotificationChannel(CANAL_ETAPAS, textos.getString(R.string.notificaciones_canal_etapas), NotificationManager.IMPORTANCE_HIGH)
        canal.description = textos.getString(R.string.notificaciones_canal_etapas_descripcion)
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
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

    /**
     * Abre la aplicación directamente en el destino de esta notificación: el
     * detalle del pago; con [siniestro], la línea de tiempo (HU-M3.3-01), y con
     * [poliza], el detalle de la póliza (HU-M3.2-01).
     */
    private fun abrir(id: String, siniestro: String? = null, poliza: String? = null): PendingIntent {
        // La actividad de entrada del paquete, sin nombrarla: un dominio no
        // conoce la raíz de composición de la aplicación.
        val intent = (contexto.packageManager.getLaunchIntentForPackage(contexto.packageName) ?: Intent())
            .setPackage(contexto.packageName)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(AperturaDesdeNotificacion.EXTRA_NOTIFICACION, id)
        if (siniestro != null) intent.putExtra(AperturaDesdeNotificacion.EXTRA_SINIESTRO, siniestro)
        if (poliza != null) intent.putExtra(AperturaDesdeNotificacion.EXTRA_POLIZA, poliza)
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

        /** El canal y el grupo de los cambios de etapa (HU-M3.3-01). */
        const val CANAL_ETAPAS = "etapas-siniestro"
        const val GRUPO_ETAPAS = "co.solventa.movil.etapas-siniestro"
        private const val ID_ETAPA = 3201
        private const val ID_RESUMEN_ETAPAS = 3200

        /** El canal y el grupo de las alertas de vencimiento (HU-M3.2-01). */
        const val CANAL_VENCIMIENTOS = "vencimientos-poliza"
        const val GRUPO_VENCIMIENTOS = "co.solventa.movil.vencimientos-poliza"
        private const val ID_VENCIMIENTO = 3301
        private const val ID_RESUMEN_VENCIMIENTOS = 3300
    }
}
