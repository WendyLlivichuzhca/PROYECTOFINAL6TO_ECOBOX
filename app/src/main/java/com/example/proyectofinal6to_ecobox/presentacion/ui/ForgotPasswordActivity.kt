package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.UsuarioDao
import com.example.proyectofinal6to_ecobox.utils.EmailUtil
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etForgotEmail: TextInputEditText
    private lateinit var btnSendForgot: Button
    private lateinit var btnBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        etForgotEmail = findViewById(R.id.etForgotEmail)
        btnSendForgot = findViewById(R.id.btnSendForgot)
        btnBackToLogin = findViewById(R.id.btnBackToLoginForgot)

        btnSendForgot.setOnClickListener { procesarRecuperacion() }
        btnBackToLogin.setOnClickListener { finish() }
    }

    private fun procesarRecuperacion() {
        val email = etForgotEmail.text.toString().trim()

        if (email.isEmpty()) {
            etForgotEmail.error = "Escribe tu correo"
            return
        }

        btnSendForgot.isEnabled = false
        btnSendForgot.text = "Enviando..."
        Toast.makeText(this, "Verificando cuenta...", Toast.LENGTH_SHORT).show()

        Thread {
            if (UsuarioDao.existeUsuarioPorEmail(email)) {

                // 1. GENERAR TOKEN (Código de 6 dígitos)
                val token = String.format("%06d", Random().nextInt(999999))

                // 2. CALCULAR EXPIRACIÓN (Ahora + 1 hora)
                val calendario = Calendar.getInstance()
                calendario.add(Calendar.HOUR, 1) // Sumar 1 hora

                // Formato compatible con MySQL DateTime: "yyyy-MM-dd HH:mm:ss"
                val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val fechaExpiracion = formatoFecha.format(calendario.time)

                // 3. GUARDAR EN BD (Actualizamos token y fecha)
                val guardado = UsuarioDao.guardarTokenRecuperacion(email, token, fechaExpiracion)

                if (guardado) {
                    // 4. ENVIAR CORREO
                    val enviado = EmailUtil.enviarCorreoRecuperacion(email, token)

                    runOnUiThread {
                        if (enviado) {
                            Toast.makeText(this, "Código enviado a tu correo", Toast.LENGTH_LONG).show()

                            // AQUI DEBERÍAS IR A LA PANTALLA DONDE INGRESAN EL TOKEN
                            // Por ahora, volvemos al login o abrimos una actividad "ResetPasswordActivity"
                            // val intent = Intent(this, ResetPasswordActivity::class.java)
                            // intent.putExtra("EMAIL", email)
                            // startActivity(intent)

                            finish()
                        } else {
                            Toast.makeText(this, "Error al enviar correo. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                            btnSendForgot.isEnabled = true
                            btnSendForgot.text = "Enviar instrucciones"
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Error al guardar datos en el servidor", Toast.LENGTH_SHORT).show()
                        btnSendForgot.isEnabled = true
                        btnSendForgot.text = "Enviar instrucciones"
                    }
                }
            } else {
                runOnUiThread {
                    etForgotEmail.error = "Este correo no existe"
                    Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    btnSendForgot.isEnabled = true
                    btnSendForgot.text = "Enviar instrucciones"
                }
            }
        }.start()
    }
}