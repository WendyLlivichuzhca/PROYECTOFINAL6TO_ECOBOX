package com.example.proyectofinal6to_ecobox.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.proyectofinal6to_ecobox.data.dao.AlertaPlantaDao
import com.example.proyectofinal6to_ecobox.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class EcoBoxAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        val lastNotifiedId = prefs.getLong("last_alert_notified_id", -1)

        if (token == null) return Result.success()

        Log.d("EcoBoxWorker", "🔍 Comprobando alertas en segundo plano...")

        try {
            // Consultar alertas reales desde el Backend
            val alerts = AlertaPlantaDao.obtenerAlertasDesdeApi(token)
            
            // Solo notificar si hay alertas nuevas (ID mayor al último notificado)
            val newAlerts = alerts.filter { it.id > lastNotifiedId && !it.leida && !it.resuelta }

            if (newAlerts.isNotEmpty()) {
                val mostRecent = newAlerts.maxByOrNull { it.id }!!
                
                // Mostrar notificación local
                NotificationHelper.showNotification(
                    applicationContext,
                    mostRecent.id.toInt(),
                    "⚠️ EcoBox: ${mostRecent.titulo}",
                    "${mostRecent.plantNombre ?: "Tu planta"}: ${mostRecent.mensaje}"
                )

                // Guardar el ID de la última alerta notificada para no repetir
                prefs.edit().putLong("last_alert_notified_id", mostRecent.id).apply()
                Log.d("EcoBoxWorker", "✅ Notificación enviada para alerta ${mostRecent.id}")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("EcoBoxWorker", "❌ Error en tarea de fondo: ${e.message}")
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "EcoBoxAlertPolling"

        /**
         * Programa la tarea periódica (ej: cada 15 minutos)
         */
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<EcoBoxAlertWorker>(
                15, TimeUnit.MINUTES, // MINIMO OBLIGATORIO DE ANDROID (15m)
                5, TimeUnit.MINUTES   // Ventana de flexibilidad
            )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // ACTUALIZAR para que el cambio de 1m a 15m surta efecto
                workRequest
            )
            Log.d("EcoBoxWorker", "⏰ Tarea periódica programada satisfactoriamente")
        }
    }
}
