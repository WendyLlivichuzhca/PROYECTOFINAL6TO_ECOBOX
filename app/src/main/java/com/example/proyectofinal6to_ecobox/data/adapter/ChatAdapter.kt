package com.example.proyectofinal6to_ecobox.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.ChatMessage

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var showTypingIndicator: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_TYPING = 3
    }

    override fun getItemViewType(position: Int): Int {
        if (showTypingIndicator && position == messages.size) {
            return VIEW_TYPE_TYPING
        }
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            }
            VIEW_TYPE_TYPING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_typing, parent, false)
                TypingViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_bot, parent, false)
                BotViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TypingViewHolder) return

        val message = messages[position]
        val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(message.timestamp)

        if (holder is UserViewHolder) {
            holder.tvMessage.text = message.text
            holder.tvTimestamp.text = timeStr
        } else if (holder is BotViewHolder) {
            holder.tvMessage.text = message.text
            holder.tvTimestamp.text = timeStr
        }
    }

    override fun getItemCount(): Int = if (showTypingIndicator) messages.size + 1 else messages.size

    class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessageUser)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestampUser)
    }

    class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessageBot)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestampBot)
    }
}
