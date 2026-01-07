package com.example.proyectofinal6to_ecobox.data.dao

import android.util.Log
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Planta
import java.math.BigDecimal
import java.sql.ResultSet
import java.util.ArrayList

object PlantaDao {

    // =========================================================================
    // DATA CLASSES AUXILIARES
    // =========================================================================

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
                else -> (humedad / 20f * 20f).toInt()
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

    data class PlantaConDatos(
        val id: Long,
        val nombre: String,
        val especie: String,
        val ubicacion: String,
        val humedad: Float,
        val temperatura: Float,
        val luz: Float
    )

    // =========================================================================
    // FUNCIONES PRINCIPALES PARA DASHBOARD/HISTORIAL GENERAL
    // =========================================================================

    /**
     * Obtiene las plantas básicas del usuario (SOLO de su familia)
     */
    fun obtenerPlantasPorUsuario(userId: Long): List<Planta> {
        val listaPlantas = ArrayList<Planta>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                SELECT 
                    p.id, 
                    p.nombrePersonalizado as nombre, 
                    p.especie, 
                    p.fecha_creacion, 
                    p.descripcion, 
                    p.familia_id,
                    p.foto,
                    p.estado,
                    p.aspecto
                FROM planta p
                INNER JOIN familia_usuario fu ON p.familia_id = fu.familia_id
                WHERE fu.usuario_id = ? AND fu.activo = 1
                ORDER BY p.nombrePersonalizado ASC
            """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)

                val rs: ResultSet = stmt.executeQuery()

                while (rs.next()) {
                    val planta = Planta(
                        rs.getLong("id"),
                        rs.getString("nombre") ?: "Sin Nombre",
                        rs.getString("especie") ?: "Desconocida",
                        rs.getString("fecha_creacion") ?: "",
                        rs.getString("descripcion") ?: "",
                        rs.getLong("familia_id"),
                        "Sin ubicación",
                        rs.getString("aspecto") ?: "normal",
                        rs.getString("estado") ?: "normal",
                        rs.getString("foto") ?: ""
                    )
                    listaPlantas.add(planta)
                }

                Log.d("PlantaDao", "Encontradas ${listaPlantas.size} plantas para usuario $userId")
                rs.close()
                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo plantas: ${e.message}", e)
            return obtenerPlantasPorUsuarioVersionAnterior(userId)
        }
        return listaPlantas
    }

    /**
     * Versión anterior de la consulta (sin foto, estado, aspecto)
     */
    private fun obtenerPlantasPorUsuarioVersionAnterior(userId: Long): List<Planta> {
        val listaPlantas = ArrayList<Planta>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                SELECT 
                    p.id, 
                    p.nombrePersonalizado as nombre, 
                    p.especie, 
                    p.fecha_creacion, 
                    p.descripcion, 
                    p.familia_id
                FROM planta p
                INNER JOIN familia_usuario fu ON p.familia_id = fu.familia_id
                WHERE fu.usuario_id = ? AND fu.activo = 1
                ORDER BY p.nombrePersonalizado ASC
            """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)

                val rs: ResultSet = stmt.executeQuery()

                while (rs.next()) {
                    val planta = Planta(
                        rs.getLong("id"),
                        rs.getString("nombre") ?: "Sin Nombre",
                        rs.getString("especie") ?: "Desconocida",
                        rs.getString("fecha_creacion") ?: "",
                        rs.getString("descripcion") ?: "",
                        rs.getLong("familia_id"),
                        "Sin ubicación"
                    )
                    listaPlantas.add(planta)
                }

