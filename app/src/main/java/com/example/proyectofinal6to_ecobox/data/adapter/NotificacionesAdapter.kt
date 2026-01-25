package com.example.proyectofinal6to_ecobox.data.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.UserNotificationResponse
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class NotificacionesAdapter(
    private var lista: List<UserNotificationResponse>,
    private val onMarcarLeidaClick: (UserNotificationResponse) -> Unit
) : RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder>() {

    class NotificacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcono: ImageView = view.findViewById(R.id.imgIcono)
        val txtTipoBadge: TextView = view.findViewById(R.id.txtTipoBadge)
        val txtHora: TextView = view.findViewById(R.id.txtHora)
        val txtMensaje: TextView = view.findViewById(R.id.txtMensaje)
        val txtRelativeTime: TextView = view.findViewById(R.id.txtRelativeTime)
        val badgeNoLeido: View = view.findViewById(R.id.badgeNoLeido)
        val btnMarcarLeida: MaterialButton = view.findViewById(R.id.btnMarcarLeidaItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
        return NotificacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        val item = lista[position]

        holder.txtMensaje.text = item.mensaje
        holder.txtHora.text = formatFecha(item.fechaCreacion)
        holder.txtRelativeTime.text = getRelativeTime(item.fechaCreacion)

        // Estado de lectura
        if (item.leida) {
            holder.badgeNoLeido.visibility = View.GONE
            holder.btnMarcarLeida.visibility = View.GONE
            holder.itemView.alpha = 0.7f
        } else {
            holder.badgeNoLeido.visibility = View.VISIBLE
            holder.btnMarcarLeida.visibility = View.VISIBLE
            holder.itemView.alpha = 1.0f
        }

        // Estilo por tipo
        when (item.tipo.uppercase()) {
            "WARNING", "ALERTA" -> {
                applyTypeStyle(holder, "#F59E0B", "#FEF3C7", "ALERTA", R.drawable.ic_temp)
            }
            "ERROR", "CRITICA" -> {
                applyTypeStyle(holder, "#EF4444", "#FEE2E2", "ERROR", R.drawable.ic_alert_critical)
            }
            "SUCCESS" -> {
                applyTypeStyle(holder, "#10B981", "#D1FAE5", "ÉXITO", R.drawable.ic_check_circle)
            }
            else -> { // INFO
                applyTypeStyle(holder, "#3B82F6", "#DBEAFE", "INFO", R.drawable.ic_notification)
            }
        }

        holder.btnMarcarLeida.setOnClickListener {
            onMarcarLeidaClick(item)
        }
    }

    private fun applyTypeStyle(holder: NotificacionViewHolder, colorHex: String, bgHex: String, label: String, iconRes: Int) {
        val color = Color.parseColor(colorHex)
        val bgColor = Color.parseColor(bgHex)
        
        holder.txtTipoBadge.text = label
        holder.txtTipoBadge.setTextColor(color)
        holder.txtTipoBadge.backgroundTintList = ColorStateList.valueOf(bgColor)
        
        holder.imgIcono.setImageResource(iconRes)
        holder.imgIcono.imageTintList = ColorStateList.valueOf(color)
        
        if (holder.badgeNoLeido.visibility == View.VISIBLE) {
            holder.badgeNoLeido.backgroundTintList = ColorStateList.valueOf(color)
        }
    }

    private fun formatFecha(fechaStr: String): String {
        return try {
            // Asumiendo formato ISO del backend: 2024-03-20T10:30:00...
            val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdfInput.parse(fechaStr)
            val sdfOutput = SimpleDateFormat("dd MMM. HH:mm", Locale.getDefault())
            sdfOutput.format(date ?: Date())
        } catch (e: Exception) {
            fechaStr.split("T").getOrNull(0) ?: fechaStr
        }
    }

    private fun getRelativeTime(fechaStr: String): String {
        return "Notificación reciente" // Por simplicidad, o implementar lógica de "hace X min"
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<UserNotificationResponse>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}
