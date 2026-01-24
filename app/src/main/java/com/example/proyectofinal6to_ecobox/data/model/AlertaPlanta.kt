package com.example.proyectofinal6to_ecobox.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AlertaPlanta(
    val id: Long,
    val titulo: String,
    val mensaje: String,
    @SerializedName("tipo") val tipoAlerta: String,
    val leida: Boolean,
    val resuelta: Boolean,
    @SerializedName("creada_en") val creadaEn: String,
    @SerializedName("plant_id") val plantId: Long?,
    @SerializedName("plant_nombre") val plantNombre: String?
) : Serializable {
    
    fun getPrioridadColor(): String {
        return when (tipoAlerta.uppercase()) {
            "CRITICA" -> "#EF4444"
            "ADVERTENCIA" -> "#F59E0B"
            "INFO" -> "#3B82F6"
            "EXITO" -> "#10B981"
            else -> "#6B7280"
        }
    }

    fun getPrioridadIcon(): String {
        return when (tipoAlerta.uppercase()) {
            "CRITICA" -> "🚨"
            "ADVERTENCIA" -> "⚠️"
            "INFO" -> "ℹ️"
            "EXITO" -> "✅"
            else -> "📢"
        }
    }
}
