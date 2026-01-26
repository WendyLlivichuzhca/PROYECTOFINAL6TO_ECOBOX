package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.MedicionHistorial
import com.example.proyectofinal6to_ecobox.data.network.PlantResponse
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.example.proyectofinal6to_ecobox.presentacion.ui.AllEventsActivity
import com.example.proyectofinal6to_ecobox.presentacion.ui.LoginActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * HistoryFragment - MIGRADO A API REST
 * 
 * Cambios principales:
 * - Eliminada dependencia de PlantaDao (JDBC)
 * - Usa RetrofitClient para llamadas API
 * - Usa coroutines en lugar de Thread
 * - Usa endpoint /api/plantas/{id}/historial/
 */
class HistoryFragment : Fragment(R.layout.fragment_history) {

    // UI Components
    private lateinit var lineChart: LineChart
    private lateinit var tvHumidityAvg: TextView
    private lateinit var tvTempAvg: TextView
    private lateinit var tvLightAvg: TextView

    // Period Selectors
    private lateinit var btn24h: TextView
    private lateinit var btn7d: TextView
    private lateinit var btn30d: TextView

    // Plant Selector
    private lateinit var chipGroupPlants: ChipGroup

    // Events Containers
    private lateinit var event1: View
    private lateinit var event2: View
    private lateinit var event3: View

    // Data Management
    private var selectedPeriod = 24
    private var selectedPlantId: Long = -1
    private var userId: Long = 1
    private var authToken: String? = null
    private var plantasFamilia = mutableListOf<PlantResponse>()

    // Bandera para controlar si ya se cargaron datos
    private var isInitialLoad = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HistoryFragment", "onViewCreated - USANDO API REST")

