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
import com.bumptech.glide.Glide
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.example.proyectofinal6to_ecobox.utils.AppConfig
import com.example.proyectofinal6to_ecobox.utils.ImageUtils

class PlantAdapter(
    private var plantasData: List<PlantaConDatos>,
    private val onPlantClick: (Planta, Map<String, Any>) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    private var listaCompleta: List<PlantaConDatos> = plantasData

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPlantImage: ImageView = view.findViewById(R.id.ivPlantImage)
        val tvName: TextView = view.findViewById(R.id.tvPlantName)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)

        // Sensores
        val tvHumidity: TextView = view.findViewById(R.id.tvHumidity)
        val tvTemp: TextView = view.findViewById(R.id.tvTemp)

        // Botón
        val btnVerDetalles: View = view.findViewById(R.id.btnVerDetalles)
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
        holder.tvDescription.text = if (!item.planta.descripcion.isNullOrEmpty()) {
            item.planta.descripcion
        } else {
            "Sin descripción disponible"
        }

        // 2. Asignar valores de sensores (Lógica independiente por métrica)
        // Humedad
        if (item.humedadSuelo != null) {
            holder.tvHumidity.text = String.format("%.2f%%", item.humedadSuelo)
            holder.tvHumidity.setTextColor(Color.parseColor("#3B82F6")) // Azul
        } else {
            holder.tvHumidity.text = "N/A"
            holder.tvHumidity.setTextColor(Color.parseColor("#EF4444")) // Rojo para N/A
        }

        // Temperatura
        if (item.temperatura != null) {
            holder.tvTemp.text = String.format("%.2f°C", item.temperatura)
            holder.tvTemp.setTextColor(Color.parseColor("#EF4444")) // Rojo (como en la web)
        } else {
            holder.tvTemp.text = "N/A"
            holder.tvTemp.setTextColor(Color.parseColor("#EF4444"))
        }

        // 3. CARGAR LA FOTO DE LA PLANTA
        val fotoUrl = item.planta.foto
        if (fotoUrl.isNotEmpty()) {
            ImageUtils.loadPlantImage(fotoUrl, holder.ivPlantImage, R.drawable.img_plant_placeholder)
        } else {
            holder.ivPlantImage.setImageResource(R.drawable.img_plant_placeholder)
        }

        // 4. Configuración del Badge de Estado
        val estado = item.estado.lowercase()
        val (textoEstado, colorTexto, colorBg) = when {
            estado.contains("healthy") || estado.contains("saludable") || estado.contains("excelente") -> 
                Triple("SALUDABLE", "#047857", "#D1FAE5")
            
            estado.contains("necesita_agua") || estado.contains("sedienta") -> 
                Triple("SEDIENTA", "#D97706", "#FEF3C7")
                
            estado.contains("warning") || estado.contains("advertencia") || estado.contains("revisar") -> 
                Triple("ATENCIÓN", "#1D4ED8", "#DBEAFE")
                
            estado.contains("critical") || estado.contains("crítico") || estado.contains("peligro") -> 
                Triple("CRÍTICO", "#B91C1C", "#FEE2E2")
                
            else -> Triple("NORMAL", "#1D4ED8", "#DBEAFE")
        }

        holder.tvStatusBadge.text = textoEstado
        holder.tvStatusBadge.setTextColor(Color.parseColor(colorTexto))
        holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorBg))

        // 5. Click Listener (Tanto en tarjeta como en botón)
        val action = {
            val datos = mapOf(
                "ubicacion" to item.ubicacion,
                "humedad" to item.humedadSuelo,
                "temperatura" to item.temperatura,
                "luz" to item.luz,
                "estado" to item.estado,
                "ultimoRiego" to item.ultimoRiego,
                "sensorCount" to item.sensorCount,
                "foto" to item.planta.foto
            ).filterValues { it != null } as Map<String, Any>
            
            onPlantClick(item.planta, datos)
        }

        holder.itemView.setOnClickListener { action() }
        holder.btnVerDetalles.setOnClickListener { action() }
    }

    override fun getItemCount() = plantasData.size

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

    data class PlantaConDatos(
        val planta: Planta,
        val ubicacion: String,
        val humedadSuelo: Float?,
        val temperatura: Float?,
        val luz: Float,
        val nivelAgua: Int,
        val estado: String,
        val ultimoRiego: String,
        val sensorCount: Int,
        val aspecto: String? = null
    )
}