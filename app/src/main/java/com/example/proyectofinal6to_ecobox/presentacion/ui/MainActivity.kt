package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.PlantAdapter
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var rvPlants: RecyclerView
    private lateinit var adapter: PlantAdapter

    // Vistas de estadísticas
    private lateinit var tvCountTotal: TextView
    private lateinit var tvCountHealthy: TextView
    private lateinit var tvCountCritical: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var cardAlertCritical: View // La tarjeta roja de alerta

    // Datos del usuario actual
    private var userId: Long = -1
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Recuperar sesión (SharedPreferences)
        // Estos datos se guardaron en LoginActivity al entrar exitosamente
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)
        userEmail = prefs.getString("user_email", "") ?: ""

        // Si no hay usuario guardado (ej. primera vez o borró datos), volver al login
        if (userId == -1L) {
            irALogin()
            return
        }

        // 2. Inicializar Vistas
        initViews()

        // 3. Poner saludo personalizado
        val nombreMostrar = if (userEmail.isNotEmpty()) userEmail.substringBefore("@") else "Usuario"
        tvWelcome.text = "Hola, $nombreMostrar"

        // 4. Cargar Datos REALES de la BD
        cargarDatosDashboard(userId)
    }

    private fun initViews() {
        rvPlants = findViewById(R.id.rvPlants)
        tvCountTotal = findViewById(R.id.tvCountTotal)
        tvCountHealthy = findViewById(R.id.tvCountHealthy)
        tvCountCritical = findViewById(R.id.tvCountCritical)
        tvWelcome = findViewById(R.id.tvWelcome)
        cardAlertCritical = findViewById(R.id.cardAlertCritical)

        // Configurar RecyclerView (Lista)
        rvPlants.layoutManager = LinearLayoutManager(this)
        adapter = PlantAdapter(emptyList()) { planta ->
            // Al hacer clic en una planta
            Toast.makeText(this, "Planta: ${planta.getNombre()}", Toast.LENGTH_SHORT).show()
        }
        rvPlants.adapter = adapter

        // Botón Logout
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            // Borrar sesión
            val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
            prefs.edit().clear().apply()

            irALogin()
        }

        // Botón Agregar Planta
        findViewById<FloatingActionButton>(R.id.fabAddPlant).setOnClickListener {
            Toast.makeText(this, "Próximamente: Agregar Planta", Toast.LENGTH_SHORT).show()
            // Aquí abrirías AddPlantActivity
        }
    }

    private fun cargarDatosDashboard(userId: Long) {
        // Ejecutamos en segundo plano (Obligatorio para red/BD)
        Thread {
            // A. Consultar base de datos
            val listaPlantas = PlantaDao.obtenerPlantasPorUsuario(userId)
            val estadisticas = PlantaDao.obtenerEstadisticas(userId) // [Total, Sanas, Criticas]

            // B. Actualizar interfaz en hilo principal
            runOnUiThread {
                // Actualizar lista
                if (listaPlantas.isEmpty()) {
                    Toast.makeText(this, "No tienes plantas registradas aún", Toast.LENGTH_LONG).show()
                }
                adapter.updateList(listaPlantas)

                // Actualizar tarjetas de números
                tvCountTotal.text = estadisticas[0].toString()
                tvCountHealthy.text = estadisticas[1].toString()
                tvCountCritical.text = estadisticas[2].toString()

                // Mostrar/Ocultar alerta roja según si hay críticas
                if (estadisticas[2] > 0) {
                    cardAlertCritical.visibility = View.VISIBLE
                } else {
                    cardAlertCritical.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        // Limpiar el historial para que no puedan volver atrás con el botón "Atrás"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}