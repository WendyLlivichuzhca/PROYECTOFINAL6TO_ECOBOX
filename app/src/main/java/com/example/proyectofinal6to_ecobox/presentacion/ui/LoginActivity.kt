package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.proyectofinal6to_ecobox.R
import com.google.android.material.textfield.TextInputEditText
import com.example.proyectofinal6to_ecobox.data.dao.UsuarioDao

class LoginActivity : AppCompatActivity() {

    private lateinit var etLoginEmail: TextInputEditText
    private lateinit var etLoginPass: TextInputEditText
    private lateinit var btnGoToRegister: TextView
    private lateinit var btnGoToForgot: TextView
    private lateinit var btnLogin: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Vincular vistas
        etLoginEmail = findViewById(R.id.etLoginEmail)
        etLoginPass = findViewById(R.id.etLoginPass)
        btnGoToRegister = findViewById(R.id.btnGoToRegister)
        btnGoToForgot = findViewById(R.id.btnGoToForgot)
        btnLogin = findViewById(R.id.btnLogin)

        // 2. Configurar validaciones visuales (Iconos check/error)
        configurarValidaciones()

        // 3. Botón Login
        btnLogin.setOnClickListener {
            login()
        }

        // 4. Navegación a Registro
        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 5. Navegación a Recuperar Contraseña
        btnGoToForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun login() {
        val email = etLoginEmail.text.toString().trim()
        val pass = etLoginPass.text.toString().trim()

        // Validaciones básicas
        if (email.isEmpty()) {
            etLoginEmail.error = "Escribe tu correo"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.error = "Formato de correo inválido"
            return
        }
        if (pass.isEmpty()) {
            etLoginPass.error = "Escribe tu contraseña"
            return
        }

        Toast.makeText(this, "Verificando usuario...", Toast.LENGTH_SHORT).show()
        btnLogin.isEnabled = false // Evitar doble clic

        // 6. Hilo secundario para conexión JDBC directa
        Thread {
            // Llamamos a DAO que devuelve un ENTERO (1, 2, 3 o 4)
            val codigoResultado = UsuarioDao.validarUsuario(email, pass)

            runOnUiThread {
                btnLogin.isEnabled = true

                when (codigoResultado) {
                    UsuarioDao.LOGIN_EXITOSO -> {
                        // --- ÉXITO ---
                        mostrarFeedbackVisual(true)
                        Toast.makeText(this, "¡Bienvenido de vuelta!", Toast.LENGTH_SHORT).show()

                        // A. Obtener el ID del usuario para guardarlo en sesión
                        // (Hacemos esto en un hilo aparte rápido para no bloquear)
                        Thread {
                            val userId = UsuarioDao.obtenerIdPorEmail(email)

                            // B. Guardar Sesión en SharedPreferences
                            val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
                            val editor = prefs.edit()
                            editor.putLong("user_id", userId)
                            editor.putString("user_email", email)
                            editor.apply()

                            // C. Ir al Dashboard
                            runOnUiThread {
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish() // Cierra el login para no volver atrás
                            }
                        }.start()
                    }

                    UsuarioDao.EMAIL_NO_ENCONTRADO -> {
                        mostrarErrorCampo(etLoginEmail, "Este correo no está registrado")
                        Toast.makeText(this, "Correo incorrecto", Toast.LENGTH_LONG).show()
                    }

                    UsuarioDao.PASSWORD_INCORRECTO -> {
                        // El email estaba bien (verde), pero la pass mal (rojo)
                        val checkIcon = ContextCompat.getDrawable(this, R.drawable.ic_check_success)
                        val errorIcon = ContextCompat.getDrawable(this, R.drawable.ic_error_red)

                        etLoginEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            null,
                            null,
                            checkIcon,
                            null
                        )
                        etLoginPass.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            null,
                            null,
                            errorIcon,
                            null
                        )

                        etLoginPass.setText("")
                        etLoginPass.error = "Contraseña incorrecta"
                        Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
                    }

                    else -> {
                        Toast.makeText(
                            this,
                            "Error de conexión con la base de datos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.start()
    }

    private fun mostrarFeedbackVisual(esExito: Boolean) {
        val icono = if (esExito) R.drawable.ic_check_success else R.drawable.ic_error_red
        val drawable = ContextCompat.getDrawable(this, icono)

        etLoginEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
        etLoginPass.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)

        etLoginEmail.error = null
        etLoginPass.error = null
    }

    private fun mostrarErrorCampo(campo: TextInputEditText, mensaje: String) {
        val errorIcon = ContextCompat.getDrawable(this, R.drawable.ic_error_red)
        campo.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, errorIcon, null)
        campo.error = mensaje
    }

    private fun configurarValidaciones() {
        // Validación Email en tiempo real
        etLoginEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val emailInput = s.toString().trim()
                if (emailInput.isEmpty()) {
                    etLoginEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        null,
                        null
                    )
                } else if (Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    val checkIcon =
                        ContextCompat.getDrawable(this@LoginActivity, R.drawable.ic_check_success)
                    etLoginEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        checkIcon,
                        null
                    )
                    etLoginEmail.error = null
                } else {
                    val errorIcon =
                        ContextCompat.getDrawable(this@LoginActivity, R.drawable.ic_error_red)
                    etLoginEmail.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        errorIcon,
                        null
                    )
                }
            }
        })

        // Validación Password en tiempo real
        etLoginPass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val passInput = s.toString().trim()
                if (passInput.isEmpty()) {
                    etLoginPass.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        null,
                        null
                    )
                } else if (passInput.length >= 6) {
                    val checkIcon =
                        ContextCompat.getDrawable(this@LoginActivity, R.drawable.ic_check_success)
                    etLoginPass.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        checkIcon,
                        null
                    )
                    etLoginPass.error = null
                } else {
                    // Solo mostramos error visual (X), no mensaje de error mientras escribe para no molestar
                    val errorIcon =
                        ContextCompat.getDrawable(this@LoginActivity, R.drawable.ic_error_red)
                    etLoginPass.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        errorIcon,
                        null
                    )
                }
            }
        })
    }
}