                Log.d("PlantaDao", "Versión anterior: ${listaPlantas.size} plantas")
                rs.close()
                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error en versión anterior: ${e.message}", e)
        }
        return listaPlantas
    }

    /**
     * Obtiene estadísticas del dashboard (SOLO de mi familia)
     */
    fun obtenerEstadisticas(userId: Long): IntArray {
        val stats = intArrayOf(0, 0, 0)
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // 1. Total de plantas del usuario
                val sqlTotal = """
                    SELECT COUNT(DISTINCT p.id) as total 
                    FROM planta p
                    INNER JOIN familia_usuario fu ON p.familia_id = fu.familia_id
                    WHERE fu.usuario_id = ? AND fu.activo = 1
                """
                val stmtTotal = conexion.prepareStatement(sqlTotal)
                stmtTotal.setLong(1, userId)
                val rsTotal = stmtTotal.executeQuery()

                if (rsTotal.next()) {
                    stats[0] = rsTotal.getInt("total")
                }
                rsTotal.close()
                stmtTotal.close()

                // 2. Plantas saludables
                val sqlSaludables = """
                    SELECT COUNT(DISTINCT p.id) as saludables
                    FROM planta p
                    INNER JOIN familia_usuario fu ON p.familia_id = fu.familia_id
                    INNER JOIN (
                        SELECT sep1.* 
                        FROM seguimiento_estado_planta sep1
                        INNER JOIN (
                            SELECT planta_id, MAX(fecha_registro) as max_fecha
                            FROM seguimiento_estado_planta
                            GROUP BY planta_id
                        ) sep2 ON sep1.planta_id = sep2.planta_id AND sep1.fecha_registro = sep2.max_fecha
                    ) ult_seg ON p.id = ult_seg.planta_id
                    WHERE fu.usuario_id = ? 
                      AND fu.activo = 1
                      AND ult_seg.estado IN ('Saludable', 'Excelente')
                """
                val stmtSalud = conexion.prepareStatement(sqlSaludables)
                stmtSalud.setLong(1, userId)
                val rsSalud = stmtSalud.executeQuery()

                if (rsSalud.next()) {
                    stats[1] = rsSalud.getInt("saludables")
                }
                rsSalud.close()
                stmtSalud.close()

                // 3. Plantas críticas
                val sqlCriticas = """
                    SELECT COUNT(DISTINCT p.id) as criticas
                    FROM planta p
                    INNER JOIN familia_usuario fu ON p.familia_id = fu.familia_id
                    LEFT JOIN (
                        SELECT sep1.* FROM seguimiento_estado_planta sep1
                        INNER JOIN (
                            SELECT planta_id, MAX(fecha_registro) as max_fecha
                            FROM seguimiento_estado_planta
                            GROUP BY planta_id
                        ) sep2 ON sep1.planta_id = sep2.planta_id AND sep1.fecha_registro = sep2.max_fecha
                    ) ult_seg ON p.id = ult_seg.planta_id
                    LEFT JOIN main_sensor ms ON p.id = ms.planta_id AND ms.tipo_sensor_id = 2
                    LEFT JOIN medicion m ON ms.id = m.sensor_id 
                      AND m.fecha = (SELECT MAX(fecha) FROM medicion WHERE sensor_id = ms.id)
                    LEFT JOIN notificacion n ON n.usuario_id = fu.usuario_id AND n.leida = 0
                    WHERE fu.usuario_id = ? 
                      AND fu.activo = 1
                      AND (
                          ult_seg.estado IN ('Necesita agua', 'Advertencia', 'Crítico')
                          OR (m.valor IS NOT NULL AND m.valor < 40)
                          OR (
                              n.id IS NOT NULL 
                              AND n.mensaje LIKE CONCAT('%', p.nombrePersonalizado, '%')
                              AND (
                                  n.mensaje LIKE '%baja%' 
                                  OR n.mensaje LIKE '%advertencia%' 
                                  OR n.mensaje LIKE '%crítico%'
                                  OR n.mensaje LIKE '%necesita agua%'
                              )
                          )
                      )
                """

                val stmtCrit = conexion.prepareStatement(sqlCriticas)
                stmtCrit.setLong(1, userId)
                val rsCrit = stmtCrit.executeQuery()

                if (rsCrit.next()) {
                    stats[2] = rsCrit.getInt("criticas")
                }
                rsCrit.close()
                stmtCrit.close()

                conexion.close()
                Log.d(
                    "PlantaDao",
                    "Estadísticas: Total=${stats[0]}, Saludables=${stats[1]}, Críticas=${stats[2]}"
                )
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo estadísticas: ${e.message}", e)
        }
        return stats
    }

    /**
     * Obtiene plantas con todos sus datos combinados (para el adapter)
     */
    fun obtenerPlantasCompletas(userId: Long): List<PlantaCompleta> {
        val plantasCompletas = mutableListOf<PlantaCompleta>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val plantas = obtenerPlantasPorUsuario(userId)

                for (planta in plantas) {
                    val datosSensores = obtenerDatosSensoresPlanta(planta.id)

                    val plantaCompleta = PlantaCompleta(
                        planta = planta,
                        ubicacion = datosSensores["ubicacion"] as? String ?: "Sin ubicación",
                        humedad = datosSensores["humedad"] as? Float ?: 0f,
                        temperatura = datosSensores["temperatura"] as? Float ?: 0f,
                        luz = datosSensores["luz"] as? Float ?: 0f,
                        ultimoRiego = datosSensores["ultimo_riego"] as? String ?: "Sin registro",
                        estado = datosSensores["estado"] as? String ?: "Desconocido"
                    )
                    plantasCompletas.add(plantaCompleta)
                }

                conexion.close()
                Log.d("PlantaDao", "Plantas completas encontradas: ${plantasCompletas.size}")
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo plantas completas: ${e.message}", e)
        }

        return plantasCompletas
    }

    /**
     * Obtiene eventos recientes SOLO de plantas de mi familia
     */
    fun obtenerEventosRecientesFamiliar(
        userId: Long,
        limit: Int,
        plantaId: Long = -1
    ): List<EventoDAO> {
        val eventos = mutableListOf<EventoDAO>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val familiaId = obtenerFamiliaIdDelUsuario(userId)
                if (familiaId == -1L) {
                    return eventos
                }

                val plantaCondition = if (plantaId != -1L) "AND p.id = ?" else ""
                val params = mutableListOf<Any>()

                var sql = """
                    SELECT 
                        'Riego' as tipo,
                        p.nombrePersonalizado as planta,
                        r.fecha,
                        CONCAT('Regado: ', FORMAT(r.cantidad_agua, 1), 'L') as descripcion,
                        1 as icono_tipo
                    FROM riego r
                    INNER JOIN planta p ON r.planta_id = p.id
                    WHERE p.familia_id = ?
                """

                params.add(familiaId)

                if (plantaId != -1L) {
                    sql += " $plantaCondition"
                    params.add(plantaId)
                }

                sql += """
                
                UNION ALL
                
                SELECT 
                    'Estado Cambiado' as tipo,
                    p.nombrePersonalizado as planta,
                    sep.fecha_registro as fecha,
                    CONCAT('Nuevo estado: ', sep.estado) as descripcion,
                    3 as icono_tipo
                FROM seguimiento_estado_planta sep
                INNER JOIN planta p ON sep.planta_id = p.id
                WHERE p.familia_id = ?
                """

                params.add(familiaId)

                if (plantaId != -1L) {
                    sql += " $plantaCondition"
                    params.add(plantaId)
                }

                sql += """
                
                ORDER BY fecha DESC
                LIMIT ?
                """

                params.add(limit)

                val stmt = conexion.prepareStatement(sql)
                params.forEachIndexed { index, param ->
                    when (param) {
                        is Long -> stmt.setLong(index + 1, param)
                        is Int -> stmt.setInt(index + 1, param)
                        is String -> stmt.setString(index + 1, param)
                    }
                }

                val rs = stmt.executeQuery()

                while (rs.next()) {
                    eventos.add(
                        EventoDAO(
                            tipo = rs.getString("tipo"),
                            planta = rs.getString("planta"),
                            fecha = rs.getString("fecha"),
                            descripcion = rs.getString("descripcion"),
                            iconoTipo = rs.getInt("icono_tipo")
                        )
                    )
                }

                rs.close()
                stmt.close()
                conexion.close()

                Log.d("PlantaDao", "Eventos familiares obtenidos: ${eventos.size}")
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo eventos familiares: ${e.message}", e)
        }

        return eventos
    }

    /**
     * Obtiene estadísticas históricas de sensores SOLO para plantas de mi familia
     */
    fun obtenerEstadisticasHistorialFamiliar(
        userId: Long,
        horas: Int,
        plantaId: Long = -1
    ): Map<String, Float> {
        val stats = mutableMapOf<String, Float>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val familiaId = obtenerFamiliaIdDelUsuario(userId)
                if (familiaId == -1L) {
                    Log.e("PlantaDao", "Usuario no pertenece a ninguna familia activa")
                    return mapOf("humedad" to 0f, "temperatura" to 0f, "luz" to 0f)
                }

                val plantaCondition = if (plantaId != -1L) "AND p.id = ?" else ""
                val params = mutableListOf<Any>()

                var sql = """
                    SELECT 
                        AVG(CASE WHEN ts.nombre LIKE '%Humedad%' THEN m.valor ELSE NULL END) as humedad_avg,
                        AVG(CASE WHEN ts.nombre LIKE '%Temperatura%' THEN m.valor ELSE NULL END) as temp_avg,
                        AVG(CASE WHEN ts.nombre LIKE '%Luz%' THEN m.valor ELSE NULL END) as luz_avg
                    FROM planta p
                    INNER JOIN main_sensor ms ON p.id = ms.planta_id
                    INNER JOIN tipo_sensor ts ON ms.tipo_sensor_id = ts.id
                    INNER JOIN medicion m ON ms.id = m.sensor_id
                    WHERE p.familia_id = ?
                      AND ms.activo = 1
                      AND m.fecha >= DATE_SUB(NOW(), INTERVAL ? HOUR)
                """

                params.add(familiaId)
                params.add(horas)

                if (plantaId != -1L) {
                    sql += " $plantaCondition"
                    params.add(plantaId)
                }

                val stmt = conexion.prepareStatement(sql)
                params.forEachIndexed { index, param ->
                    when (param) {
                        is Long -> stmt.setLong(index + 1, param)
                        is Int -> stmt.setInt(index + 1, param)
                    }
                }

                val rs = stmt.executeQuery()

                if (rs.next()) {
                    stats["humedad"] = if (!rs.wasNull()) rs.getFloat("humedad_avg") else 0f
                    stats["temperatura"] = if (!rs.wasNull()) rs.getFloat("temp_avg") else 0f
                    stats["luz"] = if (!rs.wasNull()) rs.getFloat("luz_avg") else 0f
                }

                rs.close()
                stmt.close()
                conexion.close()

                Log.d("PlantaDao", "Estadísticas familiares: $stats")

                if ((stats["humedad"] ?: 0f) == 0f &&
                    (stats["temperatura"] ?: 0f) == 0f &&
                    (stats["luz"] ?: 0f) == 0f
                ) {
                    return mapOf(
                        "humedad" to 65f,
                        "temperatura" to 22f,
                        "luz" to 75f
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(
                "PlantaDao",
                "Error obteniendo estadísticas históricas familiares: ${e.message}",
                e
            )
            return mapOf("humedad" to 65f, "temperatura" to 22f, "luz" to 75f)
        }

        return stats
    }

    /**
     * Obtiene plantas de mi familia con datos actuales
     */
    fun obtenerPlantasFamiliaConDatos(userId: Long): List<PlantaConDatos> {
        val plantas = mutableListOf<PlantaConDatos>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val familiaId = obtenerFamiliaIdDelUsuario(userId)
                if (familiaId == -1L) {
                    Log.e("PlantaDao", "Usuario $userId no tiene familia activa")
                    return plantas
                }

                val sqlPlantas = """
                    SELECT 
                        p.id,
                        p.nombrePersonalizado as nombre,
                        p.especie,
                        p.descripcion
                    FROM planta p
                    WHERE p.familia_id = ?
                    ORDER BY p.nombrePersonalizado ASC
                """

                val stmtPlantas = conexion.prepareStatement(sqlPlantas)
                stmtPlantas.setLong(1, familiaId)
                val rsPlantas = stmtPlantas.executeQuery()

                while (rsPlantas.next()) {
                    val plantaId = rsPlantas.getLong("id")
                    val plantaNombre = rsPlantas.getString("nombre") ?: "Sin nombre"
                    val plantaEspecie = rsPlantas.getString("especie") ?: ""

                    val datosSensores = obtenerDatosSensoresPlanta(plantaId)

                    plantas.add(
                        PlantaConDatos(
                            id = plantaId,
                            nombre = plantaNombre,
                            especie = plantaEspecie,
                            ubicacion = datosSensores["ubicacion"] as? String ?: "Sin ubicación",
                            humedad = datosSensores["humedad"] as? Float ?: 0f,
                            temperatura = datosSensores["temperatura"] as? Float ?: 0f,
                            luz = datosSensores["luz"] as? Float ?: 0f
                        )
                    )
                }

                rsPlantas.close()
                stmtPlantas.close()
                conexion.close()

                Log.d("PlantaDao", "Plantas de la familia $familiaId: ${plantas.size} plantas")
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo plantas de la familia: ${e.message}", e)
        }

        return plantas
    }

    // =========================================================================
    // FUNCIONES PARA DETALLE DE PLANTA ESPECÍFICA
    // =========================================================================

    /**
     * Verifica si el usuario tiene acceso a una planta específica
     */
    fun verificarAccesoPlanta(userId: Long, plantaId: Long): Boolean {
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    SELECT COUNT(*) as tiene_acceso
                    FROM planta p
                    INNER JOIN familia_usuario fu ON p.familia_id = fu.familia_id
                    WHERE fu.usuario_id = ? 
                      AND fu.activo = 1
                      AND p.id = ?
                """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                stmt.setLong(2, plantaId)

                val rs = stmt.executeQuery()
                val tieneAcceso = if (rs.next()) rs.getInt("tiene_acceso") > 0 else false

                rs.close()
                stmt.close()
                conexion.close()

                Log.d("PlantaDao", "Usuario $userId tiene acceso a planta $plantaId: $tieneAcceso")
                return tieneAcceso
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error verificando acceso: ${e.message}", e)
        }

        return false
    }

    /**
     * Obtiene una planta por su ID
     */
    fun obtenerPlantaPorId(plantaId: Long, userId: Long): Planta? {
        var planta: Planta? = null
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // Primero verificar acceso
                if (!verificarAccesoPlanta(userId, plantaId)) {
                    return null
                }

                val sql = """
                SELECT 
                    id, 
                    nombrePersonalizado as nombre, 
                    especie, 
                    fecha_creacion, 
                    descripcion, 
                    familia_id,
                    foto,
                    estado,
                    aspecto
                FROM planta 
                WHERE id = ?
            """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, plantaId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    planta = Planta(
                        rs.getLong("id"),
                        rs.getString("nombre") ?: "",
                        rs.getString("especie") ?: "",
                        rs.getString("fecha_creacion") ?: "",
                        rs.getString("descripcion") ?: "",
                        rs.getLong("familia_id"),
                        "", // ubicación vacía (la obtienes de otra tabla)
                        rs.getString("aspecto") ?: "normal",
                        rs.getString("estado") ?: "normal",
                        rs.getString("foto") ?: ""
                    )
                }

                rs.close()
                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo planta por ID: ${e.message}", e)
        }

        return planta
    }

    // =========================================================================
// FUNCIONES PARA ELIMINAR PLANTA
// =========================================================================

    /**
     * Obtiene la cantidad de sensores asociados a una planta
     */
    fun obtenerCantidadSensoresPorPlanta(plantaId: Long): Int {
        var cantidad = 0
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                SELECT COUNT(*) as total
                FROM main_sensor
                WHERE planta_id = ? AND activo = 1
            """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, plantaId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    cantidad = rs.getInt("total")
                }

                rs.close()
                stmt.close()
                conexion.close()

                Log.d("PlantaDao", "Sensores encontrados para planta $plantaId: $cantidad")
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo cantidad de sensores: ${e.message}", e)
        }

        return cantidad
    }

    fun obtenerSensoresDetalladosParaVista(plantaId: Long): List<SensorVista> {
        val sensoresVista = mutableListOf<SensorVista>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
            SELECT 
                s.id,
                s.nombre,
                s.ubicacion,
                ts.nombre as tipo_sensor,
                ts.unidad_medida,
                es.nombre as estado,
                m.valor,
                DATE_FORMAT(m.fecha, '%d/%m/%Y, %H:%i:%s') as ultima_lectura,
                s.activo,
                p.nombrePersonalizado as planta_nombre  -- ← AQUÍ agregamos el nombre de la planta
            FROM main_sensor s
            JOIN tipo_sensor ts ON s.tipo_sensor_id = ts.id
            JOIN estado_sensor es ON s.estado_sensor_id = es.id
            JOIN planta p ON s.planta_id = p.id  -- ← Unimos con la tabla planta
            LEFT JOIN (
                SELECT m1.sensor_id, m1.valor, m1.fecha
                FROM medicion m1
                WHERE m1.fecha = (
                    SELECT MAX(m2.fecha)
                    FROM medicion m2
                    WHERE m2.sensor_id = m1.sensor_id
                )
            ) m ON s.id = m.sensor_id
            WHERE s.planta_id = ? AND s.activo = 1
            ORDER BY s.tipo_sensor_id
        """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, plantaId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    val sensorVista = SensorVista(
                        id = rs.getLong("id"),
                        nombre = rs.getString("nombre"),
                        ubicacion = rs.getString("ubicacion"),
                        tipoSensor = rs.getString("tipo_sensor"),
                        unidadMedida = rs.getString("unidad_medida"),
                        estado = rs.getString("estado"),
                        valor = rs.getBigDecimal("valor"),
                        ultimaLectura = rs.getString("ultima_lectura"),
                        activo = rs.getBoolean("activo"),
                        plantaNombre = rs.getString("planta_nombre")  // ← Agregamos este campo
                    )
                    sensoresVista.add(sensorVista)
                }

                rs.close()
                stmt.close()
                conexion.close()

                Log.d("SensorDao", "Sensores para vista encontrados: ${sensoresVista.size}")
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error obteniendo sensores para vista: ${e.message}", e)
        }

        return sensoresVista
    }

    // Clase simple para la vista - ACTUALIZADA con plantaNombre
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
        val plantaNombre: String? = null  // ← Campo opcional para nombre de planta
    )
    /**
     * Elimina completamente una planta y todos sus datos asociados
     */
    fun eliminarPlantaCompleta(plantaId: Long, userId: Long): Boolean {
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // Primero verificar acceso
                if (!verificarAccesoPlanta(userId, plantaId)) {
                    Log.e("PlantaDao", "Usuario $userId no tiene acceso a planta $plantaId")
                    return false
                }

                // Obtener familia_id para actualizar contador
                val sqlFamilia = """
                SELECT familia_id 
                FROM planta 
                WHERE id = ?
            """
                val stmtFamilia = conexion.prepareStatement(sqlFamilia)
                stmtFamilia.setLong(1, plantaId)
                val rsFamilia = stmtFamilia.executeQuery()
                val familiaId = if (rsFamilia.next()) rsFamilia.getLong("familia_id") else -1L
                rsFamilia.close()
                stmtFamilia.close()

                // Iniciar transacción
                conexion.autoCommit = false

                try {
                    // 1. Eliminar mediciones de sensores de esta planta
                    val sqlDeleteMediciones = """
                    DELETE m FROM medicion m
                    INNER JOIN main_sensor ms ON m.sensor_id = ms.id
                    WHERE ms.planta_id = ?
                """
                    val stmtMediciones = conexion.prepareStatement(sqlDeleteMediciones)
                    stmtMediciones.setLong(1, plantaId)
                    stmtMediciones.executeUpdate()
                    stmtMediciones.close()

                    // 2. Eliminar sensores
                    val sqlDeleteSensores = """
                    DELETE FROM main_sensor WHERE planta_id = ?
                """
                    val stmtSensores = conexion.prepareStatement(sqlDeleteSensores)
                    stmtSensores.setLong(1, plantaId)
                    stmtSensores.executeUpdate()
                    stmtSensores.close()

                    // 3. Eliminar riegos
                    val sqlDeleteRiegos = """
                    DELETE FROM riego WHERE planta_id = ?
                """
                    val stmtRiegos = conexion.prepareStatement(sqlDeleteRiegos)
                    stmtRiegos.setLong(1, plantaId)
                    stmtRiegos.executeUpdate()
                    stmtRiegos.close()

                    // 4. Eliminar seguimiento de estado
                    val sqlDeleteSeguimiento = """
                    DELETE FROM seguimiento_estado_planta WHERE planta_id = ?
                """
                    val stmtSeguimiento = conexion.prepareStatement(sqlDeleteSeguimiento)
                    stmtSeguimiento.setLong(1, plantaId)
                    stmtSeguimiento.executeUpdate()
                    stmtSeguimiento.close()

                    // 5. Eliminar la planta
                    val sqlDeletePlanta = """
                    DELETE FROM planta WHERE id = ?
                """
                    val stmtPlanta = conexion.prepareStatement(sqlDeletePlanta)
                    stmtPlanta.setLong(1, plantaId)
                    val filasEliminadas = stmtPlanta.executeUpdate()
                    stmtPlanta.close()

                    // 6. Actualizar contador de la familia si es necesario
                    if (familiaId != -1L && filasEliminadas > 0) {
                        val sqlUpdateFamilia = """
                        UPDATE familia 
                        SET cantidad_plantas = GREATEST(cantidad_plantas - 1, 0)
                        WHERE id = ?
                    """
                        val stmtUpdate = conexion.prepareStatement(sqlUpdateFamilia)
                        stmtUpdate.setLong(1, familiaId)
                        stmtUpdate.executeUpdate()
                        stmtUpdate.close()
                    }

                    // Confirmar transacción
                    conexion.commit()

                    Log.d(
                        "PlantaDao",
                        "Planta $plantaId eliminada exitosamente. Filas afectadas: $filasEliminadas"
                    )
                    return filasEliminadas > 0

                } catch (e: Exception) {
                    // Revertir en caso de error
                    conexion.rollback()
                    throw e
                } finally {
                    conexion.autoCommit = true
                    conexion.close()
                }
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error eliminando planta completa: ${e.message}", e)
        }

        return false
    }

    /**
     * Obtiene información básica para mostrar en el diálogo de eliminación
     */
    fun obtenerInfoParaDialogoEliminar(plantaId: Long, userId: Long): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        try {
            val planta = obtenerPlantaPorId(plantaId, userId)
            val sensorCount = obtenerCantidadSensoresPorPlanta(plantaId)

            info["nombre"] = planta?.nombre ?: "Planta"
            info["id"] = plantaId
            info["sensor_count"] = sensorCount
            info["tiene_acceso"] = planta != null

        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo info para diálogo: ${e.message}", e)
        }

        return info
    }

    /**
     * Obtiene datos de sensores para una planta específica (SIN verificación)
     */
    fun obtenerDatosSensoresPlanta(plantaId: Long): Map<String, Any> {
        val datos = mutableMapOf<String, Any>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // Obtener la ubicación del primer sensor activo
                val sqlUbicacion = """
                SELECT ubicacion 
                FROM main_sensor 
                WHERE planta_id = ? AND activo = 1 
                LIMIT 1
            """
                val stmtUbi = conexion.prepareStatement(sqlUbicacion)
                stmtUbi.setLong(1, plantaId)
                val rsUbi = stmtUbi.executeQuery()

                if (rsUbi.next()) {
                    datos["ubicacion"] = rsUbi.getString("ubicacion") ?: "Sin ubicación"
                } else {
                    datos["ubicacion"] = "Sin ubicación"
                }
                rsUbi.close()
                stmtUbi.close()

                // Obtener últimas mediciones de sensores - CORREGIDO
                val sqlMediciones = """
                SELECT 
                    ts.nombre as tipo_sensor,
                    m.valor,
                    m.fecha
                FROM main_sensor ms
                INNER JOIN tipo_sensor ts ON ms.tipo_sensor_id = ts.id
                INNER JOIN (
                    SELECT sensor_id, MAX(fecha) as ultima_fecha
                    FROM medicion
                    GROUP BY sensor_id
                ) ult_med ON ms.id = ult_med.sensor_id
                INNER JOIN medicion m ON ms.id = m.sensor_id AND m.fecha = ult_med.ultima_fecha
                WHERE ms.planta_id = ? AND ms.activo = 1
            """
                val stmtMed = conexion.prepareStatement(sqlMediciones)
                stmtMed.setLong(1, plantaId)
                val rsMed = stmtMed.executeQuery()

                datos["humedad"] = 0.0f
                datos["temperatura"] = 0.0f
                datos["luz"] = 0.0f

                while (rsMed.next()) {
                    val tipo = rsMed.getString("tipo_sensor") ?: ""
                    val valor = rsMed.getFloat("valor")

                    when {
                        tipo.contains("Humedad", ignoreCase = true) -> datos["humedad"] = valor
                        tipo.contains("Temperatura", ignoreCase = true) -> datos["temperatura"] = valor
                        tipo.contains("Luz", ignoreCase = true) -> datos["luz"] = valor
                    }
                }
                rsMed.close()
                stmtMed.close()

                // Obtener último riego - CORREGIDO
                val sqlRiego = """
                SELECT fecha_creacion as fecha, cantidad_ml as cantidad_agua
                FROM riego 
                WHERE planta_id = ? 
                ORDER BY fecha_creacion DESC 
                LIMIT 1
            """
                val stmtRiego = conexion.prepareStatement(sqlRiego)
                stmtRiego.setLong(1, plantaId)
                val rsRiego = stmtRiego.executeQuery()

                if (rsRiego.next()) {
                    val fecha = rsRiego.getString("fecha") ?: ""
                    val cantidad = rsRiego.getFloat("cantidad_agua")
                    datos["ultimo_riego"] = "$fecha (${cantidad}ml)"
                } else {
                    datos["ultimo_riego"] = "Sin registro"
                }
                rsRiego.close()
                stmtRiego.close()

                // Obtener estado actual de la planta
                val sqlEstado = """
                SELECT estado 
                FROM seguimiento_estado_planta 
                WHERE planta_id = ? 
                ORDER BY fecha_registro DESC 
                LIMIT 1
            """
                val stmtEstado = conexion.prepareStatement(sqlEstado)
                stmtEstado.setLong(1, plantaId)
                val rsEstado = stmtEstado.executeQuery()

                datos["estado"] = if (rsEstado.next()) {
                    rsEstado.getString("estado") ?: "Desconocido"
                } else {
                    "Desconocido"
                }
                rsEstado.close()
                stmtEstado.close()

                conexion.close()
                Log.d("PlantaDao", "Datos sensores para planta $plantaId: $datos")
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo datos sensores: ${e.message}", e)
        }

        return datos
    }

    /**
     * Obtiene datos de sensores para una planta específica (CON verificación)
     */
    fun obtenerDatosSensoresPlantaSeguro(userId: Long, plantaId: Long): Map<String, Any> {
        if (!verificarAccesoPlanta(userId, plantaId)) {
            Log.w("PlantaDao", "Usuario $userId no tiene acceso a planta $plantaId")
            return mapOf("error" to "No tiene acceso a esta planta")
        }

        return obtenerDatosSensoresPlanta(plantaId)
    }

    /**
     * Obtiene historial de una planta específica (SIN verificación)
     */
    fun obtenerHistorialPlanta(
        plantaId: Long,
        horas: Int = 24
    ): Map<String, List<Pair<String, Float>>> {
        val historial = mutableMapOf<String, List<Pair<String, Float>>>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                historial["humedad"] = obtenerHistorialSensor(plantaId, "Humedad", horas)
                historial["temperatura"] = obtenerHistorialSensor(plantaId, "Temperatura", horas)
                historial["luz"] = obtenerHistorialSensor(plantaId, "Luz", horas)

                conexion.close()
                Log.d("PlantaDao", "Historial obtenido para planta $plantaId")
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo historial: ${e.message}", e)
        }

        return historial
    }

    /**
     * Obtiene historial de una planta específica (CON verificación)
     */
    fun obtenerHistorialPlantaSeguro(
        userId: Long,
        plantaId: Long,
        horas: Int = 24
    ): Map<String, List<Pair<String, Float>>> {
        if (!verificarAccesoPlanta(userId, plantaId)) {
            Log.w("PlantaDao", "Usuario $userId no tiene acceso a planta $plantaId")
            return emptyMap()
        }

        return obtenerHistorialPlanta(plantaId, horas)
    }

    // =========================================================================
    // FUNCIONES AUXILIARES Y DE UTILIDAD
    // =========================================================================

    /**
     * Obtiene la familia ID del usuario
     */
    fun obtenerFamiliaIdDelUsuario(userId: Long): Long {
        var familiaId = -1L
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    SELECT familia_id 
                    FROM familia_usuario 
                    WHERE usuario_id = ? AND activo = 1 
                    LIMIT 1
                """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    familiaId = rs.getLong("familia_id")
                }

                rs.close()
                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo familia del usuario", e)
        }

        return familiaId
    }

    /**
     * Inserta una nueva planta con todos los campos
     */
    fun insertarPlanta(planta: Planta, userId: Long): Boolean {
        var insertado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val familiaId = obtenerFamiliaIdDelUsuario(userId)

                if (familiaId != -1L) {
                    Log.d("PlantaDao", "=== INSERTANDO PLANTA ===")
                    Log.d("PlantaDao", "Usuario ID: $userId")
                    Log.d("PlantaDao", "Familia ID: $familiaId")
                    Log.d("PlantaDao", "Nombre: ${planta.nombre}")
                    Log.d("PlantaDao", "Especie: '${planta.especie}'")
                    Log.d("PlantaDao", "Foto: '${planta.foto}'")
                    Log.d("PlantaDao", "Descripción: ${planta.descripcion}")
                    Log.d("PlantaDao", "Aspecto: ${planta.aspecto}")
                    Log.d("PlantaDao", "Estado: ${planta.estado}")

                    val sqlInsert = """
                INSERT INTO planta (
                    nombrePersonalizado, 
                    especie, 
                    fecha_creacion, 
                    descripcion, 
                    familia_id,
                    aspecto,
                    estado,
                    foto
                )
                VALUES (?, ?, NOW(), ?, ?, ?, ?, ?)
                """

                    Log.d("PlantaDao", "SQL: $sqlInsert")

                    val stmt = conexion.prepareStatement(sqlInsert)

                    stmt.setString(1, planta.nombre)
                    stmt.setString(2, planta.especie)
                    stmt.setString(3, planta.descripcion)
                    stmt.setLong(4, familiaId)
                    stmt.setString(5, planta.aspecto)
                    stmt.setString(6, planta.estado)

                    if (planta.foto != null && planta.foto.isNotEmpty()) {
                        stmt.setString(7, planta.foto)
                        Log.d("PlantaDao", "Foto no vacía, insertando: '${planta.foto}'")
                    } else {
                        stmt.setNull(7, java.sql.Types.VARCHAR)
                        Log.d("PlantaDao", "Foto vacía, insertando NULL")
                    }

                    val filas = stmt.executeUpdate()
                    Log.d("PlantaDao", "Filas afectadas: $filas")

                    if (filas > 0) {
                        insertado = true

                        val sqlUpdate = """
                    UPDATE familia 
                    SET cantidad_plantas = cantidad_plantas + 1 
                    WHERE id = ?
                    """
                        val stmtUpdate = conexion.prepareStatement(sqlUpdate)
                        stmtUpdate.setLong(1, familiaId)
                        stmtUpdate.executeUpdate()
                        stmtUpdate.close()

                        Log.d("PlantaDao", "¡Planta creada exitosamente!")
                    } else {
                        Log.e("PlantaDao", "No se insertaron filas")
                    }

                    stmt.close()
                } else {
                    Log.e("PlantaDao", "Usuario $userId no tiene familia activa")
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error al insertar planta: ${e.message}", e)

            if (e.message?.contains("aspecto") == true ||
                e.message?.contains("estado") == true ||
                e.message?.contains("foto") == true
            ) {

                Log.d("PlantaDao", "Intentando inserción básica...")
                return insertarPlantaBasica(planta, userId)
            }
        }
        return insertado
    }

    /**
     * Versión básica de inserción (sin estado, aspecto, foto)
     */
    private fun insertarPlantaBasica(planta: Planta, userId: Long): Boolean {
        var insertado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val familiaId = obtenerFamiliaIdDelUsuario(userId)

                if (familiaId != -1L) {
                    Log.d("PlantaDao", "Insertando versión básica (solo campos esenciales)")

                    val sqlInsert = """
                INSERT INTO planta (
                    nombrePersonalizado, 
                    especie, 
                    fecha_creacion, 
                    descripcion, 
                    familia_id
                )
                VALUES (?, ?, NOW(), ?, ?)
                """

                    val stmt = conexion.prepareStatement(sqlInsert)
                    stmt.setString(1, planta.nombre)
                    stmt.setString(2, planta.especie)
                    stmt.setString(3, planta.descripcion)
                    stmt.setLong(4, familiaId)

                    val filas = stmt.executeUpdate()
                    if (filas > 0) {
                        insertado = true

                        val sqlUpdate = """
                    UPDATE familia 
                    SET cantidad_plantas = cantidad_plantas + 1 
                    WHERE id = ?
                    """
                        val stmtUpdate = conexion.prepareStatement(sqlUpdate)
                        stmtUpdate.setLong(1, familiaId)
                        stmtUpdate.executeUpdate()
                        stmtUpdate.close()

                        Log.d("PlantaDao", "Planta básica insertada exitosamente")
                    }

                    stmt.close()
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error en inserción básica: ${e.message}", e)
        }
        return insertado
    }

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

    /**
     * Obtiene resumen consistente de estados de plantas del usuario
     */
    fun obtenerResumenEstadoPlantas(userId: Long): Map<String, Int> {
        val resumen = mutableMapOf(
            "total" to 0,
            "healthy" to 0,
            "warning" to 0,
            "critical" to 0
        )

        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    SELECT 
                        p.id,
                        ult_seg.estado,
                        m.valor as humedad
                    FROM planta p
                    INNER JOIN familia_usuario fu ON p.familia_id = fu.familia_id
                    LEFT JOIN (
                        SELECT sep1.* 
                        FROM seguimiento_estado_planta sep1
                        INNER JOIN (
                            SELECT planta_id, MAX(fecha_registro) as max_fecha
                            FROM seguimiento_estado_planta
                            GROUP BY planta_id
                        ) sep2 ON sep1.planta_id = sep2.planta_id AND sep1.fecha_registro = sep2.max_fecha
                    ) ult_seg ON p.id = ult_seg.planta_id
                    LEFT JOIN main_sensor ms ON p.id = ms.planta_id AND ms.tipo_sensor_id = 2
                    LEFT JOIN medicion m ON ms.id = m.sensor_id 
                        AND m.fecha = (SELECT MAX(fecha) FROM medicion WHERE sensor_id = ms.id)
                    WHERE fu.usuario_id = ? AND fu.activo = 1
                """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, userId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    resumen["total"] = resumen["total"]!! + 1

                    val estado = rs.getString("estado")
                    val humedad = if (!rs.wasNull()) rs.getFloat("humedad") else null

                    val clasificacion = clasificarEstadoPlanta(estado, humedad)

                    when (clasificacion) {
                        "critical" -> resumen["critical"] = resumen["critical"]!! + 1
                        "warning" -> resumen["warning"] = resumen["warning"]!! + 1
                        "healthy" -> resumen["healthy"] = resumen["healthy"]!! + 1
                    }
                }

                conexion.close()
                Log.d("PlantaDao", "Resumen estados: $resumen")
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo resumen estados", e)
        }

        return resumen
    }

    /**
     * Obtiene datos históricos para gráficos SOLO de plantas de mi familia
     */
    fun obtenerDatosHistoricosGraficoFamiliar(
        userId: Long,
        horas: Int,
        plantaId: Long = -1
    ): Map<String, List<DataPointDAO>> {
        val datos = mutableMapOf<String, List<DataPointDAO>>()

        val familiaId = obtenerFamiliaIdDelUsuario(userId)
        if (familiaId == -1L) {
            Log.e("PlantaDao", "Usuario no tiene familia activa")
            return datos
        }

        datos["humedad"] =
            obtenerDatosSensorHistoricoFamiliar(familiaId, horas, plantaId, "Humedad")
        datos["temperatura"] =
            obtenerDatosSensorHistoricoFamiliar(familiaId, horas, plantaId, "Temperatura")
        datos["luz"] = obtenerDatosSensorHistoricoFamiliar(familiaId, horas, plantaId, "Luz")

        Log.d(
            "PlantaDao",
            "Datos gráfico familiar: Humedad=${datos["humedad"]?.size}, Temp=${datos["temperatura"]?.size}, Luz=${datos["luz"]?.size}"
        )

        return datos
    }

    // =========================================================================
    // NUEVAS FUNCIONES PARA EDITAR PLANTA
    // =========================================================================

    /**
     * Actualiza una planta existente
     */
    fun actualizarPlanta(planta: Planta, userId: Long): Boolean {
        var actualizado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // Primero verificar que el usuario tiene acceso a esta planta
                if (!verificarAccesoPlanta(userId, planta.id)) {
                    Log.e("PlantaDao", "Usuario $userId no tiene acceso a planta ${planta.id}")
                    return false
                }

                Log.d("PlantaDao", "=== ACTUALIZANDO PLANTA ===")
                Log.d("PlantaDao", "Planta ID: ${planta.id}")
                Log.d("PlantaDao", "Nombre: ${planta.nombre}")
                Log.d("PlantaDao", "Especie: '${planta.especie}'")
                Log.d("PlantaDao", "Estado: ${planta.estado}")
                Log.d("PlantaDao", "Aspecto: ${planta.aspecto}")
                Log.d("PlantaDao", "Foto: '${planta.foto}'")
                Log.d("PlantaDao", "Descripción: ${planta.descripcion}")

                val sqlUpdate = """
                    UPDATE planta 
                    SET 
                        nombrePersonalizado = ?, 
                        especie = ?, 
                        descripcion = ?,
                        aspecto = ?,
                        estado = ?,
                        foto = ?
                    WHERE id = ?
                """

                Log.d("PlantaDao", "SQL Update: $sqlUpdate")

                val stmt = conexion.prepareStatement(sqlUpdate)

                stmt.setString(1, planta.nombre)
                stmt.setString(2, planta.especie)
                stmt.setString(3, planta.descripcion)
                stmt.setString(4, planta.aspecto)
                stmt.setString(5, planta.estado)

                if (planta.foto != null && planta.foto.isNotEmpty()) {
                    stmt.setString(6, planta.foto)
                    Log.d("PlantaDao", "Foto no vacía: '${planta.foto}'")
                } else {
                    stmt.setNull(6, java.sql.Types.VARCHAR)
                    Log.d("PlantaDao", "Foto vacía, insertando NULL")
                }

                stmt.setLong(7, planta.id)

                val filas = stmt.executeUpdate()
                Log.d("PlantaDao", "Filas afectadas en update: $filas")

                if (filas > 0) {
                    actualizado = true
                    Log.d("PlantaDao", "¡Planta actualizada exitosamente!")

                    // Registrar en historial de estado si cambió
                    if (planta.estado != "normal") {
                        registrarCambioEstado(
                            planta.id,
                            planta.estado,
                            "Actualizado desde app móvil"
                        )
                    }
                } else {
                    Log.e("PlantaDao", "No se actualizaron filas - planta no encontrada")
                }

                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error al actualizar planta: ${e.message}", e)

            if (e.message?.contains("aspecto") == true ||
                e.message?.contains("estado") == true ||
                e.message?.contains("foto") == true
            ) {

                Log.d("PlantaDao", "Intentando actualización básica...")
                return actualizarPlantaBasica(planta, userId)
            }
        }
        return actualizado
    }

    /**
     * Versión básica de actualización (sin estado, aspecto, foto)
     */
    private fun actualizarPlantaBasica(planta: Planta, userId: Long): Boolean {
        var actualizado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                if (!verificarAccesoPlanta(userId, planta.id)) {
                    return false
                }

                Log.d("PlantaDao", "Actualizando versión básica")

                val sqlUpdate = """
                    UPDATE planta 
                    SET 
                        nombrePersonalizado = ?, 
                        especie = ?, 
                        descripcion = ?
                    WHERE id = ?
                """

                val stmt = conexion.prepareStatement(sqlUpdate)
                stmt.setString(1, planta.nombre)
                stmt.setString(2, planta.especie)
                stmt.setString(3, planta.descripcion)
                stmt.setLong(4, planta.id)

                val filas = stmt.executeUpdate()
                if (filas > 0) {
                    actualizado = true
                    Log.d("PlantaDao", "Planta básica actualizada exitosamente")
                }

                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error en actualización básica: ${e.message}", e)
        }
        return actualizado
    }

    /**
     * Registra un cambio de estado en el historial
     */
    private fun registrarCambioEstado(
        plantaId: Long,
        estado: String,
        observaciones: String
    ): Boolean {
        var registrado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    INSERT INTO seguimiento_estado_planta (planta_id, estado, observaciones, fecha_registro)
                    VALUES (?, ?, ?, NOW())
                """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, plantaId)
                stmt.setString(2, estado)
                stmt.setString(3, observaciones)

                val filas = stmt.executeUpdate()
                if (filas > 0) {
                    registrado = true
                    Log.d("PlantaDao", "Cambio de estado registrado para planta $plantaId")
                }

                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error registrando cambio de estado: ${e.message}", e)
        }
        return registrado
    }

    // =========================================================================
    // FUNCIONES PRIVADAS
    // =========================================================================

    /**
     * Obtiene historial para un tipo de sensor específico
     */
    private fun obtenerHistorialSensor(
        plantaId: Long,
        tipoSensor: String,
        horas: Int
    ): List<Pair<String, Float>> {
        val datos = mutableListOf<Pair<String, Float>>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                SELECT 
                    DATE_FORMAT(m.fecha, '%H:%i') as hora,
                    AVG(m.valor) as promedio
                FROM main_sensor ms
                INNER JOIN tipo_sensor ts ON ms.tipo_sensor_id = ts.id
                INNER JOIN medicion m ON ms.id = m.sensor_id
                WHERE ms.planta_id = ? 
                  AND ts.nombre LIKE ?
                  AND m.fecha >= DATE_SUB(NOW(), INTERVAL ? HOUR)
                GROUP BY DATE_FORMAT(m.fecha, '%Y-%m-%d %H:%i')
                ORDER BY m.fecha ASC
            """.trimIndent()

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, plantaId)
                stmt.setString(2, "%$tipoSensor%")
                stmt.setInt(3, horas)

                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val hora = rs.getString("hora")
                    val valor = rs.getFloat("promedio")
                    datos.add(Pair(hora, valor))
                }

                rs.close()
                stmt.close()
                Log.d("PlantaDao", "Historial $tipoSensor: ${datos.size} registros")
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo historial de $tipoSensor: ${e.message}", e)
        }

        return datos
    }

    /**
     * Obtiene datos históricos de un sensor específico para plantas de la familia
     */
    private fun obtenerDatosSensorHistoricoFamiliar(
        familiaId: Long,
        horas: Int,
        plantaId: Long,
        tipoSensor: String
    ): List<DataPointDAO> {
        val datos = mutableListOf<DataPointDAO>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val plantaCondition = if (plantaId != -1L) "AND p.id = ?" else ""
                val params = mutableListOf<Any>()

                val (intervaloNum, formatoFecha) = when {
                    horas <= 24 -> Pair("1", "%H:00")
                    horas <= 168 -> Pair("6", "%a %H:00")
                    else -> Pair("24", "%d/%m")
                }

                var sql = """
                    SELECT 
                        DATE_FORMAT(m.fecha, ?) as hora_grupo,
                        AVG(m.valor) as valor_promedio
                    FROM planta p
                    INNER JOIN main_sensor ms ON p.id = ms.planta_id
                    INNER JOIN tipo_sensor ts ON ms.tipo_sensor_id = ts.id
                    INNER JOIN medicion m ON ms.id = m.sensor_id
                    WHERE p.familia_id = ?
                      AND ms.activo = 1
                      AND ts.nombre LIKE ?
                      AND m.fecha >= DATE_SUB(NOW(), INTERVAL ? HOUR)
                """

                params.add(formatoFecha)
                params.add(familiaId)
                params.add("%$tipoSensor%")
                params.add(horas)

                if (plantaId != -1L) {
                    sql += " $plantaCondition"
                    params.add(plantaId)
                }

                sql += """
                    GROUP BY hora_grupo
                    ORDER BY MIN(m.fecha) ASC
                """

                val stmt = conexion.prepareStatement(sql)
                params.forEachIndexed { index, param ->
                    when (param) {
                        is Long -> stmt.setLong(index + 1, param)
                        is Int -> stmt.setInt(index + 1, param)
                        is String -> stmt.setString(index + 1, param)
                    }
                }

                val rs = stmt.executeQuery()

                while (rs.next()) {
                    datos.add(
                        DataPointDAO(
                            label = rs.getString("hora_grupo") ?: "",
                            value = rs.getFloat("valor_promedio")
                        )
                    )
                }

                rs.close()
                stmt.close()
                conexion.close()

                Log.d("PlantaDao", "Datos $tipoSensor: ${datos.size} puntos")

                if (datos.isEmpty()) {
                    datos.addAll(generarDatosEjemploSensor(tipoSensor, horas))
                }
            }
        } catch (e: Exception) {
            Log.e("PlantaDao", "Error obteniendo datos históricos de $tipoSensor: ${e.message}", e)
            return generarDatosEjemploSensor(tipoSensor, horas)
        }

        return datos
    }

    /**
     * Genera datos de ejemplo para sensores cuando no hay datos reales
     */
    private fun generarDatosEjemploSensor(tipoSensor: String, horas: Int): List<DataPointDAO> {
        val datos = mutableListOf<DataPointDAO>()

        val puntos = when {
            horas <= 24 -> 8
            horas <= 168 -> 7
            else -> 10
        }

        val baseValue = when (tipoSensor) {
            "Humedad" -> 60f
            "Temperatura" -> 22f
            "Luz" -> 70f
            else -> 50f
        }

        val labels = generarEtiquetasEjemplo(horas, puntos)

        for (i in 0 until puntos) {
            val variacion = (Math.random() * 20 - 10).toFloat()
            datos.add(
                DataPointDAO(
                    label = labels[i % labels.size],
                    value = baseValue + variacion
                )
            )
        }

        return datos
    }

    private fun generarEtiquetasEjemplo(horas: Int, puntos: Int): List<String> {
        return when {
            horas <= 24 -> listOf(
                "00:00",
                "03:00",
                "06:00",
                "09:00",
                "12:00",
                "15:00",
                "18:00",
                "21:00"
            )

            horas <= 168 -> listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
            else -> listOf("01", "05", "10", "15", "20", "25", "30")
        }.take(puntos)
    }

    // =========================================================================
    // FUNCIONES ESPECÍFICAS PARA TU DISEÑO DE SENSORES
    // =========================================================================

    /**
     * Obtiene sensores de MI familia para mostrar en tu RecyclerView
     */
    fun obtenerSensoresMiFamiliaParaUI(userId: Long): List<Map<String, Any>> {
        val sensores = mutableListOf<Map<String, Any>>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val familiaId = obtenerFamiliaIdDelUsuario(userId)
                if (familiaId == -1L) {
                    Log.w("SensoresUI", "Usuario sin familia")
                    return sensores
                }

                val sql = """
                SELECT 
                    ms.id as id,
                    ms.nombre as nombre_sensor,
                    ms.ubicacion,
                    ts.nombre as tipo_sensor,
                    ts.unidad_medida,
                    es.nombre as estado,
                    es.id as estado_id,
                    p.nombrePersonalizado as planta_nombre,
                    COALESCE(m.valor, 0.0) as valor_actual,
                    COALESCE(DATE_FORMAT(m.fecha, '%H:%i'), '--:--') as ultima_hora
                FROM main_sensor ms
                INNER JOIN tipo_sensor ts ON ms.tipo_sensor_id = ts.id
                INNER JOIN estado_sensor es ON ms.estado_sensor_id = es.id
                INNER JOIN planta p ON ms.planta_id = p.id
                LEFT JOIN (
                    SELECT m1.* 
                    FROM medicion m1
                    INNER JOIN (
                        SELECT sensor_id, MAX(fecha) as max_fecha
                        FROM medicion
                        GROUP BY sensor_id
                    ) m2 ON m1.sensor_id = m2.sensor_id AND m1.fecha = m2.max_fecha
                ) m ON ms.id = m.sensor_id
                WHERE p.familia_id = ?
                  AND ms.activo = 1
                ORDER BY p.nombrePersonalizado, ms.nombre
            """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, familiaId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    val estadoId = rs.getInt("estado_id")
                    val tipo = rs.getString("tipo_sensor") ?: ""
                    val valor = rs.getFloat("valor_actual")
                    val unidad = rs.getString("unidad_medida") ?: ""

                    val (colorRes, estadoTexto, progress) = determinarEstadoUI(
                        estadoId,
                        tipo,
                        valor
                    )

                    val (iconRes, bgColorRes) = determinarIconoYFondo(tipo)

                    val sensor = mapOf(
                        "nombre" to (rs.getString("nombre_sensor") ?: "Sensor"),
                        "ubicacion" to (rs.getString("ubicacion") ?: "Sin ubicación"),
                        "valor" to formatValor(valor, unidad),
                        "estado" to estadoTexto,
                        "progress" to progress,
                        "icono_res" to iconRes,
                        "bg_color_res" to bgColorRes,
                        "color_res" to colorRes,
                        "tipo" to tipo,
                        "planta" to (rs.getString("planta_nombre") ?: ""),
                        "hora" to (rs.getString("ultima_hora") ?: "--:--"),
                        "estado_id" to estadoId,
                        "valor_raw" to valor,
                        "unidad" to unidad
                    )
                    sensores.add(sensor)
                }

                rs.close()
                stmt.close()
                conexion.close()

                Log.d("SensoresUI", "Encontrados ${sensores.size} sensores para UI")
            }
        } catch (e: Exception) {
            Log.e("SensoresUI", "Error: ${e.message}", e)
        }

        return sensores
    }

    /**
     * Obtiene los conteos EXACTOS para tus 3 tarjetas de estadísticas
     */
    fun obtenerConteosParaTarjetas(userId: Long): Triple<Int, Int, Int> {
        var activos = 0
        var optimos = 0
        var alerta = 0

        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val familiaId = obtenerFamiliaIdDelUsuario(userId)
                if (familiaId == -1L) return Triple(0, 0, 0)

                val sqlTotal = """
                SELECT COUNT(*) as total
                FROM main_sensor ms
                INNER JOIN planta p ON ms.planta_id = p.id
                WHERE p.familia_id = ?
                  AND ms.activo = 1
            """

                val stmtTotal = conexion.prepareStatement(sqlTotal)
                stmtTotal.setLong(1, familiaId)
                val rsTotal = stmtTotal.executeQuery()

                if (rsTotal.next()) {
                    activos = rsTotal.getInt("total")
                }
                rsTotal.close()
                stmtTotal.close()

                if (activos > 0) {
                    val sensores = obtenerSensoresMiFamiliaParaUI(userId)

                    optimos = sensores.count { sensor ->
                        val estadoId = sensor["estado_id"] as? Int ?: 0
                        estadoId == 1 && (sensor["estado"] as? String == "Óptimo")
                    }

                    alerta = sensores.count { sensor ->
                        val estadoId = sensor["estado_id"] as? Int ?: 0
                        estadoId != 1 || (sensor["estado"] as? String != "Óptimo")
                    }
                }

                conexion.close()

                Log.d("ConteosUI", "Tarjetas: Activos=$activos, Óptimos=$optimos, Alerta=$alerta")
            }
        } catch (e: Exception) {
            Log.e("ConteosUI", "Error: ${e.message}", e)
        }

        return Triple(activos, optimos, alerta)
    }

    // =========================================================================
    // FUNCIONES AUXILIARES ESPECÍFICAS
    // =========================================================================

    /**
     * Determina el estado UI según tu diseño (color, texto, progreso)
     */
    private fun determinarEstadoUI(
        estadoId: Int,
        tipo: String,
        valor: Float
    ): Triple<Int, String, Int> {
        return when {
            estadoId == 4 -> Triple(R.color.sensor_red, "Error", 0)
            estadoId == 3 -> Triple(R.color.sensor_orange, "Mantenimiento", 0)
            estadoId == 1 -> {
                if (esValorOptimoUI(tipo, valor)) {
                    Triple(R.color.sensor_green, "Óptimo", calcularProgress(tipo, valor))
                } else {
                    Triple(R.color.sensor_yellow, "Advertencia", calcularProgress(tipo, valor))
                }
            }

            else -> Triple(R.color.text_gray, "Inactivo", 0)
        }
    }

    /**
     * Verifica si el valor es óptimo según rangos estándar
     */
    private fun esValorOptimoUI(tipo: String, valor: Float): Boolean {
        return when {
            tipo.contains("Humedad Suelo") -> valor in 50f..80f
            tipo.contains("Humedad Aire") -> valor in 40f..70f
            tipo.contains("Temperatura") -> valor in 18f..28f
            tipo.contains("Luz") -> valor > 300f
            tipo.contains("pH") -> valor in 6f..7f
            else -> true
        }
    }

    /**
     * Calcula el progreso para la barra (0-100)
     */
    private fun calcularProgress(tipo: String, valor: Float): Int {
        return when {
            tipo.contains("Humedad") -> valor.toInt().coerceIn(0, 100)
            tipo.contains("Temperatura") -> ((valor - 10) / 30 * 100).toInt().coerceIn(0, 100)
            tipo.contains("Luz") -> (valor / 2000 * 100).toInt().coerceIn(0, 100)
            tipo.contains("pH") -> ((valor - 4) / 6 * 100).toInt().coerceIn(0, 100)
            else -> 50
        }
    }

    /**
     * Determina icono y color de fondo según tipo de sensor
     */
    private fun determinarIconoYFondo(tipo: String): Pair<Int, Int> {
        return when {
            tipo.contains("Temperatura") -> Pair(R.drawable.ic_temp, R.drawable.bg_icon_sensor_red)
            tipo.contains("Humedad Suelo") -> Pair(
                R.drawable.ic_droplet,
                R.drawable.bg_icon_sensor_blue
            )

            tipo.contains("Humedad Aire") -> Pair(R.drawable.ic_air, R.drawable.bg_icon_sensor_cyan)
            tipo.contains("Luz") -> Pair(R.drawable.ic_sun, R.drawable.bg_icon_sensor_yellow)
            tipo.contains("pH") -> Pair(R.drawable.ic_ph, R.drawable.bg_icon_sensor_purple)
            else -> Pair(R.drawable.ic_sensor_default, R.drawable.bg_icon_sensor_gray)
        }
    }

    /**
     * Formatea el valor para mostrar (ej: "75%", "25.5°C")
     */
    private fun formatValor(valor: Float, unidad: String): String {
        return when (unidad) {
            "%" -> "${valor.toInt()}%"
            "°C" -> String.format("%.1f°C", valor)
            "lux" -> "${valor.toInt()} lux"
            "pH" -> String.format("%.1f pH", valor)
            else -> String.format("%.1f", valor)
        }
    }
}