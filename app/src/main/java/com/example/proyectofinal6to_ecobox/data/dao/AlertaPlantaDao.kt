package com.example.proyectofinal6to_ecobox.data.dao

import android.util.Log
import com.example.proyectofinal6to_ecobox.data.model.AlertaPlanta
import com.example.proyectofinal6to_ecobox.utils.AppConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AlertaPlantaDao {

    /**
     * Obtiene la lista de alertas del usuario desde la API de Django
     */
    fun obtenerAlertasDesdeApi(token: String): List<AlertaPlanta> {
        val alerteList = mutableListOf<AlertaPlanta>()
        try {
            val urlString = if (AppConfig.API_BASE_URL.endsWith("/")) "${AppConfig.API_BASE_URL}alerts/" 
                           else "${AppConfig.API_BASE_URL}/alerts/"
            
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Token $token")
            conn.setRequestProperty("Content-Type", "application/json")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.use { it.readText() }
                val jsonRes = JSONObject(response)
                
                if (jsonRes.getString("status") == "success") {
                    val alertsArray = jsonRes.getJSONArray("alertas")
                    for (i in 0 until alertsArray.length()) {
                        val obj = alertsArray.getJSONObject(i)
                        alerteList.add(
                            AlertaPlanta(
                                id = obj.getLong("id"),
                                titulo = obj.getString("titulo"),
                                mensaje = obj.getString("mensaje"),
                                tipoAlerta = obj.getString("tipo"),
                                leida = obj.getBoolean("leida"),
                                resuelta = obj.getBoolean("resuelta"),
                                creadaEn = obj.getString("creada_en"),
                                plantId = if (obj.isNull("plant_id")) null else obj.getLong("plant_id"),
                                plantNombre = if (obj.isNull("plant_nombre")) null else obj.getString("plant_nombre")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AlertaPlantaDao", "Error obteniendo alertas API: ${e.message}")
        }
        return alerteList
    }

    /**
     * Marca una alerta como leída
     */
    fun marcarLeida(token: String, alertId: Long): Boolean {
        try {
            val urlString = if (AppConfig.API_BASE_URL.endsWith("/")) "${AppConfig.API_BASE_URL}alerts/mark-read/" 
                           else "${AppConfig.API_BASE_URL}/alerts/mark-read/"
            
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Token $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply { put("alert_id", alertId) }
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            return conn.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e("AlertaPlantaDao", "Error marcarLeida: ${e.message}")
            return false
        }
    }

    /**
     * Marca una alerta como resuelta
     */
    fun resolverAlerta(token: String, alertId: Long): Boolean {
        try {
            val urlString = if (AppConfig.API_BASE_URL.endsWith("/")) "${AppConfig.API_BASE_URL}alerts/mark-resolved/" 
                           else "${AppConfig.API_BASE_URL}/alerts/mark-resolved/"
            
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Token $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply { put("alert_id", alertId) }
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            return conn.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e("AlertaPlantaDao", "Error resolverAlerta: ${e.message}")
            return false
        }
    }

    /**
     * Crea una alerta de prueba en el backend
     */
    fun crearAlertaPrueba(token: String): Boolean {
        try {
            val urlString = if (AppConfig.API_BASE_URL.endsWith("/")) "${AppConfig.API_BASE_URL}alerts/test/" 
                           else "${AppConfig.API_BASE_URL}/alerts/test/"
            
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Token $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            
            // Cuerpo vacío o con datos de prueba
            OutputStreamWriter(conn.outputStream).use { it.write("{}") }

            return conn.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e("AlertaPlantaDao", "Error crearAlertaPrueba: ${e.message}")
            return false
        }
    }
}
