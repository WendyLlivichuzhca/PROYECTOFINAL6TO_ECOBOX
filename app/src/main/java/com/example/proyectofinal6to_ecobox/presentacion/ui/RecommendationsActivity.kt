package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.model.Recommendation
import com.example.proyectofinal6to_ecobox.presentacion.adapter.RecommendationsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecommendationsActivity : AppCompatActivity() {

    private lateinit var rvRecommendations: RecyclerView
    private lateinit var adapter: RecommendationsAdapter
    private lateinit var tvAiSummary: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendations)

        initViews()
        setupRecyclerView()
        loadRecommendations()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        tvAiSummary = findViewById(R.id.tvAiSummary)
        rvRecommendations = findViewById(R.id.rvRecommendations)
        emptyState = findViewById(R.id.emptyState)
    }

    private fun setupRecyclerView() {
        adapter = RecommendationsAdapter(emptyList()) { rec ->
            handleRecommendationClick(rec)
        }
        rvRecommendations.layoutManager = LinearLayoutManager(this)
        rvRecommendations.adapter = adapter
    }

    private fun loadRecommendations() {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val userId = prefs.getLong("user_id", -1)

        CoroutineScope(Dispatchers.IO).launch {
            // Simulamos la obtención de recomendaciones desde el motor de IA
            // En una implementación real, esto llamaría a un servicio que analice sensores
            val recommendations = getMockRecommendations() 

            withContext(Dispatchers.Main) {
                if (recommendations.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvRecommendations.visibility = View.GONE
                    tvAiSummary.text = "Todo parece estar en orden en tu jardín."
                } else {
                    emptyState.visibility = View.GONE
                    rvRecommendations.visibility = View.VISIBLE
                    adapter.updateItems(recommendations)
                    
                    val urgentes = recommendations.count { it.type == "URGENTE" }
                    tvAiSummary.text = if (urgentes > 0) {
                        "Atención: Tienes $urgentes acciones críticas pendientes."
                    } else {
                        "Tu jardín está sano. Mira estas sugerencias preventivas."
                    }
                }
            }
        }
    }

    private fun getMockRecommendations(): List<Recommendation> {
        return listOf(
            Recommendation(1, "URGENTE", 1, "Suculenta Mía", "Humedad crítica (18%) detectada en Maceta 1", "2 horas", "regar", 0.96f),
            Recommendation(2, "ADVERTENCIA", 3, "Orquídea Blanca", "Temperatura descendiendo (14°C). Riesgo de helada.", "5 horas", "mover", 0.88f),
            Recommendation(3, "INFO", 5, "Tomatera", "Es un buen momento para podar las hojas bajas.", "Ayer", "podar", 0.75f)
        )
    }

    private fun handleRecommendationClick(rec: Recommendation) {
        Toast.makeText(this, "Acción sugerida: ${rec.action.uppercase()}", Toast.LENGTH_SHORT).show()
        // Aquí se podría navegar al detalle de la planta o disparar la acción
    }
}
