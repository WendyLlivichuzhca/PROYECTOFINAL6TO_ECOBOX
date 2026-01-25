package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Response

class AgregarSensorActivity : AppCompatActivity() {

    private lateinit var tilNombre: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var tilUbicacion: TextInputLayout
    private lateinit var etUbicacion: TextInputEditText
    private lateinit var tilTipoSensor: TextInputLayout
    private lateinit var spinnerTipoSensor: MaterialAutoCompleteTextView
    private lateinit var btnGuardar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var progressBar: View

    private var plantaId: Long = 0
    private var plantaNombre: String = ""

    private val tiposSensorList = mutableListOf<SensorTypeResponse>()
    private val tipoSensorMap = mutableMapOf<String, Int>()

    companion object {
        const val EXTRA_PLANTA_ID = "planta_id"
        const val EXTRA_PLANTA_NOMBRE = "planta_nombre"
        const val RESULT_SENSOR_AGREGADO = "SENSOR_AGREGADO"

        fun newIntent(context: Context, plantaId: Long, plantaNombre: String): Intent {
            return Intent(context, AgregarSensorActivity::class.java).apply {
                putExtra(EXTRA_PLANTA_ID, plantaId)
                putExtra(EXTRA_PLANTA_NOMBRE, plantaNombre)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_sensor)

        plantaId = intent.getLongExtra(EXTRA_PLANTA_ID, 0)
        plantaNombre = intent.getStringExtra(EXTRA_PLANTA_NOMBRE) ?: ""

        if (plantaId == 0L) {
            Toast.makeText(this, "Error: Planta no especificada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        cargarTiposSensoresCloud()
    }

    private fun initViews() {
        tilNombre = findViewById(R.id.tilNombre)
        etNombre = findViewById(R.id.etNombre)
        tilUbicacion = findViewById(R.id.tilUbicacion)
        etUbicacion = findViewById(R.id.etUbicacion)
        tilTipoSensor = findViewById(R.id.tilTipoSensor)
        spinnerTipoSensor = findViewById(R.id.spinnerTipoSensor)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnCancelar = findViewById(R.id.btnCancelar)
        progressBar = findViewById(R.id.progressBar)

        supportActionBar?.title = "Agregar Sensor a $plantaNombre"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupListeners()
        setupTextWatchers()
    }

    private fun cargarTiposSensoresCloud() {
        showLoading(true)
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getSensorTypes("Token $token")
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val tipos = response.body()!!
                    
                    tiposSensorList.clear()
                    tipoSensorMap.clear()
                    tiposSensorList.addAll(tipos)

                    tipos.forEach { tipo ->
                        tipoSensorMap[tipo.nombre] = tipo.id
                    }

                    val nombresTipos = tipos.map { it.nombre }
                    setupSpinner(nombresTipos)
                } else {
                    mostrarError("Error al cargar tipos de sensores")
                    setupSpinner(listOf("Error al cargar"))
                }
            } catch (e: Exception) {
                showLoading(false)
                Log.e("AgregarSensor", "Error red: ${e.message}")
                mostrarError("Error de conexión")
            }
        }
    }

    private fun setupSpinner(tipos: List<String>) {
        val adapterSp = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tipos)
        spinnerTipoSensor.setAdapter(adapterSp)
        spinnerTipoSensor.threshold = 1
        spinnerTipoSensor.setText("Seleccione un tipo", false)

        spinnerTipoSensor.setOnClickListener { spinnerTipoSensor.showDropDown() }
        spinnerTipoSensor.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) spinnerTipoSensor.showDropDown()
            false
        }
        spinnerTipoSensor.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
            tilTipoSensor.error = null
        }
    }

    private fun setupListeners() {
        btnGuardar.setOnClickListener { guardarSensorCloud() }
        btnCancelar.setOnClickListener { onBackPressed() }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilNombre.error = null
                tilUbicacion.error = null
            }
        }
        etNombre.addTextChangedListener(watcher)
        etUbicacion.addTextChangedListener(watcher)
    }

    private fun guardarSensorCloud() {
        if (!validarFormulario()) return

        showLoading(true)
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        lifecycleScope.launch {
            try {
                val nombre = etNombre.text.toString().trim()
                val ubicacion = etUbicacion.text.toString().trim()
                val tipoSeleccionado = spinnerTipoSensor.text.toString()
                val tipoSensorId = tipoSensorMap[tipoSeleccionado]!!

                val requestData = mapOf(
                    "nombre" to nombre,
                    "ubicacion" to ubicacion,
                    "tipo_sensor" to tipoSensorId,
                    "planta" to plantaId,
                    "activo" to true,
                    "estado_sensor" to 1 // Activo
                )

                val response = RetrofitClient.instance.createSensor("Token $token", requestData)
                showLoading(false)

                if (response.isSuccessful) {
                    mostrarExito()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AgregarSensor", "Error API: $errorBody")
                    mostrarError("Error al guardar: Verifique los datos")
                }
            } catch (e: Exception) {
                showLoading(false)
                Log.e("AgregarSensor", "Error: ${e.message}")
                mostrarError("Error de red")
            }
        }
    }

    private fun validarFormulario(): Boolean {
        var esValido = true
        val nombre = etNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            tilNombre.error = "Ingrese un nombre"
            esValido = false
        }
        val tipoSeleccionado = spinnerTipoSensor.text.toString()
        if (tipoSeleccionado == "Seleccione un tipo" || !tipoSensorMap.containsKey(tipoSeleccionado)) {
            tilTipoSensor.error = "Seleccione un tipo"
            esValido = false
        }
        return esValido
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnGuardar.isEnabled = !show
        btnCancelar.isEnabled = !show
        if (show) btnGuardar.text = "Guardando..." else btnGuardar.text = "Guardar Sensor"
    }

    private fun mostrarExito() {
        Toast.makeText(this, "✅ Sensor agregado exitosamente", Toast.LENGTH_LONG).show()
        val resultIntent = Intent().apply {
            putExtra(RESULT_SENSOR_AGREGADO, true)
            putExtra(EXTRA_PLANTA_ID, plantaId)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, "❌ $mensaje", Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (!btnGuardar.isEnabled) return
        val nombre = etNombre.text.toString().trim()
        if (nombre.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("¿Descartar cambios?")
                .setMessage("¿Estás seguro de que quieres salir?")
                .setPositiveButton("Salir") { _, _ -> super.onBackPressed() }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
