package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.UsuarioDao
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var etRegName: TextInputEditText
    private lateinit var etRegLastName: TextInputEditText
    private lateinit var etRegEmail: TextInputEditText
    private lateinit var etRegUsername: TextInputEditText
    private lateinit var etRegPhone: TextInputEditText
    private lateinit var etRegBirthDate: TextInputEditText
    private lateinit var etRegPass: TextInputEditText
    private lateinit var etRegPassConfirm: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var btnBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Vincular vistas
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

        etRegBirthDate.setOnClickListener { mostrarCalendario() }
        btnRegister.setOnClickListener { registrar() }
        btnBackToLogin.setOnClickListener { finish() }
    }

    private fun mostrarCalendario() {
        val calendario = Calendar.getInstance()
        val anioActual = calendario.get(Calendar.YEAR)
        val mesActual = calendario.get(Calendar.MONTH)
        val diaActual = calendario.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            // --- VALIDACIÓN DE EDAD (> 18) ---
            var edad = anioActual - year

            // Si aún no ha llegado su cumpleaños este año, restamos 1 a la edad
            if (mesActual < month || (mesActual == month && diaActual < dayOfMonth)) {
                edad--
            }

            if (edad < 18) {
                // ES MENOR DE EDAD
                Toast.makeText(this, "Debes ser mayor de 18 años para registrarte", Toast.LENGTH_LONG).show()
                etRegBirthDate.setText("") // Borramos la fecha inválida
                etRegBirthDate.error = "Edad insuficiente" // Marcamos error visual
            } else {
                // ES MAYOR DE EDAD -> Procesamos la fecha
                val fechaSeleccionada = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                etRegBirthDate.setText(fechaSeleccionada)
                etRegBirthDate.error = null // Limpiar error
            }

        }, anioActual, mesActual, diaActual)

        // Opcional: No permitir seleccionar fechas futuras
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

        // 2. VALIDACIÓN PROFESIONAL (Campo por campo)
        var hayErrores = false

        if (nombre.isEmpty()) {
            etRegName.error = "El nombre es obligatorio"
            hayErrores = true
        } else {
            etRegName.error = null
        }

        if (apellido.isEmpty()) {
            etRegLastName.error = "El apellido es obligatorio"
            hayErrores = true
        } else {
            etRegLastName.error = null
        }

        if (username.isEmpty()) {
            etRegUsername.error = "El usuario es obligatorio"
            hayErrores = true
        } else {
            etRegUsername.error = null
        }

        if (email.isEmpty()) {
            etRegEmail.error = "El email es obligatorio"
            hayErrores = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.error = "Ingresa un correo válido"
            hayErrores = true
        } else {
            etRegEmail.error = null
        }

        if (telefono.isEmpty()) {
            etRegPhone.error = "El teléfono es requerido"
            hayErrores = true
        } else {
            etRegPhone.error = null
        }

        if (fechaNacimiento.isEmpty()) {
            etRegBirthDate.error = "Selecciona una fecha válida (+18)"
            hayErrores = true
        } else {
            etRegBirthDate.error = null
        }

        if (pass.isEmpty()) {
            etRegPass.error = "Ingresa una contraseña"
            hayErrores = true
        } else if (pass.length < 6) {
            etRegPass.error = "Mínimo 6 caracteres"
            hayErrores = true
        } else {
            etRegPass.error = null
        }

        if (confirm.isEmpty()) {
            etRegPassConfirm.error = "Confirma tu contraseña"
            hayErrores = true
        } else if (pass != confirm) {
            etRegPassConfirm.error = "Las contraseñas no coinciden"
            hayErrores = true
        } else {
            etRegPassConfirm.error = null
        }

        // Si encontramos algún error, detenemos el proceso aquí
        if (hayErrores) {
            Toast.makeText(this, "Por favor corrige los errores marcados", Toast.LENGTH_SHORT).show()
            return
        }

        // --- 3. GUARDAR EN BD (Solo si todo es válido) ---
        btnRegister.isEnabled = false
        Toast.makeText(this, "Registrando...", Toast.LENGTH_SHORT).show()

        Thread {
            val exito = UsuarioDao.crearUsuario(
                nombre, apellido, email, username, telefono, fechaNacimiento, pass
            )

            runOnUiThread {
                btnRegister.isEnabled = true
                if (exito) {
                    Toast.makeText(this, "¡Cuenta creada exitosamente!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error: El usuario o email ya existen", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}