package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import kotlinx.coroutines.launch

/**
 * CreateAlertDialog - SPRINT 2
 * 
 * Dialog para crear alertas manuales
 * Usa endpoint POST /api/alerts/create/
 */
class CreateAlertDialog : DialogFragment() {

    private var onAlertCreatedListener: (() -> Unit)? = null

    fun setOnAlertCreatedListener(listener: () -> Unit) {
        onAlertCreatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_create_alert, null)

        val etTitle = view.findViewById<EditText>(R.id.etAlertTitle)
        val etMessage = view.findViewById<EditText>(R.id.etAlertMessage)
        val spinnerType = view.findViewById<Spinner>(R.id.spinnerAlertType)
        val spinnerPlant = view.findViewById<Spinner>(R.id.spinnerPlant)

        // Configurar spinner de tipos
        val types = arrayOf("CRITICA", "ADVERTENCIA", "INFO")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter

        // Cargar plantas para el spinner
        loadPlants(spinnerPlant)

        builder.setView(view)
            .setTitle("Crear Alerta Manual")
            .setPositiveButton("Crear") { _, _ ->
                val title = etTitle.text.toString()
                val message = etMessage.text.toString()
                val type = spinnerType.selectedItem.toString()
                val plantId = getSelectedPlantId(spinnerPlant)

                if (title.isBlank() || message.isBlank()) {
                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                createAlert(title, message, type, plantId)
            }
            .setNegativeButton("Cancelar", null)

        return builder.create()
    }

    private fun loadPlants(spinner: Spinner) {
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMyPlants("Token $token")
                if (response.isSuccessful && response.body() != null) {
                    val plants = response.body()!!
                    val plantNames = mutableListOf("Ninguna")
                    plantNames.addAll(plants.map { it.nombre })

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, plantNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    spinner.tag = plants // Guardar lista de plantas
                }
            } catch (e: Exception) {
                Log.e("CreateAlert", "Error loading plants", e)
            }
        }
    }

    private fun getSelectedPlantId(spinner: Spinner): Long? {
        val selectedPosition = spinner.selectedItemPosition
        if (selectedPosition == 0) return null // "Ninguna"

        val plants = spinner.tag as? List<*>
        if (plants != null && selectedPosition - 1 < plants.size) {
            val plant = plants[selectedPosition - 1] as? com.example.proyectofinal6to_ecobox.data.network.PlantResponse
            return plant?.id
        }
        return null
    }

    private fun createAlert(title: String, message: String, type: String, plantId: Long?) {
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null) ?: return

        lifecycleScope.launch {
            try {
                val request = mutableMapOf<String, Any>(
                    "titulo" to title,
                    "mensaje" to message,
                    "tipo" to type
                )
                if (plantId != null) {
                    request["plant_id"] = plantId
                }

                val response = RetrofitClient.instance.createAlert("Token $token", request)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Alerta creada exitosamente", Toast.LENGTH_SHORT).show()
                    onAlertCreatedListener?.invoke()
                    dismiss()
                } else {
                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CreateAlert", "Error creating alert", e)
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "CreateAlertDialog"

        fun newInstance(): CreateAlertDialog {
            return CreateAlertDialog()
        }
    }
}
