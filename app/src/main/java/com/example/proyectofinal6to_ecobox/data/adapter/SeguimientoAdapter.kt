package com.example.proyectofinal6to_ecobox.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyectofinal6to_ecobox.R
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SeguimientoAdapter(
    private val seguimientos: List<Map<String, Any>>,
    private val onItemClick: (Map<String, Any>) -> Unit = {}
) : RecyclerView.Adapter<SeguimientoAdapter.SeguimientoViewHolder>() {

    class SeguimientoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardSeguimiento: MaterialCardView = view.findViewById(R.id.cardSeguimiento)
        val ivSeguimiento: ImageView = view.findViewById(R.id.ivSeguimiento)
        val tvBadgeReciente: TextView = view.findViewById(R.id.tvBadgeReciente)
        val tvTituloCard: TextView = view.findViewById(R.id.tvTituloCard)
        val tvNumRegistro: TextView = view.findViewById(R.id.tvNumRegistro)
        val tvEstadoCard: TextView = view.findViewById(R.id.tvEstadoCard)
        val tvObsCard: TextView = view.findViewById(R.id.tvObsCard)
        val vLeftBorder: View = view.findViewById(R.id.vLeftBorder)
        val tvFechaCard: TextView = view.findViewById(R.id.tvFechaCard)
        val tvTagImagen: TextView = view.findViewById(R.id.tvTagImagen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeguimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seguimiento, parent, false)
        return SeguimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeguimientoViewHolder, position: Int) {
        val seguimiento = seguimientos[position]

        // Configurar número de registro
        holder.tvNumRegistro.text = "#${position + 1}"

        // Estado del seguimiento
        val estado = seguimiento["estado"] as? String ?: "Sin estado"
        holder.tvEstadoCard.text = "Estado: $estado"

        // Título (usamos el estado como título)
        holder.tvTituloCard.text = estado

        // Observaciones
        val observaciones = seguimiento["observaciones"] as? String ?: "Sin observaciones"
        holder.tvObsCard.text = formatObservaciones(observaciones)

        // Fecha
        val fecha = seguimiento["fecha_formateada"] as? String ?: getCurrentDateFormatted()
        holder.tvFechaCard.text = formatFecha(fecha)

        // Imagen
        val imagenPath = seguimiento["imagen"] as? String
        if (imagenPath != null && imagenPath.isNotEmpty() && imagenPath != "null") {
            holder.tvTagImagen.visibility = View.VISIBLE
            // Cargar imagen con Glide
            Glide.with(holder.ivSeguimiento.context)
                .load(imagenPath)
                .placeholder(R.drawable.bg_leaves)
                .error(R.drawable.bg_leaves)
                .centerCrop()
                .into(holder.ivSeguimiento)
        } else {
            holder.tvTagImagen.visibility = View.GONE
            holder.ivSeguimiento.setImageResource(R.drawable.bg_leaves)
        }

        // Badge para el más reciente (primera posición)
        if (position == 0) {
            holder.tvBadgeReciente.visibility = View.VISIBLE
        } else {
            holder.tvBadgeReciente.visibility = View.GONE
        }

        // Color del borde izquierdo según el estado
        val borderColor = getColorForEstado(estado)
        holder.vLeftBorder.setBackgroundColor(holder.itemView.context.getColor(borderColor))

        // Click listener
        holder.cardSeguimiento.setOnClickListener {
            onItemClick(seguimiento)
        }
    }

    override fun getItemCount(): Int = seguimientos.size

    private fun formatObservaciones(observaciones: String): String {
        return if (observaciones.contains("\n")) {
            observaciones.split("\n")
                .joinToString("\n") { "- $it" }
        } else {
            "- $observaciones"
        }
    }

    private fun formatFecha(fechaStr: String): String {
        return try {
            if (fechaStr.contains("/")) {
                // Ya está formateada como dd/MM/yyyy HH:mm
                "📅 $fechaStr"
            } else {
                // Es un timestamp de MySQL
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale.getDefault())
                val date = inputFormat.parse(fechaStr)
                "📅 ${outputFormat.format(date ?: Date())}"
            }
        } catch (e: Exception) {
            "📅 $fechaStr"
        }
    }

    private fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getColorForEstado(estado: String): Int {
        return when (estado.toLowerCase()) {
            "saludable", "excelente" -> R.color.sensor_green
            "necesita agua", "advertencia" -> R.color.warning
            "crítico", "enfermo" -> R.color.error
            else -> R.color.eco_border
        }
    }
}