package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.data.network.*
import com.example.proyectofinal6to_ecobox.R
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import com.example.proyectofinal6to_ecobox.data.adapter.PlantAdapter
import com.example.proyectofinal6to_ecobox.data.adapter.PlantAdapter.PlantaConDatos
import com.example.proyectofinal6to_ecobox.presentacion.ui.CrearPlantaActivity
import com.example.proyectofinal6to_ecobox.presentacion.ui.PlantDetailActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlantsFragment : Fragment(R.layout.fragment_plants) {

    private lateinit var adapter: PlantAdapter
    private lateinit var etSearch: EditText

    // Agrega estos dos launchers al inicio de la clase
    private val plantDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Cuando regresamos de PlantDetailActivity
            result.data?.let { data ->
                val plantaEliminada = data.getBooleanExtra("PLANTA_ELIMINADA", false)
                val plantaEditada = data.getBooleanExtra("PLANTA_EDITADA", false)

                if (plantaEliminada || plantaEditada) {
                    // Recargar la lista
                    val prefs =
                        requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
                    val userId = prefs.getLong("user_id", -1)
                    cargarPlantas(userId)

                    // Mostrar mensaje
                    if (plantaEliminada) {
                        val plantName = data.getStringExtra("PLANTA_NOMBRE_ELIMINADA") ?: ""
                        val mensaje = if (plantName.isNotEmpty()) {
                            "✅ Planta '$plantName' eliminada"
                        } else {
                            "✅ Planta eliminada"
                        }
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                    } else if (plantaEditada) {
                        Toast.makeText(requireContext(), "✅ Planta actualizada", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    private val crearPlantaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Recargar cuando se crea una planta
            val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getLong("user_id", -1)
            cargarPlantas(userId)

            Toast.makeText(requireContext(), "✅ Nueva planta creada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getLong("user_id", -1)

        val rvPlants = view.findViewById<RecyclerView>(R.id.rvPlants)
        etSearch = view.findViewById(R.id.etSearch)

        // Configurar RecyclerView (Grid de 2 columnas estilo Web)
        rvPlants.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
        adapter = PlantAdapter(emptyList()) { planta, datos ->
            // ANTES: startActivity(intent)
            // DESPUÉS: plantDetailLauncher.launch(intent)
            val intent = PlantDetailActivity.createIntent(requireContext(), planta, datos)
            plantDetailLauncher.launch(intent) // ← CAMBIO IMPORTANTE
        }
        rvPlants.adapter = adapter

        // Configurar Buscador (igual)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filtrar(s.toString())
            }
        })

        // Configurar FAB - CAMBIA ESTAS LÍNEAS:
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddPlant)
        fab.setOnClickListener {
            crearPlantaLauncher.launch(Intent(requireContext(), CrearPlantaActivity::class.java))
        }

        cargarPlantas(userId)
    }

    private fun cargarPlantas(userId: Long) {
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        
        if (token == null) {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        // Usar coroutines en lugar de Thread
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                val response = api.getMyPlants("Token $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val plantasBase = response.body()!!
                    
                    // Obtener sensores igual que la web: directamente por planta
                    val deferreds = plantasBase.map { p: PlantResponse ->
                        async {
                            var hum: Float? = null
                            var temp: Float? = null
                            
                            try {
                                // Obtener sensores de esta planta
                                val sensorsResponse = api.getSensors("Token $token", p.id)
                                if (sensorsResponse.isSuccessful && sensorsResponse.body() != null) {
                                    val sensores = sensorsResponse.body()!!
                                    
                                    // Buscar sensor de humedad (tipo_sensor = 2, igual que la web)
                                    val sensorHumedad = sensores.find { it.tipoSensor == 2 }
                                    if (sensorHumedad != null) {
                                        // Obtener última medición
                                        val medicionResponse = api.getSensorMeasurements(
                                            "Token $token", 
                                            sensorHumedad.id, 
                                            limit = 1
                                        )
                                        if (medicionResponse.isSuccessful && medicionResponse.body() != null) {
                                            val mediciones = medicionResponse.body()!!
                                            if (mediciones.isNotEmpty()) {
                                                hum = mediciones[0].valor
                                            }
                                        }
                                    }
                                    
                                    // Buscar sensor de temperatura (tipo_sensor = 1, igual que la web)
                                    val sensorTemp = sensores.find { it.tipoSensor == 1 }
                                    if (sensorTemp != null) {
                                        // Obtener última medición
                                        val medicionResponse = api.getSensorMeasurements(
                                            "Token $token", 
                                            sensorTemp.id, 
                                            limit = 1
                                        )
                                        if (medicionResponse.isSuccessful && medicionResponse.body() != null) {
                                            val mediciones = medicionResponse.body()!!
                                            if (mediciones.isNotEmpty()) {
                                                temp = mediciones[0].valor
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("PlantsFragment", "Error obteniendo sensores para planta ${p.id}: ${e.message}")
                            }

                            PlantaConDatos(
                                planta = com.example.proyectofinal6to_ecobox.data.model.Planta(
                                    p.id, p.nombre, p.especie ?: "", p.fecha_plantacion ?: "", 
                                    p.descripcion ?: "", p.familia
                                ).apply {
                                    setFoto(p.imagen_url ?: "")
                                    setEstado(p.estado_salud ?: "Normal")
                                },
                                ubicacion = p.familia_nombre ?: "Mi Jardín",
                                humedadSuelo = hum,
                                temperatura = temp,
                                luz = 0f,
                                nivelAgua = if (p.necesita_agua == true) 25 else 85,
                                estado = p.estado_salud ?: "Normal",
                                ultimoRiego = p.ultima_medicion ?: "Sin datos",
                                sensorCount = 0,
                                aspecto = p.aspecto
                            )
                        }
                    }
                    
                    val listaParaAdapter = deferreds.awaitAll()
                    adapter.updateList(listaParaAdapter)
                } else {
                    Toast.makeText(requireContext(), "Error cargando plantas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        cargarPlantas(prefs.getLong("user_id", -1))
    }
}