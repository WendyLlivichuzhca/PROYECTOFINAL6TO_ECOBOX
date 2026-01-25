package com.example.proyectofinal6to_ecobox.presentacion.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.FamilyResponse

class FamiliesApiAdapter(
    private var families: List<FamilyResponse>,
    private val onClick: (FamilyResponse) -> Unit
) : RecyclerView.Adapter<FamiliesApiAdapter.FamilyViewHolder>() {

    class FamilyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitial: TextView = view.findViewById(R.id.tvInitial)
        val tvName: TextView = view.findViewById(R.id.tvFamilyName)
        val tvCode: TextView = view.findViewById(R.id.tvCode)
        val tvMembers: TextView = view.findViewById(R.id.tvMembersCount)
        val tvPlants: TextView = view.findViewById(R.id.tvPlantsCount)
        val chipRole: TextView = view.findViewById(R.id.tvRoleBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FamilyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_family, parent, false)
        return FamilyViewHolder(view)
    }

    override fun onBindViewHolder(holder: FamilyViewHolder, position: Int) {
        val familia = families[position]

        // Inicial del nombre de la familia
        holder.tvInitial.text = familia.nombre.firstOrNull()?.uppercase() ?: "F"
        holder.tvName.text = familia.nombre
        holder.tvCode.text = "Cód: ${familia.codigo_invitacion ?: "N/A"}"
        holder.tvMembers.text = "${familia.cantidad_miembros} miembros"
        holder.tvPlants.text = "${familia.cantidad_plantas} plantas"

        // Badge de rol (Admin o Miembro)
        holder.chipRole.text = if (familia.es_admin) "ADMIN" else "MIEMBRO"
        val background = holder.chipRole.background as GradientDrawable

        if (familia.es_admin) {
            holder.chipRole.setTextColor(Color.parseColor("#10B981")) // Verde
            background.setColor(Color.parseColor("#ECFDF5")) // Verde claro
        } else {
            holder.chipRole.setTextColor(Color.parseColor("#3B82F6")) // Azul
            background.setColor(Color.parseColor("#EFF6FF")) // Azul claro
        }

        holder.itemView.setOnClickListener { onClick(familia) }
    }

    override fun getItemCount() = families.size

    fun updateList(newList: List<FamilyResponse>) {
        families = newList
        notifyDataSetChanged()
    }
}
