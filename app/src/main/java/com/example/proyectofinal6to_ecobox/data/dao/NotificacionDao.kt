package com.example.proyectofinal6to_ecobox.data.dao

import android.util.Log
import com.example.proyectofinal6to_ecobox.data.model.Notificacion
import java.text.SimpleDateFormat
import java.util.*

object NotificacionDao {

    fun obtenerNotificaciones(userId: Long): List<Notificacion> {
        val lista = ArrayList<Notificacion>()
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = """
                    SELECT 
                        n.id,
                        n.tipo as titulo,
                        n.mensaje,
                        DATE_FORMAT(n.fecha_creacion, '%d/%m/%Y %H:%i') as fecha,
                        n.leida,
                        n.usuario_id
                    FROM notificacion n
                    WHERE n.usuario_id = ? 
                    ORDER BY n.fecha_creacion DESC
                    LIMIT 50
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    lista.add(Notificacion(
                        rs.getLong("id"),
                        obtenerTituloFormateado(rs.getString("titulo")),
                        rs.getString("mensaje"),
                        rs.getString("fecha"),
                        rs.getBoolean("leida"),
                        rs.getLong("usuario_id")
                    ))
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error obteniendo notificaciones", e)
        }
        return lista
    }

    private fun obtenerTituloFormateado(tipo: String): String {
        return when (tipo.lowercase()) {
            "alerta" -> "⚠️ Alerta"
            "info" -> "ℹ️ Información"
            else -> tipo
        }
    }

    // FUNCIÓN MEJORADA PARA EVITAR DUPLICADOS
    fun existeNotificacionRecienteMejorada(
        userId: Long,
        tipoAlerta: String,
        nombrePlanta: String,
        mensajeEspecifico: String = ""
    ): Boolean {
        var existe = false
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                // Determinar el tipo de notificación basado en el contenido
                val tipoNotificacion = when {
                    tipoAlerta.contains("critic", true) ||
                            tipoAlerta.contains("urgent", true) ||
                            tipoAlerta.contains("calor_excesivo", true) ||
                            tipoAlerta.contains("temperatura_baja_critica", true) ||
                            tipoAlerta.contains("riego_pendiente", true) ||
                            mensajeEspecifico.contains("URGENTE", true) ||
                            mensajeEspecifico.contains("CRÍTICO", true) -> "alerta"
                    else -> "info"
                }

                val sql = """
                    SELECT COUNT(*) as count
                    FROM notificacion 
                    WHERE usuario_id = ? 
                        AND mensaje LIKE ?
                        AND tipo = ?
                        AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL 24 HOUR)  -- Últimas 24h
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                stmt.setString(2, "%$nombrePlanta%")
                stmt.setString(3, tipoNotificacion)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    existe = rs.getInt("count") > 0
                    Log.d("NotificacionDao", "Verificando duplicado para $nombrePlanta ($tipoAlerta): $existe")
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error verificando notificación reciente mejorada", e)
        }
        return existe
    }

    // FUNCIÓN ORIGINAL (mantener por compatibilidad)
    fun existeNotificacionReciente(userId: Long, tipo: String, nombrePlanta: String): Boolean {
        // Usar la función mejorada por defecto
        return existeNotificacionRecienteMejorada(userId, tipo, nombrePlanta)
    }

    fun crearNotificacion(userId: Long, titulo: String, mensaje: String): Boolean {
        var exito = false
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                // Determinar tipo basado en contenido del título y mensaje
                val tipo = when {
                    titulo.contains("CRÍTICO", true) ||
                            titulo.contains("URGENTE", true) ||
                            titulo.contains("¡CALOR EXCESIVO!", true) ||
                            titulo.contains("¡TEMPERATURA BAJA!", true) ||
                            titulo.contains("⚠️", true) ||
                            mensaje.contains("URGENTE", true) ||
                            mensaje.contains("CRÍTICO", true) -> "alerta"
                    else -> "info"
                }

                // Verificar si ya existe una notificación IDÉNTICA hoy
                val sqlCheck = """
                    SELECT COUNT(*) as count
                    FROM notificacion 
                    WHERE usuario_id = ? 
                        AND mensaje = ?
                        AND DATE(fecha_creacion) = CURDATE()
                """.trimIndent()

                val stmtCheck = conexion.prepareStatement(sqlCheck)
                stmtCheck.setLong(1, userId)
                stmtCheck.setString(2, mensaje)
                val rsCheck = stmtCheck.executeQuery()

                var yaExiste = false
                if (rsCheck.next() && rsCheck.getInt("count") > 0) {
                    yaExiste = true
                    Log.d("NotificacionDao", "Notificación idéntica ya existe hoy: $mensaje")
                }
                rsCheck.close()
                stmtCheck.close()

                if (!yaExiste) {
                    val sql = """
                        INSERT INTO notificacion (mensaje, leida, fecha_creacion, tipo, usuario_id) 
                        VALUES (?, 0, NOW(), ?, ?)
                    """.trimIndent()

                    val stmt = conexion.prepareStatement(sql)
                    stmt.setString(1, mensaje)
                    stmt.setString(2, tipo)
                    stmt.setLong(3, userId)
                    exito = stmt.executeUpdate() > 0

                    if (exito) {
                        Log.d("NotificacionDao", "✅ Notificación creada: $titulo")
                        Log.d("NotificacionDao", "   Mensaje: $mensaje")
                    }
                } else {
                    Log.d("NotificacionDao", "⏭️ Notificación duplicada omitida: $titulo")
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error creando notificación", e)
        }
        return exito
    }

    fun marcarTodasComoLeidas(userId: Long): Boolean {
        var exito = false
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = "UPDATE notificacion SET leida = 1 WHERE usuario_id = ? AND leida = 0"
                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                exito = stmt.executeUpdate() > 0
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error marcando notificaciones como leídas", e)
        }
        return exito
    }

    fun marcarComoLeida(notificacionId: Long): Boolean {
        var exito = false
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = "UPDATE notificacion SET leida = 1 WHERE id = ?"
                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, notificacionId)
                exito = stmt.executeUpdate() > 0
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error marcando notificación como leída", e)
        }
        return exito
    }

    fun eliminarNotificacion(notificacionId: Long): Boolean {
        var exito = false
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = "DELETE FROM notificacion WHERE id = ?"
                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, notificacionId)
                exito = stmt.executeUpdate() > 0
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error eliminando notificación", e)
        }
        return exito
    }

    fun obtenerNotificacionesNoLeidas(userId: Long): List<Notificacion> {
        val lista = ArrayList<Notificacion>()
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = """
                    SELECT 
                        n.id,
                        n.tipo as titulo,
                        n.mensaje,
                        DATE_FORMAT(n.fecha_creacion, '%d/%m/%Y %H:%i') as fecha,
                        n.leida,
                        n.usuario_id
                    FROM notificacion n
                    WHERE n.usuario_id = ? AND n.leida = 0
                    ORDER BY n.fecha_creacion DESC
                    LIMIT 20
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    lista.add(Notificacion(
                        rs.getLong("id"),
                        obtenerTituloFormateado(rs.getString("titulo")),
                        rs.getString("mensaje"),
                        rs.getString("fecha"),
                        false,
                        rs.getLong("usuario_id")
                    ))
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error obteniendo notificaciones no leídas", e)
        }
        return lista
    }

    // FUNCIÓN ESPECÍFICA PARA EVITAR DUPLICADOS DE RIEGO
    fun existeAlertaRiegoHoy(userId: Long): Boolean {
        var existe = false
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = """
                    SELECT COUNT(*) as count
                    FROM notificacion 
                    WHERE usuario_id = ? 
                        AND (mensaje LIKE '%no ha sido regada en%' OR mensaje LIKE '%nunca ha sido regada%')
                        AND DATE(fecha_creacion) = CURDATE()
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    existe = rs.getInt("count") > 0
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error verificando alertas de riego hoy", e)
        }
        return existe
    }

    // FUNCIÓN PARA LIMPIAR NOTIFICACIONES ANTIGUAS
    fun limpiarNotificacionesAntiguas(userId: Long, dias: Int = 30): Int {
        var eliminadas = 0
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = """
                    DELETE FROM notificacion 
                    WHERE usuario_id = ? 
                        AND fecha_creacion < DATE_SUB(NOW(), INTERVAL ? DAY)
                        AND leida = 1  -- Solo eliminar notificaciones leídas
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                stmt.setInt(2, dias)
                eliminadas = stmt.executeUpdate()
                conexion.close()

                Log.d("NotificacionDao", "🗑️ Eliminadas $eliminadas notificaciones antiguas")
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error limpiando notificaciones antiguas", e)
        }
        return eliminadas
    }

    // FUNCIÓN PARA OBTENER RESUMEN POR TIPO
    fun obtenerResumenPorTipo(userId: Long): Map<String, Int> {
        val resumen = mutableMapOf(
            "total" to 0,
            "alerta" to 0,
            "info" to 0,
            "no_leidas" to 0
        )
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = """
                    SELECT 
                        COUNT(*) as total,
                        COUNT(CASE WHEN tipo = 'alerta' THEN 1 END) as alertas,
                        COUNT(CASE WHEN tipo = 'info' THEN 1 END) as infos,
                        COUNT(CASE WHEN leida = 0 THEN 1 END) as no_leidas
                    FROM notificacion 
                    WHERE usuario_id = ?
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    resumen["total"] = rs.getInt("total")
                    resumen["alerta"] = rs.getInt("alertas")
                    resumen["info"] = rs.getInt("infos")
                    resumen["no_leidas"] = rs.getInt("no_leidas")
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("NotificacionDao", "Error obteniendo resumen por tipo", e)
        }
        return resumen
    }
}