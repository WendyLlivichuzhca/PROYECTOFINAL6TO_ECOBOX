package com.example.proyectofinal6to_ecobox.data.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.MiembroUi

class MemberAdapter(private var members: List<MiembroUi>) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvMemberName)
        val tvEmail: TextView = view.findViewById(R.id.tvMemberEmail)
        val tvRole: TextView = view.findViewById(R.id.tvMemberRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val item = members[position]
        val context = holder.itemView.context

        holder.tvName.text = item.nombre
        holder.tvEmail.text = item.email
        holder.tvRole.text = item.rol

        // Lógica de colores basada en el rol
        if (item.isAdmin) {
            // Estilo para Administrador (Verde)
            holder.tvRole.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_green_soft)
            holder.tvRole.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            // Estilo para Miembro (Azul)
            holder.tvRole.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_blue_soft)
            holder.tvRole.setTextColor(Color.parseColor("#1976D2"))
        }
    }

    override fun getItemCount() = members.size

    // Función útil para actualizar la lista desde el Fragment
    fun updateList(newMembers: List<MiembroUi>) {
        this.members = newMembers
        notifyDataSetChanged()
    }
}