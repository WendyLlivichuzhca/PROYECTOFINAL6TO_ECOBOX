    package com.example.proyectofinal6to_ecobox.data.dao

    import com.example.proyectofinal6to_ecobox.data.model.FamiliaUi
    import com.example.proyectofinal6to_ecobox.data.model.MiembroUi
    import java.sql.Connection
    import java.sql.Date
    import java.sql.Statement
    import java.sql.Timestamp
    import java.util.Calendar

    object FamiliaDao {

        // ========== MÉTODOS EXISTENTES (ya los tienes) ==========

        fun obtenerFamiliasPorUsuario(usuarioId: Long): List<FamiliaUi> {
            val lista = mutableListOf<FamiliaUi>()
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
                conn = MySqlConexion.getConexion()
                if (conn != null) {
                    val ps = conn.prepareStatement(sql)
                    ps.setLong(1, usuarioId)
                    val rs = ps.executeQuery()

                    while (rs.next()) {
                        val nombre = rs.getString("nombre")
                        lista.add(
                            FamiliaUi(
                                rs.getLong("id"),
                                nombre,
                                rs.getString("codigo_invitacion") ?: "",
                                rs.getInt("total_miembros"),
                                rs.getInt("total_plantas"),
                                rs.getString("rol_nombre") ?: "Miembro",
                                if (nombre.isNotEmpty()) nombre.substring(0, 1).uppercase() else "?"
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

        fun crearFamilia(nombreFamilia: String, usuarioId: Long): Boolean {
            var conn: Connection? = null
            try {
                conn = MySqlConexion.getConexion()
                if (conn == null) return false

                conn.autoCommit = false
                val codigo = generarCodigoUnico(conn)

                val sqlFam =
                    "INSERT INTO familia (nombre, codigo_invitacion, fecha_creacion, cantidad_plantas) VALUES (?, ?, NOW(), 0)"
                val psFam = conn.prepareStatement(sqlFam, Statement.RETURN_GENERATED_KEYS)
                psFam.setString(1, nombreFamilia)
                psFam.setString(2, codigo)
                psFam.executeUpdate()

                val rsKeys = psFam.generatedKeys
                var familiaId: Long = -1
                if (rsKeys.next()) familiaId = rsKeys.getLong(1)

                if (familiaId != -1L) {
                    val sqlUnion = """
                        INSERT INTO familia_usuario 
                        (familia_id, usuario_id, rol_id, es_administrador, activo, fecha_union) 
                        VALUES (?, ?, 1, 1, 1, NOW())
                    """
                    val psUnion = conn.prepareStatement(sqlUnion)
                    psUnion.setLong(1, familiaId)
                    psUnion.setLong(2, usuarioId)
                    psUnion.executeUpdate()

                    conn.commit()
                    return true
                } else {
                    conn.rollback()
                    return false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    conn?.rollback()
                } catch (ex: Exception) {
                }
                return false
            } finally {
                try {
                    conn?.autoCommit = true
                } catch (ex: Exception) {
                }
                conn?.close()
            }
        }

        fun unirseFamilia(codigo: String, usuarioId: Long): String {
            var conn: Connection? = null
            try {
                conn = MySqlConexion.getConexion()
                if (conn == null) return "Error de conexión"

                val sqlBuscar = "SELECT id FROM familia WHERE codigo_invitacion = ?"
                val psBuscar = conn.prepareStatement(sqlBuscar)
                psBuscar.setString(1, codigo)
                val rs = psBuscar.executeQuery()

                if (rs.next()) {
                    val familiaId = rs.getLong("id")

                    val sqlCheck =
                        "SELECT id FROM familia_usuario WHERE familia_id = ? AND usuario_id = ? AND activo = 1"
                    val psCheck = conn.prepareStatement(sqlCheck)
                    psCheck.setLong(1, familiaId)
                    psCheck.setLong(2, usuarioId)
                    if (psCheck.executeQuery().next()) return "Ya perteneces a esta familia"

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
                        val miembro = com.example.proyectofinal6to_ecobox.data.model.MiembroUi(
                            rs.getLong("id"),
                            rs.getString("nombre") ?: "Sin nombre",
                            rs.getString("email") ?: "",
                            rs.getString("rol_nombre") ?: "Miembro",
                            rs.getBoolean("es_administrador"),
                            obtenerUltimaActividadUsuario(conn, rs.getLong("id")),
                            true, // estaActivo - puedes implementar lógica real
                            generarColorAvatar(rs.getString("nombre")) // Color basado en nombre
                        )
                        lista.add(miembro)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.close()
            }
            return lista
        }

        // Función auxiliar para generar color del avatar
        private fun generarColorAvatar(nombre: String?): String {
            if (nombre.isNullOrEmpty()) return "#4CAF50"

            // Generar color consistente basado en el nombre
            val colors = listOf(
                "#4CAF50", // Verde
                "#2196F3", // Azul
                "#FF9800", // Naranja
                "#9C27B0", // Púrpura
                "#F44336", // Rojo
                "#00BCD4", // Cyan
                "#795548", // Marrón
            )

            val hash = nombre.hashCode()
            val index = Math.abs(hash) % colors.size
            return colors[index]
        }

        // ========== NUEVOS MÉTODOS PARA LAS ESTADÍSTICAS ==========

        // 1. Obtener total de miembros en todas las familias del usuario
        fun obtenerTotalMiembros(usuarioId: Long): Int {
            var total = 0
            val sql = """
                SELECT COUNT(DISTINCT fu2.usuario_id) as total
                FROM familia_usuario fu1
                JOIN familia_usuario fu2 ON fu1.familia_id = fu2.familia_id
                WHERE fu1.usuario_id = ? 
                AND fu1.activo = 1 
                AND fu2.activo = 1
            """

            var conn: Connection? = null
            try {
                conn = MySqlConexion.getConexion()
                if (conn != null) {
                    val ps = conn.prepareStatement(sql)
                    ps.setLong(1, usuarioId)
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        total = rs.getInt("total")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.close()
            }
            return total
        }

        // 2. Obtener información específica de una familia
        fun obtenerFamiliaPorId(familiaId: Long): FamiliaInfo? {
            val sql = """
                SELECT 
                    f.id,
                    f.nombre,
                    f.codigo_invitacion,
                    f.fecha_creacion,
                    (SELECT COUNT(*) FROM familia_usuario fu WHERE fu.familia_id = f.id AND fu.activo = 1) as total_miembros,
                    (SELECT COUNT(*) FROM planta p WHERE p.familia_id = f.id) as total_plantas
                FROM familia f
                WHERE f.id = ?
            """

            var conn: Connection? = null
            try {
                conn = MySqlConexion.getConexion()
                if (conn != null) {
                    val ps = conn.prepareStatement(sql)
                    ps.setLong(1, familiaId)
                    val rs = ps.executeQuery()

                    if (rs.next()) {
                        return FamiliaInfo(
                            id = rs.getLong("id"),
                            nombre = rs.getString("nombre") ?: "Sin nombre",
                            codigo = rs.getString("codigo_invitacion") ?: "",
                            fechaCreacion = rs.getTimestamp("fecha_creacion"),
                            totalMiembros = rs.getInt("total_miembros"),
                            totalPlantas = rs.getInt("total_plantas")
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.close()
            }
            return null
        }

        // 3. Obtener cantidad de plantas por familia (si no lo tienes en FamiliaInfo)
        fun obtenerCantidadPlantasPorFamilia(familiaId: Long): Int {
            var cantidad = 0
            val sql = "SELECT COUNT(*) as total FROM planta WHERE familia_id = ?"

            var conn: Connection? = null
            try {
                conn = MySqlConexion.getConexion()
                if (conn != null) {
                    val ps = conn.prepareStatement(sql)
                    ps.setLong(1, familiaId)
                    val rs = ps.executeQuery()
                    if (rs.next()) {
                        cantidad = rs.getInt("total")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.close()
            }
            return cantidad
        }

        // 4. Obtener familias simples para dropdowns
        fun obtenerFamiliasSimplesPorUsuario(usuarioId: Long): List<FamiliaSimple> {
            val lista = mutableListOf<FamiliaSimple>()
            val sql = """
                SELECT f.id, f.nombre
                FROM familia f
                JOIN familia_usuario fu ON f.id = fu.familia_id
                WHERE fu.usuario_id = ? AND fu.activo = 1
                ORDER BY f.nombre ASC
            """

            var conn: Connection? = null
            try {
                conn = MySqlConexion.getConexion()
                if (conn != null) {
                    val ps = conn.prepareStatement(sql)
                    ps.setLong(1, usuarioId)
                    val rs = ps.executeQuery()

                    while (rs.next()) {
                        lista.add(
                            FamiliaSimple(
                                id = rs.getLong("id"),
                                nombre = rs.getString("nombre") ?: "Sin nombre"
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

        // 5. Invitar miembro por email (si tienes tabla de invitaciones)
        fun invitarMiembro(familiaId: Long, emailInvitado: String, usuarioIdInvitador: Long): Boolean {
            var conn: Connection? = null
            try {
                conn = MySqlConexion.getConexion()
                if (conn == null) return false

                // Verificar que el invitador pertenece a la familia
                val sqlCheck = """
                    SELECT id FROM familia_usuario 
                    WHERE familia_id = ? AND usuario_id = ? AND activo = 1
                """
                val psCheck = conn.prepareStatement(sqlCheck)
                psCheck.setLong(1, familiaId)
                psCheck.setLong(2, usuarioIdInvitador)
                if (!psCheck.executeQuery().next()) return false

                // Generar código de invitación único
                val codigoInvitacion = generarCodigoInvitacion(conn)

                // Insertar en tabla de invitaciones (si existe)
                val sqlInsert = """
                    INSERT INTO invitaciones_familia 
                    (familia_id, email_invitado, codigo_invitacion, usuario_id_invitador, fecha_invitacion, estado) 
                    VALUES (?, ?, ?, ?, NOW(), 'pendiente')
                """
                val psInsert = conn.prepareStatement(sqlInsert)
                psInsert.setLong(1, familiaId)
                psInsert.setString(2, emailInvitado)
                psInsert.setString(3, codigoInvitacion)
                psInsert.setLong(4, usuarioIdInvitador)
                psInsert.executeUpdate()

                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            } finally {
                conn?.close()
            }
        }

        // 6. Eliminar miembro de familia (solo administradores)
        fun eliminarMiembro(
            familiaId: Long,
            usuarioIdEliminar: Long,
            usuarioIdSolicitante: Long
        ): Boolean {
            var conn: Connection? = null
            try {
                conn = MySqlConexion.getConexion()
                if (conn == null) return false

                // Verificar que el solicitante es administrador
                val sqlCheckAdmin = """
                    SELECT es_administrador FROM familia_usuario 
                    WHERE familia_id = ? AND usuario_id = ? AND activo = 1
                """
                val psCheck = conn.prepareStatement(sqlCheckAdmin)
                psCheck.setLong(1, familiaId)
                psCheck.setLong(2, usuarioIdSolicitante)
                val rs = psCheck.executeQuery()

                if (!rs.next() || !rs.getBoolean("es_administrador")) {
                    return false // No es administrador
                }

                // No permitir eliminarse a sí mismo si es el único administrador
                if (usuarioIdEliminar == usuarioIdSolicitante) {
                    val sqlCheckOtrosAdmins = """
                        SELECT COUNT(*) as otros_admins FROM familia_usuario 
                        WHERE familia_id = ? AND usuario_id != ? AND es_administrador = 1 AND activo = 1
                    """
                    val psOtros = conn.prepareStatement(sqlCheckOtrosAdmins)
                    psOtros.setLong(1, familiaId)
                    psOtros.setLong(2, usuarioIdEliminar)
                    val rsOtros = psOtros.executeQuery()
                    if (rsOtros.next() && rsOtros.getInt("otros_admins") == 0) {
                        return false // Es el único administrador
                    }
                }

                // Marcar como inactivo (soft delete)
                val sqlUpdate = """
                    UPDATE familia_usuario 
                    SET activo = 0, fecha_salida = NOW() 
                    WHERE familia_id = ? AND usuario_id = ?
                """
                val psUpdate = conn.prepareStatement(sqlUpdate)
                psUpdate.setLong(1, familiaId)
                psUpdate.setLong(2, usuarioIdEliminar)
                return psUpdate.executeUpdate() > 0

            } catch (e: Exception) {
                e.printStackTrace()
                return false
            } finally {
                conn?.close()
            }
        }

        // ========== MÉTODOS PRIVADOS DE AYUDA ==========

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

        private fun generarCodigoInvitacion(conn: Connection): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            while (true) {
                val codigo = (1..10).map { chars.random() }.joinToString("")
                val ps =
                    conn.prepareStatement("SELECT id FROM invitaciones_familia WHERE codigo_invitacion = ?")
                ps.setString(1, codigo)
                val rs = ps.executeQuery()
                if (!rs.next()) return codigo
            }
        }

        private fun obtenerUltimaActividadUsuario(conn: Connection, usuarioId: Long): String {
            val sql = """
        SELECT MAX(fecha) as ultima_actividad 
        FROM (
            -- 1. Última unión a familia
            SELECT MAX(fecha_union) as fecha FROM familia_usuario WHERE usuario_id = ?
            
            UNION ALL
            
            -- 2. Última planta creada (NOTA: en tu BD, planta no tiene usuario_id, así que cambiamos esto)
            SELECT MAX(fecha_creacion) as fecha FROM planta 
            WHERE familia_id IN (
                SELECT familia_id FROM familia_usuario WHERE usuario_id = ? AND activo = 1
            )
            
            UNION ALL
            
            -- 3. Último riego (usando tabla 'riego' que SÍ existe)
            SELECT MAX(fecha_creacion) as fecha FROM riego 
            WHERE usuario_id = ?
            
            UNION ALL
            
            -- 4. Última notificación (usando tabla 'notificacion' que SÍ existe)
            SELECT MAX(fecha_creacion) as fecha FROM notificacion 
            WHERE usuario_id = ?
            
            UNION ALL
            
            -- 5. Último seguimiento de planta (usando tabla 'seguimiento_estado_planta' que SÍ existe)
            SELECT MAX(fecha_registro) as fecha FROM seguimiento_estado_planta 
            WHERE planta_id IN (
                SELECT id FROM planta WHERE familia_id IN (
                    SELECT familia_id FROM familia_usuario WHERE usuario_id = ? AND activo = 1
                )
            )
        ) as actividades
    """

            return try {
                val ps = conn.prepareStatement(sql)
                ps.setLong(1, usuarioId)
                ps.setLong(2, usuarioId)
                ps.setLong(3, usuarioId)
                ps.setLong(4, usuarioId)
                ps.setLong(5, usuarioId)
                val rs = ps.executeQuery()

                if (rs.next()) {
                    val timestamp = rs.getTimestamp("ultima_actividad")
                    timestamp?.let {
                        formatearFechaAmigable(it)
                    } ?: "Nunca"
                } else {
                    "Nunca"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Desconocido"
            }
        }

        private fun formatearFechaAmigable(timestamp: Timestamp): String {
            val ahora = System.currentTimeMillis()
            val fecha = timestamp.time
            val diferencia = ahora - fecha

            // Convertir Timestamp a Date
            val date = Date(timestamp.time)
            val calendar = Calendar.getInstance().apply { time = date }

            return when {
                diferencia < 60000 -> "Hace un momento"
                diferencia < 3600000 -> "Hace ${diferencia / 60000} min"
                diferencia < 86400000 -> {
                    // Hoy, formatear hora
                    val hora = calendar.get(Calendar.HOUR_OF_DAY)
                    val minuto = calendar.get(Calendar.MINUTE)
                    val amPm = if (hora < 12) "AM" else "PM"
                    val hora12 = if (hora > 12) hora - 12 else if (hora == 0) 12 else hora
                    "Hoy, ${hora12}:${minuto.toString().padStart(2, '0')} $amPm"
                }

                diferencia < 172800000 -> "Ayer"
                diferencia < 604800000 -> "Hace ${diferencia / 86400000} días"
                else -> {
                    // Formato de fecha simple
                    val dia = calendar.get(Calendar.DAY_OF_MONTH)
                    val mes = calendar.get(Calendar.MONTH) + 1 // Mes empieza en 0
                    val anio = calendar.get(Calendar.YEAR)
                    "$dia/$mes/$anio"
                }
            }
        }

        // ========== DATA CLASSES ==========

        data class FamiliaInfo(
            val id: Long,
            val nombre: String,
            val codigo: String,
            val fechaCreacion: Timestamp?,
            val totalMiembros: Int,
            val totalPlantas: Int
        )

        data class FamiliaSimple(
            val id: Long,
            val nombre: String
        ) {
            override fun toString(): String = nombre
        }

        // Data class para estadísticas del dashboard
        data class EstadisticasDashboard(
            val totalFamilias: Int,
            val totalMiembros: Int,
            val totalPlantas: Int,
            val familiasRecientes: List<FamiliaUi>
        )
    }