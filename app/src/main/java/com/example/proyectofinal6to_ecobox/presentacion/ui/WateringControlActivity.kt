package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.example.proyectofinal6to_ecobox.data.network.WateringItemResponse
import com.example.proyectofinal6to_ecobox.data.network.WateringHistoryStats
import com.example.proyectofinal6to_ecobox.presentacion.adapter.WateringHistoryAdapter
import kotlinx.coroutines.launch

class WateringControlActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var rvWaterings: RecyclerView
    private lateinit var adapter: WateringHistoryAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var spinnerFilter: Spinner
    private lateinit var btnRefresh: ImageButton

    // Estadísticas
    private lateinit var tvTotalRiegos: TextView
    private lateinit var tvCompletados: TextView
    private lateinit var tvEficiencia: TextView
    private lateinit var layoutStats: LinearLayout

    private var authToken: String? = null
    private var currentFilter = "todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watering_control)

        val prefs = getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

        if (authToken == null) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupFilter()
        loadWaterings()
        loadStats()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        rvWaterings = findViewById(R.id.rvWaterings)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        spinnerFilter = findViewById(R.id.spinnerFilter)
        btnRefresh = findViewById(R.id.btnRefresh)

        // Estadísticas
        layoutStats = findViewById(R.id.layoutStats)
        tvTotalRiegos = findViewById(R.id.tvTotalRiegos)
        tvCompletados = findViewById(R.id.tvCompletados)
        tvEficiencia = findViewById(R.id.tvEficiencia)

        btnRefresh.setOnClickListener {
            loadWaterings()
            loadStats()
        }
    }

    private fun setupRecyclerView() {
        adapter = WateringHistoryAdapter(emptyList()) { watering ->
            showWateringDetails(watering)
        }
        rvWaterings.layoutManager = LinearLayoutManager(this)
        rvWaterings.adapter = adapter
    }

    private fun setupFilter() {
        val filterOptions = listOf("Todos", "Completados", "Programados", "En Curso", "Cancelados")
        val filterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = filterAdapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = when (position) {
                    0 -> "todos"
                    1 -> "COMPLETADO"
                    2 -> "PROGRAMADO"
                    3 -> "EN_CURSO"
                    4 -> "CANCELADO"
                    else -> "todos"
                }
                loadWaterings()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadWaterings() {
        progressBar.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Usar el endpoint de AI que funciona
                val response = RetrofitClient.instance.getWateringHistory(
                    "Token $authToken",
                    1 // TODO: Pasar el ID de la planta seleccionada
                )

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    
                    if (result.success) {
                        val waterings = result.waterings
                        
                        // Filtrar por estado si es necesario
                        val filteredWaterings = if (currentFilter == "todos") {
                            waterings
                        } else {
                            waterings.filter { it.status.equals(currentFilter, ignoreCase = true) }
                        }
                        
                        if (filteredWaterings.isEmpty()) {
                            layoutEmpty.visibility = View.VISIBLE
                            rvWaterings.visibility = View.GONE
                        } else {
                            layoutEmpty.visibility = View.GONE
                            rvWaterings.visibility = View.VISIBLE
                            
                            // Convertir WateringHistoryItem a WateringItemResponse
                            val convertedWaterings = filteredWaterings.map { item ->
                                WateringItemResponse(
                                    id = item.id,
                                    planta = result.plantId,
                                    usuario = 0, // No disponible en el endpoint de AI
                                    tipo = item.mode,
                                    estado = item.status,
                                    duracionSegundos = item.duration,
                                    cantidadMl = null, // No disponible
                                    humedadInicial = item.initialHumidity,
                                    humedadFinal = item.finalHumidity,
                                    fechaCreacion = item.date,
                                    fechaInicio = null,
                                    fechaFin = null,
                                    fechaProgramada = null
                                )
                            }
                            adapter.updateData(convertedWaterings)
                        }
                        
                        // Actualizar estadísticas
                        updateStats(result.stats)
                    } else {
                        showError("No se pudieron cargar los riegos")
                    }
                } else {
                    showError("Error al cargar riegos")
                }
            } catch (e: Exception) {
                Log.e("WateringControl", "Error loading waterings", e)
                showError("Error de conexión")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateStats(stats: WateringHistoryStats) {
        tvTotalRiegos.text = stats.totalWaterings.toString()
        tvCompletados.text = stats.totalWaterings.toString()
        tvEficiencia.text = String.format("%.1f%%", stats.successRate)
        layoutStats.visibility = View.VISIBLE
    }

    private fun loadStats() {
        // Ya se cargan con el historial
    }

    private fun showWateringDetails(watering: WateringItemResponse) {
        val details = buildString {
            append("ID: ${watering.id}\n")
            append("Tipo: ${watering.tipo}\n")
            append("Estado: ${watering.estado}\n")
            append("Duración: ${watering.duracionSegundos}s\n")
            append("Cantidad: ${watering.cantidadMl}ml\n")
            
            if (watering.humedadInicial != null && watering.humedadFinal != null) {
                append("\nHumedad inicial: ${String.format("%.1f%%", watering.humedadInicial)}\n")
                append("Humedad final: ${String.format("%.1f%%", watering.humedadFinal)}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalles del Riego")
            .setMessage(details)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
