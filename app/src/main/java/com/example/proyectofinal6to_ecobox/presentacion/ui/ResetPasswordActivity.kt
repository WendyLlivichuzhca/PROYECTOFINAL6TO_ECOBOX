package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.UsuarioDao
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPass: TextInputEditText
    private lateinit var etConfirmPass: TextInputEditText
    private lateinit var btnChangePass: Button

    // Campos ocultos para pruebas manuales
    private lateinit var tilEmailOculto: TextInputLayout
    private lateinit var etEmailManual: TextInputEditText

    // Variables para guardar los datos
    private var emailRecibido: String = ""
    private var tokenRecibido: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // 1. VINCULAR VISTAS (IDs deben coincidir con tu XML)
        etNewPass = findViewById(R.id.etNewPass)
        etConfirmPass = findViewById(R.id.etConfirmPass)
        btnChangePass = findViewById(R.id.btnChangePass)
        tilEmailOculto = findViewById(R.id.tilEmailOculto)
        etEmailManual = findViewById(R.id.etEmailManual)

        // 2. INTENTAR LEER EL LINK DEL CORREO
        val data: Uri? = intent.data

        // ACEPTAR TANTO HTTP COMO ECOBOX (Por seguridad)
        if (data != null) {
            val urlStr = data.toString()
            if (urlStr.contains("www.ecobox-app.com/recuperar") || urlStr.startsWith("ecobox://")) {
                tokenRecibido = data.getQueryParameter("token") ?: ""
                emailRecibido = data.getQueryParameter("email") ?: ""
            }
        }

        // 3. MODO PRUEBA: Si no hay enlace, mostramos el campo para escribir el email a mano
        if (emailRecibido.isEmpty()) {
            tilEmailOculto.visibility = View.VISIBLE
            Toast.makeText(this, "Modo prueba: Ingresa el email manualmente", Toast.LENGTH_SHORT).show()
        } else {
            tilEmailOculto.visibility = View.GONE
        }

        // 4. CONFIGURAR EL BOTÓN
        btnChangePass.setOnClickListener {
            Log.d("ResetPassword", "Botón presionado") // Revisa el Logcat si sigue fallando
            cambiarContrasena()
        }
    }

    private fun cambiarContrasena() {
        val pass = etNewPass.text.toString().trim()
        val confirm = etConfirmPass.text.toString().trim()

        // Si estamos en modo prueba, leemos el email del campo manual
        if (emailRecibido.isEmpty()) {
            emailRecibido = etEmailManual.text.toString().trim()
        }

        // --- VALIDACIONES ---

        // Validar que tengamos un email a quien cambiarle la clave
        if (emailRecibido.isEmpty()) {
            Toast.makeText(this, "Error: Escribe el correo del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass.isEmpty() || pass.length < 6) {
            etNewPass.error = "Mínimo 6 caracteres"
            return
        }
        if (pass != confirm) {
            etConfirmPass.error = "Las contraseñas no coinciden"
            return
        }

        // Bloquear botón para evitar doble clic
        btnChangePass.isEnabled = false
        btnChangePass.text = "Guardando..."
        Toast.makeText(this, "Actualizando contraseña...", Toast.LENGTH_SHORT).show()

        Thread {
            // 5. LLAMADA A LA BASE DE DATOS
            // Asegúrate de que UsuarioDao tenga la función 'actualizarPassword'
            val exito = UsuarioDao.actualizarPassword(emailRecibido, pass)

            runOnUiThread {
                btnChangePass.isEnabled = true
                btnChangePass.text = "Cambiar Contraseña"

                if (exito) {
                    Toast.makeText(this, "¡Éxito! Contraseña actualizada.", Toast.LENGTH_LONG).show()

                    // Ir al Login
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error: No se encontró el email o falló la conexión.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}