package com.example.proyectofinal6to_ecobox.data

import android.util.Log
import com.example.proyectofinal6to_ecobox.data.dao.MySqlConexion
import com.example.proyectofinal6to_ecobox.data.dao.NotificacionDao
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import java.util.*
import kotlin.collections.ArrayList

object AlertGenerator {
    // Función común para clasificar (igual que en PlantaDao)
    private fun clasificarPlantaParaAlerta(estado: String?, humedad: Float?): String {
        return when {
            estado == "Necesita agua" || estado == "Crítico" || (humedad != null && humedad < 30) -> "critical"
            estado == "Advertencia" || (humedad != null && humedad < 40) -> "warning"
            estado == "Saludable" || estado == "Excelente" -> "healthy"
            else -> "unknown"
        }
    }

    fun verificarYGenerarAlertas(userId: Long) {
        try {
            // Obtener todas las plantas del usuario con sus datos actuales de sensores
            val plantasConDatos = obtenerPlantasConSensores(userId)

            for (plantaArray in plantasConDatos) {
                val plantaId = plantaArray[0] as Long
                val plantaNombre = plantaArray[1] as String
                val humedad = plantaArray[2] as? Float
                val temperatura = plantaArray[3] as? Float
                val luz = plantaArray[4] as? Float
                val humedadAire = plantaArray[5] as? Float

                // Evaluar cada parámetro y generar notificaciones si es necesario

                // 1. Evaluar humedad del suelo (CON UMBRALES UNIFICADOS)
                evaluarHumedad(userId, plantaId, plantaNombre, humedad)

                // 2. Evaluar temperatura
                evaluarTemperatura(userId, plantaId, plantaNombre, temperatura)

                // 3. Evaluar luz
                evaluarLuz(userId, plantaId, plantaNombre, luz)

                // 4. Evaluar humedad del aire
                evaluarHumedadAire(userId, plantaId, plantaNombre, humedadAire)

                // 5. Evaluar estado general basado en seguimiento_estado_planta (CON CLASIFICACIÓN UNIFICADA)
                evaluarEstadoPlanta(userId, plantaId, plantaNombre, humedad)
            }

            // Verificar riegos pendientes
            verificarRiegosPendientes(userId)

        } catch (e: Exception) {
            Log.e("AlertGen", "Error generando alertas", e)
        }
    }

    private fun obtenerPlantasConSensores(userId: Long): List<Array<Any?>> {
        val plantas = ArrayList<Array<Any?>>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    SELECT 
                        p.id as planta_id,
                        p.nombre as planta_nombre,
                        MAX(CASE WHEN ts.nombre = 'Humedad Suelo' THEN m.valor END) as humedad_suelo,
                        MAX(CASE WHEN ts.nombre = 'Temperatura' THEN m.valor END) as temperatura,
                        MAX(CASE WHEN ts.nombre = 'Luz' THEN m.valor END) as luz,
                        MAX(CASE WHEN ts.nombre = 'Humedad Aire' THEN m.valor END) as humedad_aire
                    FROM planta p
                    INNER JOIN familia f ON p.familia_id = f.id
                    INNER JOIN familia_usuario fu ON f.id = fu.familia_id
                    INNER JOIN main_sensor s ON p.id = s.planta_id
                    INNER JOIN tipo_sensor ts ON s.tipo_sensor_id = ts.id
                    INNER JOIN medicion m ON s.id = m.sensor_id
                    WHERE fu.usuario_id = ? 
                        AND fu.activo = 1
                        AND s.activo = 1
                        AND m.fecha >= DATE_SUB(NOW(), INTERVAL 1 HOUR)  -- Última hora
                    GROUP BY p.id, p.nombre
                    HAVING COUNT(m.id) > 0
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    plantas.add(
                        arrayOf(
                            rs.getLong("planta_id"),
                            rs.getString("planta_nombre"),
                            if (!rs.wasNull()) rs.getFloat("humedad_suelo") else null,
                            if (!rs.wasNull()) rs.getFloat("temperatura") else null,
                            if (!rs.wasNull()) rs.getFloat("luz") else null,
                            if (!rs.wasNull()) rs.getFloat("humedad_aire") else null
                        )
                    )
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("AlertGen", "Error obteniendo plantas con sensores", e)
        }

