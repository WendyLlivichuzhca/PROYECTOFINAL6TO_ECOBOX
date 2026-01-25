package com.example.proyectofinal6to_ecobox.presentacion.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RecommendationResponse
import com.google.android.material.card.MaterialCardView

class RecommendationsAdapter(
    private var items: List<RecommendationResponse>,
    private val onItemClick: (RecommendationResponse) -> Unit
) : RecyclerView.Adapter<RecommendationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvRecType)
        val tvMessage: TextView = view.findViewById(R.id.tvRecMessage)
        val tvTime: TextView = view.findViewById(R.id.tvRecTime)
        val ivIcon: ImageView = view.findViewById(R.id.ivRecIcon)
        val cardIcon: MaterialCardView = view.findViewById(R.id.cardIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvType.text = item.type
        holder.tvMessage.text = item.message
        holder.tvTime.text = item.time_ago

        // Personalización según el tipo (Estilo Web Premium)
        when (item.type) {
            "URGENTE" -> {
                holder.tvType.setTextColor(Color.parseColor("#D97706"))
                holder.cardIcon.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
                holder.ivIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                holder.ivIcon.setImageResource(R.drawable.ic_water_drop)
            }
            "ADVERTENCIA" -> {
                holder.tvType.setTextColor(Color.parseColor("#2563EB"))
                holder.cardIcon.setCardBackgroundColor(Color.parseColor("#DBEAFE"))
                holder.ivIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#2563EB"))
                holder.ivIcon.setImageResource(R.drawable.ic_temp)
            }
            else -> {
                holder.tvType.setTextColor(Color.parseColor("#059669"))
                holder.cardIcon.setCardBackgroundColor(Color.parseColor("#D1FAE5"))
                holder.ivIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
                holder.ivIcon.setImageResource(R.drawable.ic_leaf)
            }
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<RecommendationResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}
