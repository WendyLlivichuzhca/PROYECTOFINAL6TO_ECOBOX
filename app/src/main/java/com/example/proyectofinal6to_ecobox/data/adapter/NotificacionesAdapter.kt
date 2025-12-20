package com.example.proyectofinal6to_ecobox.data.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Notificacion

class NotificacionesAdapter(
    private var lista: List<Notificacion>,
    private val onNotificacionClick: (Notificacion) -> Unit = {}
) : RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder>() {

    class NotificacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // IDs actualizados según el nuevo item_alert.xml
        val indicator: View = view.findViewById(R.id.viewSeverityIndicator) // La barra lateral
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
        val context = holder.itemView.context

        holder.titulo.text = item.titulo
        holder.mensaje.text = item.mensaje
        holder.hora.text = item.fecha

        // Visibilidad del punto rojo
        holder.badgeNoLeido.visibility = if (!item.isLeida()) View.VISIBLE else View.GONE

        // Configurar click
        holder.itemView.setOnClickListener {
            onNotificacionClick(item)
        }

        // Lógica de Estilos (Colores Modernos)
        val tituloLower = item.titulo.lowercase()

        when {
            // CRÍTICO (Rojo)
            tituloLower.contains("crítico") ||
                    tituloLower.contains("urgente") ||
                    tituloLower.contains("peligro") -> {
                aplicarEstilo(holder, context, R.color.alert_critical_text, R.drawable.ic_alert_critical)
            }

            // ADVERTENCIA (Naranja)
            tituloLower.contains("alerta") ||
                    tituloLower.contains("advertencia") ||
                    tituloLower.contains("bajo") -> {
                aplicarEstilo(holder, context, R.color.alert_warning_text, R.drawable.ic_alert_warning)
            }

            // INFO (Azul/Verde)
            else -> {
                // Usamos el color primario o un azul info
                aplicarEstilo(holder, context, R.color.text_primary, R.drawable.ic_notification)
            }
        }
    }

    private fun aplicarEstilo(
        holder: NotificacionViewHolder,
        context: Context,
        colorResId: Int,
        iconResId: Int
    ) {
        val color = ContextCompat.getColor(context, colorResId)

        // 1. Color de la barra lateral
        holder.indicator.setBackgroundColor(color)

        // 2. Color del título
        holder.titulo.setTextColor(color)

        // 3. Color del badge de no leído (para que combine)
        holder.badgeNoLeido.backgroundTintList = ColorStateList.valueOf(color)

        // 4. Icono y su tinte (gris oscuro para el icono suele verse mejor en este diseño limpio)
        holder.icono.setImageResource(iconResId)
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<Notificacion>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}