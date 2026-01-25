package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SensoresActivity : AppCompatActivity() {

    private lateinit var sensoresRecyclerView: RecyclerView
    private lateinit var btnAddSensor: MaterialButton
    private lateinit var tvTituloSensores: TextView
    private lateinit var loadingView: View
    private lateinit var emptyStateView: View

    private lateinit var sensorAdapter: SensorAdapter
    private var sensoresList: MutableList<PlantaDao.SensorVista> = mutableListOf()

    private var plantaId: Long = 0
    private var plantaNombre: String = ""

    companion object {
        private const val EXTRA_PLANTA_ID = "planta_id"
        private const val EXTRA_PLANTA_NOMBRE = "planta_nombre"
        private const val REQUEST_CODE_AGREGAR_SENSOR = 1001

        fun newIntent(context: Context, plantaId: Long, plantaNombre: String): Intent {
            return Intent(context, SensoresActivity::class.java).apply {
                putExtra(EXTRA_PLANTA_ID, plantaId)
                putExtra(EXTRA_PLANTA_NOMBRE, plantaNombre)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensores)

        // Obtener datos de la planta
        plantaId = intent.getLongExtra(EXTRA_PLANTA_ID, 0)
        plantaNombre = intent.getStringExtra(EXTRA_PLANTA_NOMBRE) ?: ""

        Log.d("SensoresActivity", "Planta recibida - ID: $plantaId, Nombre: '$plantaNombre'")

        if (plantaId == 0L) {
            mostrarError("No se especificó la planta")
            finish()
            return
        }

        initViews()
        setupListeners()
        loadSensores()
    }

    private fun initViews() {
        sensoresRecyclerView = findViewById(R.id.rvSensores)
        btnAddSensor = findViewById(R.id.btnAddSensor)
        tvTituloSensores = findViewById(R.id.tvTituloSensores)
        loadingView = findViewById(R.id.loadingView)
        emptyStateView = findViewById(R.id.emptyStateView)

        // Configurar título
        tvTituloSensores.text = if (plantaNombre.isNotEmpty()) {
            "Sensores de $plantaNombre"
        } else {
            "Sensores Conectados"
        }

        // Configurar RecyclerView
        sensorAdapter = SensorAdapter()
        sensoresRecyclerView.layoutManager = LinearLayoutManager(this)
        sensoresRecyclerView.adapter = sensorAdapter
    }

    private fun setupListeners() {
        btnAddSensor.setOnClickListener {
            Log.d("SensoresActivity", "Abriendo AgregarSensorActivity...")
            val intent = AgregarSensorActivity.newIntent(this, plantaId, plantaNombre)
            startActivityForResult(intent, REQUEST_CODE_AGREGAR_SENSOR)
        }

        emptyStateView.findViewById<MaterialButton>(R.id.btnAgregarPrimerSensor)
            ?.setOnClickListener {
                Log.d("SensoresActivity", "Abriendo AgregarSensorActivity desde empty state...")
                val intent = AgregarSensorActivity.newIntent(this, plantaId, plantaNombre)
                startActivityForResult(intent, REQUEST_CODE_AGREGAR_SENSOR)
            }
    }

    private fun loadSensores() {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) {
            mostrarError("Sesión expirada")
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val api = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                
                // 1. Obtener sensores, tipos y estados en paralelo (o secuencial para simplicidad inicial)
                val sensorsResponse = api.getSensors("Token $token", plantaId)
                val typesResponse = api.getSensorTypes("Token $token")
                
                if (sensorsResponse.isSuccessful && typesResponse.isSuccessful) {
                    val sensors = sensorsResponse.body() ?: emptyList()
                    val types = typesResponse.body() ?: emptyList()
                    val typesMap = types.associateBy { it.id }

                    // 2. Transformar a SensorVista para mantener compatibilidad con el adapter
                    val sensoresVista = sensors.map { s ->
                        val type = typesMap[s.tipoSensor]
                        PlantaDao.SensorVista(
                            id = s.id,
                            nombre = s.nombre,
                            ubicacion = s.ubicacion ?: "Sin ubicación",
                            tipoSensor = type?.nombre ?: "Desconocido",
                            unidadMedida = type?.unidadMedida ?: "",
                            estado = if (s.activo) "Activo" else "Inactivo",
                            valor = null, // El valor se cargará dinámicamente o desde mediciones
                            ultimaLectura = null,
                            activo = s.activo,
                            plantaNombre = plantaNombre
                        )
                    }

                    showLoading(false)
                    if (sensoresVista.isEmpty()) {
                        showEmptyState()
                    } else {
                        mostrarSensores(sensoresVista)
                    }
                } else {
                    showLoading(false)
                    mostrarError("Error al sincronizar con el servidor")
                }
            } catch (e: Exception) {
                Log.e("SensoresActivity", "Error al cargar sensores Cloud: ${e.message}", e)
                showLoading(false)
                mostrarError("Error de red: ${e.message}")
            }
        }
    }

    private fun mostrarSensores(sensores: List<PlantaDao.SensorVista>) {
        sensoresList.clear()
        sensoresList.addAll(sensores)
        sensorAdapter.notifyDataSetChanged()
    }

    private fun showLoading(show: Boolean) {
        loadingView.visibility = if (show) View.VISIBLE else View.GONE
        sensoresRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun showEmptyState() {
        emptyStateView.visibility = View.VISIBLE
        sensoresRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
    }

    private fun mostrarMensaje(mensaje: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mensaje,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun mostrarError(mensaje: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mensaje,
            Snackbar.LENGTH_LONG
        ).setBackgroundTint(
            ContextCompat.getColor(this, R.color.error)
        ).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_AGREGAR_SENSOR -> {
                if (resultCode == RESULT_OK) {
                    val sensorAgregado = data?.getBooleanExtra(AgregarSensorActivity.RESULT_SENSOR_AGREGADO, false) ?: false
                    if (sensorAgregado) {
                        mostrarMensaje("✅ Sensor agregado exitosamente")
                        loadSensores()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSensores()
    }

    // Adapter para el RecyclerView
    inner class SensorAdapter : RecyclerView.Adapter<SensorAdapter.SensorViewHolder>() {

        inner class SensorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivSensorIcon: ImageView = itemView.findViewById(R.id.ivSensorIcon)
            val tvSensorStatus: TextView = itemView.findViewById(R.id.tvSensorStatus)
            val tvSensorName: TextView = itemView.findViewById(R.id.tvSensorName)
            val tvSensorLocation: TextView = itemView.findViewById(R.id.tvSensorLocation)
            val tvSensorValue: TextView = itemView.findViewById(R.id.tvSensorValue)
            val tvLastReading: TextView = itemView.findViewById(R.id.tvLastReading)
            val tvSensorId: TextView = itemView.findViewById(R.id.tvSensorId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sensor_card, parent, false)
            return SensorViewHolder(view)
        }

        override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
            val sensor = sensoresList[position]

            // Configurar icono según tipo de sensor
            val iconRes = when (sensor.tipoSensor) {
                "Temperatura" -> R.drawable.ic_thermometer
                "Humedad Suelo" -> R.drawable.ic_water_drop
                "Humedad Aire" -> R.drawable.ic_thermometer
                "Luz" -> R.drawable.ic_sun
                "pH" -> R.drawable.ic_ph
                else -> R.drawable.ic_sensor_default
            }
            holder.ivSensorIcon.setImageResource(iconRes)

            // Configurar estado del chip
            holder.tvSensorStatus.text = sensor.estado

            // Configurar color del estado
            val estadoColor = when (sensor.estado) {
                "Activo" -> Color.parseColor("#5CB85C") // Verde
                "Inactivo" -> Color.parseColor("#6B7280") // Gris
                "Mantenimiento" -> Color.parseColor("#F59E0B") // Ámbar
                "Error" -> Color.parseColor("#EF4444") // Rojo
                else -> Color.parseColor("#6B7280")
            }
            holder.tvSensorStatus.setBackgroundColor(estadoColor)

            // Configurar nombre
            holder.tvSensorName.text = sensor.nombre

            // Mostrar planta + ubicación
            val ubicacionTexto = if (sensor.plantaNombre != null && sensor.plantaNombre.isNotEmpty()) {
                "${sensor.plantaNombre} • ${sensor.ubicacion}"
            } else {
                sensor.ubicacion
            }
            holder.tvSensorLocation.text = ubicacionTexto

            // Configurar valor
            val valorTexto = if (sensor.valor != null) {
                "${sensor.valor}${sensor.unidadMedida}"
            } else {
                "Sin datos"
            }
            holder.tvSensorValue.text = valorTexto

            // Configurar color del valor según tipo de sensor
            val valueColor = when (sensor.tipoSensor) {
                "Temperatura" -> "#3B82F6" // Azul
                "Humedad Suelo", "Humedad Aire" -> "#06B6D4" // Cian
                "Luz" -> "#F59E0B" // Ámbar
                "pH" -> "#8B5CF6" // Violeta
                else -> "#374151" // Gris
            }
            holder.tvSensorValue.setTextColor(Color.parseColor(valueColor))

            // Configurar última lectura
            holder.tvLastReading.text = if (sensor.ultimaLectura != null) {
                "Última lectura: ${sensor.ultimaLectura}"
            } else {
                "Sin lecturas recientes"
            }

            // Configurar ID y Planta
            val plantaInfo = if (sensor.plantaNombre != null && sensor.plantaNombre.isNotEmpty()) {
                "ID: ${sensor.id} | Planta: ${sensor.plantaNombre}"
            } else {
                "ID: ${sensor.id}"
            }
            holder.tvSensorId.text = plantaInfo

            // Configurar click listener
            holder.itemView.setOnClickListener {
                mostrarMensaje("Detalle del sensor ${sensor.id}")
                // Aquí puedes abrir una pantalla de detalle si lo necesitas
                // abrirDetalleSensor(sensor.id)
            }
        }

        override fun getItemCount(): Int = sensoresList.size
    }
}