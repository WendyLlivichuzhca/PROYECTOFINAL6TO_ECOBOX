package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var toolbar: Toolbar
    private lateinit var tvInitials: TextView

    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        val prefs = getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

        if (authToken == null) {
            finish()
            return
        }

        initViews()
        loadProfileData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        etName = findViewById(R.id.etProfileName)
        etLastName = findViewById(R.id.etProfileLastName)
        etEmail = findViewById(R.id.etProfileEmail)
        btnSave = findViewById(R.id.btnSaveProfile)
        tvInitials = findViewById(R.id.tvProfileInitials)

        btnSave.setOnClickListener { saveProfile() }
        
        findViewById<MaterialButton>(R.id.btnChangePassword).setOnClickListener {
            val dialog = ChangePasswordDialog.newInstance()
            dialog.setOnPasswordChangedListener {
                // Opcional: cerrar la actividad después de cambiar contraseña
                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
            }
            dialog.show(supportFragmentManager, "ChangePasswordDialog")
        }
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile("Token $authToken")
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    etName.setText(user.nombre)
                    etLastName.setText(user.apellido)
                    etEmail.setText(user.email)
                    tvInitials.text = user.nombre?.take(1)?.uppercase() ?: "U"
                }
            } catch (e: Exception) {
                Log.e("ProfileEdit", "Error loading profile", e)
            }
        }
    }

    private fun saveProfile() {
        val name = etName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Requerido"
            return
        }

        btnSave.isEnabled = false
        Toast.makeText(this, "Guardando...", Toast.LENGTH_SHORT).show()

        val request = mapOf("nombre" to name, "apellido" to lastName)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.updateUserProfile("Token $authToken", request)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileEditActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ProfileEditActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileEdit", "Error saving profile", e)
            } finally {
                btnSave.isEnabled = true
            }
        }
    }
}
