package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao.DataPointDAO
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

class HistoryActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var tvHumidityAvg: TextView
    private lateinit var tvTempAvg: TextView
    private lateinit var tvLightAvg: TextView

    // Botones de periodo
    private lateinit var btn24h: TextView
    private lateinit var btn7d: TextView
    private lateinit var btn30d: TextView

    // Botones de plantas
    private lateinit var plantsContainer: LinearLayout

    // Eventos
    private lateinit var event1: CardView
    private lateinit var event2: CardView
    private lateinit var event3: CardView

    // Listas
    private val plantButtons = mutableListOf<TextView>()
    private var selectedPeriod = 24
    private var selectedPlantId: Long = -1 // -1 para "Todas las plantas"
    private var userId: Long = 1 // Obtener del SharedPreferences/Login
    private var plantasFamilia = mutableListOf<PlantaDao.PlantaConDatos>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 1. Obtener usuario de sesión
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadPlantasFamilia()
    }

    private fun initViews() {
        lineChart = findViewById(R.id.lineChart)
        tvHumidityAvg = findViewById(R.id.tvHumidityAvg)
        tvTempAvg = findViewById(R.id.tvTempAvg)
        tvLightAvg = findViewById(R.id.tvLightAvg)

        // Period buttons
        btn24h = findViewById(R.id.btn24h)
        btn7d = findViewById(R.id.btn7d)
        btn30d = findViewById(R.id.btn30d)

        // Plant container
        plantsContainer = findViewById(R.id.plantsContainer)

        // Event cards
        event1 = findViewById(R.id.event1)
        event2 = findViewById(R.id.event2)
        event3 = findViewById(R.id.event3)
    }

    private fun setupClickListeners() {
        // Listeners para periodos
        btn24h.setOnClickListener {
            selectedPeriod = 24
            updatePeriodSelection()
            loadHistoryData()
        }

        btn7d.setOnClickListener {
            selectedPeriod = 168 // 24 * 7
            updatePeriodSelection()
            loadHistoryData()
        }

        btn30d.setOnClickListener {
            selectedPeriod = 720 // 24 * 30
            updatePeriodSelection()
            loadHistoryData()
        }
    }

    private fun updatePeriodSelection() {
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_button_selected)
        val normalBg = ContextCompat.getDrawable(this, android.R.color.transparent)
        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)
        val normalTextColor = ContextCompat.getColor(this, R.color.text_gray)

        // Reset all
        btn24h.background = normalBg
        btn24h.setTextColor(normalTextColor)
        btn7d.background = normalBg
        btn7d.setTextColor(normalTextColor)
        btn30d.background = normalBg
        btn30d.setTextColor(normalTextColor)

        // Set selected
        when (selectedPeriod) {
            24 -> {
                btn24h.background = selectedBg
                btn24h.setTextColor(selectedTextColor)
            }
            168 -> {
                btn7d.background = selectedBg
                btn7d.setTextColor(selectedTextColor)
            }
            720 -> {
                btn30d.background = selectedBg
                btn30d.setTextColor(selectedTextColor)
            }
        }
    }

    private fun loadPlantasFamilia() {
        Thread {
            try {
                // Obtener plantas de mi familia
                plantasFamilia = PlantaDao.obtenerPlantasFamiliaConDatos(userId).toMutableList()

                runOnUiThread {
                    createPlantButtons()
                    // Seleccionar "Todas" por defecto
                    if (plantButtons.isNotEmpty()) {
                        selectPlantButton(plantButtons[0])
                        loadHistoryData() // Cargar datos iniciales
                    } else {
                        // No hay plantas en la familia
                        showNoPlantsMessage()
                    }
                }

            } catch (e: Exception) {
                Log.e("HistoryActivity", "Error cargando plantas familia", e)
                runOnUiThread {
                    Toast.makeText(this, "Error al cargar plantas", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showNoPlantsMessage() {
        Toast.makeText(this, "No hay plantas en tu familia", Toast.LENGTH_LONG).show()
        tvHumidityAvg.text = "0%"
        tvTempAvg.text = "0°C"
        tvLightAvg.text = "0%"
        lineChart.clear()
        lineChart.setNoDataText("No hay datos de plantas")
        event1.visibility = View.GONE
        event2.visibility = View.GONE
        event3.visibility = View.GONE
    }

    private fun createPlantButtons() {
        plantsContainer.removeAllViews()
        plantButtons.clear()

        // Botón "Todas"
        val allButton = createPlantButton("Todas", -1)
        plantsContainer.addView(allButton)
        plantButtons.add(allButton)

        // Botones para cada planta de la familia
        plantasFamilia.forEach { plant ->
            val button = createPlantButton(plant.nombre, plant.id)
            plantsContainer.addView(button)
            plantButtons.add(button)
        }
    }

    private fun createPlantButton(text: String, plantId: Long): TextView {
        val button = TextView(this).apply {
            this.text = text
            setPadding(20.dpToPx(), 10.dpToPx(), 20.dpToPx(), 10.dpToPx())
            setTextColor(ContextCompat.getColor(this@HistoryActivity, R.color.text_gray))
            textSize = 14f
            setOnClickListener {
                selectedPlantId = plantId
                updatePlantSelection()
                loadHistoryData()
            }
        }

        button.tag = plantId
        return button
    }

    private fun updatePlantSelection() {
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_button_selected)
        val normalBg = ContextCompat.getDrawable(this, android.R.color.transparent)
        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)
        val normalTextColor = ContextCompat.getColor(this, R.color.text_gray)

        plantButtons.forEach { button ->
            val plantId = button.tag as Long
            if (plantId == selectedPlantId) {
                button.background = selectedBg
                button.setTextColor(selectedTextColor)
            } else {
                button.background = normalBg
                button.setTextColor(normalTextColor)
            }
        }
    }

    private fun selectPlantButton(button: TextView) {
        selectedPlantId = button.tag as Long
        updatePlantSelection()
        loadHistoryData()
    }

    private fun loadHistoryData() {
        // Mostrar loading
        tvHumidityAvg.text = "--%"
        tvTempAvg.text = "--°C"
        tvLightAvg.text = "--%"

        Thread {
            try {
                // Obtener estadísticas familiares
                val stats = PlantaDao.obtenerEstadisticasHistorialFamiliar(
                    userId,
                    selectedPeriod,
                    selectedPlantId
                )

                // Obtener datos para el gráfico
                val graficoData = PlantaDao.obtenerDatosHistoricosGraficoFamiliar(
                    userId,
                    selectedPeriod,
                    selectedPlantId
                )

                // Obtener eventos familiares (con manejo de error)
                val eventos = try {
                    PlantaDao.obtenerEventosRecientesFamiliar(
                        userId,
                        3,
                        selectedPlantId
                    )
                } catch (e: Exception) {
                    Log.w("HistoryActivity", "Error al obtener eventos, usando lista vacía", e)
                    emptyList()
                }

                runOnUiThread {
                    // Actualizar estadísticas
                    tvHumidityAvg.text = String.format("%.1f%%", stats["humedad"] ?: 0f)
                    tvTempAvg.text = String.format("%.1f°C", stats["temperatura"] ?: 0f)
                    tvLightAvg.text = String.format("%.1f%%", stats["luz"] ?: 0f)

                    // Configurar gráfico
                    setupChart(graficoData)

                    // Mostrar eventos
                    mostrarEventos(eventos)
                }

            } catch (e: Exception) {
                Log.e("HistoryActivity", "Error cargando datos", e)
                runOnUiThread {
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                    // Mostrar valores por defecto
                    tvHumidityAvg.text = "65%"
                    tvTempAvg.text = "22°C"
                    tvLightAvg.text = "75%"
                    lineChart.setNoDataText("Error cargando datos")
                    event1.visibility = View.GONE
                    event2.visibility = View.GONE
                    event3.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun setupChart(graficoData: Map<String, List<DataPointDAO>>) {
        lineChart.clear()

        // Si no hay datos, mostrar mensaje
        if (graficoData.isEmpty() || graficoData["humedad"].isNullOrEmpty()) {
            lineChart.setNoDataText("No hay datos disponibles")
            lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.text_gray))
            lineChart.invalidate()
            return
        }

        // Configuración básica
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.setDrawGridBackground(false)

        // Leyenda
        val legend = lineChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.textSize = 11f
        legend.textColor = ContextCompat.getColor(this, R.color.text_dark)

        // Eje X
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = ContextCompat.getColor(this, R.color.chart_grid)
        xAxis.gridLineWidth = 0.5f
        xAxis.textColor = ContextCompat.getColor(this, R.color.text_gray)
        xAxis.textSize = 10f

        val labels = graficoData["humedad"]?.map { it.label } ?: emptyList()
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in labels.indices) labels[index] else ""
            }
        }

        // Eje Y izquierdo
        val leftAxis = lineChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
        leftAxis.granularity = 20f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = ContextCompat.getColor(this, R.color.chart_grid)
        leftAxis.gridLineWidth = 0.5f
        leftAxis.textColor = ContextCompat.getColor(this, R.color.text_gray)
        leftAxis.textSize = 10f
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}"
            }
        }

        // Eje Y derecho
        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false

        // Preparar datos
        val entriesHumedad = mutableListOf<Entry>()
        val entriesTemp = mutableListOf<Entry>()
        val entriesLuz = mutableListOf<Entry>()

        // Datos de humedad
        graficoData["humedad"]?.forEachIndexed { index, point ->
            entriesHumedad.add(Entry(index.toFloat(), point.value))
        }

        // Datos de temperatura (escalar si es necesario)
        graficoData["temperatura"]?.forEachIndexed { index, point ->
            // Ajustar escala de temperatura si es muy alta
            val tempValue = if (point.value > 50) point.value / 2 else point.value
            entriesTemp.add(Entry(index.toFloat(), tempValue))
        }

        // Datos de luz
        graficoData["luz"]?.forEachIndexed { index, point ->
            entriesLuz.add(Entry(index.toFloat(), point.value))
        }

        // Crear datasets
        val setHumedad = LineDataSet(entriesHumedad, "Humedad (%)")
        setHumedad.color = ContextCompat.getColor(this, R.color.green_header)
        setHumedad.lineWidth = 2.5f
        setHumedad.setDrawCircles(false)
        setHumedad.setDrawValues(false)
        setHumedad.mode = LineDataSet.Mode.CUBIC_BEZIER

        val setTemp = LineDataSet(entriesTemp, "Temperatura (°C)")
        setTemp.color = ContextCompat.getColor(this, R.color.sensor_temp_icon)
        setTemp.lineWidth = 2.5f
        setTemp.setDrawCircles(false)
        setTemp.setDrawValues(false)
        setTemp.mode = LineDataSet.Mode.CUBIC_BEZIER

        val setLuz = LineDataSet(entriesLuz, "Luz (%)")
        setLuz.color = ContextCompat.getColor(this, R.color.sensor_light_icon)
        setLuz.lineWidth = 2.5f
        setLuz.setDrawCircles(false)
        setLuz.setDrawValues(false)
        setLuz.mode = LineDataSet.Mode.CUBIC_BEZIER

        // Crear LineData
        val lineData = LineData(setHumedad, setTemp, setLuz)
        lineData.setValueTextSize(10f)

        lineChart.data = lineData
        lineChart.invalidate()
    }

    private fun mostrarEventos(eventos: List<PlantaDao.EventoDAO>) {
        val eventViews = listOf(event1, event2, event3)

        eventos.forEachIndexed { index, evento ->
            if (index < eventViews.size) {
                updateEventView(eventViews[index], evento)
            }
        }

        // Ocultar eventos no usados
        for (i in eventos.size until eventViews.size) {
            eventViews[i].visibility = View.GONE
        }

        // Si no hay eventos, mostrar mensaje
        if (eventos.isEmpty()) {
            event1.visibility = View.VISIBLE
            event1.findViewById<TextView>(R.id.tvEventTitle).text = "No hay eventos recientes"
            event1.findViewById<TextView>(R.id.tvEventSubtitle).text = ""
            event1.findViewById<ImageView>(R.id.imgEventIcon).setImageResource(R.drawable.ic_leaf)
            event2.visibility = View.GONE
            event3.visibility = View.GONE
        }
    }

    private fun updateEventView(eventView: CardView, evento: PlantaDao.EventoDAO) {
        eventView.visibility = View.VISIBLE

        val icon = eventView.findViewById<ImageView>(R.id.imgEventIcon)
        val title = eventView.findViewById<TextView>(R.id.tvEventTitle)
        val subtitle = eventView.findViewById<TextView>(R.id.tvEventSubtitle)

        // Configurar icono según tipo
        when (evento.iconoTipo) {
            1 -> { // Riego
                icon.setImageResource(R.drawable.ic_water)
                icon.setColorFilter(ContextCompat.getColor(this, R.color.green_header))
            }
            2 -> { // Alerta
                icon.setImageResource(R.drawable.ic_thermometer)
                icon.setColorFilter(ContextCompat.getColor(this, R.color.sensor_temp_icon))
            }
            3 -> { // Estado cambiado
                icon.setImageResource(R.drawable.ic_leaf)
                icon.setColorFilter(ContextCompat.getColor(this, R.color.sensor_light_icon))
            }
            else -> {
                icon.setImageResource(R.drawable.ic_leaf)
                icon.setColorFilter(ContextCompat.getColor(this, R.color.text_gray))
            }
        }

        title.text = evento.tipo
        subtitle.text = "${evento.planta} - ${formatTimeAgo(evento.fecha)}"

        eventView.setOnClickListener {
            Toast.makeText(this, "${evento.tipo}: ${evento.descripcion}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTimeAgo(fecha: String): String {
        // Implementar lógica para mostrar "Hace X horas/días"
        // Por simplicidad, devolver la fecha tal cual
        return fecha
    }

    // Extension para convertir dp a px
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}