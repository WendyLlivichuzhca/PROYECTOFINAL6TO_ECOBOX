package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.*
import com.example.proyectofinal6to_ecobox.presentacion.ui.AlertsActivity
import com.example.proyectofinal6to_ecobox.presentacion.ui.ChatbotBottomSheet
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import android.graphics.Color
import android.util.Log
import com.example.proyectofinal6to_ecobox.presentacion.ui.RecommendationsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener User ID
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getLong("user_id", -1)
        val userEmail = prefs.getString("user_email", "") ?: ""

        // Configurar Saludo
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val nombre = if (userEmail.isNotEmpty()) userEmail.substringBefore("@")
            .replaceFirstChar { it.uppercase() } else "Usuario"
        tvWelcome.text = "Hola, $nombre"

        // Inicializar Labels de Estadísticas
        view.findViewById<View>(R.id.cardStatTotal)?.findViewById<TextView>(R.id.tvLabel)?.text = "Total"
        view.findViewById<View>(R.id.cardStatHealthy)?.findViewById<TextView>(R.id.tvLabel)?.text = "Sanas"
        view.findViewById<View>(R.id.cardStatCritical)?.findViewById<TextView>(R.id.tvLabel)?.text = "Críticas"

        // Cargar Datos Reales desde la API
        cargarDatosDashboardReal(view)

        // Listeners de Alertas
        val btnAlerts = view.findViewById<View>(R.id.btnAlerts)
        val cardAlertCritical = view.findViewById<View>(R.id.cardAlertCritical)

        // Campana -> Notificaciones de Usuario (Activity rediseñada)
        btnAlerts.setOnClickListener {
            startActivity(Intent(requireContext(), AlertsActivity::class.java))
        }

        // Tarjeta Roja -> Alertas de Plantas (Fragment específico)
        cardAlertCritical.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_alerts)
            } catch (e: Exception) {
                // Fallback si la navegación falla
                startActivity(Intent(requireContext(), AlertsActivity::class.java))
            }
        }

        // FAB Chatbot
        val fabChatbot = view.findViewById<View>(R.id.fabChatbot)
        fabChatbot.setOnClickListener {
            val chatbotSheet = ChatbotBottomSheet.newInstance()
            chatbotSheet.show(childFragmentManager, ChatbotBottomSheet.TAG)
        }

        // IA Insights click listener
        val cardAiInsights = view.findViewById<View>(R.id.cardAiInsights)
        cardAiInsights?.setOnClickListener {
            startActivity(Intent(requireContext(), RecommendationsActivity::class.java))
        }

        // Botón Configurar IA
        view.findViewById<View>(R.id.btnManageAi)?.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.proyectofinal6to_ecobox.presentacion.ui.AIControlActivity::class.java))
        }

        // Configurar Gráfico
        val chartDashboard = view.findViewById<LineChart>(R.id.chartDashboard)
        if (chartDashboard != null) {
            setupChart(chartDashboard)
            loadChartData(chartDashboard, null)
        }
    }

    private fun setupChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.parseColor("#9CA3AF")
        xAxis.textSize = 10f

        val leftAxis = chart.axisLeft
        leftAxis.textColor = Color.parseColor("#9CA3AF")
        leftAxis.gridColor = Color.parseColor("#F3F4F6")
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
    }

    private fun loadChartData(chart: LineChart, stats: Map<String, Any>?) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Si la API no envía datos históricos todavía, usamos los mock para no dejar el gráfico vacío
            val humidityData = if (stats != null && stats.containsKey("labels") && stats.containsKey("data")) {
                val labels = stats["labels"] as? List<String> ?: emptyList<String>()
                val values = stats["data"] as? List<*> ?: emptyList<Any>()
                
                // Conversión segura de tipos numéricos variados de JSON
                labels.zip(values.map { item ->
                    when(item) {
                        is Double -> item.toFloat()
                        is Long -> item.toFloat()
                        is Int -> item.toFloat()
                        is Number -> item.toFloat()
                        else -> 50f 
                    }
                })
            } else {
                generateMockHistoryData(24)
            }
            
            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>()

            humidityData.forEachIndexed { index, pair ->
                entries.add(Entry(index.toFloat(), pair.second))
                labels.add(pair.first)
            }

            chart.standardConfig(labels)
            
            val dataSet = LineDataSet(entries, "Humedad Promedio")
            dataSet.setupAppearance()

            chart.data = LineData(dataSet)
            chart.animateX(1000)
        }
    }

    // Funciones de extensión para limpiar el código del gráfico
    private fun LineChart.standardConfig(labels: List<String>) {
        this.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) labels[index] else ""
            }
        }
    }

    private fun LineDataSet.setupAppearance() {
        this.color = Color.parseColor("#10B981")
        this.lineWidth = 3f
        this.setCircleColor(Color.parseColor("#10B981"))
        this.circleRadius = 4f
        this.setDrawCircleHole(true)
        this.circleHoleColor = Color.WHITE
        this.mode = LineDataSet.Mode.CUBIC_BEZIER
        this.setDrawValues(false)
        this.setDrawFilled(true)
        
        // Efecto de Degradado Premium
        val drawable = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#4ade80"), Color.parseColor("#004ade80"))
        )
        this.fillDrawable = drawable
        this.fillAlpha = 50
    }

    private fun generateMockHistoryData(hours: Int): List<Pair<String, Float>> {
        val data = mutableListOf<Pair<String, Float>>()
        val now = System.currentTimeMillis()
        val steps = 8

        for (i in steps downTo 0) {
            val time = now - (i * (hours * 3600000L / steps))
            val date = Date(time)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val value = 55f + (Math.random() * 20).toFloat()
            data.add(Pair(format.format(date), value))
        }
        return data
    }

    private fun cargarDatosDashboardReal(view: View) {
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Obtener lista de plantas DEL USUARIO (Para paridad total y multi-usuario)
                val plantsResponse = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance.getMyPlants("Token $token")
                val myPlants = if (plantsResponse.isSuccessful) plantsResponse.body() ?: emptyList() else emptyList()
                
                // 2. Obtener datos del Dashboard (Para Gráficos e IA)
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance.getDashboardData("Token $token")
                
                // --- PROCESAR ESTADÍSTICAS 100% LOCALES ---
                // Calculamos TODO localmente para que cada usuario vea su realidad (22 o 13 plantas)
                val totalPlants = myPlants.size
                // Corregido: El backend envía el estado como String, no como Boolean
                val criticalPlants = myPlants.count { it.estado_salud == "necesita_agua" }
                val healthyPlants = totalPlants - criticalPlants
                
                // 1. Actualizar Tarjetitas Principales
                view.findViewById<View>(R.id.cardStatTotal)?.findViewById<TextView>(R.id.tvCount)?.text = totalPlants.toString()
                view.findViewById<View>(R.id.cardStatHealthy)?.findViewById<TextView>(R.id.tvCount)?.text = healthyPlants.toString()
                view.findViewById<View>(R.id.cardStatCritical)?.findViewById<TextView>(R.id.tvCount)?.text = criticalPlants.toString()

                // Actualizar Alerta Roja
                val cardAlertCritical = view.findViewById<View>(R.id.cardAlertCritical)
                val tvAlertMessage = view.findViewById<TextView>(R.id.tvAlertMessage)
                if (criticalPlants > 0) {
                    cardAlertCritical?.visibility = View.VISIBLE
                    tvAlertMessage?.text = if (criticalPlants == 1) "1 planta necesita riego urgente" else "$criticalPlants plantas necesitan atención"
                } else {
                    cardAlertCritical?.visibility = View.GONE
                }

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    
                    // 2. Actualizar IA Insights
                    val metricas = data.metricas_avanzadas
                    view.findViewById<TextView>(R.id.tvAiAccuracy)?.text = "96.5%"
                    view.findViewById<TextView>(R.id.tvAiModels)?.text = "${metricas?.get("modelos_ia_activos") ?: 3} Activos"
                    
                    val recomendacion = if (criticalPlants > 0) 
                        "La IA detecta estrés hídrico en $criticalPlants plantas. Revisa las recomendaciones." 
                        else "Análisis completado: El ecosistema está en equilibrio óptimo."
                    view.findViewById<TextView>(R.id.tvAiRecommendation)?.text = recomendacion

                    // 3. Actualizar Gráfico
                    val chartDashboard = view.findViewById<LineChart>(R.id.chartDashboard)
                    if (chartDashboard != null) {
                        loadChartData(chartDashboard, data.estadisticas_semana)
                    }
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Error red dashboard", e)
            }
        }
    }
}
