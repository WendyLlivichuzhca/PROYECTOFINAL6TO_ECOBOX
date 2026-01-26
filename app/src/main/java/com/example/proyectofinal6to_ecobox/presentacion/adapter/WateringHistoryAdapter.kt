package com.example.proyectofinal6to_ecobox.presentacion.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.WateringItemResponse
import java.text.SimpleDateFormat
import java.util.*

class WateringHistoryAdapter(
    private var waterings: List<WateringItemResponse>,
    private val onItemClick: (WateringItemResponse) -> Unit
) : RecyclerView.Adapter<WateringHistoryAdapter.WateringViewHolder>() {

    class WateringViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardWatering)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvHumidity: TextView = view.findViewById(R.id.tvHumidity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WateringViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watering, parent, false)
        return WateringViewHolder(view)
    }

    override fun onBindViewHolder(holder: WateringViewHolder, position: Int) {
        val watering = waterings[position]
        val context = holder.itemView.context

        // Formatear fecha
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(watering.fechaCreacion)
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            holder.tvDate.text = date?.let { dateFormat.format(it) } ?: "N/A"
            holder.tvTime.text = date?.let { timeFormat.format(it) } ?: "N/A"
        } catch (e: Exception) {
            holder.tvDate.text = "N/A"
            holder.tvTime.text = "N/A"
        }

        // Tipo
        holder.tvType.text = when (watering.tipo) {
            "MANUAL" -> "⏱️ Manual"
            "AUTOMATICO" -> "🤖 Automático"
            else -> watering.tipo
        }

        // Estado
        val (statusText, statusColor) = when (watering.estado) {
            "PROGRAMADO" -> "⏰ Programado" to R.color.warning
            "EN_CURSO" -> "💧 En curso" to R.color.info
            "COMPLETADO" -> "✅ Completado" to R.color.success
            "CANCELADO" -> "❌ Cancelado" to R.color.error
            else -> watering.estado to R.color.text_secondary
        }
        holder.tvStatus.text = statusText
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, statusColor))

        // Duración
        holder.tvDuration.text = "${watering.duracionSegundos}s / ${watering.cantidadMl}ml"

        // Humedad
        if (watering.humedadInicial != null && watering.humedadFinal != null) {
            holder.tvHumidity.text = String.format(
                "%.1f%% → %.1f%%",
                watering.humedadInicial,
                watering.humedadFinal
            )
            holder.tvHumidity.visibility = View.VISIBLE
        } else {
            holder.tvHumidity.visibility = View.GONE
        }

        // Click listener
        holder.cardView.setOnClickListener {
            onItemClick(watering)
        }
    }

    override fun getItemCount() = waterings.size

    fun updateData(newWaterings: List<WateringItemResponse>) {
        waterings = newWaterings
        notifyDataSetChanged()
    }
}
