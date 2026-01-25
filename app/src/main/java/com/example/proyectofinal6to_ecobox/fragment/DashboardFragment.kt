package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
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

        // Cargar Estadísticas
        cargarEstadisticas(view, userId)

        // Cargar IA Insights
        cargarIAInsights(view, userId)

        // Listeners de Alertas
        val btnAlerts = view.findViewById<View>(R.id.btnAlerts)
        val cardAlertCritical = view.findViewById<View>(R.id.cardAlertCritical)

        val goToAlerts = View.OnClickListener {
            startActivity(Intent(requireContext(), AlertsActivity::class.java))
        }
        btnAlerts.setOnClickListener(goToAlerts)
        cardAlertCritical.setOnClickListener(goToAlerts)

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

        // Configurar Gráfico
        val chartDashboard = view.findViewById<LineChart>(R.id.chartDashboard)
        if (chartDashboard != null) {
            setupChart(chartDashboard)
            loadChartData(chartDashboard, userId)
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

    private fun loadChartData(chart: LineChart, userId: Long) {
        Thread {
            val humidityData = generateMockHistoryData(24)
            
            activity?.runOnUiThread {
                val entries = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                humidityData.forEachIndexed { index, pair ->
                    entries.add(Entry(index.toFloat(), pair.second))
                    labels.add(pair.first)
                }

                chart.xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < labels.size) labels[index] else ""
                    }
                }

                val dataSet = LineDataSet(entries, "Humedad Promedio")
                dataSet.color = Color.parseColor("#10B981")
                dataSet.lineWidth = 3f
                dataSet.setCircleColor(Color.parseColor("#10B981"))
                dataSet.circleRadius = 4f
                dataSet.setDrawCircleHole(true)
                dataSet.circleHoleColor = Color.WHITE
                dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
                dataSet.setDrawValues(false)
                dataSet.setDrawFilled(true)
                dataSet.fillColor = Color.parseColor("#10B981")
                dataSet.fillAlpha = 30

                chart.data = LineData(dataSet)
                chart.animateX(1000)
            }
        }.start()
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

    private fun cargarEstadisticas(view: View, userId: Long) {
        val tvTotal = view.findViewById<View>(R.id.cardStatTotal)?.findViewById<TextView>(R.id.tvCount)
        val tvHealthy = view.findViewById<View>(R.id.cardStatHealthy)?.findViewById<TextView>(R.id.tvCount)
        val tvCritical = view.findViewById<View>(R.id.cardStatCritical)?.findViewById<TextView>(R.id.tvCount)

        view.findViewById<View>(R.id.cardStatTotal)?.findViewById<TextView>(R.id.tvLabel)?.text = "Total"
        view.findViewById<View>(R.id.cardStatHealthy)?.findViewById<TextView>(R.id.tvLabel)?.text = "Sanas"
        view.findViewById<View>(R.id.cardStatCritical)?.findViewById<TextView>(R.id.tvLabel)?.text = "Críticas"

        val cardAlertCritical = view.findViewById<View>(R.id.cardAlertCritical)
        val tvAlertMessage = view.findViewById<TextView>(R.id.tvAlertMessage)

        Thread {
            val stats = PlantaDao.obtenerEstadisticas(userId) 
            activity?.runOnUiThread {
                tvTotal?.text = stats[0].toString()
                tvHealthy?.text = stats[1].toString()
                tvCritical?.text = stats[2].toString()

                val numCriticas = stats[2]
                if (numCriticas > 0) {
                    cardAlertCritical?.visibility = View.VISIBLE
                    tvAlertMessage?.text = if (numCriticas == 1) "1 planta necesita atención" else "$numCriticas plantas necesitan atención"
                } else {
                    cardAlertCritical?.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun cargarIAInsights(view: View, userId: Long) {
        val tvAiAccuracy = view.findViewById<TextView>(R.id.tvAiAccuracy)
        val tvAiModels = view.findViewById<TextView>(R.id.tvAiModels)
        val tvAiRecommendation = view.findViewById<TextView>(R.id.tvAiRecommendation)

        Thread {
            try {
                Thread.sleep(800)
                activity?.runOnUiThread {
                    tvAiAccuracy?.text = "94.2%"
                    tvAiModels?.text = "4 Activos"
                    tvAiRecommendation?.text = "¡Buen día! Tus plantas muestran una humedad estable. No es necesario regar hoy."
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Error cargando IA Insights: ${e.message}")
            }
        }.start()
    }
}