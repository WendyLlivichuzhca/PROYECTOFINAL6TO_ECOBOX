package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.presentacion.ui.LoginActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.graphics.Color
import android.util.Log
import com.google.android.material.textfield.TextInputEditText

class ProfileFragment : Fragment() {

    private var idUsuario: Long = -1
    private var isEditMode = false

    private var originalNombre = ""
    private var originalApellido = ""
    private var originalTelefono = ""

    companion object {
        private val AVATAR_COLORS = listOf(
            "#10B981", "#3B82F6", "#8B5CF6", "#F59E0B", 
            "#EF4444", "#EC4899", "#14B8A6", "#F97316",
            "#06B6D4", "#8B5CF6", "#84CC16", "#F43F5E"
        )
        
        private val USER_BIOS = listOf(
            "🌿 Entusiasta de la jardinería y monitoreo de plantas",
            "💧 Amante del cuidado responsable de plantas",
            "📊 Apasionado por el seguimiento de métricas de plantas",
            "🌱 Comprometido con la jardinería sostenible",
            "✨ Explorando el mundo de la botánica digital",
            "🌸 Jardinería urbana como estilo de vida",
            "💚 Cultivando un futuro más verde",
            "🌳 Conectando con la naturaleza a través de la tecnología"
        )
    }

    private var currentColorIndex = 0
    private var currentBioIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        idUsuario = prefs.getLong("user_id", -1L)

        inicializarEstadisticasConColores(view)

        if (idUsuario != -1L) {
            cargarDatosDesdeApi(view)
        }

        setupActions(view, prefs)
        setupCustomization(view)

