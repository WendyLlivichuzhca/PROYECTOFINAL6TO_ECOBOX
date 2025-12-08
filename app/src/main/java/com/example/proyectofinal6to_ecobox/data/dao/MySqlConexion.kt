package com.example.proyectofinal6to_ecobox.data.dao

import android.os.StrictMode
import android.util.Log
import java.sql.Connection
import java.sql.DriverManager

object MySqlConexion {

    fun getConexion(): Connection? {
        return try {
            // Permitir operaciones de red (solo para pruebas locales)
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            // Usamos el driver clásico que es más compatible con Android
            Class.forName("com.mysql.jdbc.Driver")

            // --- DATOS DE CONEXIÓN ---
            val dbName = "base_ecobox"  // Asegúrate que sea el nombre exacto en Workbench
            val ip = "10.0.2.2"         // Si usas EMULADOR
            // val ip = "192.168.1.X"   // Si usas CELULAR FÍSICO (Descomenta y pon tu IP real)
            val port = "3306"
            val usuario = "root"
            val contrasena = "1234"     // Tu contraseña de Workbench

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