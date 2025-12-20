package com.example.proyectofinal6to_ecobox.data.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao

class EventsAdapter(private var events: List<PlantaDao.EventoDAO>) :
    RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgEventIcon)
        val title: TextView = view.findViewById(R.id.tvEventTitle)
        val subtitle: TextView = view.findViewById(R.id.tvEventSubtitle)
        // Asegúrate de que item_event.xml tenga un TextView para la hora,
        // si no, usaremos el subtitle para poner fecha y planta.
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        // Reutilizamos tu layout 'item_event.xml'
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.title.text = event.tipo
        holder.subtitle.text = "${event.planta} • ${event.fecha}"

        // Lógica de colores e iconos según el tipo de evento
        when (event.iconoTipo) {
            1 -> { // Riego
                holder.icon.setImageResource(R.drawable.ic_water_drop)
                holder.icon.setColorFilter(Color.parseColor("#3B82F6")) // Azul
            }
            2 -> { // Alerta Temperatura
                holder.icon.setImageResource(R.drawable.ic_thermometer)
                holder.icon.setColorFilter(Color.parseColor("#EF4444")) // Rojo
            }
            3 -> { // Luz / Otro
                holder.icon.setImageResource(R.drawable.ic_sun)
                holder.icon.setColorFilter(Color.parseColor("#F59E0B")) // Amarillo
            }
            else -> { // General
                holder.icon.setImageResource(R.drawable.ic_leaf)
                holder.icon.setColorFilter(Color.parseColor("#10B981")) // Verde
            }
        }
    }

    override fun getItemCount() = events.size

    fun updateData(newEvents: List<PlantaDao.EventoDAO>) {
        events = newEvents
        notifyDataSetChanged()
    }
}