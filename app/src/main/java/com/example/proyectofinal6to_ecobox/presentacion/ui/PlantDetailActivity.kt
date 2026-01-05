package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
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

    // Datos
    private var planta: Planta? = null
    private var plantaId: Long = -1
    private var userId: Long = 1
    private var plantaNombre: String = ""
    private var plantaUbicacion: String = ""
    private var plantaFoto: String = ""

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

        switchWaterAuto = findViewById(R.id.switchWaterAuto)
        switchLightAuto = findViewById(R.id.switchLightAuto)
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
    }

    private fun setupClickListeners() {
        btnWaterNow.setOnClickListener {
            waterPlantNow()
        }

        //btnSeguimiento.setOnClickListener {
        //navigateToTracking()
        //}

        btnEdit.setOnClickListener {
            navigateToEditPlant()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
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

    //private fun navigateToTracking() {
    //// Navegar a la actividad de seguimiento
    //val intent = Intent(this, TrackingActivity::class.java).apply {
    //putExtra("PLANT_ID", plantaId)
    //putExtra("PLANT_NAME", plantaNombre)
    //putExtra("PLANT_LOCATION", plantaUbicacion)
    //}
    //startActivity(intent)


//}

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

    // En el método onActivityResult, agrega setResult:
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EDIT_PLANT_REQUEST && resultCode == RESULT_OK) {
            data?.let {
                val updatedName = it.getStringExtra("UPDATED_NAME") ?: plantaNombre
                val updatedLocation = it.getStringExtra("UPDATED_LOCATION") ?: plantaUbicacion
                val updatedPhoto = it.getStringExtra("UPDATED_PHOTO") ?: plantaFoto

                // Actualizar UI con los nuevos datos
                tvPlantName.text = updatedName
                tvLocation.text = "📍 $updatedLocation"

                if (updatedPhoto.isNotEmpty()) {
                    Glide.with(this)
                        .load(updatedPhoto)
                        .placeholder(R.drawable.img_plant_placeholder)
                        .error(R.drawable.img_plant_placeholder)
                        .centerCrop()
                        .into(ivPlant)
                }

                // ¡¡¡AGREGA ESTO!!! Enviar resultado a PlantsFragment
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

            R.id.btnEdit -> {
                navigateToEditPlant()
                true
            }

            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadPlantData() {
        plantaId = intent.getLongExtra("PLANT_ID", -1)

        if (plantaId == -1L) {
            Toast.makeText(this, "Error: Planta no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Obtener más datos de la BD
        Thread {
            try {
                // Necesitas implementar este método
                val plantaCompleta = PlantaDao.obtenerPlantaPorId(plantaId, userId)
                val datosSensores = PlantaDao.obtenerDatosSensoresPlanta(plantaId)

                runOnUiThread {
                    if (plantaCompleta != null) {
                        planta = plantaCompleta
                        plantaNombre = plantaCompleta.nombre
                        plantaUbicacion = datosSensores["ubicacion"] as? String ?: "Mi Jardín"
                        plantaFoto = plantaCompleta.foto ?: ""

                        val humedad = datosSensores["humedad"] as? Float ?: 0f
                        val temperatura = datosSensores["temperatura"] as? Float ?: 0f
                        val luz = datosSensores["luz"] as? Float ?: 0f
                        val estado = datosSensores["estado"] as? String ?: "healthy"

                        updateUI(
                            humedad,
                            temperatura,
                            luz,
                            estado,
                            plantaUbicacion,
                            plantaNombre,
                            plantaFoto
                        )
                        loadAdditionalData()
                        loadHistoryData(24)
                    } else {
                        Toast.makeText(
                            this,
                            "Error: Planta no encontrada en BD",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error cargando planta", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }.start()
    }

    private fun updateUI(
        humedad: Float,
        temperatura: Float,
        luz: Float,
        estado: String,
        ubicacion: String,
        nombre: String,
        fotoUrl: String
    ) {
        tvPlantName.text = nombre
        tvLocation.text = "📍 $ubicacion"

        // Cargar la foto
        if (fotoUrl.isNotEmpty()) {
            try {
                Glide.with(this)
                    .load(fotoUrl)
                    .placeholder(R.drawable.img_plant_placeholder)
                    .error(R.drawable.img_plant_placeholder)
                    .centerCrop()
                    .into(ivPlant)
            } catch (e: Exception) {
                ivPlant.setImageResource(R.drawable.img_plant_placeholder)
            }
        } else {
            ivPlant.setImageResource(R.drawable.img_plant_placeholder)
        }

        // Valores numéricos
        tvHumidityValue.text = "${humedad.toInt()}%"
        tvLightValue.text = "${luz.toInt()} lux"
        tvTempValue.text = "${temperatura.toInt()}°C"

        // Colores de las tarjetas
        updateCardColor(cardHumidity, humedad, "humidity")
        updateCardColor(cardLight, luz, "light")
        updateCardColor(cardTemp, temperatura, "temperature")

        // Lógica de estado General
        val (icono, texto, colorRes) = when (estado.toLowerCase(Locale.ROOT)) {
            "healthy", "saludable", "excelente" -> Triple("🌿", "Sana", R.color.status_healthy_dark)
            "warning", "advertencia", "moderado" -> Triple(
                "⚠️",
                "Atención",
                R.color.status_warning_dark
            )

            "critical", "crítico", "grave" -> Triple("🥀", "Crítico", R.color.status_critical_dark)
            else -> Triple("🤔", "Desconocido", R.color.text_gray)
        }

        tvStatusText.text = "$icono $texto"
        tvStatusText.setTextColor(ContextCompat.getColor(this, colorRes))

        // Actualizar color de fondo de la card de estado
        val backgroundColor = when (estado.toLowerCase(Locale.ROOT)) {
            "healthy", "saludable", "excelente" -> R.color.status_healthy_light
            "warning", "advertencia", "moderado" -> R.color.status_warning_light
            "critical", "crítico", "grave" -> R.color.status_critical_light
            else -> R.color.status_unknown_light
        }
        cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, backgroundColor))

        // Nivel de agua
        val nivelAgua = calculateWaterLevel(humedad)
        tvWaterLevelPercent.text = "$nivelAgua%"
        progressWater.progress = nivelAgua

        // Actualizar color del texto del porcentaje de agua
        val waterColor = when {
            nivelAgua > 70 -> Color.parseColor("#10B981")
            nivelAgua > 30 -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#EF4444")
        }
        tvWaterLevelPercent.setTextColor(waterColor)
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

    private fun loadAdditionalData() {
        Thread {
            try {
                Thread.sleep(300)
                runOnUiThread {
                    loadAutoSettings()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error cargando configuración", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun loadAutoSettings() {
        // Cargar configuraciones desde la base de datos
        // Por ahora valores mock
        switchWaterAuto.isChecked = false
        switchLightAuto.isChecked = true
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
        Toast.makeText(this, "🌊 Iniciando riego...", Toast.LENGTH_SHORT).show()
        btnWaterNow.isEnabled = false
        btnWaterNow.text = "Regando..."
        btnWaterNow.icon = null

        btnWaterNow.postDelayed({
            btnWaterNow.isEnabled = true
            btnWaterNow.text = "Regar Ahora"
            btnWaterNow.icon = ContextCompat.getDrawable(this, R.drawable.ic_water_drop)
            Toast.makeText(this, "✅ Riego completado", Toast.LENGTH_SHORT).show()

            // Actualizar valores después del riego
            progressWater.progress = 100
            tvWaterLevelPercent.text = "100%"
            tvHumidityValue.text = "95%"

            // Actualizar estado de la planta
            tvStatusText.text = "🌿 Sana"
            tvStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_healthy_dark))

            // Actualizar color de la card de estado
            cardStatus.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.status_healthy_light
                )
            )

            // Actualizar color de la card de humedad
            updateCardColor(cardHumidity, 95f, "humidity")
        }, 2000)
    }

    private fun showDeleteConfirmation() {
        if (planta == null) {
            Toast.makeText(this, "Error: Planta no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener información de la planta en un hilo separado
        Thread {
            try {
                val info = PlantaDao.obtenerInfoParaDialogoEliminar(plantaId, userId)

                runOnUiThread {
                    val plantName = planta?.nombre ?: info["nombre"] as? String ?: "esta planta"
                    val sensorCount = info["sensor_count"] as? Int ?: 0
                    val hasAccess = info["tiene_acceso"] as? Boolean ?: false

                    if (!hasAccess) {
                        Toast.makeText(
                            this,
                            "No tienes permiso para eliminar esta planta",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@runOnUiThread
                    }

                    showCustomDeleteDialog(plantName, sensorCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Error al cargar información de la planta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun showCustomDeleteDialog(plantName: String, sensorCount: Int) {
        // Crear el diálogo personalizado
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_plant, null)

        val tvDeletePlantName = dialogView.findViewById<TextView>(R.id.tvDeletePlantName)
        val tvSensorCount = dialogView.findViewById<TextView>(R.id.tvSensorCount)

        // Configurar textos
        tvDeletePlantName.text = "$plantName (ID: $plantaId)"

        val sensorText = when (sensorCount) {
            0 -> "Esta planta no tiene sensores asociados"
            1 -> "Atención: Esta planta tiene 1 sensor asociado"
            else -> "Atención: Esta planta tiene $sensorCount sensores asociados"
        }
        tvSensorCount.text = sensorText

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

    private fun deletePlant() {
        val plantName = planta?.nombre ?: "Planta"

        // Mostrar progress dialog
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminando...")
            .setMessage("Eliminando $plantName y todos sus datos asociados...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                // Eliminar la planta de la base de datos
                val success = PlantaDao.eliminarPlantaCompleta(plantaId, userId)

                runOnUiThread {
                    progressDialog.dismiss()

                    if (success) {
                        Toast.makeText(this, "✅ Planta '$plantName' eliminada", Toast.LENGTH_SHORT)
                            .show()

                        // Crear intent de resultado para actualizar la actividad anterior
                        val resultIntent = Intent().apply {
                            putExtra("PLANTA_ELIMINADA", true)
                            putExtra("PLANTA_ID_ELIMINADA", plantaId)
                        }
                        setResult(RESULT_OK, resultIntent)

                        // Regresar a la actividad anterior
                        finish()
                    } else {
                        Toast.makeText(this, "❌ Error al eliminar la planta", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "❌ Error al eliminar la planta", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updateAutoWaterSetting(enabled: Boolean) {
        val message = if (enabled) {
            "Riego automático activado"
        } else {
            "Riego automático desactivado"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateAutoLightSetting(enabled: Boolean) {
        val message = if (enabled) {
            "Luces UV automáticas activadas"
        } else {
            "Luces UV automáticas desactivadas"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EDIT_PLANT_REQUEST = 1001

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
                putExtra("PLANT_SENSOR_COUNT", datos["sensorCount"] as? Int ?: 0) // NUEVO
            }
        }

        fun createSimpleIntent(context: Context, plantaId: Long): Intent {
            return Intent(context, PlantDetailActivity::class.java).apply {
                putExtra("PLANT_ID", plantaId)
            }
        }
    }
}