package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.example.proyectofinal6to_ecobox.data.network.*
import com.example.proyectofinal6to_ecobox.utils.ImageUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.data.network.*
import kotlinx.coroutines.launch
import java.util.Date

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
    private lateinit var btnWaterNow: MaterialButton
    private lateinit var btnSeguimiento: MaterialButton
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnDelete: MaterialButton

    // Cards
    private lateinit var cardHumidity: CardView
    private lateinit var cardLight: CardView
    private lateinit var cardTemp: CardView
    private lateinit var cardStatus: CardView

    // Gráfico
    private lateinit var chartHistory: LineChart
    private var historialTitle: TextView? = null

    // Nuevas vistas - Paridad Web
    private lateinit var pbHumidity: ProgressBar
    private lateinit var tvHumidityTarget: TextView
    private lateinit var pbLight: ProgressBar
    private lateinit var tvLightTarget: TextView
    private lateinit var pbTemp: ProgressBar
    private lateinit var tvTempTarget: TextView
    
    private lateinit var cardAIAnalysis: CardView
    private lateinit var tvAIPrediction: TextView
    private lateinit var tvAIProbability: TextView
    private lateinit var tvDetailAspect: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailSensors: TextView
    private lateinit var tvDetailFamily: TextView

    // Datos
    private var planta: Planta? = null
    private var plantaId: Long = -1
    private var userId: Long = 1
    private var plantaNombre: String = ""
    private var plantaUbicacion: String = ""
    private var plantaFoto: String = ""

    private lateinit var btnGestionarSensores: MaterialButton
    private var isAdmin: Boolean = false

    // Modos de Riego (Vistas del ToggleGroup)
    private lateinit var modeToggleGroup: com.google.android.material.button.MaterialButtonToggleGroup
    private lateinit var layoutManualMode: android.widget.LinearLayout
    private lateinit var layoutAssistedMode: android.widget.LinearLayout
    private lateinit var layoutAutoMode: android.widget.LinearLayout
    private lateinit var tvAIPredictionDetail: TextView
    private lateinit var tvAIConfidenceDetail: TextView
    private lateinit var btnAcceptAI: MaterialButton
    private lateinit var aiStatusLight: View
    private lateinit var switchAutoIrrigate: MaterialSwitch
    private lateinit var listWateringHistory: android.widget.LinearLayout
    private lateinit var tvNoWateringHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)

        // Configurar toolbar si existe en el layout
        try {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        } catch (e: Exception) {
            // Si no hay toolbar en el layout, continuar sin ella
        }

        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadPlantData()
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

        btnWaterNow = findViewById(R.id.btnWaterNow)
        btnSeguimiento = findViewById(R.id.btnSeguimiento)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)

        cardHumidity = findViewById(R.id.cardHumidity)
        cardLight = findViewById(R.id.cardLight)
        cardTemp = findViewById(R.id.cardTemp)
        cardStatus = findViewById(R.id.cardStatus)

        chartHistory = findViewById(R.id.chartHistory)
        historialTitle = findViewById(R.id.tvHistoryTitle)
        btnGestionarSensores = findViewById(R.id.btnGestionarSensores)

        // Vistas de Paridad Web
        pbHumidity = findViewById(R.id.pbHumidity)
        tvHumidityTarget = findViewById(R.id.tvHumidityTarget)
        pbLight = findViewById(R.id.pbLight)
        tvLightTarget = findViewById(R.id.tvLightTarget)
        pbTemp = findViewById(R.id.pbTemp)
        tvTempTarget = findViewById(R.id.tvTempTarget)
        
        cardAIAnalysis = findViewById(R.id.cardAIAnalysis)
        tvAIPrediction = findViewById(R.id.tvAIPrediction)
        tvAIProbability = findViewById(R.id.tvAIProbability)

        // Información General
        tvDetailAspect = findViewById(R.id.tvDetailAspect)
        tvDetailDate = findViewById(R.id.tvDetailDate)
        tvDetailSensors = findViewById(R.id.tvDetailSensors)
        tvDetailFamily = findViewById(R.id.tvDetailFamily)

        // Inicializar nuevas vistas de control
        modeToggleGroup = findViewById(R.id.modeToggleGroup)
        layoutManualMode = findViewById(R.id.layoutManualMode)
        layoutAssistedMode = findViewById(R.id.layoutAssistedMode)
        layoutAutoMode = findViewById(R.id.layoutAutoMode)
        tvAIPredictionDetail = findViewById(R.id.tvAIPredictionDetail)
        tvAIConfidenceDetail = findViewById(R.id.tvAIConfidenceDetail)
        btnAcceptAI = findViewById(R.id.btnAcceptAI)
        aiStatusLight = findViewById(R.id.aiStatusLight)
        switchAutoIrrigate = findViewById(R.id.switchAutoIrrigate)
        listWateringHistory = findViewById(R.id.listWateringHistory)
        tvNoWateringHistory = findViewById(R.id.tvNoWateringHistory)
    }

    private fun setupClickListeners() {
        btnWaterNow.setOnClickListener {
            waterPlantNow()
        }

        // BOTÓN DE SEGUIMIENTO - NUEVO
        btnSeguimiento.setOnClickListener {
            navigateToSeguimiento()
        }

        // BOTÓN DE SENSORES
        btnGestionarSensores.setOnClickListener {
            navigateToSensors()
        }

        btnEdit.setOnClickListener {
            navigateToEditPlant()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        historialTitle?.setOnClickListener {
            showHistoryPeriodSelector()
        }

        // --- LISTENERS DE MODOS ---
        modeToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                updateModeVisibility(checkedId)
            }
        }

        findViewById<View>(R.id.chip1min).setOnClickListener { waterPlantManual(60) }
        findViewById<View>(R.id.chip2min).setOnClickListener { waterPlantManual(120) }
        findViewById<View>(R.id.chip3min).setOnClickListener { waterPlantManual(180) }
        findViewById<View>(R.id.chip5min).setOnClickListener { waterPlantManual(300) }

        btnAcceptAI.setOnClickListener {
            waterPlantWithAI()
        }

        switchAutoIrrigate.setOnCheckedChangeListener { _, isChecked ->
            updateAutoIrrigateSetting(isChecked)
        }
    }

    private fun updateModeVisibility(checkedId: Int) {
        layoutManualMode.visibility = if (checkedId == R.id.btnModeManual) View.VISIBLE else View.GONE
        layoutAssistedMode.visibility = if (checkedId == R.id.btnModeAssisted) View.VISIBLE else View.GONE
        layoutAutoMode.visibility = if (checkedId == R.id.btnModeAuto) View.VISIBLE else View.GONE
        
        if (checkedId == R.id.btnModeAssisted) {
            loadAIPrediction()
        }
    }

    // NUEVO: Método para navegar al seguimiento
    private fun navigateToSeguimiento() {
        if (planta == null || plantaId == -1L) {
            Toast.makeText(this, "Error: Planta no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        // En la versión API, el acceso se verifica al cargar la planta o al intentar la acción
        // Por ahora permitimos la navegación si la planta se cargó con éxito
        val intent = Intent(this, HistorialSeguimientoActivity::class.java).apply {
            putExtra("PLANTA_ID", plantaId)
            putExtra("PLANTA_NOMBRE", plantaNombre)
            putExtra("USER_ID", userId)
        }
        startActivityForResult(intent, SEGUIMIENTO_REQUEST_CODE)
    }

    private fun navigateToSensors() {
        // Verificar que tenemos los datos de la planta
        if (planta == null) {
            Toast.makeText(this, "Error: Planta no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Usar el método de tu clase SensoresActivity
        try {
            val intent = SensoresActivity.newIntent(
                context = this,
                plantaId = plantaId,
                plantaNombre = plantaNombre
            )
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: método simple si el otro no funciona
            val intent = Intent(this, SensoresActivity::class.java).apply {
                putExtra("plantaId", plantaId)
                putExtra("plantaNombre", plantaNombre)
            }
            startActivity(intent)
        }
    }

    private fun navigateToEditPlant() {
        // Asegúrate de tener el objeto planta
        if (planta == null) {
            Toast.makeText(this, "Error: Planta no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, EditarPlantaActivity::class.java).apply {
            putExtra("planta", planta)  // Enviar objeto completo
            putExtra("user_id", userId) // Enviar userId
        }
        startActivityForResult(intent, EDIT_PLANT_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EDIT_PLANT_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    data?.let {
                        val updatedName = it.getStringExtra("UPDATED_NAME") ?: plantaNombre
                        val updatedLocation = it.getStringExtra("UPDATED_LOCATION") ?: plantaUbicacion
                        val updatedPhoto = it.getStringExtra("UPDATED_PHOTO") ?: plantaFoto

                        // Actualizar UI con los nuevos datos
                        tvPlantName.text = updatedName
                        tvLocation.text = "📍 $updatedLocation"

                        if (updatedPhoto.isNotEmpty()) {
                            ImageUtils.loadPlantImage(
                                imageData = updatedPhoto,
                                imageView = ivPlant,
                                placeholderResId = R.drawable.img_plant_placeholder
                            )
                        }

                        // Enviar resultado a PlantsFragment
                        val resultIntent = Intent().apply {
                            putExtra("PLANTA_EDITADA", true)
                            putExtra("PLANTA_ID_EDITADA", plantaId)
                            putExtra("PLANTA_NOMBRE_EDITADA", updatedName)
                        }
                        setResult(RESULT_OK, resultIntent)

                        Toast.makeText(this, "Planta actualizada correctamente", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            SEGUIMIENTO_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    // Recargar datos si se agregó un nuevo seguimiento
                    loadPlantData()
                    Toast.makeText(this, "Seguimiento actualizado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflar menú si es necesario
        menuInflater.inflate(R.menu.menu_plant_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            R.id.action_config -> {
                navigateToConfig()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToConfig() {
        val intent = Intent(this, PlantConfigActivity::class.java).apply {
            putExtra("PLANTA_ID", plantaId)
            putExtra("PLANTA_NOMBRE", plantaNombre)
        }
        startActivity(intent)
    }

    private fun loadPlantData() {
        plantaId = intent.getLongExtra("PLANT_ID", -1)

        if (plantaId == -1L) {
            Toast.makeText(this, "Error: Planta no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // Peticiones base en paralelo
                val deferredPlant = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                    .getPlant("Token $token", plantaId)
                
                val deferredConfig = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                    .getPlantConfig("Token $token", plantaId)
                
                val deferredSensors = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                    .getSensors("Token $token", plantaId)

                if (deferredPlant.isSuccessful && deferredPlant.body() != null) {
                    val p = deferredPlant.body()!!
                    
                    // Crear objeto local Planta para compatibilidad
                    planta = com.example.proyectofinal6to_ecobox.data.model.Planta(
                        p.id, p.nombre, p.especie ?: "", p.fecha_plantacion ?: "", 
                        p.descripcion ?: "", p.familia
                    ).apply {
                        setFoto(p.imagen_url ?: "")
                        setEstado(p.estado_salud ?: "Normal")
                    }

                    plantaNombre = p.nombre
                    plantaUbicacion = p.familia_nombre ?: "Mi Jardín"
                    plantaFoto = p.imagen_url ?: ""

                    // Obtener configuración
                    val config = if (deferredConfig.isSuccessful && !deferredConfig.body().isNullOrEmpty()) {
                        deferredConfig.body()!!.firstOrNull { it.planta == plantaId } 
                            ?: deferredConfig.body()!![0]
                    } else null

                    // Sensores Count
                    val sensorCount = if (deferredSensors.isSuccessful) deferredSensors.body()?.size ?: 0 else 0

                    // Obtener mediciones reales de sensores
                    var realHumidity: Float? = null
                    var realTemp: Float? = null
                    var realLight: Float? = null
                    
                    if (deferredSensors.isSuccessful && !deferredSensors.body().isNullOrEmpty()) {
                        deferredSensors.body()!!.forEach { sensor ->
                            try {
                                val mResponse = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                                    .getSensorMeasurements("Token $token", sensor.id)
                                
                                if (mResponse.isSuccessful && !mResponse.body().isNullOrEmpty()) {
                                    val valor = mResponse.body()!![0].valor
                                    when (sensor.tipoSensor) {
                                        1 -> realTemp = valor
                                        2 -> realHumidity = valor
                                        3 -> realHumidity = valor // Humedad suelo
                                        4 -> realLight = valor // Luz
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("PlantDetail", "Error fetching measurement for sensor ${sensor.id}")
                            }
                        }
                    }

                    // 1. Actualizar Base UI
                    updateBaseUI(p, plantaUbicacion)

                    // 2. Información General
                    tvDetailAspect.text = p.aspecto ?: "Normal"
                    tvDetailDate.text = p.fecha_plantacion?.split("T")?.get(0) ?: "Desconocida"
                    tvDetailSensors.text = "$sensorCount"
                    tvDetailFamily.text = "${p.familia}"

                    // 3. Calcular y Actualizar UI Detallada e IA (Emulación Web)
                    updateEmulatedDetailedUI(p, config, realHumidity, realTemp, realLight)
                    
                    loadHistoryData(24)
                    loadWateringHistory() // NUEVO: Cargar historial de riegos
                    loadAIPrediction()    // NUEVO: Cargar predicción IA
                    checkAdminPermissions()
                } else {
                    Toast.makeText(this@PlantDetailActivity, "Error: Planta no accesible", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error loading plant: ${e.message}", e)
                Toast.makeText(this@PlantDetailActivity, "Error de red al cargar planta", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateBaseUI(p: PlantResponse, ubicacion: String) {
        tvPlantName.text = p.nombre
        val aspectoDisplay = p.aspecto ?: "Normal"
        tvLocation.text = "📍 $ubicacion | ✨ $aspectoDisplay"

        // Cargar foto
        if (!p.imagen_url.isNullOrEmpty()) {
            ImageUtils.loadPlantImage(p.imagen_url, ivPlant, R.drawable.img_plant_placeholder)
        } else {
            ivPlant.setImageResource(R.drawable.img_plant_placeholder)
        }

        // Estado General Badge
        val estado = p.estado_salud ?: "normal"
        val (info, bgRes) = when (estado.lowercase(Locale.ROOT)) {
            "healthy", "saludable", "excelente" -> Triple("🌿", "Sana", R.color.status_healthy_dark) to R.color.status_healthy_light
            "warning", "atención", "necesita_agua" -> Triple("💧", "Sedienta", R.color.status_warning_dark) to R.color.status_warning_light
            "critical", "peligro", "crítico" -> Triple("🥀", "Crítico", R.color.status_critical_dark) to R.color.status_critical_light
            else -> Triple("✅", "Normal", R.color.eco_primary) to R.color.status_healthy_light
        }
        val (icono, texto, colorRes) = info

        tvStatusText.text = "${icono} ${texto}"
        tvStatusText.setTextColor(ContextCompat.getColor(this, colorRes))
        cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, bgRes))
    }

    private fun updateEmulatedDetailedUI(
        p: PlantResponse, 
        config: PlantConfigResponse?,
        realHumidity: Float?,
        realTemp: Float?,
        realLight: Float?
    ) {
        // Combinar datos: Sensores Reales > Datos Planta API > Fallback (Estilo Web)
        val humedad = realHumidity ?: p.humedad_actual
        val temp = realTemp ?: p.temperatura_actual
        val luz = realLight

        // Actualizar valores numéricos (Formato .00 como en la web)
        tvHumidityValue.text = if (humedad != null) String.format("%.2f%%", humedad) else "N/A"
        tvTempValue.text = if (temp != null) String.format("%.2f°C", temp) else "N/A"
        tvLightValue.text = if (luz != null) String.format("%.2f lux", luz) else "No instalado"

        // Actualizar barras de progreso y objetivos
        pbHumidity.progress = (humedad ?: 0f).toInt()
        val humTarget = config?.humedadObjetivo?.toInt() ?: 60
        tvHumidityTarget.text = "Objetivo: $humTarget%"

        pbTemp.progress = if (temp != null) ((temp - 10) / (40 - 10) * 100).toInt().coerceIn(0, 100) else 0
        val tMin = config?.tempMin?.toInt() ?: 18
        val tMax = config?.tempMax?.toInt() ?: 28
        tvTempTarget.text = "Rango: $tMin-$tMax°C"

        pbLight.progress = if (luz != null) (luz / 1000 * 100).toInt().coerceIn(0, 100) else 0
        tvLightTarget.text = if (luz != null) "Recomendado: 500-1000 lux" else "Sensor no detectado"

        // Agua Card
        val nivelAgua = calculateWaterLevel(humedad ?: 65f)
        tvWaterLevelPercent.text = "$nivelAgua%"
        progressWater.progress = nivelAgua
        
        // --- LÓGICA IA LOCAL (Paridad Web Violeta) ---
        cardAIAnalysis.visibility = View.VISIBLE
        
        val isSedienta = (p.estado_salud?.lowercase()?.contains("agua") == true || 
                          p.estado_salud?.lowercase()?.contains("sedienta") == true)
        
        val prob = if (isSedienta) 100 else if ((humedad ?: 65f) < 50) 90 else if ((humedad ?: 65f) < 65) 75 else 25
        val recomendacion = when {
            isSedienta -> "ESTADO MANUAL: Se ha marcado que requiere riego inmediato"
            prob > 80 -> "Se recomienda riego ALTO en las próximas horas"
            prob > 50 -> "Se recomienda riego MODERADO pronto"
            else -> "Riego bajo en las próximas horas"
        }
        
        tvAIProbability.text = "Probabilidad de riego: $prob%"
        tvAIPrediction.text = recomendacion
        
        val colorIA = when {
            prob >= 90 -> Color.parseColor("#DC2626") // Rojo
            prob > 50 -> Color.parseColor("#D97706") // Naranja
            else -> Color.parseColor("#15803D") // Verde
        }
        tvAIProbability.setTextColor(colorIA)

        // Sincronizar switches
        config?.let {
            switchAutoIrrigate.isChecked = it.riegoAutomatico
        }
    }

    private fun updateCardColor(card: CardView, value: Float, type: String) {
        val isWarning = when (type) {
            "humidity" -> value < 30f || value > 85f
            "temperature" -> value < 15f || value > 32f
            "light" -> value < 200f || value > 1000f
            else -> false
        }

        if (isWarning) {
            card.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
        } else {
            card.setCardBackgroundColor(Color.WHITE)
        }
    }

    private fun calculateWaterLevel(humedad: Float): Int {
        return when {
            humedad > 100f -> 100
            humedad < 0f -> 0
            else -> humedad.toInt()
        }
    }

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
        chartHistory.isDragEnabled = true
        chartHistory.setScaleEnabled(true)
        chartHistory.setPinchZoom(true)

        val xAxis = chartHistory.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.parseColor("#9CA3AF")
        xAxis.textSize = 10f

        val leftAxis = chartHistory.axisLeft
        leftAxis.textColor = Color.parseColor("#9CA3AF")
        leftAxis.gridColor = Color.parseColor("#F3F4F6")
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f

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
            .setCancelable(true)
            .show()
    }

    private fun waterPlantNow() {
        // Redirigir al modo manual si se pulsa el botón flotante
        modeToggleGroup.check(R.id.btnModeManual)
        Toast.makeText(this, "Seleccione la duración en el panel de control", Toast.LENGTH_SHORT).show()
    }

    private fun waterPlantManual(seconds: Int) {
        if (plantaId == -1L) return
        val token = getSharedPreferences("ecobox_prefs", MODE_PRIVATE).getString("auth_token", "") ?: ""
        
        lifecycleScope.launch {
            try {
                btnWaterNow.isEnabled = false
                val response = RetrofitClient.instance.activateWatering(
                    "Token $token", 
                    plantaId, 
                    mapOf("duration_seconds" to seconds, "mode" to "manual")
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@PlantDetailActivity, "🌊 Riego manual iniciado ($seconds seg)", Toast.LENGTH_SHORT).show()
                    loadWateringHistory() // Recargar historial
                }
            } catch (e: Exception) {
                Toast.makeText(this@PlantDetailActivity, "Error al iniciar riego", Toast.LENGTH_SHORT).show()
            } finally {
                btnWaterNow.isEnabled = true
            }
        }
    }

    private fun loadAIPrediction() {
        if (plantaId == -1L) return
        val token = getSharedPreferences("ecobox_prefs", MODE_PRIVATE).getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getWateringPrediction("Token $token", plantaId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.prediction
                    data?.let {
                        tvAIPredictionDetail.text = it.reason
                        tvAIConfidenceDetail.text = "Confianza: ${(it.confidence * 100).toInt()}%"
                        btnAcceptAI.visibility = if (it.action == "WATER") View.VISIBLE else View.GONE
                        aiStatusLight.setBackgroundResource(if (it.action == "WATER") R.drawable.bg_circle_blue else R.drawable.bg_circle_light_green)
                    }
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error loading AI prediction", e)
            }
        }
    }

    private fun waterPlantWithAI() {
        // Implementar lógica similar a manual pero modo 'assisted'
        waterPlantManual(180) // Duración estándar o de la predicción
    }

    private fun updateAutoIrrigateSetting(enabled: Boolean) {
        val token = getSharedPreferences("ecobox_prefs", MODE_PRIVATE).getString("auth_token", "") ?: ""
        lifecycleScope.launch {
            try {
                // Sincronizar con el campo riego_automatico de la configuración
                // Nota: Usamos patchwork para actualizar solo ese campo
                // Primero necesitamos el ID de la configuración (se carga en loadPlantData)
                // Por ahora simulamos éxito
                Toast.makeText(this@PlantDetailActivity, "Modo automático: ${if(enabled) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {}
        }
    }

    private fun loadWateringHistory() {
        if (plantaId == -1L) return
        val token = getSharedPreferences("ecobox_prefs", MODE_PRIVATE).getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getWateringHistory("Token $token", plantaId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val history = response.body()?.waterings ?: emptyList()
                    renderWateringHistory(history)
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error history", e)
            }
        }
    }

    private fun renderWateringHistory(history: List<WateringHistoryItem>) {
        listWateringHistory.removeAllViews()
        
        if (history.isEmpty()) {
            tvNoWateringHistory.visibility = View.VISIBLE
            listWateringHistory.addView(tvNoWateringHistory)
            return
        }

        tvNoWateringHistory.visibility = View.GONE
        
        // Mostrar los últimos 5 riegos
        history.take(5).forEach { item ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_event, listWateringHistory, false)
            val icon = itemView.findViewById<ImageView>(R.id.imgEventIcon)
            val title = itemView.findViewById<TextView>(R.id.tvEventTitle)
            val subtitle = itemView.findViewById<TextView>(R.id.tvEventSubtitle)

            icon.setImageResource(R.drawable.ic_water_drop)
            icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))
            
            title.text = "Riego ${item.mode.uppercase()}"
            subtitle.text = "${item.date.split("T")[0]} • ${item.duration}s • Hum: ${item.initialHumidity?.toInt() ?: 0}% -> ${item.finalHumidity?.toInt() ?: 0}%"
            
            listWateringHistory.addView(itemView)
        }
    }

    private fun showDeleteConfirmation() {
        if (planta == null) {
            Toast.makeText(this, "Error: Planta no disponible", Toast.LENGTH_SHORT).show()
            return
        }
        
        val plantName = planta?.nombre ?: "esta planta"
        // En la versión API simplificada, no mostramos el conteo de sensores antes de eliminar
        // ya que requeriría otra llamada a la API. Mostramos un mensaje genérico.
        showCustomDeleteDialog(plantName, 0)
    }

    private fun showCustomDeleteDialog(plantName: String, sensorCount: Int) {
        // Crear el diálogo personalizado
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_plant, null)

        val tvDeletePlantName = dialogView.findViewById<TextView>(R.id.tvDeletePlantName)
        val tvSensorCountLabel = dialogView.findViewById<TextView>(R.id.tvSensorCount)

        // Configurar textos
        tvDeletePlantName.text = "$plantName (ID: $plantaId)"
        tvSensorCountLabel.text = "Se eliminarán también todos los datos históricos"

        // Crear el diálogo
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Planta")
            .setView(dialogView)
            .setPositiveButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("SI, eliminar") { dialog, _ ->
                dialog.dismiss()
                deletePlant()
            }
            .setCancelable(true)
            .create()

        // Personalizar colores de botones
        dialog.setOnShowListener {
            val positiveButton =
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            val negativeButton =
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)

            // Color gris para Cancelar
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.text_gray))

            // Color rojo para Eliminar
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.error))
        }

        dialog.show()
    }

    private fun checkAdminPermissions() {
        val currentPlanta = planta ?: return
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""

        if (token.isEmpty()) return

        lifecycleScope.launch {
            try {
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                    .getFamilies("Token $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val familias = response.body()!!
                    val familiaDePlanta = familias.find { it.id == currentPlanta.familiaId }
                    
                    isAdmin = familiaDePlanta?.es_admin ?: false
                    invalidateOptionsMenu()
                    Log.d("PlantDetail", "Admin status for family ${currentPlanta.familiaId}: $isAdmin")
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error checking permissions: ${e.message}")
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // Todos pueden ver la configuración
        menu?.findItem(R.id.action_config)?.isVisible = true
        return super.onPrepareOptionsMenu(menu)
    }

    private fun deletePlant() {
        val plantName = planta?.nombre ?: "Planta"

        // Mostrar progress dialog
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminando...")
            .setMessage("Eliminando $plantName y todos sus datos asociados...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            try {
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                    .deletePlant("Token $token", plantaId)

                progressDialog.dismiss()

                if (response.isSuccessful) {
                    Toast.makeText(this@PlantDetailActivity, "✅ Planta '$plantName' eliminada", Toast.LENGTH_SHORT).show()

                    val resultIntent = Intent().apply {
                        putExtra("PLANTA_ELIMINADA", true)
                        putExtra("PLANTA_ID_ELIMINADA", plantaId)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this@PlantDetailActivity, "❌ Error al eliminar: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PlantDetail", "Error deleting plant: ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this@PlantDetailActivity, "❌ Error de red al eliminar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EDIT_PLANT_REQUEST = 1001
        const val SEGUIMIENTO_REQUEST_CODE = 1002  // NUEVA CONSTANTE

        fun createIntent(
            context: Context,
            planta: Planta,
            datos: Map<String, Any>
        ): Intent {
            return Intent(context, PlantDetailActivity::class.java).apply {
                putExtra("PLANT_ID", planta.id)
                putExtra("PLANT_NAME", planta.nombre)
                putExtra("PLANT_LOCATION", datos["ubicacion"] as? String ?: "")
                putExtra("PLANT_HUMIDITY", (datos["humedad"] as? Number)?.toFloat() ?: 0f)
                putExtra("PLANT_TEMP", (datos["temperatura"] as? Number)?.toFloat() ?: 0f)
                putExtra("PLANT_LIGHT", (datos["luz"] as? Number)?.toFloat() ?: 0f)
                putExtra("PLANT_STATUS", datos["estado"] as? String ?: "healthy")
                putExtra("PLANT_PHOTO", planta.foto ?: "")
                putExtra("PLANT_SENSOR_COUNT", datos["sensorCount"] as? Int ?: 0)
            }
        }

        fun createSimpleIntent(context: Context, plantaId: Long): Intent {
            return Intent(context, PlantDetailActivity::class.java).apply {
                putExtra("PLANT_ID", plantaId)
            }
        }
    }
}