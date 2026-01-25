package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etForgotEmail: EditText
    private lateinit var btnSendForgot: Button
    private lateinit var btnBackToLogin: TextView
    private lateinit var btnBackArrow: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        etForgotEmail = findViewById(R.id.etForgotEmail)
        btnSendForgot = findViewById(R.id.btnSendForgot)
        btnBackToLogin = findViewById(R.id.btnBackToLoginForgot)
        btnBackArrow = findViewById(R.id.btnBack)

        btnSendForgot.setOnClickListener { procesarRecuperacionCloud() }
        btnBackToLogin.setOnClickListener { finish() }
        btnBackArrow.setOnClickListener { finish() }
    }

    private fun procesarRecuperacionCloud() {
        val email = etForgotEmail.text.toString().trim()

        if (email.isEmpty()) {
            etForgotEmail.error = "Escribe tu correo"
            return
        }

        btnSendForgot.isEnabled = false
        btnSendForgot.text = "Enviando..."
        Toast.makeText(this, "Procesando solicitud...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val requestData = mapOf("email" to email)
                val response = RetrofitClient.instance.requestResetPassword(requestData)

                if (response.isSuccessful && response.body() != null) {
                    val msg = response.body()!!.message
                    Toast.makeText(this@ForgotPasswordActivity, msg, Toast.LENGTH_LONG).show()
                    finish() // Vuelve al login
                } else {
                    Log.e("ForgotPass", "Error API: ${response.code()}")
                    etForgotEmail.error = "No se pudo procesar. Verifica tu correo."
                    restaurarBoton()
                }
            } catch (e: Exception) {
                Log.e("ForgotPass", "Error de red: ${e.message}")
                Toast.makeText(this@ForgotPasswordActivity, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
                restaurarBoton()
            }
        }
    }

    private fun restaurarBoton() {
        btnSendForgot.isEnabled = true
        btnSendForgot.text = "Enviar instrucciones"
    }
}
