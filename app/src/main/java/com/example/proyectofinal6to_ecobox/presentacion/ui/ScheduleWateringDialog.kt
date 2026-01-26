package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleWateringDialog(
    context: Context,
    private val plantId: Long,
    private val plantName: String,
    private val authToken: String,
    private val onSuccess: () -> Unit
) : Dialog(context) {

    private lateinit var tvPlantName: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var spinnerDuration: Spinner
    private lateinit var spinnerRecurrence: Spinner
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar

    private var selectedDate: Calendar = Calendar.getInstance()
    private val durationOptions = listOf(15, 30, 60, 120)
    private val recurrenceOptions = listOf("Una vez", "Diaria", "Semanal")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_schedule_watering, null)
        setContentView(view)

        initViews(view)
        setupSpinners()
        setupListeners()
        updateDateTimeButtons()
    }

    private fun initViews(view: android.view.View) {
        tvPlantName = view.findViewById(R.id.tvPlantName)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        btnSelectTime = view.findViewById(R.id.btnSelectTime)
        spinnerDuration = view.findViewById(R.id.spinnerDuration)
        spinnerRecurrence = view.findViewById(R.id.spinnerRecurrence)
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

        // Recurrencia
        val recurrenceAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, recurrenceOptions)
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRecurrence.adapter = recurrenceAdapter
    }

    private fun setupListeners() {
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        btnConfirm.setOnClickListener {
            scheduleWatering()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeButtons()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                updateDateTimeButtons()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateTimeButtons() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        btnSelectDate.text = dateFormat.format(selectedDate.time)
        btnSelectTime.text = timeFormat.format(selectedDate.time)
    }

    private fun scheduleWatering() {
        val duration = durationOptions[spinnerDuration.selectedItemPosition]
        val recurrence = recurrenceOptions[spinnerRecurrence.selectedItemPosition].lowercase()
        
        // Formato ISO 8601 para el backend
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val fechaProgramada = isoFormat.format(selectedDate.time)

        btnConfirm.isEnabled = false
        btnCancel.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE

        (context as? androidx.appcompat.app.AppCompatActivity)?.lifecycleScope?.launch {
            try {
                val response = RetrofitClient.instance.createWatering(
                    "Token $authToken",
                    mapOf(
                        "planta" to plantId,
                        "tipo" to "PROGRAMADO",
                        "estado" to "PROGRAMADO",
                        "fecha_programada" to fechaProgramada,
                        "duracion_segundos" to duration,
                        "cantidad_ml" to 500
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val riego = response.body()!!
                    Toast.makeText(context, "Riego programado: ${riego.estado}", Toast.LENGTH_SHORT).show()
                    onSuccess()
                    dismiss()
                } else {
                    showError("Error al programar el riego")
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
