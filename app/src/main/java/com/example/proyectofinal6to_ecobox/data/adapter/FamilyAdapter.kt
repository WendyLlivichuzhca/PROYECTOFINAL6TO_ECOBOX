package com.example.proyectofinal6to_ecobox.data.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.FamiliaUi

class FamilyAdapter(
    private var families: List<FamiliaUi>,
    private val onClick: (FamiliaUi) -> Unit
) : RecyclerView.Adapter<FamilyAdapter.FamilyViewHolder>() {

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
        val item = families[position]

        holder.tvInitial.text = item.inicial
        holder.tvName.text = item.nombre
        holder.tvCode.text = "Cód: ${item.codigo}"
        holder.tvMembers.text = "${item.cantidadMiembros} miembros"
        holder.tvPlants.text = "${item.cantidadPlantas} plantas"

        // Lógica de colores según el rol (Admin vs Miembro)
        holder.chipRole.text = item.rolNombre
        val background = holder.chipRole.background as GradientDrawable

        if (item.rolNombre.equals("Administrador", ignoreCase = true) || item.rolNombre.equals("Admin", ignoreCase = true)) {
            holder.chipRole.setTextColor(Color.parseColor("#10B981")) // Verde Texto
            background.setColor(Color.parseColor("#ECFDF5")) // Verde Fondo
        } else {
            holder.chipRole.setTextColor(Color.parseColor("#3B82F6")) // Azul Texto
            background.setColor(Color.parseColor("#EFF6FF")) // Azul Fondo
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = families.size

    fun updateList(newList: List<FamiliaUi>) {
        families = newList
        notifyDataSetChanged()
    }
}