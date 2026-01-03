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

        if (idUsuario != -1L) {
            cargarDatosDesdeBD(view)
        }

        // Configurar Botones
        view.findViewById<Button>(R.id.btnActionEditProfile).setOnClickListener {
            mostrarDialogoEdicion(view)
        }

        view.findViewById<Button>(R.id.btnActionLogout).setOnClickListener {
            cerrarSesion(prefs)
        }

        return view
    }

    private fun cargarDatosDesdeBD(root: View) {
        thread {
            val d = UsuarioDao.obtenerPerfilCompleto(idUsuario)
            activity?.runOnUiThread {
                if (d.isNotEmpty()) {
                    root.findViewById<TextView>(R.id.tvProfileNameDisplay).text = "${d["nombre"]} ${d["apellido"]}"
                    root.findViewById<TextView>(R.id.tvAvatarInitials).text = d["nombre"]?.take(1)?.uppercase()
                    root.findViewById<TextView>(R.id.tvUserRole).text = "Rol: ${d["rol"]}"
                    root.findViewById<TextView>(R.id.tvDisplayEmail).text = d["email"]
                    root.findViewById<TextView>(R.id.tvTelefono).text = d["telefono"]
                    root.findViewById<TextView>(R.id.tvFamilyName).text = d["familia"] ?: "Sin asignar"

                    // Estadísticas
                    root.findViewById<TextView>(R.id.tvStatPlantas).text = d["plantas"]
                    root.findViewById<TextView>(R.id.tvStatSensores).text = d["sensores"]
                    root.findViewById<TextView>(R.id.tvStatFamilias).text = d["familias"]
                    root.findViewById<TextView>(R.id.tvStatIA).text = d["ia"]
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