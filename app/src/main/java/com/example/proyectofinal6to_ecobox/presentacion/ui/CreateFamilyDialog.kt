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

class CreateFamilyDialog : DialogFragment() {

    private var authToken: String? = null
    private var onFamilyCreated: ((Long, String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_create_family, null)

        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

        val etFamilyName = view.findViewById<TextInputEditText>(R.id.etFamilyName)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Crear Nueva Familia")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val familyName = etFamilyName.text.toString().trim()
                
                if (familyName.isEmpty()) {
                    Toast.makeText(requireContext(), "Ingrese el nombre de la familia", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                createFamily(familyName)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun createFamily(familyName: String) {
        if (authToken == null) {
            Toast.makeText(requireContext(), "Error de autenticación", Toast.LENGTH_SHORT).show()
            return
        }

        val request = mapOf("nombre" to familyName)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.createFamily("Token $authToken", request)
                
                if (response.isSuccessful && response.body() != null) {
                    val family = response.body()!!
                    Toast.makeText(requireContext(), "Familia '${family.nombre}' creada", Toast.LENGTH_SHORT).show()
                    onFamilyCreated?.invoke(family.id, family.nombre)
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Datos inválidos"
                        else -> "Error al crear familia"
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CreateFamily", "Error creating family", e)
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setOnFamilyCreatedListener(listener: (Long, String) -> Unit) {
        onFamilyCreated = listener
    }

    companion object {
        fun newInstance(): CreateFamilyDialog {
            return CreateFamilyDialog()
        }
    }
}
