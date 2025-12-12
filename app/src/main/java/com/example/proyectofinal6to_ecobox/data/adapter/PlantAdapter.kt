package com.example.proyectofinal6to_ecobox.data.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.example.proyectofinal6to_ecobox.presentacion.ui.MainActivity.PlantaConDatos

class PlantAdapter(
    private var plantasData: List<PlantaConDatos>,
    private val onPlantClick: (Planta, Map<String, Any>) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPlantName)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvHumidity: TextView = view.findViewById(R.id.tvHumidity)
        val tvLight: TextView = view.findViewById(R.id.tvLight)
        val tvTemp: TextView = view.findViewById(R.id.tvTemp)
        val tvWaterLevel: TextView = view.findViewById(R.id.tvWaterLevelText)
        val progressWater: ProgressBar = view.findViewById(R.id.progressWater)
        val viewStatusDot: View = view.findViewById(R.id.viewStatusDot)
        val tvLastWatered: TextView = view.findViewById(R.id.tvLastWatered)
        val ivHumIcon: ImageView = view.findViewById(R.id.ivHumIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val item = plantasData[position]
        val planta = item.planta
        val context = holder.itemView.context

        // Asignar textos con datos reales
        holder.tvName.text = planta.nombre
        holder.tvLocation.text = item.ubicacion
        holder.tvHumidity.text = "${item.humedadSuelo.toInt()}%"
        holder.tvLight.text = "${item.luz.toInt()} lux"
        holder.tvTemp.text = "${item.temperatura.toInt()}°C"
        holder.tvWaterLevel.text = "${item.nivelAgua}%"
        holder.tvLastWatered.text = item.ultimoRiego

        // Barra de progreso
        holder.progressWater.progress = item.nivelAgua

        // --- COLORES DINÁMICOS ---

        // Color Estado (Punto)
        val colorEstado = when(item.estado) {
            "healthy" -> ContextCompat.getColor(context, R.color.status_healthy)
            "warning" -> ContextCompat.getColor(context, R.color.status_warning)
            "critical" -> ContextCompat.getColor(context, R.color.status_critical)
            else -> Color.GRAY
        }
        holder.viewStatusDot.backgroundTintList = ColorStateList.valueOf(colorEstado)

        // Color Humedad (Icono y texto)
        val colorHum = if (item.humedadSuelo > 40f)
            ContextCompat.getColor(context, R.color.eco_primary)
        else
            ContextCompat.getColor(context, R.color.status_critical)

        holder.tvHumidity.setTextColor(colorHum)
        holder.ivHumIcon.imageTintList = ColorStateList.valueOf(colorHum)

        // Color Barra de Agua
        val colorBarra = when {
            item.nivelAgua > 50 -> ContextCompat.getColor(context, R.color.eco_primary)
            item.nivelAgua > 20 -> ContextCompat.getColor(context, R.color.status_warning)
            else -> ContextCompat.getColor(context, R.color.status_critical)
        }
        holder.progressWater.progressTintList = ColorStateList.valueOf(colorBarra)

        // Configurar click
        holder.itemView.setOnClickListener {
            val datos = mapOf(
                "ubicacion" to item.ubicacion,
                "humedad" to item.humedadSuelo,
                "temperatura" to item.temperatura,
                "luz" to item.luz,
                "estado" to item.estado,
                "ultimoRiego" to item.ultimoRiego
            )
            onPlantClick(planta, datos)
        }
    }

    override fun getItemCount() = plantasData.size

    fun updateList(newData: List<PlantaConDatos>) {
        plantasData = newData
        notifyDataSetChanged()
    }
}