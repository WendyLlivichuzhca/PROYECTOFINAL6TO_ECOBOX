// Modelos adicionales para sistema de riego
package com.example.proyectofinal6to_ecobox.data.network

import com.google.gson.annotations.SerializedName

// Modelo para el historial de AI que ya funciona
data class WateringHistoryApiResponse(
    val success: Boolean,
    @SerializedName("plant_id")
    val plantId: Long,
    val waterings: List<WateringHistoryItem>,
    val predictions: List<Any>,
    val stats: WateringHistoryStats
)

data class WateringHistoryItem(
    val id: Long,
    val date: String,
    val duration: Int,
    val mode: String,
    val status: String,
    @SerializedName("initial_humidity")
    val initialHumidity: Float?,
    @SerializedName("final_humidity")
    val finalHumidity: Float?,
    val success: Boolean,
    val effectiveness: String
)

data class WateringHistoryStats(
    @SerializedName("total_waterings")
    val totalWaterings: Int,
    @SerializedName("success_rate")
    val successRate: Float,
    @SerializedName("avg_duration")
    val avgDuration: Float
)

// Modelos para el ViewSet de riegos
data class WateringItemResponse(
    val id: Long,
    val planta: Long,
    val usuario: Long,
    val tipo: String,
    val estado: String,
    @SerializedName("duracion_segundos")
    val duracionSegundos: Int?,
    @SerializedName("cantidad_ml")
    val cantidadMl: Int?,
    @SerializedName("humedad_inicial")
    val humedadInicial: Float?,
    @SerializedName("humedad_final")
    val humedadFinal: Float?,
    @SerializedName("fecha_creacion")
    val fechaCreacion: String,
    @SerializedName("fecha_inicio")
    val fechaInicio: String?,
    @SerializedName("fecha_fin")
    val fechaFin: String?,
    @SerializedName("fecha_programada")
    val fechaProgramada: String?
)

data class WateringCreateResponse(
    val status: String,
    val message: String,
    val riego: WateringItemResponse?
)

data class WateringStatsApiResponse(
    val status: String,
    val estadisticas: WateringStatsData,
    @SerializedName("ultima_actualizacion")
    val ultimaActualizacion: String
)

data class WateringStatsData(
    @SerializedName("total_7dias")
    val total7dias: Int,
    val completados: Int,
    val manuales: Int,
    val automaticos: Int,
    val eficiencia: Float,
    @SerializedName("riegos_por_dia")
    val riegosPorDia: Map<String, Int>,
    @SerializedName("top_plantas")
    val topPlantas: List<PlantWateringCount>
)

data class PlantWateringCount(
    val planta: String,
    val cantidad: Int
)
