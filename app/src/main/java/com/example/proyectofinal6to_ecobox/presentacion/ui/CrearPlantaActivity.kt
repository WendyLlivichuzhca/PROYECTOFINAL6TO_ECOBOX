package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.FamiliaDao
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrearPlantaActivity : AppCompatActivity() {

    // Views del formulario
    private lateinit var etPlantName: TextInputEditText
    private lateinit var cbCustomName: MaterialCheckBox
    private lateinit var etPlantFamily: AutoCompleteTextView
    private lateinit var etPlantDesc: TextInputEditText
    private lateinit var spinnerStatus: AutoCompleteTextView
    private lateinit var spinnerAppearance: AutoCompleteTextView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var tvNoFileSelected: TextView
    private lateinit var ivPlantPreview: ImageView
    private lateinit var btnSavePlant: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnBack: ImageView

    // Campo para especie
    private lateinit var etPlantSpecies: TextInputEditText

    // Variables
    private var selectedImageUri: Uri? = null
    private var userId: Long = -1
    private var familiaIdSeleccionada: Long = -1

    // Listas para dropdowns
    private val statusOptions = listOf(
        "Saludable",
        "En crecimiento",
        "Floreciendo",
        "Necesita atención",
        "Enferma",
        "normal"
    )

    private val appearanceOptions = listOf(
        "Normal",
        "Vigorosa",
        "Algo marchita",
        "Con flores",
        "Con frutos",
        "Recién plantada",
        "normal"
    )

    // Contract para seleccionar imagen
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                selectedImageUri = uri
                showSelectedImage(uri)
                Log.d("CrearPlanta", "Imagen seleccionada: $uri")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_plant)

        // Recuperar ID de usuario
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        if (userId == -1L) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupSpinners()
        setupListeners()
        cargarFamiliasUsuario()
    }

    private fun initViews() {
        etPlantName = findViewById(R.id.etPlantName)
        cbCustomName = findViewById(R.id.cbCustomName)
        etPlantFamily = findViewById(R.id.etPlantFamily)
        etPlantDesc = findViewById(R.id.etPlantDesc)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        spinnerAppearance = findViewById(R.id.spinnerAppearance)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        tvNoFileSelected = findViewById(R.id.tvNoFileSelected)
        ivPlantPreview = findViewById(R.id.ivPlantPreview)
        btnSavePlant = findViewById(R.id.btnSavePlant)
        btnCancel = findViewById(R.id.btnCancel)
        btnBack = findViewById(R.id.btnBack)

        // Inicializar el campo de especie
        etPlantSpecies = findViewById(R.id.etPlantSpecies)
    }

    private fun setupSpinners() {
        // Configurar adaptadores para estado y aspecto
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            statusOptions
        )
        spinnerStatus.setAdapter(statusAdapter)

        val appearanceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            appearanceOptions
        )
        spinnerAppearance.setAdapter(appearanceAdapter)

        // Configurar listeners para abrir dropdowns
        spinnerStatus.setOnClickListener {
            spinnerStatus.showDropDown()
        }

        spinnerAppearance.setOnClickListener {
            spinnerAppearance.showDropDown()
        }

        // Establecer valores por defecto
        spinnerStatus.setText("normal", false)
        spinnerAppearance.setText("normal", false)
    }

    private fun setupListeners() {
        // Botón Atrás
        btnBack.setOnClickListener {
            showCancelConfirmationDialog()
        }

        // Botón Cancelar
        btnCancel.setOnClickListener {
            showCancelConfirmationDialog()
        }

        // Checkbox nombre personalizado
        cbCustomName.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPlantName.hint = "Ingresa tu nombre personalizado"
            } else {
                etPlantName.hint = "Ej: 'Rosa del jardín', 'Suculenta de oficina'"
            }
        }

        // Botón seleccionar imagen
        btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        // Configurar AutoCompleteTextView para familias
        etPlantFamily.setOnItemClickListener { parent, _, position, _ ->
            val familiaSeleccionada = parent.getItemAtPosition(position) as FamiliaDao.FamiliaSimple
            familiaIdSeleccionada = familiaSeleccionada.id
            Log.d(
                "CrearPlanta",
                "Familia seleccionada: ${familiaSeleccionada.nombre} (ID: ${familiaSeleccionada.id})"
            )
        }

        // Botón guardar planta
        btnSavePlant.setOnClickListener {
            guardarPlanta()
        }
    }

    private fun cargarFamiliasUsuario() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val familias = FamiliaDao.obtenerFamiliasSimplesPorUsuario(userId)

                withContext(Dispatchers.Main) {
                    if (familias.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            this@CrearPlantaActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            familias
                        )
                        etPlantFamily.setAdapter(adapter)

                        // Seleccionar la primera familia por defecto
                        val primeraFamilia = familias[0]
                        etPlantFamily.setText(primeraFamilia.nombre, false)
                        familiaIdSeleccionada = primeraFamilia.id

                        Log.d("CrearPlanta", "${familias.size} familias cargadas")
                    } else {
                        Toast.makeText(
                            this@CrearPlantaActivity,
                            "No perteneces a ninguna familia. Crea o únete a una primero.",
                            Toast.LENGTH_LONG
                        ).show()
                        btnSavePlant.isEnabled = false
                        btnSavePlant.text = "Sin familia disponible"
                    }
                }
            } catch (e: Exception) {
                Log.e("CrearPlanta", "Error cargando familias: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CrearPlantaActivity,
                        "Error cargando familias",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png", "image/gif")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        imagePickerLauncher.launch(intent)
    }

    private fun showSelectedImage(uri: Uri) {
        ivPlantPreview.visibility = android.view.View.VISIBLE
        ivPlantPreview.setImageURI(uri)
        tvNoFileSelected.text = "Archivo seleccionado"
        tvNoFileSelected.setTextColor(resources.getColor(R.color.eco_primary, null))
    }

    private fun guardarPlanta() {
        // Validar campos obligatorios
        val nombrePlanta = etPlantName.text.toString().trim()

        if (nombrePlanta.isEmpty()) {
            etPlantName.error = "El nombre de la planta es obligatorio"
            return
        }

        if (familiaIdSeleccionada == -1L) {
            etPlantFamily.error = "Debes seleccionar una familia"
            return
        }

        // Obtener otros datos del formulario
        val descripcion = etPlantDesc.text.toString().trim()
        val estado = spinnerStatus.text.toString()
        val aspecto = spinnerAppearance.text.toString()

        // Obtener especie del campo de entrada
        val especie = etPlantSpecies.text.toString().trim()

        // Obtener la foto (URI como string)
        val foto = selectedImageUri?.toString() ?: ""

        // Ubicación por defecto
        val ubicacion = ""

        // LOGS PARA DEPURAR
        Log.d("CrearPlanta", "=== DATOS CAPTURADOS ===")
        Log.d("CrearPlanta", "Nombre: $nombrePlanta")
        Log.d("CrearPlanta", "Especie: '$especie'")
        Log.d("CrearPlanta", "Foto URI: $foto")
        Log.d("CrearPlanta", "Descripción: $descripcion")
        Log.d("CrearPlanta", "Familia ID: $familiaIdSeleccionada")
        Log.d("CrearPlanta", "Estado: $estado")
        Log.d("CrearPlanta", "Aspecto: $aspecto")

        // Deshabilitar botón mientras se procesa
        btnSavePlant.isEnabled = false
        btnSavePlant.text = "Creando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val nuevaPlanta = Planta()

                // Establecer todos los campos usando setters
                nuevaPlanta.nombre = nombrePlanta
                nuevaPlanta.especie = especie  // ¡Especie capturada!
                nuevaPlanta.fechaCreacion = "" // La BD usará NOW()
                nuevaPlanta.descripcion = descripcion
                nuevaPlanta.familiaId = familiaIdSeleccionada
                nuevaPlanta.ubicacion = ubicacion
                nuevaPlanta.aspecto = aspecto
                nuevaPlanta.estado = estado
                nuevaPlanta.foto = foto  // ¡Foto capturada!

                // LOG del objeto completo
                Log.d("CrearPlanta", "Objeto Planta creado: $nuevaPlanta")
                Log.d("CrearPlanta", "Especie: ${nuevaPlanta.especie}")
                Log.d("CrearPlanta", "Foto: ${nuevaPlanta.foto}")

                // Insertar en la base de datos
                val exito = PlantaDao.insertarPlanta(nuevaPlanta, userId)
                Log.d("CrearPlanta", "Resultado inserción: $exito")

                withContext(Dispatchers.Main) {
                    btnSavePlant.isEnabled = true
                    btnSavePlant.text = "Crear Planta"

                    if (exito) {
                        Toast.makeText(
                            this@CrearPlantaActivity,
                            "¡Planta creada exitosamente!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Regresar a la actividad anterior
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@CrearPlantaActivity,
                            "Error al crear la planta. Intenta nuevamente.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CrearPlanta", "Error creando planta: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    btnSavePlant.isEnabled = true
                    btnSavePlant.text = "Crear Planta"

                    Toast.makeText(
                        this@CrearPlantaActivity,
                        "Error: ${e.message ?: "Error desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showCancelConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancelar creación")
            .setMessage("¿Estás seguro de que quieres cancelar? Se perderán los datos no guardados.")
            .setPositiveButton("Sí, cancelar") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("Continuar editando") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}