package com.example.proyectofinal6to_ecobox.data.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.AlertaPlanta
import java.text.SimpleDateFormat
import java.util.Locale

class AlertPlantaAdapter(
    private var alerts: List<AlertaPlanta>,
    private val onResolveClick: (AlertaPlanta) -> Unit,
    private val onItemClick: (AlertaPlanta) -> Unit
) : RecyclerView.Adapter<AlertPlantaAdapter.AlertViewHolder>() {

    fun updateList(newAlerts: List<AlertaPlanta>) {
        this.alerts = newAlerts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert_plantas, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        holder.bind(alert)
    }

    override fun getItemCount(): Int = alerts.size

    inner class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvEmoji: TextView = view.findViewById(R.id.tvAlertEmoji)
        private val tvTitle: TextView = view.findViewById(R.id.tvAlertTitle)
        private val tvPlant: TextView = view.findViewById(R.id.tvAlertPlantName)
        private val tvMessage: TextView = view.findViewById(R.id.tvAlertMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvAlertTime)
        private val vUnread: View = view.findViewById(R.id.vUnreadDot)
        private val btnResolve: Button = view.findViewById(R.id.btnResolveAlert)

        fun bind(alert: AlertaPlanta) {
            tvEmoji.text = alert.getPrioridadIcon()
            tvTitle.text = alert.titulo
            tvTitle.setTextColor(Color.parseColor(alert.getPrioridadColor()))
            
            tvPlant.text = "🌿 ${alert.plantNombre ?: "Planta"}"
            tvMessage.text = alert.mensaje
            
            // Formatear Fecha (Simplificado)
            tvTime.text = formatAlertTime(alert.creadaEn)
            
            vUnread.visibility = if (alert.leida) View.GONE else View.VISIBLE
            
            btnResolve.visibility = if (alert.resuelta) View.GONE else View.VISIBLE
            btnResolve.setOnClickListener { onResolveClick(alert) }
            
            itemView.setOnClickListener { onItemClick(alert) }
        }

        private fun formatAlertTime(dateStr: String): String {
            return try {
                // El formato de Django suele ser ISO 8601
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) } ?: dateStr
            } catch (e: Exception) {
                dateStr
            }
        }
    }
}
