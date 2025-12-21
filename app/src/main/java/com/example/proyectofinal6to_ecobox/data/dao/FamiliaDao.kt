package com.example.proyectofinal6to_ecobox.data.dao

import com.example.proyectofinal6to_ecobox.data.model.FamiliaUi
import java.sql.Connection
import java.sql.Statement

object FamiliaDao {

    // 1. LISTAR FAMILIAS (Retorna objetos FamiliaUi)
    fun obtenerFamiliasPorUsuario(usuarioId: Long): List<FamiliaUi> {
        val lista = mutableListOf<FamiliaUi>()

        // SQL: Trae datos de la familia + Nombre del Rol + Conteo de Miembros + Conteo de Plantas
        val sql = """
            SELECT 
                f.id, 
                f.nombre, 
                f.codigo_invitacion,
                r.nombre as rol_nombre,
                (SELECT COUNT(*) FROM familia_usuario fu2 WHERE fu2.familia_id = f.id AND fu2.activo = 1) as total_miembros,
                (SELECT COUNT(*) FROM planta p WHERE p.familia_id = f.id) as total_plantas
            FROM familia f
            JOIN familia_usuario fu ON f.id = fu.familia_id
            LEFT JOIN rol r ON fu.rol_id = r.id
            WHERE fu.usuario_id = ? AND fu.activo = 1
        """

        var conn: Connection? = null
        try {
            conn = MySqlConexion.getConexion() // Tu objeto de conexión
            if (conn != null) {
                val ps = conn.prepareStatement(sql)
                ps.setLong(1, usuarioId)
                val rs = ps.executeQuery()

                while (rs.next()) {
                    val nombre = rs.getString("nombre")

                    // --- CORRECCIÓN AQUÍ ---
                    // Se pasan los valores en el ORDEN EXACTO del constructor de Java.
                    // Sin poner "id =", "nombre =", etc.
                    lista.add(
                        FamiliaUi(
                            rs.getLong("id"),                                    // 1. id
                            nombre,                                              // 2. nombre
                            rs.getString("codigo_invitacion") ?: "",             // 3. codigo
                            rs.getInt("total_miembros"),                         // 4. cantidadMiembros
                            rs.getInt("total_plantas"),                          // 5. cantidadPlantas
                            rs.getString("rol_nombre") ?: "Miembro",             // 6. rolNombre
                            if (nombre.isNotEmpty()) nombre.substring(0, 1).uppercase() else "?" // 7. inicial
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.close()
        }
        return lista
    }

    // 2. CREAR FAMILIA (Transacción Atómica)
    fun crearFamilia(nombreFamilia: String, usuarioId: Long): Boolean {
        var conn: Connection? = null
        try {
            conn = MySqlConexion.getConexion()
            if (conn == null) return false

            conn.autoCommit = false // INICIO TRANSACCIÓN

            // A. Generar código
            val codigo = generarCodigoUnico(conn)

            // B. Insertar Familia
            val sqlFam = "INSERT INTO familia (nombre, codigo_invitacion, fecha_creacion, cantidad_plantas) VALUES (?, ?, NOW(), 0)"
            val psFam = conn.prepareStatement(sqlFam, Statement.RETURN_GENERATED_KEYS)
            psFam.setString(1, nombreFamilia)
            psFam.setString(2, codigo)
            psFam.executeUpdate()

            val rsKeys = psFam.generatedKeys
            var familiaId: Long = -1
            if (rsKeys.next()) {
                familiaId = rsKeys.getLong(1)
            }

            if (familiaId != -1L) {
                // C. Insertar Usuario como ADMIN (Rol ID 1)
                val sqlUnion = """
                    INSERT INTO familia_usuario 
                    (familia_id, usuario_id, rol_id, es_administrador, activo, fecha_union) 
                    VALUES (?, ?, 1, 1, 1, NOW())
                """
                val psUnion = conn.prepareStatement(sqlUnion)
                psUnion.setLong(1, familiaId)
                psUnion.setLong(2, usuarioId)
                psUnion.executeUpdate()

                conn.commit() // ÉXITO
                return true
            } else {
                conn.rollback()
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try { conn?.rollback() } catch (ex: Exception) {}
            return false
        } finally {
            try { conn?.autoCommit = true } catch (ex: Exception) {}
            conn?.close()
        }
    }

    // 3. UNIRSE A FAMILIA
    fun unirseFamilia(codigo: String, usuarioId: Long): String {
        var conn: Connection? = null
        try {
            conn = MySqlConexion.getConexion()
            if (conn == null) return "Error de conexión"

            // A. Buscar familia
            val sqlBuscar = "SELECT id FROM familia WHERE codigo_invitacion = ?"
            val psBuscar = conn.prepareStatement(sqlBuscar)
            psBuscar.setString(1, codigo)
            val rs = psBuscar.executeQuery()

            if (rs.next()) {
                val familiaId = rs.getLong("id")

                // B. Verificar si ya está
                val sqlCheck = "SELECT id FROM familia_usuario WHERE familia_id = ? AND usuario_id = ? AND activo = 1"
                val psCheck = conn.prepareStatement(sqlCheck)
                psCheck.setLong(1, familiaId)
                psCheck.setLong(2, usuarioId)
                if (psCheck.executeQuery().next()) {
                    return "Ya perteneces a esta familia"
                }

                // C. Insertar como MIEMBRO (Rol ID 2)
                val sqlInsert = """
                    INSERT INTO familia_usuario 
                    (familia_id, usuario_id, rol_id, es_administrador, activo, fecha_union) 
                    VALUES (?, ?, 2, 0, 1, NOW())
                """
                val psInsert = conn.prepareStatement(sqlInsert)
                psInsert.setLong(1, familiaId)
                psInsert.setLong(2, usuarioId)
                psInsert.executeUpdate()

                return "OK"
            } else {
                return "Código inválido"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: ${e.message}"
        } finally {
            conn?.close()
        }
    }

    private fun generarCodigoUnico(conn: Connection): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        while (true) {
            val codigo = (1..8).map { chars.random() }.joinToString("")
            val ps = conn.prepareStatement("SELECT id FROM familia WHERE codigo_invitacion = ?")
            ps.setString(1, codigo)
            val rs = ps.executeQuery()
            if (!rs.next()) return codigo
        }
    }
    // 4. OBTENER MIEMBROS DE UNA FAMILIA
    fun obtenerMiembrosPorFamilia(familiaId: Long): List<com.example.proyectofinal6to_ecobox.data.model.MiembroUi> {
        val lista = mutableListOf<com.example.proyectofinal6to_ecobox.data.model.MiembroUi>()
        val sql = """
        SELECT u.id, u.first_name as nombre, u.email, r.nombre as rol_nombre, fu.es_administrador
        FROM main_usuario u
        JOIN familia_usuario fu ON u.id = fu.usuario_id
        JOIN rol r ON fu.rol_id = r.id
        WHERE fu.familia_id = ? AND fu.activo = 1
    """

        var conn: Connection? = null
        try {
            conn = MySqlConexion.getConexion()
            if (conn != null) {
                val ps = conn.prepareStatement(sql)
                ps.setLong(1, familiaId)
                val rs = ps.executeQuery()

                while (rs.next()) {
                    lista.add(
                        com.example.proyectofinal6to_ecobox.data.model.MiembroUi(
                            rs.getLong("id"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("rol_nombre"),
                            rs.getBoolean("es_administrador")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.close()
        }
        return lista
    }
}