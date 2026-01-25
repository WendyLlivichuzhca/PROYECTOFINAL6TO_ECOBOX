package com.example.proyectofinal6to_ecobox.presentacion.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.FamilyMemberResponse

class FamilyMembersAdapter(
    private var members: List<FamilyMemberResponse>,
    private val iAmAdmin: Boolean = false,
    private val currentUserId: Long = -1,
    private val onChangeRole: (FamilyMemberResponse) -> Unit = {},
    private val onRemoveMember: (FamilyMemberResponse) -> Unit = {}
) : RecyclerView.Adapter<FamilyMembersAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitial: TextView = view.findViewById(R.id.tvMemberInitial)
        val tvName: TextView = view.findViewById(R.id.tvMemberName)
        val tvEmail: TextView = view.findViewById(R.id.tvMemberEmail)
        val tvRole: TextView = view.findViewById(R.id.tvMemberRole)
        val layoutAdminActions: View = view.findViewById(R.id.layoutAdminActions)
        val btnChangeRole: View = view.findViewById(R.id.btnChangeRole)
        val btnRemoveMember: View = view.findViewById(R.id.btnRemoveMember)
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

        // Gestión de acciones administrativas
        val isMe = userInfo.id == currentUserId
        
        if (iAmAdmin && !isMe) {
            holder.layoutAdminActions.visibility = View.VISIBLE
            holder.btnChangeRole.setOnClickListener { onChangeRole(member) }
            holder.btnRemoveMember.setOnClickListener { onRemoveMember(member) }
        } else {
            holder.layoutAdminActions.visibility = View.GONE
        }
    }

    override fun getItemCount() = members.size

    fun updateMembers(newMembers: List<FamilyMemberResponse>) {
        this.members = newMembers
        notifyDataSetChanged()
    }
}
