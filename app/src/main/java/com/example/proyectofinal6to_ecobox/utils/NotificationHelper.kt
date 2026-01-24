package com.example.proyectofinal6to_ecobox.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.presentacion.ui.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "ecobox_alerts_channel"
    private const val CHANNEL_NAME = "Alertas de Plantas EcoBox"
    private const val CHANNEL_DESC = "Notificaciones críticas de salud del jardín"

    /**
     * Crea el canal de notificación (Requerido para Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("NotificationHelper", "🛠️ Creando canal de notificaciones EcoBox...")
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación al usuario
     */
    fun showNotification(context: Context, id: Int, title: String, message: String) {
        Log.d("NotificationHelper", "🚀 Intentando mostrar notificación [$id]: $title")
        
        // Verificar si las notificaciones están habilitadas globalmente
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w("NotificationHelper", "❌ ¡Notificaciones DESACTIVADAS en ajustes del celular!")
            return
        }

        // Intent para abrir la App al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, Vibración, Luces
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(id, builder.build())
                Log.d("NotificationHelper", "✅ Notificación enviada al sistema Android")
            } catch (e: SecurityException) {
                Log.e("NotificationHelper", "❌ Error de seguridad (Permisos): ${e.message}")
            }
        }
    }
}
