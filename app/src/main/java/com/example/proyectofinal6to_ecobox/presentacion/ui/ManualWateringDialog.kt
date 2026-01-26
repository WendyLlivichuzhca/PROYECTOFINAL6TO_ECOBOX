package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import kotlinx.coroutines.launch

class ManualWateringDialog(
    context: Context,
    private val plantId: Long,
    private val plantName: String,
    private val authToken: String,
    private val onSuccess: () -> Unit
) : Dialog(context) {

    private lateinit var tvPlantName: TextView
    private lateinit var spinnerDuration: Spinner
    private lateinit var spinnerAmount: Spinner
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar

    private val durationOptions = listOf(15, 30, 60, 120)
    private val amountOptions = listOf(250, 500, 1000, 2000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_manual_watering, null)
        setContentView(view)

        initViews(view)
        setupSpinners()
        setupListeners()
    }

    private fun initViews(view: android.view.View) {
        tvPlantName = view.findViewById(R.id.tvPlantName)
        spinnerDuration = view.findViewById(R.id.spinnerDuration)
        spinnerAmount = view.findViewById(R.id.spinnerAmount)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        btnCancel = view.findViewById(R.id.btnCancel)
        progressBar = view.findViewById(R.id.progressBar)

        tvPlantName.text = plantName
        progressBar.visibility = android.view.View.GONE
    }

    private fun setupSpinners() {
        // Duración
        val durationLabels = durationOptions.map { "${it}s" }
        val durationAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, durationLabels)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuration.adapter = durationAdapter
        spinnerDuration.setSelection(1) // 30s por defecto

        // Cantidad
        val amountLabels = amountOptions.map { "${it}ml" }
        val amountAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, amountLabels)
        amountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAmount.adapter = amountAdapter
        spinnerAmount.setSelection(1) // 500ml por defecto
    }

    private fun setupListeners() {
        btnConfirm.setOnClickListener {
            createManualWatering()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun createManualWatering() {
        val duration = durationOptions[spinnerDuration.selectedItemPosition]
        val amount = amountOptions[spinnerAmount.selectedItemPosition]

        btnConfirm.isEnabled = false
        btnCancel.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE

        (context as? androidx.appcompat.app.AppCompatActivity)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.instance.createWatering(
                    "Token $authToken",
                    mapOf(
                        "planta_id" to plantId,
                        "tipo" to "MANUAL",
                        "duracion_segundos" to duration,
                        "cantidad_ml" to amount
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val riego = response.body()!!
                    Toast.makeText(context, "Riego manual creado: ${riego.estado}", Toast.LENGTH_SHORT).show()
                    onSuccess()
                    dismiss()
                } else {
                    showError("Error al crear el riego")
                }
            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        btnConfirm.isEnabled = true
        btnCancel.isEnabled = true
        progressBar.visibility = android.view.View.GONE
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
