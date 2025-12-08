package com.example.proyectofinal6to_ecobox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.data.model.Planta

class PlantAdapter(
    private var plants: List<Planta>,
    private val onPlantClick: (Planta) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPlantName)
        val tvSpecies: TextView = view.findViewById(R.id.tvPlantSpecies)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusBadge)
        // val icon: ImageView = view.findViewById(R.id.ivPlantIcon) // Si quisieras cambiar el icono dinámicamente
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]

        // Asignar datos reales
        holder.tvName.text = plant.getNombre()
        holder.tvSpecies.text = plant.getEspecie()

        // Lógica visual de estado (Simulada por ahora)
        // Aquí podrías poner: if (plant.tienePlagas) ...
        val isHealthy = true

        if (isHealthy) {
            holder.tvStatus.text = "Saludable"
            holder.tvStatus.background.setTint(ContextCompat.getColor(holder.itemView.context, R.color.status_healthy))
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
        } else {
            holder.tvStatus.text = "Crítico"
            holder.tvStatus.background.setTint(ContextCompat.getColor(holder.itemView.context, R.color.status_critical))
        }

        holder.itemView.setOnClickListener { onPlantClick(plant) }
    }

    override fun getItemCount() = plants.size

    fun updateList(newPlants: List<Planta>) {
        plants = newPlants
        notifyDataSetChanged()
    }
}