package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.example.proyectofinal6to_ecobox.data.network.UserProfileResponse
import kotlinx.coroutines.launch
import kotlin.random.Random

class ProfileEditActivity : AppCompatActivity() {

    companion object {
        private val AVATAR_COLORS = listOf(
            "#EF4444", "#F59E0B", "#10B981", "#3B82F6",
            "#6366F1", "#8B5CF6", "#EC4899", "#F97316",
            "#14B8A6", "#06B6D4", "#8B5065", "#6B7280"
        )
        
        private val USER_BIOS = listOf(
            "🌱 Apasionado/a por las plantas",
            "🌿 Guardián/a del jardín digital",
            "💚 Amante de la naturaleza tech",
            "🌻 Cultivando el futuro verde",
            "🍃 Eco-warrior con superpoderes",
            "🌾 Jardinero/a del siglo XXI",
            "🌳 Plantlover y tech enthusiast",
            "🪴 Mi jardín es mi santuario"
        )
    }

    private lateinit var etName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var toolbar: Toolbar
    private lateinit var tvInitials: TextView
    private lateinit var avatarCard: MaterialCardView
    private lateinit var btnChangeAvatarColor: FloatingActionButton
    private lateinit var tvUserBio: TextView
    private lateinit var btnChangeBio: ImageButton
    private lateinit var layoutProfileStats: LinearLayout

    private var authToken: String? = null
    private var userProfileData: UserProfileResponse? = null
    private var currentColorIndex = 0
    private var currentBioIndex = 0

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
        setupAvatarColor()
        setupBio()
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
        avatarCard = tvInitials.parent as MaterialCardView
        btnChangeAvatarColor = findViewById(R.id.btnChangeAvatarColor)
        tvUserBio = findViewById(R.id.tvUserBio)
        btnChangeBio = findViewById(R.id.btnChangeBio)
        layoutProfileStats = findViewById(R.id.layoutProfileStats)

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
                    userProfileData = user
                    etName.setText(user.nombre)
                    etLastName.setText(user.apellido)
                    etEmail.setText(user.email)
                    tvInitials.text = user.nombre?.take(1)?.uppercase() ?: "U"
                    loadProfileStats(user)
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

    private fun loadProfileStats(user: UserProfileResponse) {
        val stats = user.estadisticas ?: return
        
        try {
            val plantas = (stats["plantas_count"] as? Number)?.toInt() ?: 0
            val mediciones = (stats["mediciones_count"] as? Number)?.toInt() ?: 0
            val riegos = (stats["riegos_hoy"] as? Number)?.toInt() ?: 0
            val semanas = (stats["semanas_activo"] as? Number)?.toInt() ?: 0
            
            // Configurar card de Plantas
            findViewById<View>(R.id.statPlantas).apply {
                findViewById<TextView>(R.id.tvStatValue).text = plantas.toString()
                findViewById<TextView>(R.id.tvStatLabel).text = "Plantas"
            }
            
            // Configurar card de Mediciones
            findViewById<View>(R.id.statMediciones).apply {
                findViewById<TextView>(R.id.tvStatValue).text = mediciones.toString()
                findViewById<TextView>(R.id.tvStatLabel).text = "Mediciones"
            }
            
            // Configurar card de Riegos
            findViewById<View>(R.id.statRiegos).apply {
                findViewById<TextView>(R.id.tvStatValue).text = riegos.toString()
                findViewById<TextView>(R.id.tvStatLabel).text = "Riegos Hoy"
            }
            
            // Configurar card de Semanas
            findViewById<View>(R.id.statSemanas).apply {
                findViewById<TextView>(R.id.tvStatValue).text = semanas.toString()
                findViewById<TextView>(R.id.tvStatLabel).text = "Semanas"
            }
        } catch (e: Exception) {
            Log.e("ProfileEdit", "Error loading stats", e)
            // Si hay error, ocultar las estadísticas
            layoutProfileStats.visibility = android.view.View.GONE
        }
    }

    private fun setupAvatarColor() {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        currentColorIndex = prefs.getInt("avatar_color_index", 0)
        applyAvatarColor()
        
        btnChangeAvatarColor.setOnClickListener {
            showColorPicker()
        }
    }

    private fun applyAvatarColor() {
        try {
            val color = Color.parseColor(AVATAR_COLORS[currentColorIndex])
            avatarCard.setCardBackgroundColor(color)
        } catch (e: Exception) {
            Log.e("ProfileEdit", "Error applying avatar color", e)
        }
    }

    private fun showColorPicker() {
        val colorNames = arrayOf(
            "Rojo", "Naranja", "Verde", "Azul", 
            "Índigo", "Púrpura", "Rosa", "Naranja Oscuro",
            "Verde Azulado", "Cian", "Marrón", "Gris"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Elige un color para tu avatar")
            .setItems(colorNames) { _, which ->
                currentColorIndex = which
                applyAvatarColor()
                saveAvatarColor()
            }
            .show()
    }

    private fun saveAvatarColor() {
        getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
            .edit()
            .putInt("avatar_color_index", currentColorIndex)
            .apply()
    }

    private fun setupBio() {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        currentBioIndex = prefs.getInt("user_bio_index", Random.nextInt(USER_BIOS.size))
        applyBio()
        
        btnChangeBio.setOnClickListener {
            changeBio()
        }
    }

    private fun applyBio() {
        tvUserBio.text = USER_BIOS[currentBioIndex]
    }

    private fun changeBio() {
        currentBioIndex = (currentBioIndex + 1) % USER_BIOS.size
        applyBio()
        saveBio()
        
        // Animación de fade
        tvUserBio.alpha = 0f
        tvUserBio.animate().alpha(1f).setDuration(300).start()
    }

    private fun saveBio() {
        getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
            .edit()
            .putInt("user_bio_index", currentBioIndex)
            .apply()
    }
}
