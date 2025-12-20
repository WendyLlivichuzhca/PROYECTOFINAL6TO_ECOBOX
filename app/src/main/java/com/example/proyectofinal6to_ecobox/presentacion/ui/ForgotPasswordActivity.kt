package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.UsuarioDao
import com.example.proyectofinal6to_ecobox.utils.EmailUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random

class ForgotPasswordActivity : AppCompatActivity() {

    // CAMBIO: Usamos EditText en lugar de TextInputEditText para coincidir con el XML
    private lateinit var etForgotEmail: EditText
    private lateinit var btnSendForgot: Button
    private lateinit var btnBackToLogin: TextView
    private lateinit var btnBackArrow: ImageButton // Nuevo botón de flecha

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // 1. Vincular vistas
        etForgotEmail = findViewById(R.id.etForgotEmail)
        btnSendForgot = findViewById(R.id.btnSendForgot)
        btnBackToLogin = findViewById(R.id.btnBackToLoginForgot)
        btnBackArrow = findViewById(R.id.btnBack) // Vinculamos la flecha

        // 2. Listeners
        btnSendForgot.setOnClickListener { procesarRecuperacion() }

        // Ambos botones hacen lo mismo: volver atrás
        btnBackToLogin.setOnClickListener { finish() }
        btnBackArrow.setOnClickListener { finish() }
    }

    private fun procesarRecuperacion() {
        val email = etForgotEmail.text.toString().trim()

        if (email.isEmpty()) {
            etForgotEmail.error = "Escribe tu correo"
            return
        }

        // Bloquear botón para evitar doble clic
        btnSendForgot.isEnabled = false
        btnSendForgot.text = "Enviando..."
        Toast.makeText(this, "Verificando cuenta...", Toast.LENGTH_SHORT).show()

        Thread {
            // Verificar si el correo existe en la BD
            if (UsuarioDao.existeUsuarioPorEmail(email)) {

                // 1. GENERAR TOKEN (Código de 6 dígitos)
                val token = String.format("%06d", Random().nextInt(999999))

                // 2. CALCULAR EXPIRACIÓN (Ahora + 1 hora)
                val calendario = Calendar.getInstance()
                calendario.add(Calendar.HOUR, 1)

                // Formato compatible con MySQL DateTime: "yyyy-MM-dd HH:mm:ss"
                val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val fechaExpiracion = formatoFecha.format(calendario.time)

                // 3. GUARDAR EN BD (Actualizamos token y fecha en la tabla usuario)
                val guardado = UsuarioDao.guardarTokenRecuperacion(email, token, fechaExpiracion)

                if (guardado) {
                    // 4. ENVIAR CORREO (Usando tu utilidad EmailUtil)
                    val enviado = EmailUtil.enviarCorreoRecuperacion(email, token)

                    runOnUiThread {
                        if (enviado) {
                            Toast.makeText(this, "Código enviado a tu correo", Toast.LENGTH_LONG).show()

                            // --- FLUJO SIGUIENTE ---
                            // Lo ideal aquí es abrir una pantalla para validar el token.
                            // Por ahora cerramos esta actividad para volver al Login.

                            // val intent = Intent(this, ResetPasswordActivity::class.java)
                            // intent.putExtra("EMAIL", email)
                            // startActivity(intent)

                            finish()
                        } else {
                            Toast.makeText(this, "Error de red al enviar correo.", Toast.LENGTH_LONG).show()
                            restaurarBoton()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Error al guardar el token en la base de datos", Toast.LENGTH_SHORT).show()
                        restaurarBoton()
                    }
                }
            } else {
                runOnUiThread {
                    etForgotEmail.error = "Este correo no está registrado"
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    restaurarBoton()
                }
            }
        }.start()
    }

    // Función auxiliar para volver a habilitar el botón si falla algo
    private fun restaurarBoton() {
        btnSendForgot.isEnabled = true
        btnSendForgot.text = "Enviar instrucciones"
    }
}