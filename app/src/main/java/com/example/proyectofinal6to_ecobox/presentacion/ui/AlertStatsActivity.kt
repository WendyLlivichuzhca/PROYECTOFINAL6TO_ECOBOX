package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import kotlinx.coroutines.launch

class AlertStatsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tvTotalAlertas: TextView
    private lateinit var tvNoLeidas: TextView
    private lateinit var tvResueltas: TextView
    private lateinit var tvUltimas24h: TextView
    private lateinit var layoutStatsContainer: LinearLayout
    private lateinit var layoutEmpty: LinearLayout

    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_stats)

        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

        if (authToken == null) {
            finish()
            return
        }

        initViews()
        loadAlertStats()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tvTotalAlertas = findViewById(R.id.tvTotalAlertas)
        tvNoLeidas = findViewById(R.id.tvNoLeidas)
        tvResueltas = findViewById(R.id.tvResueltas)
        tvUltimas24h = findViewById(R.id.tvUltimas24h)
        layoutStatsContainer = findViewById(R.id.layoutStatsContainer)
        layoutEmpty = findViewById(R.id.layoutEmpty)
    }

    private fun loadAlertStats() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAlertStats("Token $authToken")
                
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!.estadisticas
                    
                    if (stats.isEmpty()) {
                        showEmptyState()
                    } else {
                        updateStatsUI(stats)
                    }
                } else {
                    Log.e("AlertStats", "Error: ${response.code()}")
                    showEmptyState()
                }
            } catch (e: Exception) {
                Log.e("AlertStats", "Network error", e)
                showEmptyState()
            }
        }
    }

    private fun updateStatsUI(stats: Map<String, Any>) {
        layoutStatsContainer.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE

        val total = (stats["total"] as? Number)?.toInt() ?: 0
        val noLeidas = (stats["no_leidas"] as? Number)?.toInt() ?: 0
        val resueltas = (stats["resueltas"] as? Number)?.toInt() ?: 0
        val ultimas24h = (stats["ultimas_24h"] as? Number)?.toInt() ?: 0

        tvTotalAlertas.text = total.toString()
        tvNoLeidas.text = noLeidas.toString()
        tvResueltas.text = resueltas.toString()
        tvUltimas24h.text = ultimas24h.toString()
    }

    private fun showEmptyState() {
        layoutStatsContainer.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
    }
}
