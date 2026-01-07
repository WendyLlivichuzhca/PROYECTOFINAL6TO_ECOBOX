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
import com.example.proyectofinal6to_ecobox.data.dao.SensorDao
import com.example.proyectofinal6to_ecobox.data.model.Sensor
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgregarSensorActivity : AppCompatActivity() {

    // Vistas
    private lateinit var tilNombre: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var tilUbicacion: TextInputLayout
    private lateinit var etUbicacion: TextInputEditText
    private lateinit var tilTipoSensor: TextInputLayout
    private lateinit var spinnerTipoSensor: MaterialAutoCompleteTextView
    private lateinit var btnGuardar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var progressBar: View

    // Datos
    private var plantaId: Long = 0
    private var plantaNombre: String = ""

    // Lista de tipos de sensores desde BD
    private val tiposSensorList = mutableListOf<Pair<Long, String>>()
    private val tipoSensorMap = mutableMapOf<String, Long>()

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

        // Obtener datos de la planta
        plantaId = intent.getLongExtra(EXTRA_PLANTA_ID, 0)
        plantaNombre = intent.getStringExtra(EXTRA_PLANTA_NOMBRE) ?: ""

        Log.d("AgregarSensor", "Recibido - plantaId: $plantaId, plantaNombre: '$plantaNombre'")

        if (plantaId == 0L) {
            Toast.makeText(this, "Error: Planta no especificada", Toast.LENGTH_SHORT).show()
            Log.e("AgregarSensor", "PlantaId es 0, cerrando actividad")
            finish()
            return
        }

        initViews()
        cargarTiposSensores()
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

        // Configurar título
        supportActionBar?.title = "Agregar Sensor a $plantaNombre"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupListeners()
        setupTextWatchers()
    }

    private fun cargarTiposSensores() {
        showLoading(true)
        Log.d("AgregarSensor", "Cargando tipos de sensores desde BD...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener tipos de sensores desde la base de datos
                val tipos = SensorDao.obtenerTiposSensores()
                Log.d("AgregarSensor", "Tipos obtenidos de BD: ${tipos.size}")

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (tipos.isEmpty()) {
                        Log.e("AgregarSensor", "La lista de tipos está vacía")
                        mostrarError("No se encontraron tipos de sensores")
                        // Mostrar opción por defecto
                        setupSpinner(listOf("No hay tipos disponibles"))
                        return@withContext
                    }

                    // Guardar tipos
                    tiposSensorList.clear()
                    tipoSensorMap.clear()
                    tiposSensorList.addAll(tipos)

                    tipos.forEach { (id, nombre) ->
                        Log.d("AgregarSensor", "Tipo: $nombre (ID: $id)")
                        tipoSensorMap[nombre] = id
                    }

                    // Configurar spinner
                    val nombresTipos = tipos.map { it.second }
                    Log.d("AgregarSensor", "Configurando spinner con: $nombresTipos")
                    setupSpinner(nombresTipos)
                }
            } catch (e: Exception) {
                Log.e("AgregarSensor", "Error cargando tipos de sensores: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    mostrarError("Error al cargar tipos de sensores: ${e.message}")
                    // Mostrar opción por defecto
                    setupSpinner(listOf("Error al cargar tipos"))
                }
            }
        }
    }

    private fun setupSpinner(tipos: List<String>) {
        // Usar el layout correcto para MaterialAutoCompleteTextView

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tipos)

        spinnerTipoSensor.setAdapter(adapter)

        // IMPORTANTE: Configurar el threshold (número de caracteres para mostrar sugerencias)
        spinnerTipoSensor.threshold = 1

        // Establecer texto por defecto
        spinnerTipoSensor.setText("Seleccione un tipo", false)

        // Hacer que el dropdown aparezca al hacer clic
        spinnerTipoSensor.setOnClickListener {
            spinnerTipoSensor.showDropDown()
        }

        // También cuando se toca cualquier parte del campo
        spinnerTipoSensor.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                spinnerTipoSensor.showDropDown()
            }
            false
        }

        // Listener para validación
        spinnerTipoSensor.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                tilTipoSensor.error = null
            }

        Log.d("AgregarSensor", "Spinner configurado con ${tipos.size} elementos: $tipos")
    }

    private fun setupListeners() {
        btnGuardar.setOnClickListener {
            guardarSensor()
        }

        btnCancelar.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupTextWatchers() {
        etNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilNombre.error = null
            }
        })

        etUbicacion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilUbicacion.error = null
            }
        })
    }

    private fun guardarSensor() {
        if (!validarFormulario()) {
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nombre = etNombre.text.toString().trim()
                val ubicacion = etUbicacion.text.toString().trim()
                val tipoSeleccionado = spinnerTipoSensor.text.toString()

                // Obtener ID del tipo seleccionado
                val tipoSensorId = tipoSensorMap[tipoSeleccionado] ?: run {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        tilTipoSensor.error = "Tipo de sensor no válido"
                    }
                    return@launch
                }

                // Crear el sensor
                val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date())

                val sensor = Sensor(
                    0,                 // id
                    nombre,           // nombre
                    ubicacion,        // ubicacion
                    fechaActual,      // fechaInstalacion
                    true,             // activo
                    1,                // estadoSensorId (Activo por defecto)
                    plantaId,         // plantaId
                    tipoSensorId      // tipoSensorId
                )

                // Verificar si ya existe un sensor con el mismo nombre en esta planta
                val yaExiste = SensorDao.existeSensorConNombre(nombre, plantaId)

                withContext(Dispatchers.Main) {
                    if (yaExiste) {
                        showLoading(false)
                        tilNombre.error = "Ya existe un sensor con este nombre en esta planta"
                        return@withContext
                    }
                }

                // Guardar en la base de datos
                val resultado = SensorDao.insertarSensor(sensor)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (resultado) {
                        mostrarExito()
                    } else {
                        mostrarError("Error al guardar el sensor")
                    }
                }
            } catch (e: Exception) {
                Log.e("AgregarSensor", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    mostrarError("Error: ${e.message}")
                }
            }
        }
    }

    private fun validarFormulario(): Boolean {
        var esValido = true

        // Validar nombre
        val nombre = etNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            tilNombre.error = "Ingrese un nombre para el sensor"
            esValido = false
        } else if (nombre.length < 3) {
            tilNombre.error = "El nombre debe tener al menos 3 caracteres"
            esValido = false
        }

        // Validar ubicación
        val ubicacion = etUbicacion.text.toString().trim()
        if (ubicacion.isEmpty()) {
            tilUbicacion.error = "Ingrese la ubicación del sensor"
            esValido = false
        }

        // Validar tipo de sensor
        val tipoSeleccionado = spinnerTipoSensor.text.toString()
        if (tipoSeleccionado == "Seleccione un tipo" || !tipoSensorMap.containsKey(tipoSeleccionado)) {
            tilTipoSensor.error = "Seleccione un tipo de sensor"
            esValido = false
        }

        return esValido
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnGuardar.isEnabled = !show
        btnCancelar.isEnabled = !show
        spinnerTipoSensor.isEnabled = !show

        if (show) {
            btnGuardar.text = "Guardando..."
        } else {
            btnGuardar.text = "Guardar Sensor"
        }
    }

    private fun mostrarExito() {
        Toast.makeText(
            this,
            "✅ Sensor agregado exitosamente",
            Toast.LENGTH_LONG
        ).show()

        // Crear intent de resultado
        val resultIntent = Intent().apply {
            putExtra(RESULT_SENSOR_AGREGADO, true)
            putExtra(EXTRA_PLANTA_ID, plantaId)
        }
        setResult(RESULT_OK, resultIntent)

        // Cerrar la actividad
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
        if (!btnGuardar.isEnabled) return // Si está cargando, no permitir salir

        val nombre = etNombre.text.toString().trim()
        val ubicacion = etUbicacion.text.toString().trim()

        if (nombre.isNotEmpty() || ubicacion.isNotEmpty()) {
            mostrarDialogoConfirmacionSalida()
        } else {
            super.onBackPressed()
        }
    }

    private fun mostrarDialogoConfirmacionSalida() {
        AlertDialog.Builder(this)
            .setTitle("¿Descartar cambios?")
            .setMessage("Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?")
            .setPositiveButton("Salir") { dialog, _ ->
                dialog.dismiss()
                super.onBackPressed()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}