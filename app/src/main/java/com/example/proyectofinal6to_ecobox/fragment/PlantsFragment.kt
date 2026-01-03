package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getLong("user_id", -1)

        val rvPlants = view.findViewById<RecyclerView>(R.id.rvPlants)
        etSearch = view.findViewById(R.id.etSearch)

        // Configurar RecyclerView
        rvPlants.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlantAdapter(emptyList()) { planta, datos ->
            val intent = PlantDetailActivity.createIntent(requireContext(), planta, datos)
            startActivity(intent)
        }
        rvPlants.adapter = adapter

        // Configurar Buscador
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filtrar(s.toString())
            }
        })

        // Configurar FAB
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddPlant)
        fab.setOnClickListener {
            startActivity(Intent(requireContext(), CrearPlantaActivity::class.java))
        }

        cargarPlantas(userId)
    }

    private fun cargarPlantas(userId: Long) {
        Thread {
            try {
                val plantasCompletas = PlantaDao.obtenerPlantasCompletas(userId)
                val listaParaAdapter = plantasCompletas.map { pc ->
                    PlantaConDatos(
                        planta = pc.planta,
                        ubicacion = pc.ubicacion,
                        humedadSuelo = pc.humedad,
                        temperatura = pc.temperatura,
                        luz = pc.luz,
                        nivelAgua = pc.calcularNivelAgua(), // Usar la función correcta
                        estado = pc.determinarEstadoUI(),
                        ultimoRiego = pc.ultimoRiego
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