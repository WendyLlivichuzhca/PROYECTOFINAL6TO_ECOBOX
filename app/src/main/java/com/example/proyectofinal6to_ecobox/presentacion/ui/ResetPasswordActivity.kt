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
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPass: TextInputEditText
    private lateinit var etConfirmPass: TextInputEditText
    private lateinit var btnChangePass: Button

    private lateinit var tilEmailOculto: TextInputLayout
    private lateinit var etEmailManual: TextInputEditText

    private var emailRecibido: String = ""
    private var tokenRecibido: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        etNewPass = findViewById(R.id.etNewPass)
        etConfirmPass = findViewById(R.id.etConfirmPass)
        btnChangePass = findViewById(R.id.btnChangePass)
        tilEmailOculto = findViewById(R.id.tilEmailOculto)
        etEmailManual = findViewById(R.id.etEmailManual)

        val data: Uri? = intent.data
        if (data != null) {
            val urlStr = data.toString()
            if (urlStr.contains("www.ecobox-app.com/recuperar") || urlStr.startsWith("ecobox://")) {
                tokenRecibido = data.getQueryParameter("token") ?: ""
                emailRecibido = data.getQueryParameter("email") ?: ""
            }
        }

        if (emailRecibido.isEmpty()) {
            tilEmailOculto.visibility = View.VISIBLE
            Toast.makeText(this, "Modo prueba: Ingresa el email manualmente", Toast.LENGTH_SHORT).show()
        } else {
            tilEmailOculto.visibility = View.GONE
        }

        btnChangePass.setOnClickListener {
            cambiarContrasenaCloud()
        }
    }

    private fun cambiarContrasenaCloud() {
        val pass = etNewPass.text.toString().trim()
        val confirm = etConfirmPass.text.toString().trim()

        if (emailRecibido.isEmpty()) {
            emailRecibido = etEmailManual.text.toString().trim()
        }

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

        btnChangePass.isEnabled = false
        btnChangePass.text = "Guardando..."
        Toast.makeText(this, "Actualizando contraseña...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Si el token es vacío, intentamos usar un valor de prueba (esto depende de cómo el backend maneje el link)
                val finalToken = if (tokenRecibido.isNotEmpty()) tokenRecibido else "TEST_TOKEN"
                
                val requestData = mapOf(
                    "email" to emailRecibido,
                    "new_password" to pass
                )
                
                val response = RetrofitClient.instance.resetPassword(finalToken, requestData)

                btnChangePass.isEnabled = true
                btnChangePass.text = "Cambiar Contraseña"

                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "¡Éxito! Contraseña actualizada.", Toast.LENGTH_LONG).show()

                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("ResetPass", "Error API: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@ResetPasswordActivity, "Error: Token inválido o expirado.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ResetPass", "Error de red: ${e.message}")
                btnChangePass.isEnabled = true
                btnChangePass.text = "Cambiar Contraseña"
                Toast.makeText(this@ResetPasswordActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
