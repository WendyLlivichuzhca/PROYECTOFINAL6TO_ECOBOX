package com.example.proyectofinal6to_ecobox.data.dao

import android.util.Log
import com.example.proyectofinal6to_ecobox.utils.DjangoPasswordHasher
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.UUID

object UsuarioDao {

    // Códigos de respuesta para el Login
    const val LOGIN_EXITOSO = 1
    const val EMAIL_NO_ENCONTRADO = 2
    const val PASSWORD_INCORRECTO = 3
    const val ERROR_CONEXION = 4

    // Tablas según tu base de datos MySQL
    private object Tablas {
        const val USUARIO = "main_usuario"
        const val SENSOR = "main_sensor"
        const val PLANTA = "planta"
        const val FAMILIA = "familia"
        const val FAMILIA_USUARIO = "familia_usuario"
        const val ROL = "rol"
        const val AUTH_TOKEN = "authtoken_token"
        const val PREDICCIONES_IA = "predicciones_ia"
    }

    // --- LOGIN ---
    fun validarUsuario(email: String, passwordPlano: String): Int {
        var resultado = ERROR_CONEXION
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = "SELECT password, username FROM ${Tablas.USUARIO} WHERE email = ?"
                val stmt = conexion.prepareStatement(sql)
                stmt.setString(1, email)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    val passwordHashEnBd = rs.getString("password")

                    if (DjangoPasswordHasher.checkPassword(passwordPlano, passwordHashEnBd)) {
                        resultado = LOGIN_EXITOSO
                        Log.d("UsuarioDao", "Login exitoso: ${rs.getString("username")}")

                        // Actualizar last_login
                        val sqlUpdate =
                            "UPDATE ${Tablas.USUARIO} SET last_login = NOW() WHERE email = ?"
                        val stmtUpdate = conexion.prepareStatement(sqlUpdate)
                        stmtUpdate.setString(1, email)
                        stmtUpdate.executeUpdate()
                        stmtUpdate.close()

                    } else {
                        resultado = PASSWORD_INCORRECTO
                    }
                } else {
                    resultado = EMAIL_NO_ENCONTRADO
                }
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error login", e)
            resultado = ERROR_CONEXION
        }
        return resultado
    }

    // --- REGISTRO ---
    fun crearUsuario(
        nombre: String,
        apellido: String,
        email: String,
        username: String,
        telefono: String,
        fechaNacimiento: String,
        passwordPlano: String
    ): Boolean {
        var registrado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                conexion.autoCommit = false

                val passwordHash = DjangoPasswordHasher.hashPassword(passwordPlano)

                val sqlUsuario = """
                    INSERT INTO ${Tablas.USUARIO} 
                    (password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined, telefono, fecha_nacimiento) 
                    VALUES (?, NOW(), 0, ?, ?, ?, ?, 0, 1, NOW(), ?, ?)
                """

                val stmtUsuario =
                    conexion.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)

                stmtUsuario.setString(1, passwordHash)
                stmtUsuario.setString(2, username)
                stmtUsuario.setString(3, nombre)
                stmtUsuario.setString(4, apellido)
                stmtUsuario.setString(5, email)
                stmtUsuario.setString(6, telefono)

                if (fechaNacimiento.isNotEmpty()) {
                    stmtUsuario.setString(7, fechaNacimiento)
                } else {
                    stmtUsuario.setNull(7, java.sql.Types.DATE)
                }

                val filasUsuario = stmtUsuario.executeUpdate()

                if (filasUsuario > 0) {
                    val generatedKeys = stmtUsuario.generatedKeys
                    if (generatedKeys.next()) {
                        val userIdGenerado = generatedKeys.getLong(1)

                        // Generar Token
                        val rawToken =
                            UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID()
                                .toString().replace("-", "")
                        val tokenKey = rawToken.substring(0, 40)

                        // Insertar token en authtoken_token
                        val sqlToken =
                            "INSERT INTO ${Tablas.AUTH_TOKEN} (`key`, created, user_id) VALUES (?, NOW(), ?)"

                        val stmtToken = conexion.prepareStatement(sqlToken)
                        stmtToken.setString(1, tokenKey)
                        stmtToken.setLong(2, userIdGenerado)

                        stmtToken.executeUpdate()

                        conexion.commit()
                        registrado = true
                    }
                }
                stmtUsuario.close()
                conexion.close()
            }
        } catch (e: Exception) {
            try {
                conexion?.rollback()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            Log.e("UsuarioDao", "Error registro", e)
        }
        return registrado
    }

    // --- VERIFICAR EXISTENCIA ---
    fun existeUsuarioPorEmail(email: String): Boolean {
        var existe = false
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = "SELECT id FROM ${Tablas.USUARIO} WHERE email = ?"
                val stmt = conexion.prepareStatement(sql)
                stmt.setString(1, email)
                val rs = stmt.executeQuery()
                if (rs.next()) existe = true
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error verificando email", e)
        }
        return existe
    }

    // --- GUARDAR TOKEN DE RECUPERACIÓN ---
    fun guardarTokenRecuperacion(email: String, token: String, fechaExpiracion: String): Boolean {
        var actualizado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql =
                    "UPDATE ${Tablas.USUARIO} SET reset_password_token = ?, reset_password_expires = ? WHERE email = ?"
                val stmt = conexion.prepareStatement(sql)

                stmt.setString(1, token)
                stmt.setString(2, fechaExpiracion)
                stmt.setString(3, email)

                val filas = stmt.executeUpdate()
                if (filas > 0) actualizado = true

                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error guardando token", e)
        }
        return actualizado
    }

    // --- ACTUALIZAR PASSWORD ---
    fun actualizarPassword(email: String, nuevaPasswordPlana: String): Boolean {
        var actualizado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // 1. Cifrar la nueva contraseña
                val nuevoHash = DjangoPasswordHasher.hashPassword(nuevaPasswordPlana)

                // 2. Actualizar password y borrar el token de recuperación (ya se usó)
                val sql =
                    "UPDATE ${Tablas.USUARIO} SET password = ?, reset_password_token = NULL, reset_password_expires = NULL WHERE email = ?"
                val stmt = conexion.prepareStatement(sql)

                stmt.setString(1, nuevoHash)
                stmt.setString(2, email)

                val filas = stmt.executeUpdate()
                if (filas > 0) {
                    actualizado = true
                    Log.d("UsuarioDao", "Contraseña actualizada para $email")
                }
                stmt.close()
                conexion.close()
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error actualizando password", e)
        }
        return actualizado
    }

    fun obtenerIdPorEmail(email: String): Long {
        var id: Long = -1
        val conexion = MySqlConexion.getConexion()
        try {
            if (conexion != null) {
                val sql = "SELECT id FROM ${Tablas.USUARIO} WHERE email = ?"
                val stmt = conexion.prepareStatement(sql)
                stmt.setString(1, email)
                val rs = stmt.executeQuery()
                if (rs.next()) id = rs.getLong("id")
                conexion.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    // --- OBTENER PERFIL COMPLETO ---
    fun obtenerPerfilCompleto(idUsuario: Long): Map<String, String> {
        val datos = mutableMapOf<String, String>()
        val conn = MySqlConexion.getConexion()

        try {
            if (conn != null) {
                // 1. Datos del usuario (Django style)
                val sql = """
                    SELECT u.first_name, u.last_name, u.email, u.telefono, u.username,
                           r.nombre as rol_nombre
                    FROM ${Tablas.USUARIO} u 
                    LEFT JOIN ${Tablas.ROL} r ON u.rol_id = r.id 
                    WHERE u.id = ?
                """.trimIndent()

                val ps: PreparedStatement = conn.prepareStatement(sql)
                ps.setLong(1, idUsuario)
                val rs: ResultSet = ps.executeQuery()

                if (rs.next()) {
                    datos["nombre"] = rs.getString("first_name") ?: ""
                    datos["apellido"] = rs.getString("last_name") ?: ""
                    datos["email"] = rs.getString("email") ?: ""
                    datos["username"] = rs.getString("username") ?: ""
                    datos["telefono"] = rs.getString("telefono") ?: "No registrado"
                    datos["rol"] = rs.getString("rol_nombre") ?: "Miembro"
                }

                // 2. Estadísticas según tu base de datos
                val sqlStats = """
                    SELECT 
                        (SELECT COUNT(*) FROM ${Tablas.PLANTA} p 
                         INNER JOIN ${Tablas.FAMILIA_USUARIO} fu ON p.familia_id = fu.familia_id 
                         WHERE fu.usuario_id = ? AND fu.activo = 1) as total_plantas,
                         
                        (SELECT COUNT(*) FROM ${Tablas.SENSOR} s 
                         INNER JOIN ${Tablas.PLANTA} p ON s.planta_id = p.id 
                         INNER JOIN ${Tablas.FAMILIA_USUARIO} fu ON p.familia_id = fu.familia_id 
                         WHERE fu.usuario_id = ? AND fu.activo = 1) as total_sensores,
                         
                        (SELECT COUNT(DISTINCT familia_id) FROM ${Tablas.FAMILIA_USUARIO} 
                         WHERE usuario_id = ? AND activo = 1) as total_familias,
                         
                        (SELECT COUNT(*) FROM ${Tablas.PREDICCIONES_IA} pi
                         INNER JOIN ${Tablas.PLANTA} p ON pi.planta_id = p.id 
                         INNER JOIN ${Tablas.FAMILIA_USUARIO} fu ON p.familia_id = fu.familia_id 
                         WHERE fu.usuario_id = ? AND fu.activo = 1) as total_ia
                """.trimIndent()

                val psStats = conn.prepareStatement(sqlStats)
                psStats.setLong(1, idUsuario)
                psStats.setLong(2, idUsuario)
                psStats.setLong(3, idUsuario)
                psStats.setLong(4, idUsuario)
                val rsStats = psStats.executeQuery()

                if (rsStats.next()) {
                    datos["plantas"] = rsStats.getInt("total_plantas").toString()
                    datos["sensores"] = rsStats.getInt("total_sensores").toString()
                    datos["familias"] = rsStats.getInt("total_familias").toString()
                    datos["ia"] = rsStats.getInt("total_ia").toString()
                }

                // 3. Obtener nombre de la familia principal (activa)
                val sqlFam = """
                    SELECT f.nombre 
                    FROM ${Tablas.FAMILIA} f 
                    INNER JOIN ${Tablas.FAMILIA_USUARIO} fu ON f.id = fu.familia_id 
                    WHERE fu.usuario_id = ? AND fu.activo = 1 
                    ORDER BY fu.fecha_union DESC 
                    LIMIT 1
                """.trimIndent()

                val psFam = conn.prepareStatement(sqlFam)
                psFam.setLong(1, idUsuario)
                val rsFam = psFam.executeQuery()
                if (rsFam.next()) {
                    datos["familia"] = rsFam.getString("nombre")
                } else {
                    datos["familia"] = "Sin familia activa"
                }

                conn.close()
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error obteniendo perfil: ${e.message}", e)
        }
        return datos
    }

    // --- ACTUALIZAR DATOS ---
    fun actualizarUsuario(idUsuario: Long, nombre: String, apellido: String): Boolean {
        var actualizado = false
        val conn = MySqlConexion.getConexion()

        try {
            if (conn != null) {
                val sql = "UPDATE ${Tablas.USUARIO} SET first_name = ?, last_name = ? WHERE id = ?"
                val ps = conn.prepareStatement(sql)
                ps.setString(1, nombre)
                ps.setString(2, apellido)
                ps.setLong(3, idUsuario)
                val filas = ps.executeUpdate()
                actualizado = filas > 0
                conn.close()
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error actualizando usuario: ${e.message}")
        }
        return actualizado
    }

    // --- OBTENER USUARIO POR TOKEN DE RECUPERACIÓN ---
    fun obtenerUsuarioPorToken(token: String): Map<String, Any?>? {
        val conn = MySqlConexion.getConexion()
        try {
            if (conn != null) {
                val sql = """
                    SELECT id, email, reset_password_expires 
                    FROM ${Tablas.USUARIO} 
                    WHERE reset_password_token = ? 
                    AND reset_password_expires > NOW()
                """.trimIndent()

                val ps = conn.prepareStatement(sql)
                ps.setString(1, token)
                val rs = ps.executeQuery()

                if (rs.next()) {
                    return mapOf(
                        "id" to rs.getLong("id"),
                        "email" to rs.getString("email"),
                        "expires" to rs.getString("reset_password_expires")
                    )
                }
                conn.close()
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error obteniendo usuario por token: ${e.message}")
        }
        return null
    }

    // --- OBTENER USUARIO POR ID ---
    fun obtenerUsuarioPorId(idUsuario: Long): Map<String, String>? {
        val conn = MySqlConexion.getConexion()
        try {
            if (conn != null) {
                val sql = """
                    SELECT first_name, last_name, email, username, telefono 
                    FROM ${Tablas.USUARIO} 
                    WHERE id = ?
                """.trimIndent()

                val ps = conn.prepareStatement(sql)
                ps.setLong(1, idUsuario)
                val rs = ps.executeQuery()

                if (rs.next()) {
                    return mapOf(
                        "nombre" to (rs.getString("first_name") ?: ""),
                        "apellido" to (rs.getString("last_name") ?: ""),
                        "email" to (rs.getString("email") ?: ""),
                        "username" to (rs.getString("username") ?: ""),
                        "telefono" to (rs.getString("telefono") ?: "")
                    )
                }
                conn.close()
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error obteniendo usuario por ID: ${e.message}")
        }
        return null
    }

}