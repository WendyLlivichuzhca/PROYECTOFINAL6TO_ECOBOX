package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    // Usamos EditText porque así está en el XML personalizado
    private lateinit var etRegName: EditText
    private lateinit var etRegLastName: EditText
    private lateinit var etRegEmail: EditText
    private lateinit var etRegUsername: EditText
    private lateinit var etRegPhone: EditText
    private lateinit var etRegBirthDate: EditText
    private lateinit var etRegPass: EditText
    private lateinit var etRegPassConfirm: EditText

    private lateinit var btnRegister: Button
    private lateinit var btnBackToLogin: TextView
    private lateinit var btnBackReg: ImageButton // La flecha de arriba
    private lateinit var btnToggleRegPass: ImageView // El ojito

    private var isPasswordVisible = false // Variable para controlar el ojito

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 1. Vincular vistas (IDs deben coincidir con tu XML)
        etRegName = findViewById(R.id.etRegName)
        etRegLastName = findViewById(R.id.etRegLastName)
        etRegEmail = findViewById(R.id.etRegEmail)
        etRegUsername = findViewById(R.id.etRegUsername)
        etRegPhone = findViewById(R.id.etRegPhone)
        etRegBirthDate = findViewById(R.id.etRegBirthDate)
        etRegPass = findViewById(R.id.etRegPass)
        etRegPassConfirm = findViewById(R.id.etRegPassConfirm)

        btnRegister = findViewById(R.id.btnRegister)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        btnBackReg = findViewById(R.id.btnBack)
        btnToggleRegPass = findViewById(R.id.btnToggleRegPass)

        // 2. Configurar Listeners
        etRegBirthDate.setOnClickListener { mostrarCalendario() }
        btnRegister.setOnClickListener { registrar() }

        // Botones de navegación
        btnBackToLogin.setOnClickListener { finish() } // Vuelve al Login
        btnBackReg.setOnClickListener { finish() }     // La flecha también vuelve

        // Lógica del Ojo (Ver/Ocultar contraseña)
        btnToggleRegPass.setOnClickListener { togglePasswordVisibility() }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Ocultar Contraseña
            etRegPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnToggleRegPass.setImageResource(R.drawable.ic_eye_visibility) // Ícono de ojo normal
        } else {
            // Mostrar Contraseña
            etRegPass.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            // Aquí podrías cambiar el icono a uno de "ojo tachado" si lo tienes, o dejar el mismo
            btnToggleRegPass.setColorFilter(getColor(R.color.eco_primary)) // Pinta el ojo de verde al activar
        }
        isPasswordVisible = !isPasswordVisible
        // Mueve el cursor al final del texto para que no moleste al escribir
        etRegPass.setSelection(etRegPass.text.length)
    }

    private fun mostrarCalendario() {
        val calendario = Calendar.getInstance()
        val anioActual = calendario.get(Calendar.YEAR)
        val mesActual = calendario.get(Calendar.MONTH)
        val diaActual = calendario.get(Calendar.DAY_OF_MONTH)

        // Estilo de calendario por defecto (o puedes definir uno en styles)
        val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->

            // --- VALIDACIÓN DE EDAD (> 18) ---
            var edad = anioActual - year

            // Si aún no ha llegado su cumpleaños este año, restamos 1
            if (mesActual < month || (mesActual == month && diaActual < dayOfMonth)) {
                edad--
            }

            if (edad < 18) {
                Toast.makeText(this, "Debes ser mayor de 18 años para registrarte", Toast.LENGTH_LONG).show()
                etRegBirthDate.setText("")
                etRegBirthDate.error = "Edad insuficiente"
            } else {
                // Formato YYYY-MM-DD para guardar limpio en BD
                val fechaSeleccionada = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                etRegBirthDate.setText(fechaSeleccionada)
                etRegBirthDate.error = null
            }

        }, anioActual, mesActual, diaActual)

        // No permitir fechas futuras (nadie nace mañana)
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun registrar() {
        // 1. Obtener valores y limpiar espacios
        val nombre = etRegName.text.toString().trim()
        val apellido = etRegLastName.text.toString().trim()
        val email = etRegEmail.text.toString().trim()
        val username = etRegUsername.text.toString().trim()
        val telefono = etRegPhone.text.toString().trim()
        val fechaNacimiento = etRegBirthDate.text.toString().trim()
        val pass = etRegPass.text.toString().trim()
        val confirm = etRegPassConfirm.text.toString().trim()

        // 2. VALIDACIÓN (Campo por campo)
        var hayErrores = false

        if (nombre.isEmpty()) { etRegName.error = "Requerido"; hayErrores = true }
        if (apellido.isEmpty()) { etRegLastName.error = "Requerido"; hayErrores = true }
        if (username.isEmpty()) { etRegUsername.error = "Requerido"; hayErrores = true }

        if (email.isEmpty()) {
            etRegEmail.error = "Requerido"
            hayErrores = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.error = "Correo inválido"
            hayErrores = true
        }

        if (telefono.isEmpty()) { etRegPhone.error = "Requerido"; hayErrores = true }
        if (fechaNacimiento.isEmpty()) { etRegBirthDate.error = "Requerido"; hayErrores = true }

        if (pass.isEmpty()) {
            etRegPass.error = "Requerido"
            hayErrores = true
        } else if (pass.length < 6) {
            etRegPass.error = "Mínimo 6 caracteres"
            hayErrores = true
        }

        if (confirm.isEmpty()) {
            etRegPassConfirm.error = "Confirma tu contraseña"
            hayErrores = true
        } else if (pass != confirm) {
            etRegPassConfirm.error = "No coinciden"
            hayErrores = true
        }

        if (hayErrores) {
            Toast.makeText(this, "Verifica los errores marcados", Toast.LENGTH_SHORT).show()
            return
        }

        // --- 3. GUARDAR A TRAVÉS DE LA API (REST) ---
        btnRegister.isEnabled = false // Evitar doble clic
        Toast.makeText(this, "Procesando registro...", Toast.LENGTH_SHORT).show()

        val requestData = mapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "email" to email,
            "username" to username,
            "telefono" to telefono,
            "password" to pass
        )

        lifecycleScope.launch {
            try {
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance.register(requestData)
                
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@RegisterActivity, "¡Cuenta creada exitosamente!", Toast.LENGTH_LONG).show()
                    finish() // Vuelve al login
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("RegisterActivity", "Error reg: $errorBody")
                    
                    if (errorBody?.contains("usuario con este email") == true) {
                        etRegEmail.error = "El email ya está registrado"
                    } else if (errorBody?.contains("nombre de usuario") == true) {
                        etRegUsername.error = "El username ya está en uso"
                    } else {
                        Toast.makeText(this@RegisterActivity, "Error al registrar: Verifica los datos", Toast.LENGTH_LONG).show()
                    }
                    btnRegister.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Error de red", e)
                Toast.makeText(this@RegisterActivity, "No hay conexión con el servidor", Toast.LENGTH_LONG).show()
                btnRegister.isEnabled = true
            }
        }
    }
}