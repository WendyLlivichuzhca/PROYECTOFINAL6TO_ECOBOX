package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.PlantAdapter
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    // --- VARIABLES DE VISTA ---

    // Contenedores Principales
    private lateinit var layoutDashboard: LinearLayout
    private lateinit var layoutPlants: LinearLayout

    // Componentes de Listas y Búsqueda
    private lateinit var rvPlants: RecyclerView
    private lateinit var adapter: PlantAdapter
    private lateinit var etSearch: EditText

    // Componentes del Dashboard
    private lateinit var tvWelcome: TextView
    private lateinit var btnAlerts: FrameLayout
    private lateinit var fabAdd: FloatingActionButton

    // Variables para las Tarjetas de Estadísticas
    private lateinit var tvTotalValue: TextView
    private lateinit var tvHealthyValue: TextView
    private lateinit var tvCriticalValue: TextView

    // Alerta Roja Grande
    private lateinit var cardAlertCritical: MaterialCardView
    private lateinit var tvAlertMessage: TextView

    // Variables de Sesión
    private var userId: Long = -1
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. VALIDAR SESIÓN
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)
        userEmail = prefs.getString("user_email", "") ?: ""

        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 2. INICIALIZAR VISTAS
        initViews()
        setupBottomNav()

        // 3. PERSONALIZAR SALUDO
        val nombre = if (userEmail.isNotEmpty()) userEmail.substringBefore("@")
            .replaceFirstChar { it.uppercase() } else "Usuario"
        tvWelcome.text = "Hola, $nombre"

        // 4. CARGAR DATOS (DB)
        cargarDatos(userId)
    }

    private fun initViews() {
        // --- VINCULACIÓN DE VISTAS ---

        // Contenedores
        layoutDashboard = findViewById(R.id.layoutDashboard)
        layoutPlants = findViewById(R.id.layoutPlants)

        // Elementos Dashboard
        tvWelcome = findViewById(R.id.tvWelcome)
        btnAlerts = findViewById(R.id.btnAlerts)
        cardAlertCritical = findViewById(R.id.cardAlertCritical)
        tvAlertMessage = findViewById(R.id.tvAlertMessage)

        // Elementos Lista
        rvPlants = findViewById(R.id.rvPlants)
        etSearch = findViewById(R.id.etSearch)
        fabAdd = findViewById(R.id.fabAddPlant)

        // --- CONFIGURACIÓN DE LOS INCLUDES (ESTADÍSTICAS) ---
        val cardTotal = findViewById<View>(R.id.cardStatTotal)
        tvTotalValue = cardTotal.findViewById(R.id.tvCount)
        cardTotal.findViewById<TextView>(R.id.tvLabel).text = "Total"

        val cardHealthy = findViewById<View>(R.id.cardStatHealthy)
        tvHealthyValue = cardHealthy.findViewById(R.id.tvCount)
        cardHealthy.findViewById<TextView>(R.id.tvLabel).text = "Sanas"

        val cardCritical = findViewById<View>(R.id.cardStatCritical)
        tvCriticalValue = cardCritical.findViewById(R.id.tvCount)
        cardCritical.findViewById<TextView>(R.id.tvLabel).text = "Críticas"

        // --- CONFIGURACIÓN RECYCLER VIEW ---
        rvPlants.layoutManager = LinearLayoutManager(this)
        adapter = PlantAdapter(emptyList()) { planta, datos ->
            val intent = PlantDetailActivity.createIntent(this@MainActivity, planta, datos)
            startActivity(intent)
        }
        rvPlants.adapter = adapter

        // --- LISTENERS ---

        // Campanita y Alerta Roja -> Ir a Actividad de Alertas
        val goToAlerts = View.OnClickListener { startActivity(Intent(this, AlertsActivity::class.java)) }
        btnAlerts.setOnClickListener(goToAlerts)
        cardAlertCritical.setOnClickListener(goToAlerts)

        // FAB (+) Agregar Planta
        fabAdd.setOnClickListener {
            startActivity(Intent(this, AddPlantActivity::class.java))
        }

        // --- BUSCADOR EN TIEMPO REAL ---
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Llamamos a la función filtrar del Adapter
                adapter.filtrar(s.toString())
            }
        })
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // MODO INICIO: Ver Dashboard, Ocultar Lista
                    layoutDashboard.visibility = View.VISIBLE
                    layoutPlants.visibility = View.GONE
                    fabAdd.hide()
                    true
                }

                R.id.nav_my_plants -> {
                    // MODO LISTA: Ocultar Dashboard, Ver Lista
                    layoutDashboard.visibility = View.GONE
                    layoutPlants.visibility = View.VISIBLE
                    fabAdd.show()

                    // Resetear scroll al tope si es necesario
                    rvPlants.smoothScrollToPosition(0)
                    true
                }

                R.id.nav_sensors -> {
                    Toast.makeText(this, "Vista Sensores", Toast.LENGTH_SHORT).show()
                    false
                }

                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    false
                }

                R.id.nav_profile -> {
                    mostrarDialogoCerrarSesion()
                    false
                }
                else -> false
            }
        }
    }

    private fun cargarDatos(userId: Long) {
        Thread {
            try {
                val plantasCompletas = PlantaDao.obtenerPlantasCompletas(userId)
                val stats = PlantaDao.obtenerEstadisticas(userId)

                runOnUiThread {
                    // Actualizar Lista (usando updateList del adapter nuevo)
                    if (plantasCompletas.isNotEmpty()) {
                        val listaParaAdapter = plantasCompletas.map { pc ->
                            PlantaConDatos(
                                planta = pc.planta,
                                ubicacion = pc.ubicacion,
                                humedadSuelo = pc.humedad,
                                temperatura = pc.temperatura,
                                luz = pc.luz,
                                // --- CORRECCIÓN AQUÍ ---
                                // Antes usabas: pc.calcularNivelAgua() (que inflaba el valor)
                                // Ahora usamos: pc.humedad.toInt() (valor real del sensor)
                                nivelAgua = pc.humedad.toInt(),
                                // -----------------------
                                estado = pc.determinarEstadoUI(),
                                ultimoRiego = pc.ultimoRiego
                            )
                        }
                        adapter.updateList(listaParaAdapter)
                    }

                    // Actualizar Dashboard
                    tvTotalValue.text = stats[0].toString()
                    tvHealthyValue.text = stats[1].toString()
                    tvCriticalValue.text = stats[2].toString()

                    val numCriticas = stats[2]
                    if (numCriticas > 0) {
                        cardAlertCritical.visibility = View.VISIBLE
                        val mensaje = if (numCriticas == 1) "1 planta necesita atención" else "$numCriticas plantas necesitan atención"
                        tvAlertMessage.text = mensaje
                    } else {
                        cardAlertCritical.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Log.e("MainActivity", "Error cargando datos", e) }
            }
        }.start()
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Perfil")
            .setMessage("¿Deseas cerrar sesión?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
                prefs.edit().clear().apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1L) cargarDatos(userId)
    }

    // Clase auxiliar para Adapter
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