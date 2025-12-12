package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.data.adapter.PlantAdapter
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var rvPlants: RecyclerView
    private lateinit var adapter: PlantAdapter

    // Stats
    private lateinit var tvCountTotal: TextView
    private lateinit var tvCountHealthy: TextView
    private lateinit var tvCountCritical: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var cardAlertCritical: View
    private lateinit var tvAlertMessage: TextView

    // Sesión
    private var userId: Long = -1
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Obtener usuario de sesión
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)
        userEmail = prefs.getString("user_email", "") ?: ""

        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 2. Configurar UI
        initViews()
        setupBottomNav()

        // Saludo
        val nombre = if (userEmail.isNotEmpty()) userEmail.substringBefore("@")
            .replaceFirstChar { it.uppercase() } else "Usuario"
        tvWelcome.text = "Hola, $nombre"

        // 3. CARGAR DATOS
        cargarDatos(userId)
    }

    private fun initViews() {
        rvPlants = findViewById(R.id.rvPlants)
        tvCountTotal = findViewById(R.id.tvCountTotal)
        tvCountHealthy = findViewById(R.id.tvCountHealthy)
        tvCountCritical = findViewById(R.id.tvCountCritical)
        tvWelcome = findViewById(R.id.tvWelcome)
        cardAlertCritical = findViewById(R.id.cardAlertCritical)

        try {
            tvAlertMessage = findViewById(R.id.tvAlertMessage)
        } catch (e: Exception) {
        }

        rvPlants.layoutManager = LinearLayoutManager(this)

        // Inicializar adapter con datos temporales vacíos
        adapter = PlantAdapter(emptyList()) { planta, datos ->
            // Abrir la actividad de detalle
            val intent = PlantDetailActivity.createIntent(this@MainActivity, planta, datos)
            startActivity(intent)
        }
        rvPlants.adapter = adapter

        // Botones
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<FloatingActionButton>(R.id.fabAddPlant).setOnClickListener {
            startActivity(Intent(this, AddPlantActivity::class.java))
        }
    }

    private fun cargarDatos(userId: Long) {
        Thread {
            try {
                // Obtener plantas completas con datos de sensores
                val plantasCompletas = PlantaDao.obtenerPlantasCompletas(userId)
                val stats = PlantaDao.obtenerEstadisticas(userId)

                runOnUiThread {
                    if (plantasCompletas.isEmpty()) {
                        Toast.makeText(this, "No se encontraron plantas", Toast.LENGTH_SHORT).show()
                    } else {
                        // Convertir a formato que entienda el adapter
                        val plantasParaAdapter = plantasCompletas.map { plantaCompleta ->
                            PlantaConDatos(
                                planta = plantaCompleta.planta,
                                ubicacion = plantaCompleta.ubicacion,
                                humedadSuelo = plantaCompleta.humedad,
                                temperatura = plantaCompleta.temperatura,
                                luz = plantaCompleta.luz,
                                nivelAgua = plantaCompleta.calcularNivelAgua(),
                                estado = plantaCompleta.determinarEstadoUI(),
                                ultimoRiego = plantaCompleta.ultimoRiego
                            )
                        }

                        // Actualizar el adapter
                        adapter.updateList(plantasParaAdapter)
                        Log.d(
                            "MainActivity",
                            "Cargadas ${plantasParaAdapter.size} plantas completas"
                        )
                    }

                    // Actualizar contadores
                    tvCountTotal.text = stats[0].toString()
                    tvCountHealthy.text = stats[1].toString()
                    tvCountCritical.text = stats[2].toString()

                    // Mostrar alerta roja si hay críticas
                    if (stats[2] > 0) {
                        cardAlertCritical.visibility = View.VISIBLE
                        if (::tvAlertMessage.isInitialized) {
                            tvAlertMessage.text = "${stats[2]} plantas necesitan atención"
                        }
                    } else {
                        cardAlertCritical.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("MainActivity", "Error en cargarDatos", e)
                }
            }
        }.start()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_plants -> {
                    // Ya estás en la pantalla de plantas, no hace nada
                    true
                }
                R.id.nav_alerts -> {
                    // AQUÍ abrimos la pantalla de Alertas
                    val intent = Intent(this, AlertsActivity::class.java)
                    startActivity(intent)
                    false // Retornamos false para que no se quede marcado el icono en el Main al irse
                }
                R.id.nav_sensors -> {
                    Toast.makeText(this, "Sensores: Próximamente", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                    false // Retornamos false para que no se quede marcado el icono en el Main al irse
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando se vuelva a la actividad
        if (userId != -1L) {
            cargarDatos(userId)
        }
    }

    // Clase auxiliar para pasar datos al adapter
    data class PlantaConDatos(
        val planta: Planta,
        val ubicacion: String,
        val humedadSuelo: Float,
        val temperatura: Float,
        val luz: Float,
        val nivelAgua: Int,
        val estado: String,
        val ultimoRiego: String
    )
}