        return view
    }

    private fun setupActions(root: View, prefs: android.content.SharedPreferences) {
        // Toggle Edit Mode
        root.findViewById<View>(R.id.btnToggleEditMode).setOnClickListener {
            toggleEditMode(root)
        }

        // Cancel Edit
        root.findViewById<View>(R.id.btnCancelChanges).setOnClickListener {
            revertChanges(root)
            toggleEditMode(root)
        }

        // Save Changes
        root.findViewById<View>(R.id.btnSaveChanges).setOnClickListener {
            guardarCambiosEnServidor(root)
        }

        // Logout
        root.findViewById<View>(R.id.btnNavLogout).setOnClickListener {
            cerrarSesion(prefs)
        }
        
        // Change Password
        root.findViewById<View>(R.id.btnNavChangePassword).setOnClickListener {
            val dialog = com.example.proyectofinal6to_ecobox.presentacion.ui.ChangePasswordDialog.newInstance()
            dialog.setOnPasswordChangedListener {
                Toast.makeText(context, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
            }
            dialog.show(parentFragmentManager, "ChangePasswordDialog")
        }

        // Volver al Dashboard
        root.findViewById<View>(R.id.btnBackToDashboard).setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun toggleEditMode(root: View) {
        isEditMode = !isEditMode
        
        root.findViewById<TextInputEditText>(R.id.etProfileName).isEnabled = isEditMode
        root.findViewById<TextInputEditText>(R.id.etProfileLastName).isEnabled = isEditMode
        root.findViewById<TextInputEditText>(R.id.etProfilePhone).isEnabled = isEditMode
        
                root.findViewById<View>(R.id.layoutActionButtons).visibility = if (isEditMode) View.VISIBLE else View.GONE
        
        val btnToggle = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnToggleEditMode)
        btnToggle.text = if (isEditMode) "CANCELAR EDICIÓN" else "EDITAR"
        btnToggle.setTextColor(if (isEditMode) Color.RED else Color.parseColor("#10B981"))
    }
        

    private fun revertChanges(root: View) {
        root.findViewById<TextInputEditText>(R.id.etProfileName).setText(originalNombre)
        root.findViewById<TextInputEditText>(R.id.etProfileLastName).setText(originalApellido)
        root.findViewById<TextInputEditText>(R.id.etProfilePhone).setText(originalTelefono)
    }

    private fun guardarCambiosEnServidor(root: View) {
        val nuevoNombre = root.findViewById<TextInputEditText>(R.id.etProfileName).text.toString()
        val nuevoApellido = root.findViewById<TextInputEditText>(R.id.etProfileLastName).text.toString()
        val nuevoTelefono = root.findViewById<TextInputEditText>(R.id.etProfilePhone).text.toString()

        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""

        val request = mapOf(
            "nombre" to nuevoNombre,
            "apellido" to nuevoApellido,
            "telefono" to nuevoTelefono
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance.updateUserProfile(
                    "Token $token", request
                )
                if (response.isSuccessful) {
                    originalNombre = nuevoNombre
                    originalApellido = nuevoApellido
                    originalTelefono = nuevoTelefono
                    
                    root.findViewById<TextView>(R.id.tvProfileNameDisplay).text = "$nuevoNombre $nuevoApellido".trim()
                    root.findViewById<TextView>(R.id.tvAvatarInitials).text = nuevoNombre.take(1).ifEmpty { "U" }.uppercase()
                    
                    toggleEditMode(root)
                    Toast.makeText(context, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCustomization(root: View) {
        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        
        currentColorIndex = prefs.getInt("avatar_color_index", 0)
        applyAvatarColor(root)
        
        root.findViewById<View>(R.id.btnMainChangeAvatarColor).setOnClickListener {
            currentColorIndex = (currentColorIndex + 1) % AVATAR_COLORS.size
            applyAvatarColor(root)
            prefs.edit().putInt("avatar_color_index", currentColorIndex).apply()
        }

        currentBioIndex = prefs.getInt("user_bio_index", 0)
        applyBio(root)
        
        root.findViewById<View>(R.id.btnMainChangeBio).setOnClickListener {
            currentBioIndex = (currentBioIndex + 1) % USER_BIOS.size
            applyBio(root)
            prefs.edit().putInt("user_bio_index", currentBioIndex).apply()
            
            val tvBio = root.findViewById<TextView>(R.id.tvMainUserBio)
            tvBio.alpha = 0.5f
            tvBio.animate().alpha(1.0f).setDuration(300).start()
        }
    }

    private fun applyAvatarColor(root: View) {
        try {
            val color = Color.parseColor(AVATAR_COLORS[currentColorIndex])
            root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAvatarFrame)
                .setCardBackgroundColor(color)
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error aplicando color", e)
        }
    }

    private fun applyBio(root: View) {
        root.findViewById<TextView>(R.id.tvMainUserBio).text = USER_BIOS[currentBioIndex]
    }

    private fun inicializarEstadisticasConColores(root: View) {
        configurarFondoStat(root.findViewById(R.id.statPlantas), "#D1FAE5", "Plantas") // Light Green
        configurarFondoStat(root.findViewById(R.id.statMediciones), "#DBEAFE", "Mediciones") // Light Blue
        configurarFondoStat(root.findViewById(R.id.statRiegos), "#FEF3C7", "Riegos Hoy") // Light Yellow
        configurarFondoStat(root.findViewById(R.id.statSemanas), "#F3E8FF", "Semanas") // Light Purple
    }

    private fun configurarFondoStat(view: View?, colorHex: String, label: String) {
        if (view == null) return
        
        // El id del root se sobreescribe en el include, por lo que 'view' ya es el MaterialCardView
        (view as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(Color.parseColor(colorHex))
        view.findViewById<TextView>(R.id.tvStatLabel)?.text = label
    }

    private fun cargarDatosDesdeApi(root: View) {
        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance.getUserProfile("Token $token")
                if (response.isSuccessful && response.body() != null) {
                    val d = response.body()!!
                    
                    originalNombre = d.nombre ?: ""
                    originalApellido = d.apellido ?: ""
                    originalTelefono = d.telefono ?: ""
                    
                    root.findViewById<TextView>(R.id.tvProfileNameDisplay).text = "$originalNombre $originalApellido".trim()
                    root.findViewById<TextView>(R.id.tvUsernameDisplay).text = "@${d.username ?: "usuario"}"
                    root.findViewById<TextView>(R.id.tvAvatarInitials).text = originalNombre.take(1).ifEmpty { "U" }.uppercase()
                    root.findViewById<TextView>(R.id.tvMemberSince).text = "📅 Miembro desde: ${d.fecha_registro_formatted ?: "---"}"
                    
                    root.findViewById<TextInputEditText>(R.id.etProfileName).setText(originalNombre)
                    root.findViewById<TextInputEditText>(R.id.etProfileLastName).setText(originalApellido)
                    root.findViewById<TextInputEditText>(R.id.etProfileEmail).setText(d.email)
                    root.findViewById<TextInputEditText>(R.id.etProfilePhone).setText(originalTelefono)

                    actualizarEstadisticasConDatosPerfil(root, d)
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error red perfil", e)
            }
        }
    }

    private fun actualizarEstadisticasConDatosPerfil(root: View, user: com.example.proyectofinal6to_ecobox.data.network.UserProfileResponse) {
        val stats = user.estadisticas ?: return
        try {
            val plantas = (stats["plantas_count"] as? Number)?.toInt() ?: 0
            val mediciones = (stats["mediciones_count"] as? Number)?.toInt() ?: 0
            val riegos = (stats["riegos_hoy"] as? Number)?.toInt() ?: 0
            val semanas = (stats["semanas_activo"] as? Number)?.toInt() ?: 0
            
            root.findViewById<View>(R.id.statPlantas).findViewById<TextView>(R.id.tvStatValue).text = plantas.toString()
            root.findViewById<View>(R.id.statMediciones).findViewById<TextView>(R.id.tvStatValue).text = mediciones.toString()
            root.findViewById<View>(R.id.statRiegos).findViewById<TextView>(R.id.tvStatValue).text = riegos.toString()
            root.findViewById<View>(R.id.statSemanas).findViewById<TextView>(R.id.tvStatValue).text = semanas.toString()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error parseando stats", e)
        }
    }

    private fun cerrarSesion(prefs: android.content.SharedPreferences) {
        prefs.edit().clear().apply()
        startActivity(Intent(activity, LoginActivity::class.java))
        activity?.finish()
    }
}