        // Validar sesión
        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)
        authToken = prefs.getString("auth_token", null)

        if (userId == -1L || authToken == null) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        initViews(view)
        setupClickListeners(view)

        // Seleccionar periodo inicial (24h)
        updatePeriodUI(btn24h)

        // Configurar observer para cuando el fragmento se vuelve visible
        setupVisibilityObserver()
    }

    private fun setupVisibilityObserver() {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        Log.d("HistoryFragment", "Fragment visible - ON_RESUME")
                        if (::lineChart.isInitialized) {
                            if (isInitialLoad) {
                                isInitialLoad = false
                                loadPlantasFamilia()
                            } else {
                                loadHistoryData()
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        lifecycle.addObserver(observer)
        viewLifecycleOwner.lifecycle.addObserver(observer)
    }

    private fun initViews(view: View) {
        view.findViewById<View>(R.id.btnBack)?.visibility = View.GONE

        lineChart = view.findViewById(R.id.lineChart)

        tvHumidityAvg = view.findViewById(R.id.tvHumidityAvg)
        tvTempAvg = view.findViewById(R.id.tvTempAvg)
        tvLightAvg = view.findViewById(R.id.tvLightAvg)

        btn24h = view.findViewById(R.id.btn24h)
        btn7d = view.findViewById(R.id.btn7d)
        btn30d = view.findViewById(R.id.btn30d)

        chipGroupPlants = view.findViewById(R.id.chipGroupPlants)

        event1 = view.findViewById(R.id.event1)
        event2 = view.findViewById(R.id.event2)
        event3 = view.findViewById(R.id.event3)
    }

    private fun setupClickListeners(view: View) {
        btn24h.setOnClickListener {
            if (selectedPeriod != 24) {
                selectedPeriod = 24
                updatePeriodUI(btn24h)
                loadHistoryData()
            }
        }
        btn7d.setOnClickListener {
            if (selectedPeriod != 168) {
                selectedPeriod = 168
                updatePeriodUI(btn7d)
                loadHistoryData()
            }
        }
        btn30d.setOnClickListener {
            if (selectedPeriod != 720) {
                selectedPeriod = 720
                updatePeriodUI(btn30d)
                loadHistoryData()
            }
        }

        view.findViewById<TextView>(R.id.btnSeeAllEvents).setOnClickListener {
            val intent = Intent(requireContext(), AllEventsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    private fun updatePeriodUI(selectedView: TextView) {
        val defaultColor = Color.parseColor("#9CA3AF")
        val selectedColor = Color.WHITE

        listOf(btn24h, btn7d, btn30d).forEach { btn ->
            btn.background = null
            btn.setBackgroundColor(Color.TRANSPARENT)
            btn.setTextColor(defaultColor)
        }

        selectedView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_toggle_selected)
        selectedView.setTextColor(selectedColor)
    }

    /**
     * Carga plantas de la familia usando API REST
     */
    private fun loadPlantasFamilia() {
        Log.d("HistoryFragment", "Cargando plantas familia - API REST")
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMyPlants("Token $authToken")
                if (response.isSuccessful && response.body() != null) {
                    plantasFamilia = response.body()!!.toMutableList()
                    setupPlantChips()
                    loadHistoryData()
                } else {
                    Log.e("HistoryFragment", "Error API: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Error cargando plantas", e)
            }
        }
    }

    private fun setupPlantChips() {
        chipGroupPlants.removeAllViews()

        val allChip = createChip("Todas", -1)
        allChip.isChecked = true
        chipGroupPlants.addView(allChip)

        plantasFamilia.forEach { plant ->
            val chip = createChip(plant.nombre, plant.id)
            chipGroupPlants.addView(chip)
        }

        chipGroupPlants.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) {
                group.check(allChip.id)
                return@setOnCheckedChangeListener
            }

            val chip = group.findViewById<Chip>(checkedId)
            val newPlantId = chip.tag as Long

            if (newPlantId != selectedPlantId) {
                selectedPlantId = newPlantId
                loadHistoryData()
            }
        }
    }

    private fun createChip(label: String, tagId: Long): Chip {
        return Chip(requireContext()).apply {
            text = label
            tag = tagId
            isCheckable = true
            id = View.generateViewId()

            setChipBackgroundColorResource(R.color.selector_chip_background_color)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_chip_text_color))
            setChipStrokeColorResource(android.R.color.transparent)
            chipStrokeWidth = 0f
            isClickable = true
            isCheckedIconVisible = false
        }
    }

    /**
     * Carga datos históricos usando API REST
     */
    private fun loadHistoryData() {
        Log.d("HistoryFragment", "Cargando datos históricos - API REST - periodo: $selectedPeriod, planta: $selectedPlantId")

        tvHumidityAvg.text = "..."
        tvTempAvg.text = "..."
        tvLightAvg.text = "..."

        if (selectedPlantId == -1L) {
            // Modo "Todas las plantas" - Usar datos agregados
            loadAggregatedHistory()
        } else {
            // Modo planta específica
            loadPlantHistory(selectedPlantId)
        }
    }

    /**
     * Carga historial agregado de todas las plantas
     */
    private fun loadAggregatedHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Cargar datos de todas las plantas y agregar
                var totalHumidity = 0f
                var totalTemp = 0f
                var totalLight = 0f
                var count = 0

                val allEntries = mutableMapOf<String, MutableList<Entry>>()
                allEntries["humedad"] = mutableListOf()
                allEntries["temperatura"] = mutableListOf()
                allEntries["luz"] = mutableListOf()

                for (plant in plantasFamilia) {
                    val response = RetrofitClient.instance.getPlantHistory("Token $authToken", plant.id)
                    if (response.isSuccessful && response.body() != null) {
                        val history = response.body()!!
                        
                        // ✅ Procesar estadísticas usando data classes estructuradas
                        val stats = history.estadisticas
                        totalHumidity += stats.humedad.promedio
                        totalTemp += stats.temperatura.promedio
                        
                        // Para luz, calcular desde mediciones
                        val luzPromedio = history.ultimasMediciones
                            .filter { it.tipo_sensor == "luz" }
                            .map { it.valor }
                            .average()
                            .toFloat()
                        if (!luzPromedio.isNaN()) {
                            totalLight += luzPromedio
                        }
                        count++
                    }
                }

                // Calcular promedios
                if (count > 0) {
                    tvHumidityAvg.text = String.format("%.0f%%", totalHumidity / count)
                    tvTempAvg.text = String.format("%.1f°C", totalTemp / count)
                    tvLightAvg.text = String.format("%.0f%%", totalLight / count)
                }

                // Mostrar gráfico con datos mock por ahora
                setupMockChart()
                renderMockEvents()

            } catch (e: Exception) {
                Log.e("HistoryFragment", "Error cargando historial agregado", e)
                setupMockChart()
            }
        }
    }

    /**
     * Carga historial de una planta específica usando API
     */
    private fun loadPlantHistory(plantId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPlantHistory("Token $authToken", plantId)
                
                if (response.isSuccessful && response.body() != null) {
                    val history = response.body()!!
                    
                    // ✅ Actualizar estadísticas usando las data classes estructuradas
                    val stats = history.estadisticas
                    tvHumidityAvg.text = String.format("%.0f%%", stats.humedad.promedio)
                    tvTempAvg.text = String.format("%.1f°C", stats.temperatura.promedio)
                    
                    // Para luz, buscar en las mediciones si no está en estadísticas
                    val luzPromedio = history.ultimasMediciones
                        .filter { it.tipo_sensor == "luz" }
                        .map { it.valor }
                        .average()
                        .toFloat()
                    
                    tvLightAvg.text = if (luzPromedio.isNaN()) "N/A" else String.format("%.0f%%", luzPromedio)

                    // Procesar mediciones para gráfico
                    val measurements = history.ultimasMediciones
                    setupChartFromAPI(measurements)

                    // Procesar eventos
                    val events = history.eventos
                    renderEventsFromAPI(events)

                } else {
                    Log.e("HistoryFragment", "Error API: ${response.code()}")
                    // Mostrar valores por defecto
                    tvHumidityAvg.text = "N/A"
                    tvTempAvg.text = "N/A"
                    tvLightAvg.text = "N/A"
                    setupMockChart()
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Error cargando historial de planta", e)
                // Mostrar valores por defecto
                tvHumidityAvg.text = "N/A"
                tvTempAvg.text = "N/A"
                tvLightAvg.text = "N/A"
                setupMockChart()
            }
        }
    }

    /**
     * Configura gráfico con datos de API
     */
    private fun setupChartFromAPI(measurements: List<MedicionHistorial>) {
        lineChart.clear()

        if (measurements.isEmpty()) {
            lineChart.setNoDataText("Sin datos para este periodo")
            lineChart.invalidate()
            return
        }

        // Configurar gráfico
        configureChart()

        val sets = mutableListOf<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()

        // Agrupar mediciones por tipo
        val humedadEntries = mutableListOf<Entry>()
        val tempEntries = mutableListOf<Entry>()
        val luzEntries = mutableListOf<Entry>()

        measurements.forEachIndexed { index, measurement ->
            val tipo = measurement.tipo_sensor
            val valor = measurement.valor

            when (tipo.lowercase()) {
                "humedad suelo", "humedad" -> humedadEntries.add(Entry(index.toFloat(), valor))
                "temperatura" -> tempEntries.add(Entry(index.toFloat(), valor))
                "luz" -> luzEntries.add(Entry(index.toFloat(), valor))
            }
        }

        // Crear datasets
        if (humedadEntries.isNotEmpty()) {
            val set = LineDataSet(humedadEntries, "Humedad").apply {
                color = Color.parseColor("#2D5A40")
                setCircleColor(Color.parseColor("#2D5A40"))
                lineWidth = 2.5f
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_chart)
            }
            sets.add(set)
        }

        if (tempEntries.isNotEmpty()) {
            val set = LineDataSet(tempEntries, "Temperatura").apply {
                color = Color.parseColor("#EF4444")
                setCircleColor(Color.parseColor("#EF4444"))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            sets.add(set)
        }

        if (luzEntries.isNotEmpty()) {
            val set = LineDataSet(luzEntries, "Luz").apply {
                color = Color.parseColor("#F59E0B")
                setCircleColor(Color.parseColor("#F59E0B"))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            sets.add(set)
        }

        if (sets.isNotEmpty()) {
            lineChart.data = LineData(sets)
            lineChart.animateX(1000)
        }

        lineChart.invalidate()
    }

    /**
     * Configura el gráfico con mock data temporal
     */
    private fun setupMockChart() {
        lineChart.clear()
        configureChart()

        val entries = mutableListOf<Entry>()
        for (i in 0..7) {
            entries.add(Entry(i.toFloat(), 55f + (Math.random() * 20).toFloat()))
        }

        val set = LineDataSet(entries, "Humedad").apply {
            color = Color.parseColor("#2D5A40")
            setCircleColor(Color.parseColor("#2D5A40"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_chart)
        }

        lineChart.data = LineData(set)
        lineChart.animateX(1000)
        lineChart.invalidate()
    }

    /**
     * Configuración común del gráfico
     */
    private fun configureChart() {
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.xAxis.textColor = Color.parseColor("#9CA3AF")
        lineChart.axisLeft.textColor = Color.parseColor("#9CA3AF")
        lineChart.axisLeft.setDrawGridLines(true)
        lineChart.axisLeft.gridColor = Color.parseColor("#F3F4F6")
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
    }

    /**
     * Renderiza eventos desde API
     */
    private fun renderEventsFromAPI(eventos: List<com.example.proyectofinal6to_ecobox.data.network.PlantEventResponse>) {
        val eventViews = listOf(event1, event2, event3)
        eventViews.forEach { it.visibility = View.GONE }

        if (eventos.isEmpty()) {
            renderMockEvents()
            return
        }

        eventos.take(3).forEachIndexed { index, evento ->
            val view = eventViews[index]
            view.visibility = View.VISIBLE

            val icon = view.findViewById<ImageView>(R.id.imgEventIcon)
            val title = view.findViewById<TextView>(R.id.tvEventTitle)
            val subtitle = view.findViewById<TextView>(R.id.tvEventSubtitle)

            title.text = evento.tipo
            subtitle.text = "${getRelativeTime(evento.fecha)}"

            when (evento.tipo.lowercase()) {
                "riego" -> {
                    icon.setImageResource(R.drawable.ic_water_drop)
                    icon.setColorFilter(Color.parseColor("#3B82F6"))
                }
                "temperatura" -> {
                    icon.setImageResource(R.drawable.ic_thermometer)
                    icon.setColorFilter(Color.parseColor("#EF4444"))
                }
                else -> {
                    icon.setImageResource(R.drawable.ic_leaf)
                    icon.setColorFilter(Color.parseColor("#10B981"))
                }
            }
        }
    }

    /**
     * Renderiza eventos mock
     */
    private fun renderMockEvents() {
        event1.visibility = View.VISIBLE
        event1.findViewById<TextView>(R.id.tvEventTitle).text = "Sin eventos recientes"
        event1.findViewById<TextView>(R.id.tvEventSubtitle).text = "Todo está tranquilo"
        event1.findViewById<ImageView>(R.id.imgEventIcon).setImageResource(R.drawable.ic_leaf)
        event1.findViewById<ImageView>(R.id.imgEventIcon).setColorFilter(Color.parseColor("#9CA3AF"))
    }

    private fun getRelativeTime(dateString: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = format.parse(dateString)
            if (date != null) {
                DateUtils.getRelativeTimeSpanString(
                    date.time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HistoryFragment", "onDestroyView")
    }
}