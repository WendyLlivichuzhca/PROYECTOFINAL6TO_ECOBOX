package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.AlertGenerator
import com.example.proyectofinal6to_ecobox.data.adapter.NotificacionesAdapter
import com.example.proyectofinal6to_ecobox.data.dao.NotificacionDao

class AlertsActivity : AppCompatActivity() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var adapter: NotificacionesAdapter
    private lateinit var txtCritical: TextView
    private lateinit var txtWarning: TextView
    private lateinit var txtTotal: TextView  // ✅ Solo estos tres
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
        rvAlerts = findViewById(R.id.recyclerAlertas)
        txtCritical = findViewById(R.id.txtCountCritical)
        txtWarning = findViewById(R.id.txtCountWarning)
        txtTotal = findViewById(R.id.txtCountTotal)  // ✅ Solo Total, NO txtCountInfo

        rvAlerts.layoutManager = LinearLayoutManager(this)
        adapter = NotificacionesAdapter(emptyList())
        rvAlerts.adapter = adapter

        findViewById<Button>(R.id.btnMarcarLeidas).setOnClickListener {
            Thread {
                val marcadas = NotificacionDao.marcarTodasComoLeidas(userId)
                runOnUiThread {
                    if (marcadas) {
                        Toast.makeText(this, "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show()
                        cargarDatos()
                    } else {
                        Toast.makeText(this, "No hay notificaciones por marcar", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        findViewById<Button>(R.id.btnRegarTodas).setOnClickListener {
            Toast.makeText(this, "Función 'Regar todas' en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatos() {
        if (userId == -1L) return

        Thread {
            // 1. Generar alertas basadas en el estado actual
            AlertGenerator.verificarYGenerarAlertas(userId)

            // 2. Obtener todas las notificaciones
            val notificaciones = NotificacionDao.obtenerNotificaciones(userId)

            // 3. Obtener resumen de alertas
            val resumen = AlertGenerator.obtenerResumenAlertas(userId)

            runOnUiThread {
                // Actualizar adapter
                adapter.actualizarLista(notificaciones)

                // ✅ Actualizar SOLO estos tres contadores
                txtCritical.text = resumen["criticas"]?.toString() ?: "0"
                txtWarning.text = resumen["advertencias"]?.toString() ?: "0"
                txtTotal.text = notificaciones.size.toString()  // Total de todas las notificaciones

                // Mostrar mensaje si no hay notificaciones
                if (notificaciones.isEmpty()) {
                    Toast.makeText(this, "No hay notificaciones", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }
}