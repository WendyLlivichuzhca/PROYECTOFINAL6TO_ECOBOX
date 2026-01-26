package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class QuickWateringDialog(
    context: Context,
    private val plantId: Long,
    private val plantName: String,
    private val authToken: String,
    private val onSuccess: () -> Unit
) : Dialog(context) {

    private lateinit var tvPlantName: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_quick_watering, null)
        setContentView(view)

        initViews(view)
        setupListeners()
    }

    private fun initViews(view: android.view.View) {
        tvPlantName = view.findViewById(R.id.tvPlantName)
        tvInfo = view.findViewById(R.id.tvInfo)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        btnCancel = view.findViewById(R.id.btnCancel)
        progressBar = view.findViewById(R.id.progressBar)

        tvPlantName.text = plantName
        tvInfo.text = "Se regará durante 30 segundos con aproximadamente 500ml de agua."
        progressBar.visibility = android.view.View.GONE
    }

    private fun setupListeners() {
        btnConfirm.setOnClickListener {
            executeQuickWatering()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun executeQuickWatering() {
        // Deshabilitar botones
        btnConfirm.isEnabled = false
        btnCancel.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE
        tvInfo.text = "Regando planta..."

        (context as? androidx.appcompat.app.AppCompatActivity)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.instance.quickWatering(
                    "Token $authToken",
                    plantId,
                    mapOf("duration_seconds" to 30, "mode" to "manual")
                )

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    
                    if (result.status == "success") {
                        // Mostrar resultado
                        val riego = result.riego
                        val humedadInicial = riego?.humedadInicial ?: 0f
                        val humedadFinal = riego?.humedadFinal ?: 0f
                        
                        tvInfo.text = "✅ Riego completado!\n\n" +
                                "Humedad inicial: ${String.format("%.1f", humedadInicial)}%\n" +
                                "Humedad final: ${String.format("%.1f", humedadFinal)}%"
                        
                        btnConfirm.text = "Cerrar"
                        btnConfirm.isEnabled = true
                        btnConfirm.setOnClickListener {
                            onSuccess()
                            dismiss()
                        }
                        btnCancel.visibility = android.view.View.GONE
                        progressBar.visibility = android.view.View.GONE
                        
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    } else {
                        showError("Error: ${result.message}")
                    }
                } else {
                    showError("Error al regar la planta")
                }
            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        tvInfo.text = message
        btnConfirm.isEnabled = true
        btnCancel.isEnabled = true
        progressBar.visibility = android.view.View.GONE
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
