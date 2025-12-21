package com.example.proyectofinal6to_ecobox.fragment // Asegúrate de que el paquete sea correcto

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

// 1. Cambiamos de AppCompatActivity a Fragment
class HistoryFragment : Fragment(R.layout.activity_history) {

    // UI Components
    // Nota: Eliminamos btnBack porque en el menú principal no suele haber botón atrás
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

    // 2. Usamos onViewCreated en lugar de onCreate
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Validar sesión usando requireActivity()
        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        if (userId == -1L) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        initViews(view) // Pasamos la vista
        setupClickListeners(view)
        loadPlantasFamilia()
    }

    private fun initViews(view: View) {
        // Nota: Si tenías un btnBack en el XML, puedes ocultarlo aquí:
        // view.findViewById<View>(R.id.btnBack).visibility = View.GONE

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
        // Eliminamos el listener de btnBack

        btn24h.setOnClickListener { changePeriod(24, btn24h) }
        btn7d.setOnClickListener { changePeriod(168, btn7d) }
        btn30d.setOnClickListener { changePeriod(720, btn30d) }

        view.findViewById<TextView>(R.id.btnSeeAllEvents).setOnClickListener {
            val intent = Intent(requireContext(), AllEventsActivity::class.java)
            intent.putExtra("USER_ID", userId)
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
        val defaultColor = Color.parseColor("#9CA3AF")
        val selectedColor = Color.WHITE
        val transparent = Color.TRANSPARENT
        // Usamos requireContext() para obtener recursos
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_toggle_selected)

        listOf(btn24h, btn7d, btn30d).forEach { btn ->
            btn.background = null
            btn.setBackgroundColor(transparent)
            btn.setTextColor(defaultColor)
        }

        selectedView.background = selectedBg
        selectedView.setTextColor(selectedColor)
    }

    private fun loadPlantasFamilia() {
        Thread {
            try {
                plantasFamilia = PlantaDao.obtenerPlantasFamiliaConDatos(userId).toMutableList()
                // Usamos activity?.runOnUiThread para evitar crash si el fragmento se cierra
                activity?.runOnUiThread {
                    setupPlantChips()
                    loadHistoryData()
                }
            } catch (e: Exception) {
                Log.e("History", "Error cargando plantas", e)
            }
        }.start()
    }

    private fun setupPlantChips() {
        // Verificamos que el contexto exista antes de crear chips
        if (context == null) return

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
                selectedPlantId = -1
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                selectedPlantId = chip.tag as Long
            }
            loadHistoryData()
        }
    }

    private fun createChip(label: String, tagId: Long): Chip {
        // Usamos requireContext()
        val chip = Chip(requireContext())
        chip.text = label
        chip.tag = tagId
        chip.isCheckable = true

        chip.setChipBackgroundColorResource(R.color.selector_chip_background_color)
        chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_chip_text_color))
        chip.setChipStrokeColorResource(android.R.color.transparent)
        chip.chipStrokeWidth = 0f

        chip.isClickable = true
        chip.isCheckedIconVisible = false

        return chip
    }

    private fun loadHistoryData() {
        tvHumidityAvg.text = "..."

        Thread {
            try {
                val stats = PlantaDao.obtenerEstadisticasHistorialFamiliar(userId, selectedPeriod, selectedPlantId)
                val graficoData = PlantaDao.obtenerDatosHistoricosGraficoFamiliar(userId, selectedPeriod, selectedPlantId)
                val eventos = try {
                    PlantaDao.obtenerEventosRecientesFamiliar(userId, 3, selectedPlantId)
                } catch (e: Exception) { emptyList() }

                activity?.runOnUiThread {
                    // Verificamos isAdded para asegurar que el fragmento sigue vivo
                    if (!isAdded) return@runOnUiThread

                    tvHumidityAvg.text = String.format("%.0f%%", stats["humedad"] ?: 0f)
                    tvTempAvg.text = String.format("%.1f°C", stats["temperatura"] ?: 0f)
                    tvLightAvg.text = String.format("%.0f%%", stats["luz"] ?: 0f)

                    setupExpertChart(graficoData)
                    renderEvents(eventos)
                }

            } catch (e: Exception) {
                Log.e("History", "Error data", e)
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

        lineChart.animateX(1000)

        val sets = ArrayList<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()

        fun createSet(entries: List<DataPointDAO>?, label: String, colorHex: String, fillDrawableId: Int?): LineDataSet? {
            if (entries.isNullOrEmpty()) return null

            val entryList = entries.mapIndexed { index, point -> Entry(index.toFloat(), point.value) }
            val set = LineDataSet(entryList, label)

            val color = Color.parseColor(colorHex)
            set.color = color
            set.setCircleColor(color)
            set.lineWidth = 2.5f
            set.circleRadius = 0f
            set.setDrawValues(false)
            set.mode = LineDataSet.Mode.CUBIC_BEZIER

            if (fillDrawableId != null) {
                set.setDrawFilled(true)
                // Usamos requireContext()
                set.fillDrawable = ContextCompat.getDrawable(requireContext(), fillDrawableId)
            }
            return set
        }

        val setHum = createSet(data["humedad"], "Hum", "#2D5A40", R.drawable.gradient_chart)
        if (setHum != null) sets.add(setHum)

        val setTemp = createSet(data["temperatura"], "Temp", "#EF4444", null)
        if (setTemp != null) sets.add(setTemp)

        val setLuz = createSet(data["luz"], "Luz", "#F59E0B", null)
        if (setLuz != null) sets.add(setLuz)

        val lineData = LineData(sets)
        lineChart.data = lineData

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

        views.forEach { it.visibility = View.GONE }

        eventos.forEachIndexed { index, evento ->
            if (index < views.size) {
                val view = views[index]
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

        if (eventos.isEmpty()) {
            event1.visibility = View.VISIBLE
            event1.findViewById<TextView>(R.id.tvEventTitle).text = "Sin eventos recientes"
            event1.findViewById<TextView>(R.id.tvEventSubtitle).text = "Todo está tranquilo"
        }
    }

    private fun getRelativeTime(dateString: String): String {
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
            return dateString
        }
        return dateString
    }

}