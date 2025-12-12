package com.example.proyectofinal6to_ecobox.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Notificacion

class NotificacionesAdapter(
    private var lista: List<Notificacion>,
    private val onNotificacionClick: (Notificacion) -> Unit = {}
) : RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder>() {

    class NotificacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.containerAlerta)
        val titulo: TextView = view.findViewById(R.id.txtTitulo)
        val mensaje: TextView = view.findViewById(R.id.txtMensaje)
        val hora: TextView = view.findViewById(R.id.txtHora)
        val icono: ImageView = view.findViewById(R.id.imgIcono)
        val badgeNoLeido: View = view.findViewById(R.id.badgeNoLeido)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
        return NotificacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        val item = lista[position]
        holder.titulo.text = item.titulo
        holder.mensaje.text = item.mensaje
        holder.hora.text = item.fecha

        // ✅ CAMBIADO: Usar getter isLeida() en lugar de propiedad directa
        holder.badgeNoLeido.visibility = if (!item.isLeida()) View.VISIBLE else View.GONE

        // Configurar click
        holder.itemView.setOnClickListener {
            onNotificacionClick(item)
        }

        // Determinar color y icono basado en el título/tipo
        val tituloLower = item.titulo.lowercase()
        when {
            tituloLower.contains("crítico") ||
                    tituloLower.contains("urgente") ||
                    tituloLower.contains("calor excesivo") ||
                    tituloLower.contains("temperatura baja") -> {
                // Rojo para crítico
                holder.container.setBackgroundResource(R.drawable.bg_card_red_rounded)
                holder.icono.setImageResource(R.drawable.ic_alert_critical)
                holder.titulo.setTextColor(holder.itemView.context.getColor(R.color.status_critical))
            }
            tituloLower.contains("alerta") ||
                    tituloLower.contains("advertencia") ||
                    tituloLower.contains("necesita") -> {
                // Naranja para advertencia
                holder.container.setBackgroundResource(R.drawable.bg_card_orange_rounded)
                holder.icono.setImageResource(R.drawable.ic_alert_warning)
                holder.titulo.setTextColor(holder.itemView.context.getColor(R.color.status_warning))
            }
            tituloLower.contains("información") ||
                    tituloLower.contains("info") ||
                    tituloLower.contains("completado") -> {
                // Azul para informativo
                holder.container.setBackgroundResource(R.drawable.bg_card_blue_rounded)
                holder.icono.setImageResource(R.drawable.ic_info)
                holder.titulo.setTextColor(holder.itemView.context.getColor(R.color.eco_primary))
            }
            else -> {
                // Gris por defecto
                holder.container.setBackgroundResource(R.drawable.bg_card_gray_rounded)
                holder.icono.setImageResource(R.drawable.ic_notification)
                holder.titulo.setTextColor(holder.itemView.context.getColor(R.color.text_gray))
            }
        }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<Notificacion>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}