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
import kotlin.concurrent.thread

class ProfileFragment : Fragment() {

    private var idUsuario: Long = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val prefs = requireActivity().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        idUsuario = prefs.getLong("user_id", -1L)

        inicializarComponentesUI(view)

        if (idUsuario != -1L) {
            cargarDatosDesdeBD(view)
        }

        // Configurar Botones Principales
        view.findViewById<View>(R.id.btnActionEditProfile).setOnClickListener {
            mostrarDialogoEdicion(view)
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

    private fun cargarDatosDesdeBD(root: View) {
        thread {
            val d = UsuarioDao.obtenerPerfilCompleto(idUsuario)
            activity?.runOnUiThread {
                if (d.isNotEmpty()) {
                    root.findViewById<TextView>(R.id.tvProfileNameDisplay).text = "${d["nombre"]} ${d["apellido"]}"
                    root.findViewById<TextView>(R.id.tvAvatarInitials).text = d["nombre"]?.take(1)?.uppercase()
                    root.findViewById<TextView>(R.id.tvUserRole).text = d["rol"] ?: "Usuario"
                    
                    // Acceder a views dentro de includes
                    root.findViewById<View>(R.id.itemEmail).findViewById<TextView>(R.id.tvInfoValue).text = d["email"]
                    root.findViewById<View>(R.id.itemPhone).findViewById<TextView>(R.id.tvInfoValue).text = d["telefono"] ?: "No registrado"
                    
                    val familyInfo = root.findViewById<View>(R.id.itemFamily)
                    familyInfo.findViewById<TextView>(R.id.tvInfoValue).text = d["familia"] ?: "Sin asignar"
                    if (d["familia"] != null) {
                        val subValue = familyInfo.findViewById<TextView>(R.id.tvInfoSubValue)
                        subValue.text = "Rol: Miembro"
                        subValue.visibility = View.VISIBLE
                    }

                    // Estadísticas dentro de includes
                    root.findViewById<View>(R.id.statPlantas).findViewById<TextView>(R.id.tvStatValue).text = d["plantas"] ?: "0"
                    root.findViewById<View>(R.id.statSensores).findViewById<TextView>(R.id.tvStatValue).text = d["sensores"] ?: "0"
                    root.findViewById<View>(R.id.statFamilias).findViewById<TextView>(R.id.tvStatValue).text = d["familias"] ?: "0"
                    root.findViewById<View>(R.id.statIA).findViewById<TextView>(R.id.tvStatValue).text = d["ia"] ?: "0"
                }
            }
        }
    }

    private fun mostrarDialogoEdicion(root: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etNom = dialogView.findViewById<EditText>(R.id.etEditName)
        val etApe = dialogView.findViewById<EditText>(R.id.etEditLastName)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Perfil")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                thread {
                    val exito = UsuarioDao.actualizarUsuario(idUsuario, etNom.text.toString(), etApe.text.toString())
                    activity?.runOnUiThread {
                        if (exito) {
                            Toast.makeText(context, "Actualizado", Toast.LENGTH_SHORT).show()
                            cargarDatosDesdeBD(root)
                        }
                    }
                }
            }.show()
    }

    private fun cerrarSesion(prefs: android.content.SharedPreferences) {
        prefs.edit().clear().apply()
        startActivity(Intent(activity, LoginActivity::class.java))
        activity?.finish()
    }
}