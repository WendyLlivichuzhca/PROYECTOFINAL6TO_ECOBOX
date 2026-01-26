package com.example.proyectofinal6to_ecobox.data.model

import com.example.proyectofinal6to_ecobox.data.model.Planta
import java.math.BigDecimal

/**
 * PlantaData - Modelos de datos para el manejo de plantas y sensores.
 * Extraído de PlantaDao para eliminar dependencias de JDBC en la UI.
 */

data class PlantaCompleta(
    val planta: Planta,
    val ubicacion: String,
    val humedad: Float,
    val temperatura: Float,
    val luz: Float,
    val ultimoRiego: String,
    val estado: String
) {
    fun calcularNivelAgua(): Int {
        return when {
            humedad >= 70f -> 90
            humedad >= 40f -> ((humedad - 40f) / 30f * 50f + 40f).toInt()
            humedad >= 20f -> ((humedad - 20f) / 20f * 40f).toInt()
            else -> (humedad/20f * 20f).toInt()
        }.coerceIn(0, 100)
    }

    fun determinarEstadoUI(): String {
        return clasificarEstadoPlanta(estado, humedad)
    }

    fun obtenerEstadoTexto(): String {
        return when (determinarEstadoUI()) {
            "critical" -> "Crítica"
            "warning" -> "Advertencia"
            "healthy" -> "Saludable"
            else -> "Desconocido"
        }
    }
}

data class EventoDAO(
    val tipo: String,
    val planta: String,
    val fecha: String,
    val descripcion: String,
    val iconoTipo: Int
)

data class DataPointDAO(
    val label: String,
    val value: Float
)

data class ConfiguracionPlanta(
    val plantaId: Long,
    val humedadObjetivo: Float,
    val tempMin: Float,
    val tempMax: Float
)

data class PlantaConDatos(
    val id: Long,
    val nombre: String,
    val especie: String,
    val ubicacion: String,
    val humedad: Float,
    val temperatura: Float,
    val luz: Float
)

data class SensorVista(
    val id: Long,
    val nombre: String,
    val ubicacion: String,
    val tipoSensor: String,
    val unidadMedida: String,
    val estado: String,
    val valor: BigDecimal?,
    val ultimaLectura: String?,
    val activo: Boolean,
    val plantaNombre: String? = null
)

/**
 * FUNCIÓN COMÚN PARA CLASIFICAR ESTADO DE PLANTA
 */
fun clasificarEstadoPlanta(estado: String?, humedad: Float?): String {
    return when {
        estado == "Crítico" || (humedad != null && humedad < 30) -> "critical"
        estado == "Necesita agua" || 
        estado == "Advertencia" || 
        (humedad != null && humedad < 40) -> "warning"
        
        estado == "Saludable" || estado == "Excelente" -> "healthy"
        else -> "unknown"
    }
}

/**
 * Función auxiliar para obtener el texto del estado
 */
fun obtenerTextoEstado(clasificacion: String): String {
    return when (clasificacion) {
        "critical" -> "Crítica"
        "warning" -> "Advertencia"
        "healthy" -> "Saludable"
        else -> "Desconocido"
    }
}
