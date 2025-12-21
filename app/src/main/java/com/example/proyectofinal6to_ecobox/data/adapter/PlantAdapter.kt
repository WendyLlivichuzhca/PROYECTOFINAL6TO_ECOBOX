package com.example.proyectofinal6to_ecobox.data.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Planta

class PlantAdapter(
    private var plantasData: List<PlantaConDatos>,
    private val onPlantClick: (Planta, Map<String, Any>) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    // Lista de respaldo para el buscador
    private var listaCompleta: List<PlantaConDatos> = plantasData

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // IDs actualizados según tu XML
        val tvName: TextView = view.findViewById(R.id.tvPlantName)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge) // El badge "Saludable"

        // Sensores
        val tvHumidity: TextView = view.findViewById(R.id.tvHumidity)
        val tvLight: TextView = view.findViewById(R.id.tvLight)
        val tvTemp: TextView = view.findViewById(R.id.tvTemp)

        // Agua
        val tvWaterPercent: TextView = view.findViewById(R.id.tvWaterPercent)
        val progressWater: ProgressBar = view.findViewById(R.id.progressWater)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val item = plantasData[position]
        val context = holder.itemView.context

        // 1. Asignar textos básicos
        holder.tvName.text = item.planta.nombre
        holder.tvLocation.text = item.ubicacion

        // 2. Asignar valores de sensores
        holder.tvHumidity.text = "${item.humedadSuelo.toInt()}%"
        holder.tvLight.text = "${item.luz.toInt()}%" // Asumiendo porcentaje, si es lux cambia el símbolo
        holder.tvTemp.text = "${item.temperatura.toInt()}°C"

        // 3. Configurar Barra de Agua
        holder.tvWaterPercent.text = "${item.nivelAgua}%"
        holder.progressWater.progress = item.nivelAgua

        // --- LÓGICA VISUAL (COLORES) ---

        // A. Configuración del Badge de Estado (Saludable/Cuidado/Crítico)
        when (item.estado) {
            "healthy" -> {
                holder.tvStatusBadge.text = "Saludable"
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_healthy)) // Verde oscuro
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5E9")) // Verde muy claro
            }
            "warning" -> {
                holder.tvStatusBadge.text = "Revisar"
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_warning)) // Naranja/Amarillo oscuro
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF8E1")) // Amarillo muy claro
            }
            "critical" -> {
                holder.tvStatusBadge.text = "Crítico"
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_critical)) // Rojo
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFEBEE")) // Rojo muy claro
            }
        }

        // B. Color de la Barra de Progreso (Agua)
        val colorBarra = when {
            item.nivelAgua > 50 -> ContextCompat.getColor(context, R.color.eco_primary) // Verde
            item.nivelAgua > 20 -> ContextCompat.getColor(context, R.color.status_warning) // Amarillo
            else -> ContextCompat.getColor(context, R.color.status_critical) // Rojo
        }
        holder.progressWater.progressTintList = ColorStateList.valueOf(colorBarra)
        holder.tvWaterPercent.setTextColor(colorBarra) // El texto del % también cambia de color

        // 4. Click Listener
        holder.itemView.setOnClickListener {
            val datos = mapOf(
                "ubicacion" to item.ubicacion,
                "humedad" to item.humedadSuelo,
                "temperatura" to item.temperatura,
                "luz" to item.luz,
                "estado" to item.estado,
                "ultimoRiego" to item.ultimoRiego
            )
            onPlantClick(item.planta, datos)
        }
    }

    override fun getItemCount() = plantasData.size

    // Funciones para el buscador
    fun updateList(newData: List<PlantaConDatos>) {
        this.plantasData = newData
        this.listaCompleta = newData
        notifyDataSetChanged()
    }

    fun filtrar(texto: String) {
        val busqueda = texto.lowercase().trim()
        if (busqueda.isEmpty()) {
            plantasData = listaCompleta
        } else {
            plantasData = listaCompleta.filter { item ->
                item.planta.nombre.lowercase().contains(busqueda) ||
                        item.ubicacion.lowercase().contains(busqueda)
            }
        }
        notifyDataSetChanged()
    }

    // Clase auxiliar para la UI (antes estaba dentro de MainActivity)
    data class PlantaConDatos(
        val planta: Planta,
        val ubicacion: String,
        val humedadSuelo: Float,
        val temperatura: Float,
        val luz: Float,
        val nivelAgua: Int,
        val estado: String,
        val ultimoRiego: String
    )
}