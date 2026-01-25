package com.example.proyectofinal6to_ecobox.presentacion.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.FamilyMemberResponse

class FamilyMembersAdapter(private var members: List<FamilyMemberResponse>) :
    RecyclerView.Adapter<FamilyMembersAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitial: TextView = view.findViewById(R.id.tvMemberInitial)
        val tvName: TextView = view.findViewById(R.id.tvMemberName)
        val tvEmail: TextView = view.findViewById(R.id.tvMemberEmail)
        val tvRole: TextView = view.findViewById(R.id.tvMemberRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_family_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        val userInfo = member.usuario_info
        
        holder.tvName.text = userInfo.first_name ?: "Sin nombre"
        holder.tvEmail.text = userInfo.email
        holder.tvInitial.text = userInfo.first_name?.take(1)?.uppercase() ?: "U"
        holder.tvRole.text = if (member.es_administrador) "ADMIN" else "MIEMBRO"
        
        if (member.es_administrador) {
            holder.tvRole.setBackgroundResource(R.drawable.bg_badge_success)
        } else {
            holder.tvRole.setBackgroundResource(R.drawable.bg_badge_blue_soft)
        }
    }

    override fun getItemCount() = members.size

    fun updateMembers(newMembers: List<FamilyMemberResponse>) {
        this.members = newMembers
        notifyDataSetChanged()
    }
}
