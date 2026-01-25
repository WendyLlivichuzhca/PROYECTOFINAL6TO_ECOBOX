package com.example.proyectofinal6to_ecobox.data.model

data class Recommendation(
    val id: Int,
    val type: String, // URGENTE, ADVERTENCIA, INFO
    val plantaId: Long,
    val plantaNombre: String,
    val message: String,
    val timeAgo: String,
    val action: String, // regar, mover, luz, etc.
    val confidence: Float
)
