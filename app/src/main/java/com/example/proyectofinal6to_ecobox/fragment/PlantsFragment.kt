package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.PlantAdapter
import com.example.proyectofinal6to_ecobox.data.adapter.PlantAdapter.PlantaConDatos
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
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

        // Configurar RecyclerView - CAMBIA ESTAS LÍNEAS:
        rvPlants.layoutManager = LinearLayoutManager(requireContext())
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
        Thread {
            try {
                val plantasCompletas = PlantaDao.obtenerPlantasCompletas(userId)
                val listaParaAdapter = plantasCompletas.map { pc ->
                    // NUEVO: Obtener número de sensores para cada planta
                    val sensorCount = PlantaDao.obtenerCantidadSensoresPorPlanta(pc.planta.id)
                    PlantaConDatos(
                        planta = pc.planta,
                        ubicacion = pc.ubicacion,
                        humedadSuelo = pc.humedad,
                        temperatura = pc.temperatura,
                        luz = pc.luz,
                        nivelAgua = pc.calcularNivelAgua(),
                        estado = pc.determinarEstadoUI(),
                        ultimoRiego = pc.ultimoRiego,
                        sensorCount = sensorCount

                    )
                }
                activity?.runOnUiThread {
                    adapter.updateList(listaParaAdapter)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        cargarPlantas(prefs.getLong("user_id", -1))
    }
}