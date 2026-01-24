package com.example.proyectofinal6to_ecobox.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class EcoBoxBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Se activa cuando el celular se enciende o reinicia
        if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
            Log.d("EcoBoxBoot", "🔄 Sistema reiniciado, reactivando vigilancia de plantas...")
            EcoBoxAlertWorker.schedulePeriodicWork(context)
        }
    }
}
