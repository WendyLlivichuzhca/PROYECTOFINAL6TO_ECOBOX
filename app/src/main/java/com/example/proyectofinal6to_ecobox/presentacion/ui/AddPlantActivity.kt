package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.google.android.material.textfield.TextInputEditText

class AddPlantActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etSpecies: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etDesc: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: View

    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_plant)

        // Recuperar ID de usuario
        val prefs = getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        initViews()
    }

    private fun initViews() {
        etName = findViewById(R.id.etPlantName)
        etSpecies = findViewById(R.id.etPlantSpecies)
        etLocation = findViewById(R.id.etPlantLocation)
        etDesc = findViewById(R.id.etPlantDesc)
        btnSave = findViewById(R.id.btnSavePlant)
        btnBack = findViewById(R.id.btnBack)

        btnSave.setOnClickListener { guardarPlanta() }
        btnBack.setOnClickListener { finish() }

        // El botón de foto es solo visual por ahora
        findViewById<View>(R.id.btnUploadPhoto).setOnClickListener {
            Toast.makeText(this, "Cámara no implementada aún", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarPlanta() {
        val nombre = etName.text.toString().trim()
        val especie = etSpecies.text.toString().trim()
        val ubicacion = etLocation.text.toString().trim()
        val descripcion = etDesc.text.toString().trim()

        if (nombre.isEmpty() || especie.isEmpty()) {
            Toast.makeText(this, "Nombre y Especie son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Guardando..."

        Thread {
            try {
                // CREAR OBJETO PLANTA CORRECTAMENTE
                val nuevaPlanta = Planta(
                    0,          // id - temporal
                    nombre,     // nombre
                    especie,    // especie
                    "",         // fechaCreacion - se establecerá en BD
                    descripcion,// descripcion
                    0           // familiaId - será obtenido dentro del DAO
                )

                // Configurar la ubicación usando setter
                nuevaPlanta.setUbicacion(ubicacion)

                // USAR EL MÉTODO CORRECTO DEL DAO
                // Este método ya busca el familia_id internamente
                val exito = PlantaDao.insertarPlanta(nuevaPlanta, userId)

                runOnUiThread {
                    btnSave.isEnabled = true
                    btnSave.text = "Guardar Planta"

                    if (exito) {
                        Toast.makeText(this, "¡Planta agregada!", Toast.LENGTH_SHORT).show()
                        finish() // Volver al Dashboard y se recargará la lista
                    } else {
                        Toast.makeText(this, "Error al guardar. Revisa tu conexión.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    btnSave.isEnabled = true
                    btnSave.text = "Guardar Planta"
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}