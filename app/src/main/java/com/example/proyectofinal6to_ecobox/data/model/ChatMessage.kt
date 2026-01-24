package com.example.proyectofinal6to_ecobox.data.model

/**
 * Modelo para los mensajes del Chatbot
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
