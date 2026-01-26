package com.example.proyectofinal6to_ecobox.data.network

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del endpoint /ai/watering/predict/{plantId}/
 */
data class WateringPredictionResponse(
    val success: Boolean,
    @SerializedName("humidity_current")
    val humidityCurrent: Float?,
    val prediction: WateringPredictionData?,
    val message: String?
)

data class WateringPredictionData(
    val action: String,
    val confidence: Float,
    val reason: String,
    @SerializedName("current_humidity")
    val currentHumidity: Float?,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    val timestamp: String?
)

/**
 * Respuesta del endpoint /mediciones/
 */
data class MedicionResponse(
    val id: Long,
    val humedad: Float?,
    val temperatura: Float?,
    val fecha: String,
    val planta: Long
)

/**
 * Datos para el gráfico de monitoreo
 */
data class MonitoringDataPoint(
    val hora: String,           // "01:40"
    val humedad: Float,         // 72.5
    val temperatura: Float,     // 26.4
    val timestamp: Long,
    val esReal: Boolean         // true si viene de datos reales
)

/**
 * Estadísticas calculadas para la UI
 */
data class MonitoringStats(
    val avgHumidity: Float,
    val minHumidity: Float,
    val maxHumidity: Float,
    val avgTemperature: Float,
    val trend: String,          // "rising", "falling", "stable"
    val esHumedadReal: Boolean,
    val humedadRealPlanta: Float?,
    val plantasNecesitanAgua: Int
)

/**
 * Contenedor de datos de monitoreo
 */
data class MonitoringData(
    val dataPoints: List<MonitoringDataPoint>,
    val stats: MonitoringStats,
    val esReal: Boolean
)
