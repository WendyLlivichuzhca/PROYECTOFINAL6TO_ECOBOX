package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ChangePasswordDialog : DialogFragment() {

    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnChange: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private var authToken: String? = null
    private var onPasswordChanged: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_change_password, null)

        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        btnChange = view.findViewById(R.id.btnChangePassword)
        btnCancel = view.findViewById(R.id.btnCancelPassword)

        btnChange.setOnClickListener { changePassword() }
        btnCancel.setOnClickListener { dismiss() }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validaciones
        if (currentPassword.isEmpty()) {
            etCurrentPassword.error = "Ingrese su contraseña actual"
            return
        }

        if (newPassword.isEmpty()) {
            etNewPassword.error = "Ingrese la nueva contraseña"
            return
        }

        if (newPassword.length < 6) {
            etNewPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        if (newPassword != confirmPassword) {
            etConfirmPassword.error = "Las contraseñas no coinciden"
            return
        }

        if (authToken == null) {
            Toast.makeText(requireContext(), "Error de autenticación", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        btnChange.isEnabled = false
        btnChange.text = "Cambiando..."

        val request = mapOf(
            "old_password" to currentPassword,
            "new_password" to newPassword,
            "confirm_password" to confirmPassword
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.changePassword("Token $authToken", request)
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Contraseña cambiada exitosamente", Toast.LENGTH_SHORT).show()
                    onPasswordChanged?.invoke()
                    dismiss()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Contraseña actual incorrecta"
                        else -> "Error al cambiar contraseña"
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ChangePassword", "Error changing password", e)
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                btnChange.isEnabled = true
                btnChange.text = "Cambiar Contraseña"
            }
        }
    }

    fun setOnPasswordChangedListener(listener: () -> Unit) {
        onPasswordChanged = listener
    }

    companion object {
        fun newInstance(): ChangePasswordDialog {
            return ChangePasswordDialog()
        }
    }
}
