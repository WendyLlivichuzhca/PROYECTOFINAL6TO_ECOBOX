package com.example.proyectofinal6to_ecobox.data.dao

import android.os.StrictMode
import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import com.example.proyectofinal6to_ecobox.utils.AppConfig

object MySqlConexion {

    fun getConexion(): Connection? {
        return try {
            // Permitir operaciones de red (solo para pruebas locales)
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            // Usamos el driver clásico que es más compatible con Android
            Class.forName("com.mysql.jdbc.Driver")

            // --- DATOS DE CONEXIÓN ---
            val dbName = AppConfig.DB_NAME
            val ip = AppConfig.SERVER_IP
            //val ip ="192.168.54.30"
            //val ip = "192.168.0.106"   // Si usas CELULAR FÍSICO
            val port = AppConfig.DB_PORT
            val usuario = AppConfig.DB_USER
            val contrasena = AppConfig.DB_PASSWORD

            // --- URL OPTIMIZADA ---
            // Agregamos connectTimeout para que no se quede colgado si falla
            val url = "jdbc:mysql://$ip:$port/$dbName?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000"

            DriverManager.getConnection(url, usuario, contrasena)

        } catch (e: ClassNotFoundException) {
            Log.e("MySqlConexion", "❌ Error: Driver no encontrado", e)
            null
        } catch (e: Exception) {
            Log.e("MySqlConexion", "❌ Error de conexión: Verifica IP y Firewall", e)
            null
        }
    }
}