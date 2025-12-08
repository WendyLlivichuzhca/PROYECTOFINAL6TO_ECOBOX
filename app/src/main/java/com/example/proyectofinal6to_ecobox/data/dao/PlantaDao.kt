package com.example.proyectofinal6to_ecobox.data.dao

import android.util.Log
import com.example.proyectofinal6to_ecobox.data.model.Planta
import java.sql.ResultSet
import java.util.ArrayList

object PlantaDao {

    /**
     * Obtiene la lista de plantas asociadas a las familias del usuario.
     * Relación: Usuario -> FamiliaUsuario -> Familia -> Planta
     */
    fun obtenerPlantasPorUsuario(userId: Long): List<Planta> {
        val listaPlantas = ArrayList<Planta>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // CONSULTA SQL REAL basada en tu archivo base_ecobox.txt
                // Unimos la tabla de plantas con la de usuarios por medio de la familia
                val sql = """
                    SELECT p.id, p.nombre, p.especie, p.fecha_plantacion, p.descripcion, p.familia_id 
                    FROM main_planta p
                    INNER JOIN main_familia_usuario fu ON p.familia_id = fu.familia_id
                    WHERE fu.usuario_id = ? 
                    ORDER BY p.id DESC
                """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)

                val rs: ResultSet = stmt.executeQuery()

                while (rs.next()) {
                    // Mapeamos los resultados a tu objeto Planta
                    val planta = Planta(
                        rs.getLong("id"),
                        rs.getString("nombre") ?: "Sin Nombre",
                        rs.getString("especie") ?: "Desconocida",
                        rs.getString("fecha_plantacion") ?: "",
                        rs.getString("descripcion") ?: "",
                        rs.getLong("familia_id")
                    )
                    listaPlantas.add(planta)
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo plantas", e)
        }
        return listaPlantas
    }

    /**
     * Obtiene estadísticas rápidas para el header del Dashboard.
     * Retorna un arreglo: [Total, Saludables, Críticas]
     */
    fun obtenerEstadisticas(userId: Long): IntArray {
        // [0] = Total, [1] = Saludables, [2] = Críticas
        val stats = intArrayOf(0, 0, 0)
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // 1. Contar Total de Plantas del usuario
                val sqlTotal = """
                    SELECT COUNT(*) as total 
                    FROM main_planta p
                    INNER JOIN main_familia_usuario fu ON p.familia_id = fu.familia_id
                    WHERE fu.usuario_id = ?
                """
                val stmt = conexion.prepareStatement(sqlTotal)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    stats[0] = rs.getInt("total")
                }

                // 2. Simulamos el estado de salud (En el futuro esto vendrá de la tabla 'medicion' o 'notificacion')
                // Por ahora asumimos: 0 críticas, todas saludables.
                stats[2] = 0
                stats[1] = stats[0] - stats[2] // Saludables = Total - Críticas

                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error estadísticas", e)
        }
        return stats
    }
}