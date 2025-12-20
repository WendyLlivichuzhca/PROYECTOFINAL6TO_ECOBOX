package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantDetailActivity : AppCompatActivity() {

    // Vistas
    private lateinit var ivPlant: ImageView
    private lateinit var tvPlantName: TextView
    private lateinit var tvLocation: TextView

    // Valores numéricos
    private lateinit var tvHumidityValue: TextView
    private lateinit var tvLightValue: TextView
    private lateinit var tvTempValue: TextView

    // Estado y Agua
    private lateinit var tvStatusText: TextView
    private lateinit var tvWaterLevelPercent: TextView
    private lateinit var progressWater: ProgressBar

    // Controles
    private lateinit var switchWaterAuto: MaterialSwitch
    private lateinit var switchLightAuto: MaterialSwitch
    private lateinit var btnWaterNow: Button

    // Cards (Para cambiar colores de fondo)
    private lateinit var cardHumidity: CardView
    private lateinit var cardLight: CardView
    private lateinit var cardTemp: CardView
    private lateinit var cardStatus: CardView

    // Gráfico
    private lateinit var chartHistory: LineChart
    private var historialTitle: TextView? = null

    // Datos
    private var planta: Planta? = null
    private var plantaId: Long = -1
    private var userId: Long = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setupToolbar()
        initViews()
        setupClickListeners()
        loadPlantData()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun initViews() {
        ivPlant = findViewById(R.id.ivPlant)
        tvPlantName = findViewById(R.id.tvPlantName)
        tvLocation = findViewById(R.id.tvLocation)

        tvHumidityValue = findViewById(R.id.tvHumidityValue)
        tvLightValue = findViewById(R.id.tvLightValue)
        tvTempValue = findViewById(R.id.tvTempValue)

        tvStatusText = findViewById(R.id.tvStatusText)
        tvWaterLevelPercent = findViewById(R.id.tvWaterLevelPercent)
        progressWater = findViewById(R.id.progressWater)

        switchWaterAuto = findViewById(R.id.switchWaterAuto)
        switchLightAuto = findViewById(R.id.switchLightAuto)
        btnWaterNow = findViewById(R.id.btnWaterNow)

        cardHumidity = findViewById(R.id.cardHumidity)
        cardLight = findViewById(R.id.cardLight)
        cardTemp = findViewById(R.id.cardTemp)
        cardStatus = findViewById(R.id.cardStatus)

        chartHistory = findViewById(R.id.chartHistory)
        historialTitle = findViewById(R.id.tvHistoryTitle)
    }

    private fun setupClickListeners() {
        btnWaterNow.setOnClickListener {
            waterPlantNow()
        }

        switchWaterAuto.setOnCheckedChangeListener { _, isChecked ->
            updateAutoWaterSetting(isChecked)
        }

        switchLightAuto.setOnCheckedChangeListener { _, isChecked ->
            updateAutoLightSetting(isChecked)
        }

        historialTitle?.setOnClickListener {
            showHistoryPeriodSelector()
        }
    }

    // ================== MENÚ SUPERIOR ==================
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_plant_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ================== CARGA DE DATOS ==================

    private fun loadPlantData() {
        plantaId = intent.getLongExtra("PLANT_ID", -1)
        val plantaNombre = intent.getStringExtra("PLANT_NAME") ?: "Planta"
        val ubicacion = intent.getStringExtra("PLANT_LOCATION") ?: "Mi Jardín"
        val humedad = intent.getFloatExtra("PLANT_HUMIDITY", 0f)
        val temperatura = intent.getFloatExtra("PLANT_TEMP", 0f)
        val luz = intent.getFloatExtra("PLANT_LIGHT", 0f)
        val estado = intent.getStringExtra("PLANT_STATUS") ?: "healthy"

        // Crear objeto Planta temporal
        planta = Planta().apply {
            id = plantaId
            nombre = plantaNombre
            this.ubicacion = ubicacion
        }

        updateUI(humedad, temperatura, luz, estado, ubicacion, plantaNombre)
        loadAdditionalData()
        loadHistoryData(24)
    }

    private fun updateUI(
        humedad: Float,
        temperatura: Float,
        luz: Float,
        estado: String,
        ubicacion: String,
        nombre: String
    ) {
        tvPlantName.text = nombre
        tvLocation.text = "📍 $ubicacion"

        // Valores numéricos (Texto grande)
        tvHumidityValue.text = "${humedad.toInt()}%"
        tvLightValue.text = "${luz.toInt()} lux"
        tvTempValue.text = "${temperatura.toInt()}°C"

        // Colores de las tarjetas
        updateCardColor(cardHumidity, humedad, "humidity")
        updateCardColor(cardLight, luz, "light")
        updateCardColor(cardTemp, temperatura, "temperature")

        // Lógica de estado General
        val estadoInfo = when (estado) {
            "healthy", "Saludable", "Excelente" -> Triple("🌿", "Sana", R.color.status_healthy_dark)
            "warning", "Advertencia" -> Triple("⚠️", "Atención", R.color.status_warning_dark)
            "critical", "Crítico" -> Triple("🥀", "Crítico", R.color.status_critical_dark)
            else -> Triple("🤔", "Desconocido", R.color.text_gray)
        }

        tvStatusText.text = "${estadoInfo.first} ${estadoInfo.second}"
        tvStatusText.setTextColor(ContextCompat.getColor(this, estadoInfo.third))

        // Nivel de agua (Barra de progreso)
        val nivelAgua = calculateWaterLevel(humedad)
        tvWaterLevelPercent.text = "$nivelAgua%"
        progressWater.progress = nivelAgua
    }

    private fun updateCardColor(card: CardView, value: Float, type: String) {
        val isWarning = when (type) {
            "humidity" -> value < 30f
            "temperature" -> value < 10f || value > 35f
            else -> false
        }

        if (isWarning) {
            card.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
        } else {
            card.setCardBackgroundColor(Color.WHITE)
        }
    }

    // --- AQUÍ ESTABA EL PROBLEMA ---
    // He quitado el multiplicador * 1.2 para que sea el valor real
    private fun calculateWaterLevel(humedad: Float): Int {
        return humedad.toInt().coerceIn(0, 100)
    }

    // ================== DATOS SIMULADOS Y HILOS ==================

    private fun loadAdditionalData() {
        Thread {
            try {
                Thread.sleep(500)
                loadAutoSettings()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun loadAutoSettings() {
        runOnUiThread {
            switchWaterAuto.isChecked = false
            switchLightAuto.isChecked = true
        }
    }

    // ================== LÓGICA DEL GRÁFICO (CHART) ==================

    private fun loadHistoryData(hours: Int) {
        val mockData = generateMockHistoryData(hours)
        setupHistoryChart(mockData, hours)
    }

    private fun generateMockHistoryData(hours: Int): Map<String, List<Pair<String, Float>>> {
        val humidityData = mutableListOf<Pair<String, Float>>()
        val now = System.currentTimeMillis()

        val steps = 10
        for (i in steps downTo 0) {
            val time = now - (i * (hours * 3600000L / steps))
            val date = Date(time)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())

            val value = 60f + (Math.random() * 15).toFloat()
            humidityData.add(Pair(format.format(date), value))
        }

        return mapOf("humedad" to humidityData)
    }

    private fun setupHistoryChart(historial: Map<String, List<Pair<String, Float>>>, hours: Int) {
        chartHistory.clear()
        chartHistory.description.isEnabled = false
        chartHistory.axisRight.isEnabled = false
        chartHistory.setDrawGridBackground(false)
        chartHistory.setTouchEnabled(true)

        val xAxis = chartHistory.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.parseColor("#9CA3AF")

        val leftAxis = chartHistory.axisLeft
        leftAxis.textColor = Color.parseColor("#9CA3AF")
        leftAxis.gridColor = Color.parseColor("#F3F4F6")

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        historial["humedad"]?.forEachIndexed { index, pair ->
            entries.add(Entry(index.toFloat(), pair.second))
            labels.add(pair.first)
        }

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < labels.size) labels[index] else ""
            }
        }

        val dataSet = LineDataSet(entries, "Humedad del Suelo")
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

        chartHistory.data = LineData(dataSet)
        chartHistory.animateX(1000)

        historialTitle?.text = "Monitor (${hours}h)"
    }

    private fun showHistoryPeriodSelector() {
        val periods = arrayOf("24 Horas", "7 Días", "30 Días")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Periodo del historial")
            .setItems(periods) { _, which ->
                val hours = when (which) {
                    0 -> 24
                    1 -> 168
                    else -> 720
                }
                loadHistoryData(hours)
            }
            .show()
    }

    // ================== ACCIONES DE USUARIO ==================

    private fun waterPlantNow() {
        Toast.makeText(this, "🌊 Iniciando riego...", Toast.LENGTH_SHORT).show()
        btnWaterNow.isEnabled = false
        btnWaterNow.text = "Regando..."

        btnWaterNow.postDelayed({
            btnWaterNow.isEnabled = true
            btnWaterNow.text = "Regar Ahora"
            Toast.makeText(this, "✅ Riego completado", Toast.LENGTH_SHORT).show()

            progressWater.progress = 100
            tvWaterLevelPercent.text = "100%"
            tvHumidityValue.text = "95%"
        }, 2000)
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Planta")
            .setMessage("¿Estás seguro? Perderás todo el historial de esta planta.")
            .setPositiveButton("Eliminar") { _, _ -> deletePlant() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletePlant() {
        Toast.makeText(this, "Planta eliminada", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateAutoWaterSetting(enabled: Boolean) { /* Guardar en DB */ }

    private fun updateAutoLightSetting(enabled: Boolean) { /* Guardar en DB */ }

    // =========================================================================
    //  COMPANION OBJECT (CORRECTO PARA RECIBIR DATOS CRUDOS)
    // =========================================================================
    companion object {
        fun createIntent(
            context: Context,
            planta: Planta,
            datos: Map<String, Any>
        ): Intent {
            val intent = Intent(context, PlantDetailActivity::class.java)
            intent.putExtra("PLANT_ID", planta.id)
            intent.putExtra("PLANT_NAME", planta.nombre)

            intent.putExtra("PLANT_LOCATION", datos["ubicacion"] as? String ?: "")
            intent.putExtra("PLANT_HUMIDITY", (datos["humedad"] as? Number)?.toFloat() ?: 0f)
            intent.putExtra("PLANT_TEMP", (datos["temperatura"] as? Number)?.toFloat() ?: 0f)
            intent.putExtra("PLANT_LIGHT", (datos["luz"] as? Number)?.toFloat() ?: 0f)
            intent.putExtra("PLANT_STATUS", datos["estado"] as? String ?: "healthy")

            return intent
        }
    }
}