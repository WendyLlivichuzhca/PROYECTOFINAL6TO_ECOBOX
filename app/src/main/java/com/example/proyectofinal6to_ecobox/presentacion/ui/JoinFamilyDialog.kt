package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class JoinFamilyDialog : DialogFragment() {

    private var authToken: String? = null
    private var onFamilyJoined: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_join_family, null)

        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

        val etFamilyCode = view.findViewById<TextInputEditText>(R.id.etFamilyCode)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Unirse a Familia")
            .setView(view)
            .setPositiveButton("Unirse") { _, _ ->
                val code = etFamilyCode.text.toString().trim()
                
                if (code.isEmpty()) {
                    Toast.makeText(requireContext(), "Ingrese el código de invitación", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                joinFamily(code)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun joinFamily(code: String) {
        if (authToken == null) {
            Toast.makeText(requireContext(), "Error de autenticación", Toast.LENGTH_SHORT).show()
            return
        }

        val request = mapOf("codigo_invitacion" to code)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.joinFamily("Token $authToken", request)
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Te has unido a la familia exitosamente", Toast.LENGTH_SHORT).show()
                    onFamilyJoined?.invoke()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Código inválido o ya eres miembro"
                        404 -> "Código no encontrado"
                        else -> "Error al unirse a la familia"
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("JoinFamily", "Error joining family", e)
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setOnFamilyJoinedListener(listener: () -> Unit) {
        onFamilyJoined = listener
    }

    companion object {
        fun newInstance(): JoinFamilyDialog {
            return JoinFamilyDialog()
        }
    }
}
