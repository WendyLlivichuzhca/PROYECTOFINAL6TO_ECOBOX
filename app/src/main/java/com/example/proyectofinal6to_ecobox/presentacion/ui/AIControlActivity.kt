package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.graphics.Color
import com.example.proyectofinal6to_ecobox.data.network.AiStatusResponse

class AIControlActivity : AppCompatActivity() {

    private lateinit var tvMonitStatus: TextView
    private lateinit var ivMonitStatus: ImageView
    private lateinit var tvSchedStatus: TextView
    private lateinit var ivSchedStatus: ImageView
    private lateinit var tvModelStatus: TextView
    private lateinit var ivModelStatus: ImageView
    
    private lateinit var btnStartAi: MaterialButton
    private lateinit var btnStopAi: MaterialButton
    private lateinit var btnRetrainAi: MaterialButton
    
    private lateinit var loadingIndicator: LinearProgressIndicator
    private lateinit var toolbar: Toolbar

    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_control)

        val prefs = getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

        if (authToken == null) {
            Toast.makeText(this, "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadAiStatus()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        tvMonitStatus = findViewById(R.id.tvMonitStatus)
        ivMonitStatus = findViewById(R.id.ivMonitStatus)
        tvSchedStatus = findViewById(R.id.tvSchedStatus)
        ivSchedStatus = findViewById(R.id.ivSchedStatus)
        tvModelStatus = findViewById(R.id.tvModelStatus)
        ivModelStatus = findViewById(R.id.ivModelStatus)

        btnStartAi = findViewById(R.id.btnStartAi)
        btnStopAi = findViewById(R.id.btnStopAi)
        btnRetrainAi = findViewById(R.id.btnRetrainAi)
        
        loadingIndicator = findViewById(R.id.loadingIndicator)
    }

    private fun setupListeners() {
        btnStartAi.setOnClickListener { performAiAction("start") }
        btnStopAi.setOnClickListener { performAiAction("stop") }
        btnRetrainAi.setOnClickListener { performAiAction("train_all") }
    }

    private fun loadAiStatus() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAiStatus("Token $authToken")
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    updateStatusUI(status)
                } else {
                    Log.e("AIControl", "Error cargando estado: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AIControl", "Error red: ${e.message}")
            }
        }
    }

    private fun updateStatusUI(status: AiStatusResponse) {
        val isActive = status.status == "active"
        
        // Monitoreo (Usamos status como proxy)
        tvMonitStatus.text = if (isActive) "ACTIVO" else "INACTIVO"
        val monitorColor = if (isActive) "#10B981" else "#EF4444"
        tvMonitStatus.setTextColor(Color.parseColor(monitorColor))
        ivMonitStatus.imageTintList = ColorStateList.valueOf(Color.parseColor(monitorColor))
        
        // Botones de control (Deshabilitados si no hay endpoint en el backend)
        btnStartAi.isEnabled = !isActive
        btnStopAi.isEnabled = isActive

        // Scheduler (Buscamos en sistema o fallback)
        val schedulerActive = (status.sistema?.get("sensores_activos") as? Boolean) ?: isActive
        tvSchedStatus.text = if (schedulerActive) "ACTIVO" else "INACTIVO"
        val schedColor = if (schedulerActive) "#10B981" else "#EF4444"
        tvSchedStatus.setTextColor(Color.parseColor(schedColor))
        ivSchedStatus.imageTintList = ColorStateList.valueOf(Color.parseColor(schedColor))

        // Modelos (Usamos el String directo que envía el backend)
        tvModelStatus.text = status.modelosEntrenados
        val modelsAvailable = status.modelosActivos > 0
        val modelColor = if (modelsAvailable) "#6366F1" else "#F59E0B"
        tvModelStatus.setTextColor(Color.parseColor(modelColor))
        ivModelStatus.imageTintList = ColorStateList.valueOf(Color.parseColor(modelColor))
    }

    private fun performAiAction(action: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.controlAi("Token $authToken", mapOf("action" to action))
                if (response.isSuccessful) {
                    Toast.makeText(this@AIControlActivity, response.body()?.message ?: "Acción completada", Toast.LENGTH_SHORT).show()
                    loadAiStatus() // Refrescar estado despué de la acción
                } else {
                    Toast.makeText(this@AIControlActivity, "Error en la acción: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AIControlActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnStartAi.isEnabled = !isLoading && tvMonitStatus.text != "ACTIVO"
        btnStopAi.isEnabled = !isLoading && tvMonitStatus.text == "ACTIVO"
        btnRetrainAi.isEnabled = !isLoading
    }
}
