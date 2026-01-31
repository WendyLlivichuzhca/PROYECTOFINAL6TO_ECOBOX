package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.PlantResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class DataTabFragment : Fragment() {
    
    private lateinit var tvTotalPlantas: TextView
    private lateinit var tvPlantasNecesitanAgua: TextView
    private lateinit var tvTotalSensores: TextView
    private lateinit var tvDiasDatos: TextView
    private lateinit var rvDataPlants: RecyclerView
    
    private var plants: List<PlantResponse> = emptyList()
    private var onConsultarClick: ((PlantResponse) -> Unit)? = null
    
    companion object {
        private const val ARG_PLANTS_JSON = "plants_json"
        
        fun newInstance(plantsJson: String): DataTabFragment {
            val fragment = DataTabFragment()
            val args = Bundle()
            args.putString(ARG_PLANTS_JSON, plantsJson)
            fragment.arguments = args
            return fragment
        }
        
        fun newInstance(): DataTabFragment = DataTabFragment()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_PLANTS_JSON)?.let { json ->
            val type = object : TypeToken<List<PlantResponse>>() {}.type
            plants = Gson().fromJson(json, type)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chatbot_data, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        updateStatistics()
        setupRecyclerView()
    }
    
    private fun initViews(view: View) {
        tvTotalPlantas = view.findViewById(R.id.tvTotalPlantas)
        tvPlantasNecesitanAgua = view.findViewById(R.id.tvPlantasNecesitanAgua)
        tvTotalSensores = view.findViewById(R.id.tvTotalSensores)
        tvDiasDatos = view.findViewById(R.id.tvDiasDatos)
        rvDataPlants = view.findViewById(R.id.rvDataPlants)
    }
    
    private fun updateStatistics() {
        // Total de plantas
        tvTotalPlantas.text = plants.size.toString()
        
        // Plantas que necesitan agua
        val plantasNecesitanAgua = plants.count { 
            it.estado_salud?.lowercase()?.contains("agua") == true
        }
        tvPlantasNecesitanAgua.text = plantasNecesitanAgua.toString()
        
        // Total de sensores (4 por planta: temp, humedad suelo, humedad aire, luz)
        tvTotalSensores.text = (plants.size * 4).toString()
        
        // Días de datos (hardcoded por ahora, puede calcularse desde fechaCreacion)
        tvDiasDatos.text = "90"
    }
    
    private fun setupRecyclerView() {
        val adapter = DataPlantsAdapter(plants) { plant ->
            onConsultarClick?.invoke(plant)
        }
        
        rvDataPlants.layoutManager = LinearLayoutManager(context)
        rvDataPlants.adapter = adapter
    }
    
    fun setOnConsultarClickListener(listener: (PlantResponse) -> Unit) {
        onConsultarClick = listener
    }
    
    fun updatePlants(newPlants: List<PlantResponse>) {
        plants = newPlants
        if (::tvTotalPlantas.isInitialized) {
            updateStatistics()
            setupRecyclerView()
        }
    }
}

// Adapter para la lista de plantas
class DataPlantsAdapter(
    private val plants: List<PlantResponse>,
    private val onConsultarClick: (PlantResponse) -> Unit
) : RecyclerView.Adapter<DataPlantsAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivStatus: ImageView = view.findViewById(R.id.ivPlantStatus)
        val tvName: TextView = view.findViewById(R.id.tvPlantName)
        val tvSpecies: TextView = view.findViewById(R.id.tvPlantSpecies)
        val btnConsultar: Button = view.findViewById(R.id.btnConsultar)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_plant, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plant = plants[position]
        
        holder.tvName.text = plant.nombre
        holder.tvSpecies.text = plant.especie ?: "Especie desconocida"
        
        // Icono según estado
        val estado = plant.estado_salud?.lowercase() ?: "normal"
        when {
            estado.contains("saludable") || estado.contains("normal") -> {
                holder.ivStatus.setImageResource(R.drawable.ic_check_circle)
                holder.ivStatus.setColorFilter(Color.parseColor("#4CAF50"))
            }
            estado.contains("necesita") && estado.contains("agua") -> {
                holder.ivStatus.setImageResource(R.drawable.ic_leaf)
                holder.ivStatus.setColorFilter(Color.parseColor("#2196F3"))
            }
            else -> {
                holder.ivStatus.setImageResource(R.drawable.ic_leaf)
                holder.ivStatus.setColorFilter(Color.parseColor("#FF9800"))
            }
        }
        
        holder.btnConsultar.setOnClickListener {
            onConsultarClick(plant)
        }
    }
    
    override fun getItemCount() = plants.size
}
