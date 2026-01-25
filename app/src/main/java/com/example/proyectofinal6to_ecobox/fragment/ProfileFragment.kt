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
import com.example.proyectofinal6to_ecobox.data.dao.UsuarioDao
import com.example.proyectofinal6to_ecobox.presentacion.ui.LoginActivity
import com.example.proyectofinal6to_ecobox.presentacion.ui.FamilyManagementActivity
import com.example.proyectofinal6to_ecobox.presentacion.ui.ProfileEditActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log

class ProfileFragment : Fragment() {

    private var idUsuario: Long = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        idUsuario = prefs.getLong("user_id", -1L)

        inicializarComponentesUI(view)

        if (idUsuario != -1L) {
            cargarDatosDesdeApi(view)
        }

        // Configurar Botones Principales
        view.findViewById<View>(R.id.btnActionEditProfile).setOnClickListener {
            startActivity(Intent(activity, ProfileEditActivity::class.java))
        }

        view.findViewById<View>(R.id.btnActionLogout).setOnClickListener {
            cerrarSesion(prefs)
        }
        
        // Botón de ajustes (solo logout por ahora como ejemplo)
        view.findViewById<View>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(context, "Ajustes del sistema", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun inicializarComponentesUI(root: View) {
        // Configurar Estadísticas
        configurarStat(root.findViewById(R.id.statPlantas), "🪴", "Plantas")
        configurarStat(root.findViewById(R.id.statSensores), "📡", "Sensores")
        configurarStat(root.findViewById(R.id.statFamilias), "👨‍👩‍👧‍👦", "Familias")
        configurarStat(root.findViewById(R.id.statIA), "🤖", "Consultas IA")

        // Configurar Información de Cuenta
        configurarInfo(root.findViewById(R.id.itemEmail), R.drawable.ic_email, "Email de cuenta")
        configurarInfo(root.findViewById(R.id.itemPhone), R.drawable.ic_phone, "Teléfono móvil")
        configurarInfo(root.findViewById(R.id.itemFamily), R.drawable.ic_family, "Familia EcoBox")

        // Configurar Switches
        configurarSwitch(root.findViewById(R.id.itemNotifications), R.drawable.ic_notifications, "Notificaciones")
        configurarSwitch(root.findViewById(R.id.itemDarkMode), R.drawable.ic_bulb, "Modo Oscuro")
    }

    private fun configurarStat(view: View, icon: String, label: String) {
        view.findViewById<TextView>(R.id.tvStatIcon).text = icon
        view.findViewById<TextView>(R.id.tvStatLabel).text = label
    }

    private fun configurarInfo(view: View, iconRes: Int, label: String) {
        view.findViewById<ImageView>(R.id.ivInfoIcon).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvInfoLabel).text = label
    }

    private fun configurarSwitch(view: View, iconRes: Int, label: String) {
        view.findViewById<ImageView>(R.id.ivSwitchIcon).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvSwitchLabel).text = label
    }

    private fun cargarDatosDesdeApi(root: View) {
        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Cargar datos del perfil
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance.getUserProfile("Token $token")
                if (response.isSuccessful && response.body() != null) {
                    val d = response.body()!!
                    root.findViewById<TextView>(R.id.tvProfileNameDisplay).text = "${d.nombre ?: ""} ${d.apellido ?: ""}".trim()
                    root.findViewById<TextView>(R.id.tvAvatarInitials).text = d.nombre?.take(1)?.uppercase() ?: "U"
                    
                    // Información de cuenta
                    root.findViewById<View>(R.id.itemEmail).findViewById<TextView>(R.id.tvInfoValue).text = d.email
                    root.findViewById<View>(R.id.itemPhone).findViewById<TextView>(R.id.tvInfoValue).text = 
                        d.telefono?.takeIf { it.isNotEmpty() } ?: "No registrado"
                    
                    // Cargar estadísticas REALES desde familias
                    cargarEstadisticasReales(root, token)
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error red perfil", e)
            }
        }
    }

    private fun cargarEstadisticasReales(root: View, token: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance.getFamilies("Token $token")
                
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val familias = response.body()!!
                    
                    // Calcular estadísticas agregadas
                    val totalFamilias = familias.size
                    val totalPlantas = familias.sumOf { it.cantidad_plantas }
                    val totalMiembros = familias.sumOf { it.cantidad_miembros }
                    
                    // Actualizar UI con estadísticas reales
                    root.findViewById<View>(R.id.statPlantas).findViewById<TextView>(R.id.tvStatValue).text = totalPlantas.toString()
                    root.findViewById<View>(R.id.statSensores).findViewById<TextView>(R.id.tvStatValue).text = "0" // No hay endpoint para sensores
                    root.findViewById<View>(R.id.statFamilias).findViewById<TextView>(R.id.tvStatValue).text = totalFamilias.toString()
                    root.findViewById<View>(R.id.statIA).findViewById<TextView>(R.id.tvStatValue).text = "0" // No hay endpoint para consultas IA
                    
                    // Actualizar campo de familia con la primera familia (principal)
                    val familiaPrincipal = familias.firstOrNull { it.es_admin } ?: familias.firstOrNull()
                    root.findViewById<View>(R.id.itemFamily).findViewById<TextView>(R.id.tvInfoValue).text = 
                        familiaPrincipal?.nombre ?: "Sin familia"
                    
                    Log.d("ProfileFragment", "✅ Estadísticas: $totalFamilias familias, $totalPlantas plantas, $totalMiembros miembros")
                } else {
                    // Si no hay familias, mostrar 0
                    root.findViewById<View>(R.id.statPlantas).findViewById<TextView>(R.id.tvStatValue).text = "0"
                    root.findViewById<View>(R.id.statSensores).findViewById<TextView>(R.id.tvStatValue).text = "0"
                    root.findViewById<View>(R.id.statFamilias).findViewById<TextView>(R.id.tvStatValue).text = "0"
                    root.findViewById<View>(R.id.statIA).findViewById<TextView>(R.id.tvStatValue).text = "0"
                    root.findViewById<View>(R.id.itemFamily).findViewById<TextView>(R.id.tvInfoValue).text = "Sin familia"
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error cargando estadísticas", e)
            }
        }
    }

    private fun cerrarSesion(prefs: android.content.SharedPreferences) {
        prefs.edit().clear().apply()
        startActivity(Intent(activity, LoginActivity::class.java))
        activity?.finish()
    }
}