        return plantas
    }

    private fun evaluarHumedad(
        userId: Long,
        plantaId: Long,
        plantaNombre: String,
        humedad: Float?
    ) {
        if (humedad == null) return

        // USANDO LOS MISMOS UMBRALES QUE EN ESTADÍSTICAS
        when {
            humedad < 30 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "humedad_critica",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "¡CRÍTICO! Humedad baja",
                        "$plantaNombre tiene solo ${humedad.toInt()}% de humedad. ¡Necesita riego URGENTE!"
                    )
                }
            }

            humedad < 40 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "humedad_advertencia",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "Advertencia: Humedad moderada",
                        "$plantaNombre tiene ${humedad.toInt()}% de humedad. Considera regarla pronto."
                    )
                }
            }

            humedad > 85 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "exceso_humedad",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "Exceso de humedad",
                        "$plantaNombre tiene ${humedad.toInt()}% de humedad. Podría tener demasiada agua."
                    )
                }
            }
        }
    }

    private fun evaluarTemperatura(
        userId: Long,
        plantaId: Long,
        plantaNombre: String,
        temperatura: Float?
    ) {
        if (temperatura == null) return

        when {
            temperatura > 35 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "calor_excesivo",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "¡CALOR EXCESIVO!",
                        "$plantaNombre registra ${temperatura.toInt()}°C. Considera moverla a un lugar más fresco."
                    )
                }
            }

            temperatura > 30 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "temperatura_alta",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "Temperatura alta",
                        "$plantaNombre tiene ${temperatura.toInt()}°C. Mantén un ojo en ella."
                    )
                }
            }

            temperatura < 10 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "temperatura_baja_critica",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "¡TEMPERATURA BAJA!",
                        "$plantaNombre registra ${temperatura.toInt()}°C. Podría sufrir daños por frío."
                    )
                }
            }

            temperatura < 15 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "temperatura_baja",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "Temperatura baja",
                        "$plantaNombre tiene ${temperatura.toInt()}°C. Verifica que esté en un lugar adecuado."
                    )
                }
            }
        }
    }

    private fun evaluarLuz(userId: Long, plantaId: Long, plantaNombre: String, luz: Float?) {
        if (luz == null) return

        when {
            luz < 100 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "luz_insuficiente",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "Luz insuficiente",
                        "$plantaNombre recibe solo ${luz.toInt()} lux. Considera moverla a un lugar más iluminado."
                    )
                }
            }

            luz > 2000 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "luz_excesiva",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "Luz excesiva",
                        "$plantaNombre recibe ${luz.toInt()} lux. Demasiada luz directa puede dañarla."
                    )
                }
            }
        }
    }

    private fun evaluarHumedadAire(
        userId: Long,
        plantaId: Long,
        plantaNombre: String,
        humedadAire: Float?
    ) {
        if (humedadAire == null) return

        when {
            humedadAire < 30 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "aire_seco",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "Aire muy seco",
                        "El ambiente de $plantaNombre tiene solo ${humedadAire.toInt()}% de humedad. Considera usar un humidificador."
                    )
                }
            }

            humedadAire > 80 -> {
                if (!NotificacionDao.existeNotificacionRecienteMejorada(
                        userId,
                        "aire_humedo",
                        plantaNombre
                    )
                ) {
                    NotificacionDao.crearNotificacion(
                        userId,
                        "Aire muy húmedo",
                        "El ambiente de $plantaNombre tiene ${humedadAire.toInt()}% de humedad. Alto riesgo de hongos."
                    )
                }
            }
        }
    }

    private fun evaluarEstadoPlanta(
        userId: Long,
        plantaId: Long,
        plantaNombre: String,
        humedadActual: Float? = null
    ) {
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // Obtener el último estado registrado
                val sql = """
                    SELECT sep.estado, sep.observaciones, sep.fecha_registro
                    FROM seguimiento_estado_planta sep
                    INNER JOIN (
                        SELECT planta_id, MAX(fecha_registro) as max_fecha
                        FROM seguimiento_estado_planta
                        GROUP BY planta_id
                    ) ult ON sep.planta_id = ult.planta_id AND sep.fecha_registro = ult.max_fecha
                    WHERE sep.planta_id = ?
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, plantaId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    val estado = rs.getString("estado")
                    val observaciones = rs.getString("observaciones")
                    val fechaRegistro = rs.getTimestamp("fecha_registro")

                    // USAR LA MISMA CLASIFICACIÓN QUE LAS ESTADÍSTICAS
                    val clasificacion = clasificarPlantaParaAlerta(estado, humedadActual)

                    // Calcular días desde el último registro
                    val diasDesdeRegistro = Calendar.getInstance().apply {
                        time = Date()
                    }.get(Calendar.DAY_OF_YEAR) - Calendar.getInstance().apply {
                        time = fechaRegistro
                    }.get(Calendar.DAY_OF_YEAR)

                    // Solo generar alertas si no hay una reciente
                    when (clasificacion) {
                        "critical" -> {
                            if (diasDesdeRegistro >= 1 && !NotificacionDao.existeNotificacionRecienteMejorada(
                                    userId,
                                    "estado_critico",
                                    plantaNombre
                                )
                            ) {
                                val humedadTexto = if (humedadActual != null) " (Humedad: ${humedadActual.toInt()}%)" else ""
                                NotificacionDao.crearNotificacion(
                                    userId,
                                    "⚠️ $estado - $plantaNombre",
                                    "$observaciones$humedadTexto. ¡Acción requerida!"
                                )
                            }
                        }
                        "warning" -> {
                            if (diasDesdeRegistro >= 2 && !NotificacionDao.existeNotificacionRecienteMejorada(
                                    userId,
                                    "estado_advertencia",
                                    plantaNombre
                                )
                            ) {
                                val humedadTexto = if (humedadActual != null) " (Humedad: ${humedadActual.toInt()}%)" else ""
                                NotificacionDao.crearNotificacion(
                                    userId,
                                    "ℹ️ $estado - $plantaNombre",
                                    "$observaciones$humedadTexto. Monitorea tu planta."
                                )
                            }
                        }
                    }
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("AlertGen", "Error evaluando estado planta", e)
        }
    }

    private fun verificarRiegosPendientes(userId: Long) {
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // Primero verificar si ya hay una alerta de riego hoy para evitar duplicados
                val sqlCheck = """
                    SELECT COUNT(*) as existe 
                    FROM notificacion 
                    WHERE usuario_id = ? 
                        AND mensaje LIKE '%no ha sido regada en%'
                        AND DATE(fecha_creacion) = CURDATE()
                """.trimIndent()

                val stmtCheck = conexion.prepareStatement(sqlCheck)
                stmtCheck.setLong(1, userId)
                val rsCheck = stmtCheck.executeQuery()

                if (rsCheck.next() && rsCheck.getInt("existe") > 0) {
                    Log.d("AlertGen", "Ya existe alerta de riego hoy, omitiendo...")
                    conexion.close()
                    return  // Salir si ya hay alerta hoy
                }
                rsCheck.close()
                stmtCheck.close()

                // SQL CORREGIDO: Solo plantas con último riego > 3 días o nunca regadas
                val sql = """
                    SELECT p.id, p.nombre, 
                           CASE 
                               WHEN MAX(r.fecha) IS NULL THEN NULL
                               ELSE DATEDIFF(NOW(), MAX(r.fecha))
                           END as dias_sin_regar
                    FROM planta p
                    INNER JOIN familia f ON p.familia_id = f.id
                    INNER JOIN familia_usuario fu ON f.id = fu.familia_id
                    LEFT JOIN riego r ON p.id = r.planta_id
                    WHERE fu.usuario_id = ? 
                        AND fu.activo = 1
                    GROUP BY p.id, p.nombre
                    HAVING MAX(r.fecha) IS NULL 
                        OR DATEDIFF(NOW(), MAX(r.fecha)) > 3
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    val plantaId = rs.getLong("id")
                    val plantaNombre = rs.getString("nombre")
                    val diasSinRegar = rs.getInt("dias_sin_regar")

                    if (!NotificacionDao.existeNotificacionRecienteMejorada(
                            userId,
                            "riego_pendiente",
                            plantaNombre
                        )
                    ) {
                        val mensaje = if (rs.wasNull()) {
                            "$plantaNombre nunca ha sido regada. ¡Programa su primer riego!"
                        } else {
                            "$plantaNombre no ha sido regada en $diasSinRegar días. ¡Es hora de regarla!"
                        }

                        NotificacionDao.crearNotificacion(
                            userId,
                            "⏰ Riego pendiente",
                            mensaje
                        )
                    }
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("AlertGen", "Error verificando riegos pendientes", e)
        }
    }

    // Función para obtener notificaciones de manera más eficiente
    fun obtenerResumenAlertas(userId: Long): Map<String, Int> {
        val resumen = mutableMapOf(
            "criticas" to 0,
            "advertencias" to 0
        )

        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    SELECT 
                        COUNT(CASE WHEN tipo = 'alerta' AND leida = 0 THEN 1 END) as criticas,
                        COUNT(CASE WHEN tipo = 'info' AND leida = 0 THEN 1 END) as advertencias
                    FROM notificacion 
                    WHERE usuario_id = ?
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    resumen["criticas"] = rs.getInt("criticas")
                    resumen["advertencias"] = rs.getInt("advertencias")
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("AlertGen", "Error obteniendo resumen alertas", e)
        }

        return resumen
    }
}