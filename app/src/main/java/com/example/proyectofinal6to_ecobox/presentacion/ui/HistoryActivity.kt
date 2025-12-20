package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao.DataPointDAO
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    // UI Components
    private lateinit var btnBack: ImageButton
    private lateinit var lineChart: LineChart
    private lateinit var tvHumidityAvg: TextView
    private lateinit var tvTempAvg: TextView
    private lateinit var tvLightAvg: TextView

    // Period Selectors
    private lateinit var btn24h: TextView
    private lateinit var btn7d: TextView
    private lateinit var btn30d: TextView

    // Plant Selector (NUEVO: ChipGroup)
    private lateinit var chipGroupPlants: ChipGroup

    // Events Containers (Includes)
    private lateinit var event1: View
    private lateinit var event2: View
    private lateinit var event3: View

    // Data Management
    private var selectedPeriod = 24
    private var selectedPlantId: Long = -1 // -1 = Todas
    private var userId: Long = 1
    private var plantasFamilia = mutableListOf<PlantaDao.PlantaConDatos>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Configuración de Status Bar Transparente (Opcional, si no lo hiciste en themes)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

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
        btnBack = findViewById(R.id.btnBack)
        lineChart = findViewById(R.id.lineChart)

        tvHumidityAvg = findViewById(R.id.tvHumidityAvg)
        tvTempAvg = findViewById(R.id.tvTempAvg)
        tvLightAvg = findViewById(R.id.tvLightAvg)

        btn24h = findViewById(R.id.btn24h)
        btn7d = findViewById(R.id.btn7d)
        btn30d = findViewById(R.id.btn30d)

        // CAMBIO: Usamos el ChipGroup del XML
        chipGroupPlants = findViewById(R.id.chipGroupPlants)

        // Referencias a los layouts incluidos (item_event)
        event1 = findViewById(R.id.event1)
        event2 = findViewById(R.id.event2)
        event3 = findViewById(R.id.event3)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btn24h.setOnClickListener { changePeriod(24, btn24h) }
        btn7d.setOnClickListener { changePeriod(168, btn7d) }   // 7 días
        btn30d.setOnClickListener { changePeriod(720, btn30d) } // 30 días

        // Listener para el botón "Ver todo" eventos
        findViewById<TextView>(R.id.btnSeeAllEvents).setOnClickListener {
            val intent = Intent(this, AllEventsActivity::class.java)
            intent.putExtra("USER_ID", userId) // Pasamos el ID para cargar los datos allá
            startActivity(intent)
        }
    }

    private fun changePeriod(periodHours: Int, selectedView: TextView) {
        if (selectedPeriod == periodHours) return

        selectedPeriod = periodHours
        updatePeriodUI(selectedView)
        loadHistoryData()
    }

    private fun updatePeriodUI(selectedView: TextView) {
        // Reset styles
        val defaultColor = Color.parseColor("#9CA3AF") // Gris
        val selectedColor = Color.WHITE
        val transparent = Color.TRANSPARENT
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_toggle_selected)

        listOf(btn24h, btn7d, btn30d).forEach { btn ->
            btn.background = null // O transparente
            btn.setBackgroundColor(transparent)
            btn.setTextColor(defaultColor)
        }

        // Set active
        selectedView.background = selectedBg
        selectedView.setTextColor(selectedColor)
    }

    private fun loadPlantasFamilia() {
        Thread {
            try {
                plantasFamilia = PlantaDao.obtenerPlantasFamiliaConDatos(userId).toMutableList()
                runOnUiThread {
                    setupPlantChips()
                    loadHistoryData() // Cargar datos iniciales
                }
            } catch (e: Exception) {
                Log.e("History", "Error cargando plantas", e)
            }
        }.start()
    }

    private fun setupPlantChips() {
        chipGroupPlants.removeAllViews()

        // 1. Chip "Todas" (Manual)
        val allChip = createChip("Todas", -1)
        allChip.isChecked = true
        chipGroupPlants.addView(allChip)

        // 2. Chips dinámicos
        plantasFamilia.forEach { plant ->
            val chip = createChip(plant.nombre, plant.id)
            chipGroupPlants.addView(chip)
        }

        // Listener de selección
        chipGroupPlants.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) {
                // Evitar deselección total -> volver a seleccionar "Todas"
                // Nota: Esto puede requerir buscar el chip por ID,
                // por simplicidad si no hay selección, asumimos -1
                selectedPlantId = -1
                // Opcional: forzar check visual en "Todas"
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                selectedPlantId = chip.tag as Long
            }
            loadHistoryData()
        }
    }

    private fun createChip(label: String, tagId: Long): Chip {
        val chip = Chip(this)
        chip.text = label
        chip.tag = tagId
        chip.isCheckable = true

        // Estilo visual del Chip (Programático para coincidir con XML)
        chip.setChipBackgroundColorResource(R.color.selector_chip_background_color)
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.selector_chip_text_color))
        chip.setChipStrokeColorResource(android.R.color.transparent)
        chip.chipStrokeWidth = 0f

        // Estilo Choice (Radio Button behavior)
        chip.isClickable = true
        chip.isCheckedIconVisible = false

        return chip
    }

    private fun loadHistoryData() {
        // Estado de carga simple
        tvHumidityAvg.text = "..."

        Thread {
            try {
                // 1. Estadísticas
                val stats = PlantaDao.obtenerEstadisticasHistorialFamiliar(userId, selectedPeriod, selectedPlantId)

                // 2. Gráfico
                val graficoData = PlantaDao.obtenerDatosHistoricosGraficoFamiliar(userId, selectedPeriod, selectedPlantId)

                // 3. Eventos
                val eventos = try {
                    PlantaDao.obtenerEventosRecientesFamiliar(userId, 3, selectedPlantId)
                } catch (e: Exception) { emptyList() }

                runOnUiThread {
                    // Actualizar UI Cards
                    tvHumidityAvg.text = String.format("%.0f%%", stats["humedad"] ?: 0f)
                    tvTempAvg.text = String.format("%.1f°C", stats["temperatura"] ?: 0f)
                    tvLightAvg.text = String.format("%.0f%%", stats["luz"] ?: 0f)

                    // Actualizar Gráfico
                    setupExpertChart(graficoData)

                    // Actualizar Lista Eventos
                    renderEvents(eventos)
                }

            } catch (e: Exception) {
                Log.e("History", "Error data", e)
                runOnUiThread {
                    lineChart.setNoDataText("Error al cargar datos")
                }
            }
        }.start()
    }

    private fun setupExpertChart(data: Map<String, List<DataPointDAO>>) {
        lineChart.clear()

        if (data.isEmpty() || data["humedad"].isNullOrEmpty()) {
            lineChart.setNoDataText("Sin datos para este periodo")
            lineChart.setNoDataTextColor(Color.GRAY)
            return
        }

        // --- Configuración General (Look Limpio) ---
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false // Ya tenemos leyenda personalizada en el XML
        lineChart.setDrawGridBackground(false)
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false) // Sin líneas verticales
        lineChart.xAxis.textColor = Color.parseColor("#9CA3AF")
        lineChart.axisLeft.textColor = Color.parseColor("#9CA3AF")
        lineChart.axisLeft.setDrawGridLines(true) // Líneas horizontales sutiles
        lineChart.axisLeft.gridColor = Color.parseColor("#F3F4F6")

        // Animación suave
        lineChart.animateX(1000)

        // --- Datasets ---
        val sets = ArrayList<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()

        // Helper para crear líneas bonitas
        fun createSet(entries: List<DataPointDAO>?, label: String, colorHex: String, fillDrawableId: Int?): LineDataSet? {
            if (entries.isNullOrEmpty()) return null

            val entryList = entries.mapIndexed { index, point -> Entry(index.toFloat(), point.value) }
            val set = LineDataSet(entryList, label)

            val color = Color.parseColor(colorHex)
            set.color = color
            set.setCircleColor(color)
            set.lineWidth = 2.5f
            set.circleRadius = 0f // Sin puntos en la línea (más limpio)
            set.setDrawValues(false) // Sin números sobre la línea
            set.mode = LineDataSet.Mode.CUBIC_BEZIER // Línea curva suave

            // Efecto de Relleno (Gradient)
            if (fillDrawableId != null) {
                set.setDrawFilled(true)
                set.fillDrawable = ContextCompat.getDrawable(this, fillDrawableId)
            }
            return set
        }

        // Añadir Humedad (Verde con degradado)
        // Nota: Si no creaste el drawable gradient_chart, usa fillColor = Color.GREEN
        val setHum = createSet(data["humedad"], "Hum", "#2D5A40", R.drawable.gradient_chart)
        if (setHum != null) sets.add(setHum)

        // Añadir Temp (Rojo, solo línea)
        val setTemp = createSet(data["temperatura"], "Temp", "#EF4444", null)
        if (setTemp != null) sets.add(setTemp)

        // Añadir Luz (Amarillo, solo línea)
        val setLuz = createSet(data["luz"], "Luz", "#F59E0B", null)
        if (setLuz != null) sets.add(setLuz)

        val lineData = LineData(sets)
        lineChart.data = lineData

        // Formateador eje X (Fechas/Horas)
        val labels = data["humedad"]?.map { it.label } ?: emptyList()
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in labels.indices) labels[index] else ""
            }
        }

        lineChart.invalidate()
    }

    private fun renderEvents(eventos: List<PlantaDao.EventoDAO>) {
        val views = listOf(event1, event2, event3)

        // Ocultar todos primero
        views.forEach {
            it.visibility = View.GONE
            // Separador (si existe en el padre, pero aquí manejamos el include)
        }

        eventos.forEachIndexed { index, evento ->
            if (index < views.size) {
                val view = views[index]
                view.visibility = View.VISIBLE

                val icon = view.findViewById<ImageView>(R.id.imgEventIcon)
                val title = view.findViewById<TextView>(R.id.tvEventTitle)
                val subtitle = view.findViewById<TextView>(R.id.tvEventSubtitle)

                title.text = evento.tipo
                subtitle.text = "${evento.planta} • ${getRelativeTime(evento.fecha)}"

                // Icono según tipo
                when (evento.iconoTipo) {
                    1 -> { // Riego
                        icon.setImageResource(R.drawable.ic_water_drop)
                        icon.setColorFilter(Color.parseColor("#3B82F6")) // Azul
                    }
                    2 -> { // Temp/Alerta
                        icon.setImageResource(R.drawable.ic_thermometer)
                        icon.setColorFilter(Color.parseColor("#EF4444")) // Rojo
                    }
                    else -> { // General
                        icon.setImageResource(R.drawable.ic_leaf) // Asegúrate de tener este icono
                        icon.setColorFilter(Color.parseColor("#10B981")) // Verde
                    }
                }
            }
        }

        if (eventos.isEmpty()) {
            // Manejo opcional de "Sin eventos"
            event1.visibility = View.VISIBLE
            event1.findViewById<TextView>(R.id.tvEventTitle).text = "Sin eventos recientes"
            event1.findViewById<TextView>(R.id.tvEventSubtitle).text = "Todo está tranquilo"
        }
    }

    private fun getRelativeTime(dateString: String): String {
        // Asumiendo formato de fecha ISO o similar "yyyy-MM-dd HH:mm:ss"
        // Ajusta el patrón según cómo guardes las fechas en tu BD
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        try {
            val date = format.parse(dateString)
            if (date != null) {
                return DateUtils.getRelativeTimeSpanString(
                    date.time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            }
        } catch (e: Exception) {
            return dateString // Fallback si falla el parseo
        }
        return dateString
    }
}