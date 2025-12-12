package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantDetailActivity : AppCompatActivity() {

    private lateinit var ivPlant: ImageView
    private lateinit var btnBack: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var tvPlantName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvHumidityValue: TextView
    private lateinit var tvHumidityStatus: TextView
    private lateinit var tvLightValue: TextView
    private lateinit var tvLightStatus: TextView
    private lateinit var tvTempValue: TextView
    private lateinit var tvTempStatus: TextView
    private lateinit var tvStatusText: TextView
    private lateinit var tvWaterLevelPercent: TextView
    private lateinit var progressWater: ProgressBar
    private lateinit var switchWaterAuto: SwitchCompat
    private lateinit var switchLightAuto: SwitchCompat
    private lateinit var btnWaterNow: Button
    private lateinit var cardHumidity: CardView
    private lateinit var cardLight: CardView
    private lateinit var cardTemp: CardView
    private lateinit var cardStatus: CardView
    private lateinit var chartHistory: LineChart

    private var planta: Planta? = null
    private var plantaId: Long = -1
    private var historialTitle: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)

        initViews()
        setupClickListeners()
        loadPlantData()
    }

    private fun initViews() {
        ivPlant = findViewById(R.id.ivPlant)
        btnBack = findViewById(R.id.btnBack)
        btnDelete = findViewById(R.id.btnDelete)
        tvPlantName = findViewById(R.id.tvPlantName)
        tvLocation = findViewById(R.id.tvLocation)
        tvHumidityValue = findViewById(R.id.tvHumidityValue)
        tvHumidityStatus = findViewById(R.id.tvHumidityStatus)
        tvLightValue = findViewById(R.id.tvLightValue)
        tvLightStatus = findViewById(R.id.tvLightStatus)
        tvTempValue = findViewById(R.id.tvTempValue)
        tvTempStatus = findViewById(R.id.tvTempStatus)
        tvStatusText = findViewById(R.id.tvStatusText)
        tvWaterLevelPercent = findViewById(R.id.tvWaterLevelPercent)
        progressWater = findViewById(R.id.progressWater)
        switchWaterAuto = findViewById(R.id.switchWaterAuto)
        switchLightAuto = findViewById(R.id.switchLightAuto)
        btnWaterNow = findViewById(R.id.btnWaterNow)
        chartHistory = findViewById(R.id.chartHistory)

        // Cards para cambiar colores
        cardHumidity = findViewById(R.id.cardHumidity)
        cardLight = findViewById(R.id.cardLight)
        cardTemp = findViewById(R.id.cardTemp)
        cardStatus = findViewById(R.id.cardStatus)

        // Buscar el título del historial (asegúrate de tener un TextView con id tvHistoryTitle en tu layout)
        historialTitle = findViewById(R.id.tvHistoryTitle)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        btnWaterNow.setOnClickListener {
            waterPlantNow()
        }

        switchWaterAuto.setOnCheckedChangeListener { _, isChecked ->
            updateAutoWaterSetting(isChecked)
        }

        switchLightAuto.setOnCheckedChangeListener { _, isChecked ->
            updateAutoLightSetting(isChecked)
        }

        // Listener para cambiar periodo del historial
        historialTitle?.setOnClickListener {
            showHistoryPeriodSelector()
        }
    }

    private fun loadPlantData() {
        // Obtener datos de la planta desde el Intent
        plantaId = intent.getLongExtra("PLANT_ID", -1)
        var plantaNombre = intent.getStringExtra("PLANT_NAME") ?: "Nombre de Planta"
        var plantaEspecie = intent.getStringExtra("PLANT_SPECIES") ?: "Especie"
        var ubicacion = intent.getStringExtra("PLANT_LOCATION") ?: "Sin ubicación"
        var humedad = intent.getFloatExtra("PLANT_HUMIDITY", 65f)
        var temperatura = intent.getFloatExtra("PLANT_TEMP", 22.5f)
        var luz = intent.getFloatExtra("PLANT_LIGHT", 750f)
        var estado = intent.getStringExtra("PLANT_STATUS") ?: "healthy"
        var ultimoRiego = intent.getStringExtra("PLANT_LAST_WATER") ?: "Sin registro"

        // Crear objeto Planta
        planta = Planta().apply {
            id = plantaId
            nombre = plantaNombre
            especie = plantaEspecie
            setFechaCreacion("")
            descripcion = ""
            familiaId = -1L
            ubicacion = ubicacion
        }

        // Actualizar UI con datos del intent
        updateUI(humedad, temperatura, luz, estado, ubicacion, plantaNombre, ultimoRiego)

        // Cargar datos adicionales en segundo plano
        loadAdditionalData()

        // Cargar historial
        loadHistoryData(24) // Cargar 24 horas por defecto
    }

    private fun updateUI(
        humedad: Float,
        temperatura: Float,
        luz: Float,
        estado: String,
        ubicacion: String,
        nombre: String,
        ultimoRiego: String
    ) {
        tvPlantName.text = nombre
        tvLocation.text = ubicacion

        // Humedad
        tvHumidityValue.text = "${humedad.toInt()}%"
        val humStatus = when {
            humedad >= 70f -> "Óptimo"
            humedad >= 40f -> "Adecuado"
            else -> "Bajo"
        }
        tvHumidityStatus.text = humStatus
        updateCardColor(cardHumidity, humedad, "humidity")

        // Luz
        tvLightValue.text = "${luz.toInt()} lux"
        val lightStatus = when {
            luz >= 1000f -> "Excelente"
            luz >= 500f -> "Bueno"
            else -> "Bajo"
        }
        tvLightStatus.text = lightStatus
        updateCardColor(cardLight, luz, "light")

        // Temperatura
        tvTempValue.text = "${temperatura.toInt()}°C"
        val tempStatus = when {
            temperatura >= 18f && temperatura <= 28f -> "Óptimo"
            temperatura >= 15f && temperatura <= 32f -> "Aceptable"
            else -> "Revisar"
        }
        tvTempStatus.text = tempStatus
        updateCardColor(cardTemp, temperatura, "temperature")

        // Estado general
        val estadoInfo = when (estado) {
            "healthy", "Saludable", "Excelente" -> Triple("😊", "Saludable", R.color.green_primary)
            "warning", "Advertencia" -> Triple("😐", "Necesita atención", R.color.status_warning)
            "critical", "Crítico", "Necesita agua" -> Triple(
                "😟",
                "Necesita agua urgente",
                R.color.status_critical
            )
            else -> Triple("🤔", "Estado desconocido", R.color.text_gray)
        }

        val emoji = estadoInfo.first
        val statusText = estadoInfo.second
        val colorRes = estadoInfo.third

        tvStatusText.text = "$emoji $statusText"
        tvStatusText.setTextColor(ContextCompat.getColor(this, colorRes))

        // Para el card de estado, usamos el estado directamente
        updateCardColor(cardStatus, 0f, estado)

        // Nivel de agua (calculado basado en humedad)
        val nivelAgua = calculateWaterLevel(humedad)
        tvWaterLevelPercent.text = "$nivelAgua%"
        progressWater.progress = nivelAgua

        // Configurar color de la barra de progreso
        val waterColor = when {
            nivelAgua > 70 -> R.color.green_primary
            nivelAgua > 30 -> R.color.status_warning
            else -> R.color.status_critical
        }
        progressWater.progressTintList = ContextCompat.getColorStateList(this, waterColor)

        // Cargar configuración automática
        loadAutoSettings()
    }

    private fun calculateWaterLevel(humedad: Float): Int {
        return when {
            humedad >= 70f -> 90
            humedad >= 40f -> ((humedad - 40f) / 30f * 50f + 40f).toInt()
            humedad >= 20f -> ((humedad - 20f) / 20f * 40f).toInt()
            else -> (humedad / 20f * 20f).toInt()
        }.coerceIn(0, 100)
    }

    private fun updateCardColor(card: CardView, value: Float, type: String) {
        val colorRes = when (type) {
            "humidity" -> when {
                value >= 60f -> R.color.status_healthy_light
                value >= 30f -> R.color.status_warning_light
                else -> R.color.status_critical_light
            }
            "light" -> when {
                value >= 800f -> R.color.status_healthy_light
                value >= 400f -> R.color.status_warning_light
                else -> R.color.status_critical_light
            }
            "temperature" -> when {
                value in 18f..28f -> R.color.status_healthy_light
                value in 15f..32f -> R.color.status_warning_light
                else -> R.color.status_critical_light
            }
            else -> when (type) {
                "healthy", "Saludable", "Excelente" -> R.color.status_healthy_light
                "warning", "Advertencia" -> R.color.status_warning_light
                "critical", "Crítico" -> R.color.status_critical_light
                else -> android.R.color.white
            }
        }
        card.setCardBackgroundColor(ContextCompat.getColor(this, colorRes))
    }

    private fun loadAdditionalData() {
        Thread {
            try {
                if (plantaId != -1L) {
                    // Obtener datos actualizados de la base de datos
                    val datosSensores = PlantaDao.obtenerDatosSensoresPlanta(plantaId)

                    runOnUiThread {
                        val humedad = datosSensores["humedad"] as? Float ?: 0f
                        val temperatura = datosSensores["temperatura"] as? Float ?: 0f
                        val luz = datosSensores["luz"] as? Float ?: 0f
                        val estado = datosSensores["estado"] as? String ?: "Desconocido"
                        val ubicacion = datosSensores["ubicacion"] as? String ?: "Sin ubicación"
                        val ultimoRiego = datosSensores["ultimo_riego"] as? String ?: "Sin registro"

                        updateUI(
                            humedad,
                            temperatura,
                            luz,
                            estado,
                            ubicacion,
                            tvPlantName.text.toString(),
                            ultimoRiego
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error cargando datos adicionales", e)
                runOnUiThread {
                    Toast.makeText(this, "Error al cargar datos actualizados", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }.start()
    }

    private fun loadAutoSettings() {
        Thread {
            try {
                // Simular carga de configuración
                val waterAutoEnabled = false // Valor por defecto
                val lightAutoEnabled = false

                runOnUiThread {
                    switchWaterAuto.isChecked = waterAutoEnabled
                    switchLightAuto.isChecked = lightAutoEnabled
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error cargando configuraciones", e)
            }
        }.start()
    }

    // ================== FUNCIONES PARA EL HISTORIAL ==================

    private fun loadHistoryData(hours: Int) {
        Thread {
            try {
                if (plantaId != -1L) {
                    // Aquí usarías tu función del DAO
                    // Por ahora, simulemos datos para demostración
                    val mockData = generateMockHistoryData(hours)

                    runOnUiThread {
                        setupHistoryChart(mockData, hours)
                    }
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error cargando historial", e)
                runOnUiThread {
                    // Mostrar mensaje de error en el gráfico
                    chartHistory.clear()
                    chartHistory.setNoDataText("Error cargando historial")
                    chartHistory.setNoDataTextColor(Color.GRAY)
                }
            }
        }.start()
    }

    private fun generateMockHistoryData(hours: Int): Map<String, List<Pair<String, Float>>> {
        val data = mutableMapOf<String, List<Pair<String, Float>>>()

        val humidityData = mutableListOf<Pair<String, Float>>()
        val tempData = mutableListOf<Pair<String, Float>>()
        val lightData = mutableListOf<Pair<String, Float>>()

        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        for (i in hours downTo 0 step 2) {
            val time = Date(now.time - (i * 60 * 60 * 1000))
            val timeStr = dateFormat.format(time)

            // Datos simulados con variaciones realistas
            val baseHumidity = 60f + (Math.random() * 20).toFloat()
            val baseTemp = 22f + (Math.random() * 6).toFloat()
            val baseLight = 500f + (Math.random() * 500).toFloat()

            humidityData.add(Pair(timeStr, baseHumidity))
            tempData.add(Pair(timeStr, baseTemp))
            lightData.add(Pair(timeStr, baseLight))
        }

        data["humedad"] = humidityData
        data["temperatura"] = tempData
        data["luz"] = lightData

        return data
    }

    private fun setupHistoryChart(historial: Map<String, List<Pair<String, Float>>>, hours: Int) {
        // Limpiar gráfico
        chartHistory.clear()

        // Configurar aspecto del gráfico
        chartHistory.description.isEnabled = false
        chartHistory.setTouchEnabled(true)
        chartHistory.isDragEnabled = true
        chartHistory.setScaleEnabled(true)
        chartHistory.setPinchZoom(true)
        chartHistory.setDrawGridBackground(false)

        // Configurar leyenda
        val legend = chartHistory.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.textSize = 11f

        // Configurar eje X
        val xAxis = chartHistory.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.parseColor("#E0E0E0")
        xAxis.textColor = Color.parseColor("#666666")
        xAxis.textSize = 10f

        // Obtener etiquetas de tiempo
        val timeLabels = historial["humedad"]?.map { it.first } ?: emptyList()

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in timeLabels.indices) {
                    timeLabels[index]
                } else {
                    ""
                }
            }
        }

        // Configurar eje Y izquierdo
        val leftAxis = chartHistory.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
        leftAxis.granularity = 20f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#E0E0E0")
        leftAxis.textColor = Color.parseColor("#666666")
        leftAxis.textSize = 10f

        // Configurar eje Y derecho
        val rightAxis = chartHistory.axisRight
        rightAxis.isEnabled = false

        // Preparar datos
        val entriesHumedad = mutableListOf<Entry>()
        val entriesTemperatura = mutableListOf<Entry>()
        val entriesLuz = mutableListOf<Entry>()

        // Datos de humedad
        historial["humedad"]?.forEachIndexed { index, pair ->
            entriesHumedad.add(Entry(index.toFloat(), pair.second))
        }

        // Datos de temperatura (escalados a 0-100)
        historial["temperatura"]?.forEachIndexed { index, pair ->
            val tempScaled = (pair.second / 50f * 100f).coerceIn(0f, 100f)
            entriesTemperatura.add(Entry(index.toFloat(), tempScaled))
        }

        // Datos de luz (escalados a 0-100)
        historial["luz"]?.forEachIndexed { index, pair ->
            val luzScaled = (pair.second / 1000f * 100f).coerceIn(0f, 100f)
            entriesLuz.add(Entry(index.toFloat(), luzScaled))
        }

        // Crear datasets
        val setHumedad = LineDataSet(entriesHumedad, "Humedad (%)")
        setHumedad.color = ContextCompat.getColor(this, R.color.green_primary)
        setHumedad.lineWidth = 2.5f
        setHumedad.setDrawCircles(false)
        setHumedad.mode = LineDataSet.Mode.CUBIC_BEZIER
        setHumedad.setDrawValues(false)

        val setTemperatura = LineDataSet(entriesTemperatura, "Temperatura (°C)")
        setTemperatura.color = ContextCompat.getColor(this, R.color.sensor_temp_icon)
        setTemperatura.lineWidth = 2.5f
        setTemperatura.setDrawCircles(false)
        setTemperatura.mode = LineDataSet.Mode.CUBIC_BEZIER
        setTemperatura.setDrawValues(false)

        val setLuz = LineDataSet(entriesLuz, "Luz (lux)")
        setLuz.color = ContextCompat.getColor(this, R.color.sensor_light_icon)
        setLuz.lineWidth = 2.5f
        setLuz.setDrawCircles(false)
        setLuz.mode = LineDataSet.Mode.CUBIC_BEZIER
        setLuz.setDrawValues(false)

        // Crear LineData y configurar gráfico
        val lineData = LineData(setHumedad, setTemperatura, setLuz)
        lineData.setValueTextSize(10f)

        chartHistory.data = lineData
        chartHistory.invalidate() // Refrescar gráfico

        // Actualizar título
        historialTitle?.text = when (hours) {
            24 -> "Historial (24h)"
            168 -> "Historial (7 días)"
            720 -> "Historial (30 días)"
            else -> "Historial (${hours}h)"
        }
    }

    private fun showHistoryPeriodSelector() {
        val periods = arrayOf("Últimas 24 horas", "Últimos 7 días", "Últimos 30 días")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Seleccionar periodo")
            .setItems(periods) { _, which ->
                when (which) {
                    0 -> loadHistoryData(24)
                    1 -> loadHistoryData(168)  // 24 * 7
                    2 -> loadHistoryData(720)  // 24 * 30
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun waterPlantNow() {
        Toast.makeText(this, "Regando planta...", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                // Simular riego
                Thread.sleep(2000)

                runOnUiThread {
                    Toast.makeText(this, "¡Planta regada exitosamente!", Toast.LENGTH_SHORT).show()
                    // Recargar datos después del riego
                    loadAdditionalData()
                    // También recargar historial
                    loadHistoryData(24)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error al regar la planta", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updateAutoWaterSetting(enabled: Boolean) {
        Thread {
            try {
                // Aquí actualizarías en la base de datos
                Log.d("PlantDetail", "Riego automático: $enabled")

                runOnUiThread {
                    val message =
                        if (enabled) "Riego automático activado" else "Riego automático desactivado"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error actualizando configuración", e)
            }
        }.start()
    }

    private fun updateAutoLightSetting(enabled: Boolean) {
        Thread {
            try {
                // Aquí actualizarías en la base de datos
                Log.d("PlantDetail", "Luz automática: $enabled")

                runOnUiThread {
                    val message =
                        if (enabled) "Luz automática activada" else "Luz automática desactivada"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error actualizando configuración", e)
            }
        }.start()
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Planta")
            .setMessage("¿Estás seguro de que quieres eliminar esta planta? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deletePlant()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deletePlant() {
        Thread {
            try {
                // Aquí implementarías la lógica para eliminar de la base de datos
                Thread.sleep(1000)

                runOnUiThread {
                    Toast.makeText(this, "Planta eliminada", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error al eliminar la planta", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    companion object {
        fun createIntent(
            context: AppCompatActivity,
            planta: Planta,
            datos: Map<String, Any>
        ): Intent {
            return Intent(context, PlantDetailActivity::class.java).apply {
                putExtra("PLANT_ID", planta.id)
                putExtra("PLANT_NAME", planta.nombre)
                putExtra("PLANT_SPECIES", planta.especie)
                putExtra("PLANT_LOCATION", datos["ubicacion"] as? String ?: "")
                putExtra("PLANT_HUMIDITY", datos["humedad"] as? Float ?: 0f)
                putExtra("PLANT_TEMP", datos["temperatura"] as? Float ?: 0f)
                putExtra("PLANT_LIGHT", datos["luz"] as? Float ?: 0f)
                putExtra("PLANT_STATUS", datos["estado"] as? String ?: "")
                putExtra("PLANT_LAST_WATER", datos["ultimoRiego"] as? String ?: "")
            }
        }
    }
}