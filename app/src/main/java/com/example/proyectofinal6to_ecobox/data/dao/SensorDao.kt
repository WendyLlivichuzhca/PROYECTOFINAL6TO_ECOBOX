package com.example.proyectofinal6to_ecobox.data.dao

import android.util.Log
import com.example.proyectofinal6to_ecobox.data.model.Sensor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SensorDao {

    /**
     * Inserta un nuevo sensor en la base de datos
     */
    fun insertarSensor(sensor: Sensor): Boolean {
        var insertado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val fechaInstalacion = if (sensor.fechaInstalacion.isNullOrEmpty()) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    sdf.format(Date())
                } else {
                    sensor.fechaInstalacion
                }

                val sql = """
                INSERT INTO main_sensor (
                    nombre, 
                    ubicacion, 
                    fecha_instalacion, 
                    activo, 
                    estado_sensor_id, 
                    planta_id, 
                    tipo_sensor_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """

                val stmt = conexion.prepareStatement(sql)

                stmt.setString(1, sensor.nombre)
                stmt.setString(2, sensor.ubicacion)
                stmt.setString(3, fechaInstalacion)
                stmt.setBoolean(4, sensor.isActivo())  // CORREGIDO: usar isActivo()
                stmt.setLong(5, sensor.estadoSensorId)
                stmt.setLong(6, sensor.plantaId)
                stmt.setLong(7, sensor.tipoSensorId)

                val filas = stmt.executeUpdate()
                if (filas > 0) {
                    insertado = true
                    Log.d("SensorDao", "Sensor insertado exitosamente: ${sensor.nombre}")
                }

                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error insertando sensor: ${e.message}", e)
        }

        return insertado
    }

    /**
     * Obtiene todos los tipos de sensores disponibles
     */
    /**
     * Obtiene todos los tipos de sensores disponibles
     */
    fun obtenerTiposSensores(): List<Pair<Long, String>> {
        val tipos = mutableListOf<Pair<Long, String>>()
        val conexion = MySqlConexion.getConexion()

        Log.d("SensorDao", "Obteniendo tipos de sensores de BD...")

        try {
            if (conexion != null) {
                Log.d("SensorDao", "Conexión establecida")
                val sql = "SELECT id, nombre FROM tipo_sensor ORDER BY nombre"
                Log.d("SensorDao", "Ejecutando query: $sql")

                val stmt = conexion.prepareStatement(sql)
                val rs = stmt.executeQuery()

                var count = 0
                while (rs.next()) {
                    val id = rs.getLong("id")
                    val nombre = rs.getString("nombre")
                    tipos.add(Pair(id, nombre))
                    Log.d("SensorDao", "Tipo $count: ID=$id, Nombre='$nombre'")
                    count++
                }

                Log.d("SensorDao", "Total tipos encontrados: $count")

                rs.close()
                stmt.close()
                conexion.close()
            } else {
                Log.e("SensorDao", "No se pudo establecer conexión")
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error obteniendo tipos de sensores: ${e.message}", e)
        }

        return tipos
    }

    /**
     * Obtiene todos los estados posibles de sensores
     */
    fun obtenerEstadosSensores(): List<Pair<Long, String>> {
        val estados = mutableListOf<Pair<Long, String>>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = "SELECT id, nombre FROM estado_sensor ORDER BY id"
                val stmt = conexion.prepareStatement(sql)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    estados.add(Pair(rs.getLong("id"), rs.getString("nombre")))
                }

                rs.close()
                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error obteniendo estados de sensores: ${e.message}", e)
        }

        return estados
    }

    /**
     * Verifica si ya existe un sensor con el mismo nombre en la misma planta
     */
    fun existeSensorConNombre(nombre: String, plantaId: Long): Boolean {
        var existe = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    SELECT COUNT(*) as total 
                    FROM main_sensor 
                    WHERE LOWER(nombre) = LOWER(?) AND planta_id = ?
                """

                val stmt = conexion.prepareStatement(sql)
                stmt.setString(1, nombre)
                stmt.setLong(2, plantaId)

                val rs = stmt.executeQuery()
                if (rs.next()) {
                    existe = rs.getInt("total") > 0
                }

                rs.close()
                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error verificando existencia de sensor: ${e.message}", e)
        }

        return existe
    }

    /**
     * Obtiene el último sensor insertado para una planta
     */
    fun obtenerUltimoSensorId(plantaId: Long): Long {
        var ultimoId = 0L
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    SELECT MAX(id) as ultimo_id 
                    FROM main_sensor 
                    WHERE planta_id = ?
                """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, plantaId)

                val rs = stmt.executeQuery()
                if (rs.next()) {
                    ultimoId = rs.getLong("ultimo_id")
                }

                rs.close()
                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error obteniendo último ID de sensor: ${e.message}", e)
        }

        return ultimoId
    }

    /**
     * Obtiene sensores por planta
     */
    fun obtenerSensoresPorPlanta(plantaId: Long): List<Sensor> {
        val sensores = mutableListOf<Sensor>()
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                SELECT 
                    id, nombre, ubicacion, 
                    DATE_FORMAT(fecha_instalacion, '%Y-%m-%d %H:%i:%s') as fecha_instalacion,
                    activo, estado_sensor_id, planta_id, tipo_sensor_id
                FROM main_sensor 
                WHERE planta_id = ? AND activo = 1
                ORDER BY fecha_instalacion DESC
            """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, plantaId)

                val rs = stmt.executeQuery()
                while (rs.next()) {
                    // Usando constructor con parámetros (CORREGIDO)
                    val sensor = Sensor(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("ubicacion"),
                        rs.getString("fecha_instalacion"),
                        rs.getBoolean("activo"),
                        rs.getLong("estado_sensor_id"),
                        rs.getLong("planta_id"),
                        rs.getLong("tipo_sensor_id")
                    )
                    sensores.add(sensor)
                }

                rs.close()
                stmt.close()
                conexion.close()

                Log.d("SensorDao", "Sensores encontrados: ${sensores.size} para planta $plantaId")
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error obteniendo sensores: ${e.message}", e)
        }

        return sensores
    }

    /**
     * Elimina un sensor por su ID
     */
    fun eliminarSensor(sensorId: Long): Boolean {
        var eliminado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // Primero eliminar mediciones asociadas
                val sqlDeleteMediciones = """
                    DELETE FROM medicion WHERE sensor_id = ?
                """
                val stmtMediciones = conexion.prepareStatement(sqlDeleteMediciones)
                stmtMediciones.setLong(1, sensorId)
                stmtMediciones.executeUpdate()
                stmtMediciones.close()

                // Luego eliminar el sensor
                val sql = "DELETE FROM main_sensor WHERE id = ?"
                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, sensorId)

                val filas = stmt.executeUpdate()
                if (filas > 0) {
                    eliminado = true
                    Log.d("SensorDao", "Sensor $sensorId eliminado exitosamente")
                }

                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error eliminando sensor: ${e.message}", e)
        }

        return eliminado
    }

    /**
     * Actualiza un sensor existente
     */
    fun actualizarSensor(sensor: Sensor): Boolean {
        var actualizado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    UPDATE main_sensor 
                    SET 
                        nombre = ?, 
                        ubicacion = ?, 
                        activo = ?,
                        estado_sensor_id = ?,
                        tipo_sensor_id = ?
                    WHERE id = ?
                """

                val stmt = conexion.prepareStatement(sql)
                stmt.setString(1, sensor.nombre)
                stmt.setString(2, sensor.ubicacion)
                stmt.setBoolean(3, sensor.isActivo())  // Usa el getter                stmt.setLong(4, sensor.estadoSensorId)
                stmt.setLong(5, sensor.tipoSensorId)
                stmt.setLong(6, sensor.id)

                val filas = stmt.executeUpdate()
                if (filas > 0) {
                    actualizado = true
                    Log.d("SensorDao", "Sensor ${sensor.id} actualizado exitosamente")
                }

                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error actualizando sensor: ${e.message}", e)
        }

        return actualizado
    }

    /**
     * Cambia el estado de un sensor
     */
    fun cambiarEstadoSensor(sensorId: Long, nuevoEstadoId: Long): Boolean {
        var cambiado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = """
                    UPDATE main_sensor 
                    SET estado_sensor_id = ?
                    WHERE id = ?
                """

                val stmt = conexion.prepareStatement(sql)
                stmt.setLong(1, nuevoEstadoId)
                stmt.setLong(2, sensorId)

                val filas = stmt.executeUpdate()
                if (filas > 0) {
                    cambiado = true
                    Log.d("SensorDao", "Estado del sensor $sensorId cambiado a $nuevoEstadoId")
                }

                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("SensorDao", "Error cambiando estado del sensor: ${e.message}", e)
        }

        return cambiado
    }
}