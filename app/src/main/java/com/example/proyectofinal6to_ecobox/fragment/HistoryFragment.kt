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
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao.DataPointDAO
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
import java.text.SimpleDateFormat
import java.util.Locale

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
    private var plantasFamilia = mutableListOf<PlantaDao.PlantaConDatos>()

    // Bandera para controlar si ya se cargaron datos
    private var isInitialLoad = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HistoryFragment", "onViewCreated")

        // Validar sesión
        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        if (userId == -1L) {
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
        // Observer para cuando el fragmento se vuelve visible
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        Log.d("HistoryFragment", "Fragment visible - ON_RESUME")
                        // Siempre recargar datos cuando el fragmento se vuelve visible
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

        // Agregar observer al lifecycle del fragmento
        lifecycle.addObserver(observer)

        // También agregar observer al viewLifecycleOwner para cuando la vista se recrea
        viewLifecycleOwner.lifecycle.addObserver(observer)
    }

    private fun initViews(view: View) {
        // Ocultar botón atrás si existe
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

        // Restablecer todos
        listOf(btn24h, btn7d, btn30d).forEach { btn ->
            btn.background = null
            btn.setBackgroundColor(Color.TRANSPARENT)
            btn.setTextColor(defaultColor)
        }

        // Establecer seleccionado
        selectedView.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_toggle_selected)
        selectedView.setTextColor(selectedColor)
    }

    private fun loadPlantasFamilia() {
        Log.d("HistoryFragment", "Cargando plantas familia")
        Thread {
            try {
                val plantas = PlantaDao.obtenerPlantasFamiliaConDatos(userId)
                requireActivity().runOnUiThread {
                    plantasFamilia = plantas.toMutableList()
                    setupPlantChips()
                    loadHistoryData()
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Error cargando plantas", e)
            }
        }.start()
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
                group.check(allChip.id) // Mantener "Todas" seleccionada
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
            id = View.generateViewId() // Generar ID único

            // Configurar estilo
            setChipBackgroundColorResource(R.color.selector_chip_background_color)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_chip_text_color))
            setChipStrokeColorResource(android.R.color.transparent)
            chipStrokeWidth = 0f
            isClickable = true
            isCheckedIconVisible = false
        }
    }

    private fun loadHistoryData() {
        Log.d("HistoryFragment", "Cargando datos históricos - periodo: $selectedPeriod, planta: $selectedPlantId")

        tvHumidityAvg.text = "..."
        tvTempAvg.text = "..."
        tvLightAvg.text = "..."

        Thread {
            try {
                val stats = PlantaDao.obtenerEstadisticasHistorialFamiliar(
                    userId, selectedPeriod, selectedPlantId
                )
                val graficoData = PlantaDao.obtenerDatosHistoricosGraficoFamiliar(
                    userId, selectedPeriod, selectedPlantId
                )
                val eventos = try {
                    PlantaDao.obtenerEventosRecientesFamiliar(userId, 3, selectedPlantId)
                } catch (e: Exception) {
                    emptyList()
                }

                requireActivity().runOnUiThread {
                    if (!isAdded) return@runOnUiThread

                    tvHumidityAvg.text = String.format("%.0f%%", stats["humedad"] ?: 0f)
                    tvTempAvg.text = String.format("%.1f°C", stats["temperatura"] ?: 0f)
                    tvLightAvg.text = String.format("%.0f%%", stats["luz"] ?: 0f)

                    setupChart(graficoData)
                    renderEvents(eventos)
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Error cargando datos históricos", e)
            }
        }.start()
    }

    private fun setupChart(data: Map<String, List<DataPointDAO>>) {
        lineChart.clear()
        lineChart.setNoDataText("Sin datos para este periodo")
        lineChart.setNoDataTextColor(Color.GRAY)

        if (data.isEmpty() || data["humedad"].isNullOrEmpty()) {
            lineChart.invalidate()
            return
        }

        // Configurar gráfico
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

        // Crear datasets
        val sets = mutableListOf<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()

        // Dataset humedad (con relleno)
        data["humedad"]?.let { humidityData ->
            if (humidityData.isNotEmpty()) {
                val entries = humidityData.mapIndexed { index, point ->
                    Entry(index.toFloat(), point.value)
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
                sets.add(set)
            }
        }

        // Dataset temperatura
        data["temperatura"]?.let { tempData ->
            if (tempData.isNotEmpty()) {
                val entries = tempData.mapIndexed { index, point ->
                    Entry(index.toFloat(), point.value)
                }
                val set = LineDataSet(entries, "Temperatura").apply {
                    color = Color.parseColor("#EF4444")
                    setCircleColor(Color.parseColor("#EF4444"))
                    lineWidth = 2f
                    circleRadius = 4f
                    setDrawValues(false)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
                sets.add(set)
            }
        }

        // Dataset luz
        data["luz"]?.let { lightData ->
            if (lightData.isNotEmpty()) {
                val entries = lightData.mapIndexed { index, point ->
                    Entry(index.toFloat(), point.value)
                }
                val set = LineDataSet(entries, "Luz").apply {
                    color = Color.parseColor("#F59E0B")
                    setCircleColor(Color.parseColor("#F59E0B"))
                    lineWidth = 2f
                    circleRadius = 4f
                    setDrawValues(false)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }
                sets.add(set)
            }
        }

        if (sets.isNotEmpty()) {
            val lineData = LineData(sets)
            lineChart.data = lineData

            // Configurar eje X con etiquetas
            val labels = data["humedad"]?.map { it.label } ?: emptyList()
            lineChart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in labels.indices) labels[index] else ""
                }
            }

            lineChart.animateX(1000)
        }

        lineChart.invalidate()
    }

    private fun renderEvents(eventos: List<PlantaDao.EventoDAO>) {
        val eventViews = listOf(event1, event2, event3)

        // Ocultar todas primero
        eventViews.forEach { it.visibility = View.GONE }

        if (eventos.isEmpty()) {
            event1.visibility = View.VISIBLE
            event1.findViewById<TextView>(R.id.tvEventTitle).text = "Sin eventos recientes"
            event1.findViewById<TextView>(R.id.tvEventSubtitle).text = "Todo está tranquilo"
            event1.findViewById<ImageView>(R.id.imgEventIcon).setImageResource(R.drawable.ic_leaf)
            event1.findViewById<ImageView>(R.id.imgEventIcon).setColorFilter(Color.parseColor("#9CA3AF"))
            return
        }

        eventos.forEachIndexed { index, evento ->
            if (index < eventViews.size) {
                val view = eventViews[index]
                view.visibility = View.VISIBLE

                val icon = view.findViewById<ImageView>(R.id.imgEventIcon)
                val title = view.findViewById<TextView>(R.id.tvEventTitle)
                val subtitle = view.findViewById<TextView>(R.id.tvEventSubtitle)

                title.text = evento.tipo
                subtitle.text = "${evento.planta} • ${getRelativeTime(evento.fecha)}"

                when (evento.iconoTipo) {
                    1 -> {
                        icon.setImageResource(R.drawable.ic_water_drop)
                        icon.setColorFilter(Color.parseColor("#3B82F6"))
                    }
                    2 -> {
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