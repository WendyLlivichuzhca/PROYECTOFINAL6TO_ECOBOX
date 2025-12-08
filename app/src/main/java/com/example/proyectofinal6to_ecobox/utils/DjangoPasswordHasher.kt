package com.example.proyectofinal6to_ecobox.utils

import android.util.Base64
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object DjangoPasswordHasher {

    private const val ALGORITHM = "pbkdf2_sha256"
    private const val ITERATIONS = 260000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 12

    // --- FUNCIÓN PARA CREAR HASH (REGISTRO) ---
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hash = getPbkdf2(password, salt, ITERATIONS)
        return "${ALGORITHM}$${ITERATIONS}$${salt}$${hash}"
    }

    // --- NUEVA FUNCIÓN PARA VERIFICAR (LOGIN) ---
    fun checkPassword(password: String, storedHash: String): Boolean {
        return try {
            // El hash de Django se ve así: pbkdf2_sha256$260000$salt$hash
            val parts = storedHash.split("$")
            if (parts.size != 4) return false

            val iterations = parts[1].toInt()
            val salt = parts[2]
            val storedKey = parts[3]

            // Encriptamos la contraseña que escribió el usuario usando EL MISMO salt y iteraciones
            val newHash = getPbkdf2(password, salt, iterations)

            // Comparamos si el resultado es idéntico al guardado
            newHash == storedKey
        } catch (e: Exception) {
            false
        }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(SALT_LENGTH)
        random.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP).substring(0, SALT_LENGTH)
    }

    // Modificamos para aceptar iteraciones dinámicas (importante para verificar)
    private fun getPbkdf2(password: String, salt: String, iterations: Int): String {
        return try {
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), iterations, KEY_LENGTH)
            val key = skf.generateSecret(spec)
            val res = key.encoded
            Base64.encodeToString(res, Base64.NO_WRAP)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            ""
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
            ""
        }
    }
}