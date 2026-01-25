package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.proyectofinal6to_ecobox.data.network.*
import android.content.Context

class PlantConfigActivity : AppCompatActivity() {

    private lateinit var sliderHumidity: Slider
    private lateinit var sliderTempRange: RangeSlider
    private lateinit var tvHumidityLabel: TextView
    private lateinit var tvTempRangeLabel: TextView
    private lateinit var btnSaveConfig: MaterialButton
    private lateinit var btnDefaultConfig: MaterialButton
    private lateinit var toolbar: Toolbar

    private var plantaId: Long = -1
    private var plantaNombre: String = ""
    private var configId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_config)

        plantaId = intent.getLongExtra("PLANTA_ID", -1)
        plantaNombre = intent.getStringExtra("PLANTA_NOMBRE") ?: "Planta"

        if (plantaId == -1L) {
            Toast.makeText(this, "Error: Planta no identificada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadConfiguration()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sliderHumidity = findViewById(R.id.sliderHumidity)
        sliderTempRange = findViewById(R.id.sliderTempRange)
        tvHumidityLabel = findViewById(R.id.tvHumidityLabel)
        tvTempRangeLabel = findViewById(R.id.tvTempRangeLabel)
        btnSaveConfig = findViewById(R.id.btnSaveConfig)
        btnDefaultConfig = findViewById(R.id.btnDefaultConfig)
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener { onBackPressed() }

        sliderHumidity.addOnChangeListener { _, value, _ ->
            tvHumidityLabel.text = "${value.toInt()}%"
        }

        sliderTempRange.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            tvTempRangeLabel.text = "${values[0].toInt()}°C - ${values[1].toInt()}°C"
        }

        btnSaveConfig.setOnClickListener {
            saveConfiguration()
        }

        btnDefaultConfig.setOnClickListener {
            sliderHumidity.value = 60f
            sliderTempRange.setValues(15f, 30f)
            Toast.makeText(this, "Valores restablecidos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadConfiguration() {
        val prefs = getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getPlantConfig("Token $token", plantaId)
                
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val config = response.body()!!.firstOrNull { it.planta == plantaId } 
                                ?: response.body()!![0]
                    
                    configId = config.id
                                
                    withContext(Dispatchers.Main) {
                        sliderHumidity.value = config.humedadObjetivo ?: 60f
                        val tMin = config.tempMin ?: 18f
                        val tMax = config.tempMax ?: 28f
                        sliderTempRange.setValues(tMin, tMax)
                        
                        tvHumidityLabel.text = "${(config.humedadObjetivo ?: 60f).toInt()}%"
                        tvTempRangeLabel.text = "${tMin.toInt()}°C - ${tMax.toInt()}°C"
                    }
                }
            } catch (e: Exception) {
                Log.e("PlantConfig", "Error al cargar config API: ${e.message}")
            }
        }
    }

    private fun saveConfiguration() {
        val humidity = sliderHumidity.value
        val tempMin = sliderTempRange.values[0]
        val tempMax = sliderTempRange.values[1]

        val prefs = getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null || configId == null) {
            Toast.makeText(this, "No se puede guardar: Configuración no cargada", Toast.LENGTH_SHORT).show()
            return
        }

        btnSaveConfig.isEnabled = false
        btnSaveConfig.text = "Guardando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = mapOf(
                    "humedad_objetivo" to humidity,
                    "temperatura_minima" to tempMin,
                    "temperatura_maxima" to tempMax
                )
                
                val response = RetrofitClient.instance.updatePlantConfig("Token $token", configId!!, data)

                withContext(Dispatchers.Main) {
                    btnSaveConfig.isEnabled = true
                    btnSaveConfig.text = "GUARDAR CONFIGURACIÓN"

                    if (response.isSuccessful) {
                        Toast.makeText(this@PlantConfigActivity, "¡Configuración sincronizada!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@PlantConfigActivity, "Error API: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PlantConfig", "Error saving config: ${e.message}")
                withContext(Dispatchers.Main) {
                    btnSaveConfig.isEnabled = true
                    btnSaveConfig.text = "GUARDAR CONFIGURACIÓN"
                    Toast.makeText(this@PlantConfigActivity, "Error de red al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
