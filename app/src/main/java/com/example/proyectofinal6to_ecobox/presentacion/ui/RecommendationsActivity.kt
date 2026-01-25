package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.example.proyectofinal6to_ecobox.presentacion.adapter.RecommendationsAdapter
import kotlinx.coroutines.launch

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
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        lifecycleScope.launch {
            try {
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance.getRecommendations("Token $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val recommendations = body.recommendations
                    
                    if (recommendations.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        rvRecommendations.visibility = View.GONE
                        tvAiSummary.text = "Todo parece estar en orden en tu jardín."
                    } else {
                        emptyState.visibility = View.GONE
                        rvRecommendations.visibility = View.VISIBLE
                        adapter.updateItems(recommendations)
                        
                        val urgentes = body.urgentes
                        tvAiSummary.text = if (urgentes > 0) {
                            "Atención: Tienes $urgentes acciones críticas pendientes."
                        } else {
                            "Tu jardín está sano. Mira estas sugerencias preventivas."
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Recommendations", "Error red recomendaciones", e)
                Toast.makeText(this@RecommendationsActivity, "Error al cargar recomendaciones reales", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleRecommendationClick(rec: com.example.proyectofinal6to_ecobox.data.network.RecommendationResponse) {
        // En lugar de un Toast, mostrar opciones de Feedback como en la Web
        val options = arrayOf("Fue acertada ✅", "Fue errónea ❌", "Ejecutar acción 🚀")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Retroalimentación IA")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> provideFeedback(rec.id, "correct")
                    1 -> provideFeedback(rec.id, "incorrect")
                    2 -> executeSuggestedAction(rec)
                }
            }
            .show()
    }

    private fun provideFeedback(id: Long, feedback: String) {
        val token = getSharedPreferences("ecobox_prefs", MODE_PRIVATE).getString("auth_token", "") ?: ""
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.provideAiFeedback("Token $token", id, mapOf("feedback" to feedback))
                if (response.isSuccessful) {
                    Toast.makeText(this@RecommendationsActivity, "¡Gracias! Tu feedback ayuda a mejorar la IA", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {}
        }
    }

    private fun executeSuggestedAction(rec: com.example.proyectofinal6to_ecobox.data.network.RecommendationResponse) {
        // Lógica para ejecutar el riego si la acción es de riego
        Toast.makeText(this, "Ejecutando: ${rec.action}", Toast.LENGTH_SHORT).show()
    }
}
