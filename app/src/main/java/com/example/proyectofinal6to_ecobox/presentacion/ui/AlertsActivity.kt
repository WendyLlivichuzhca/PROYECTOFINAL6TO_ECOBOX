package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.AlertGenerator
import com.example.proyectofinal6to_ecobox.data.adapter.NotificacionesAdapter
import com.example.proyectofinal6to_ecobox.data.dao.NotificacionDao
import com.google.android.material.button.MaterialButton // Importante para los botones nuevos

class AlertsActivity : AppCompatActivity() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var adapter: NotificacionesAdapter
    private lateinit var txtCritical: TextView
    private lateinit var txtWarning: TextView
    private lateinit var txtTotal: TextView
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        initViews()
        cargarDatos()
    }

    private fun initViews() {
        // Enlazar vistas (Asegúrate que estos IDs existan en tu nuevo activity_alerts.xml)
        rvAlerts = findViewById(R.id.recyclerAlertas)
        txtCritical = findViewById(R.id.txtCountCritical)
        txtWarning = findViewById(R.id.txtCountWarning)
        txtTotal = findViewById(R.id.txtCountTotal)

        // Configurar RecyclerView
        rvAlerts.layoutManager = LinearLayoutManager(this)
        adapter = NotificacionesAdapter(emptyList()) { notificacion ->
            // Click en una notificación (Opcional: Marcar solo esa como leída)
            Toast.makeText(this, notificacion.mensaje, Toast.LENGTH_SHORT).show()
        }
        rvAlerts.adapter = adapter

        // Botón: Marcar Leídas
        findViewById<MaterialButton>(R.id.btnMarcarLeidas).setOnClickListener {
            marcarTodasLeidas()
        }

        // Botón: Solucionar / Regar
        findViewById<MaterialButton>(R.id.btnRegarTodas).setOnClickListener {
            // Aquí podrías llamar a tu lógica de MQTT para regar
            Toast.makeText(this, "Enviando comando de riego... 💧", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatos() {
        if (userId == -1L) return

        Thread {
            try {
                // 1. Generar alertas (Lógica de negocio)
                AlertGenerator.verificarYGenerarAlertas(userId)

                // 2. Obtener datos de BD
                val notificaciones = NotificacionDao.obtenerNotificaciones(userId)
                val resumen = AlertGenerator.obtenerResumenAlertas(userId)

                runOnUiThread {
                    // Actualizar lista
                    adapter.actualizarLista(notificaciones)

                    // Actualizar contadores
                    // Usamos la sintaxis segura '?' por si el mapa devuelve null
                    txtCritical.text = (resumen["criticas"] ?: 0).toString()
                    txtWarning.text = (resumen["advertencias"] ?: 0).toString()
                    txtTotal.text = notificaciones.size.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun marcarTodasLeidas() {
        Thread {
            val exito = NotificacionDao.marcarTodasComoLeidas(userId)
            runOnUiThread {
                if (exito) {
                    Toast.makeText(this, "Todo marcado como leído ✅", Toast.LENGTH_SHORT).show()
                    cargarDatos() // Recargar para quitar los puntos rojos
                } else {
                    Toast.makeText(this, "Ya está todo al día", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos() // Refrescar al volver a la pantalla
    }
}