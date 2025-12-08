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

    // --- LOGIN ---
    fun validarUsuario(email: String, passwordPlano: String): Int {
        var resultado = ERROR_CONEXION
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                val sql = "SELECT password, username FROM main_usuario WHERE email = ?"
                val stmt = conexion.prepareStatement(sql)
                stmt.setString(1, email)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    val passwordHashEnBd = rs.getString("password")

                    if (DjangoPasswordHasher.checkPassword(passwordPlano, passwordHashEnBd)) {
                        resultado = LOGIN_EXITOSO
                        Log.d("UsuarioDao", "Login exitoso: ${rs.getString("username")}")

                        // Actualizar last_login
                        val sqlUpdate = "UPDATE main_usuario SET last_login = NOW() WHERE email = ?"
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
    fun crearUsuario(nombre: String, apellido: String, email: String, username: String, telefono: String, fechaNacimiento: String, passwordPlano: String): Boolean {
        var registrado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                conexion.autoCommit = false

                val passwordHash = DjangoPasswordHasher.hashPassword(passwordPlano)

                val sqlUsuario = """
                    INSERT INTO main_usuario 
                    (password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined, telefono, fecha_nacimiento) 
                    VALUES (?, NOW(), 0, ?, ?, ?, ?, 0, 1, NOW(), ?, ?)
                """

                val stmtUsuario = conexion.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)

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
                        val rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")
                        val tokenKey = rawToken.substring(0, 40)

                        // Usamos `key` con comillas invertidas
                        val sqlToken = "INSERT INTO authtoken_token (`key`, created, user_id) VALUES (?, NOW(), ?)"

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
            try { conexion?.rollback() } catch (ex: Exception) { ex.printStackTrace() }
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
                val sql = "SELECT id FROM main_usuario WHERE email = ?"
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
                val sql = "UPDATE main_usuario SET reset_password_token = ?, reset_password_expires = ? WHERE email = ?"
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

    // --- ¡ESTA ES LA QUE TE FALTABA! ---
    // Actualiza la contraseña con el nuevo hash y limpia el token usado
    fun actualizarPassword(email: String, nuevaPasswordPlana: String): Boolean {
        var actualizado = false
        val conexion = MySqlConexion.getConexion()

        try {
            if (conexion != null) {
                // 1. Cifrar la nueva contraseña
                val nuevoHash = DjangoPasswordHasher.hashPassword(nuevaPasswordPlana)

                // 2. Actualizar password y borrar el token de recuperación (ya se usó)
                val sql = "UPDATE main_usuario SET password = ?, reset_password_token = NULL, reset_password_expires = NULL WHERE email = ?"
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
                val sql = "SELECT id FROM main_usuario WHERE email = ?"
                val stmt = conexion.prepareStatement(sql)
                stmt.setString(1, email)
                val rs = stmt.executeQuery()
                if (rs.next()) id = rs.getLong("id")
                conexion.close()
            }
        } catch (e: Exception) { e.printStackTrace() }
        return id
    }
}