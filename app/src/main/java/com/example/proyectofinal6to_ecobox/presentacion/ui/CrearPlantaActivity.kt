package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.example.proyectofinal6to_ecobox.data.network.*
import com.example.proyectofinal6to_ecobox.data.model.PlantTemplate
import com.example.proyectofinal6to_ecobox.utils.AppConfig
import com.example.proyectofinal6to_ecobox.utils.ImageUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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
    private lateinit var etPlantType: AutoCompleteTextView

    // Campo para especie
    private lateinit var etPlantSpecies: TextInputEditText

    // Variables
    private var selectedImageUri: Uri? = null
    private var selectedImagePath: String? = null  // Nueva: ruta del archivo copiado
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
                Log.d("CrearPlanta", "URI seleccionado: $uri")

                // Copiar imagen a almacenamiento interno para evitar problemas de permisos
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val fileName = "planta_${System.currentTimeMillis()}"
                        val copiedPath = ImageUtils.copyUriToInternalStorage(
                            context = this@CrearPlantaActivity,
                            uri = uri,
                            fileName = fileName
                        )

                        withContext(Dispatchers.Main) {
                            if (copiedPath != null) {
                                selectedImagePath = copiedPath
                                showSelectedImage(copiedPath)
                                Log.d("CrearPlanta", "Imagen copiada a: $copiedPath")
                            } else {
                                Toast.makeText(
                                    this@CrearPlantaActivity,
                                    "Error al procesar la imagen",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("CrearPlanta", "Error copiando imagen")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CrearPlanta", "Error procesando imagen: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@CrearPlantaActivity,
                                "Error al procesar la imagen: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
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

        // Inicializar selector de plantillas
        etPlantType = findViewById(R.id.etPlantType)
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

        // Configurar adaptadores para plantillas
        val templates = PlantTemplate.getTemplates()
        val templateAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            templates
        )
        etPlantType.setAdapter(templateAdapter)

        // Listener para autocompletar según plantilla
        etPlantType.setOnItemClickListener { parent, _, position, _ ->
            val selectedTemplate = parent.getItemAtPosition(position) as PlantTemplate
            autocompletarDesdePlantilla(selectedTemplate)
        }
    }

    private fun autocompletarDesdePlantilla(template: PlantTemplate) {
        // Autocompletar especie si está vacío
        if (etPlantSpecies.text.isNullOrEmpty()) {
            etPlantSpecies.setText(template.nombre)
        }

        // Generar notas sugeridas basadas en la plantilla
        val notasSugeridas = """
            ${template.descripcion}
            
            🌱 Cuidados Óptimos:
            • Humedad: ${template.humedadOptima}%
            • Temperatura: ${template.tempMin}°C - ${template.tempMax}°C
            • Riego sugerido: cada ${template.frecuenciaRiego}
        """.trimIndent()

        etPlantDesc.setText(notasSugeridas)
        
        Toast.makeText(this, "Preset aplicado: ${template.nombre}", Toast.LENGTH_SHORT).show()
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




        // Botón guardar planta
        btnSavePlant.setOnClickListener {
            guardarPlanta()
        }
    }

    private fun cargarFamiliasUsuario() {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        lifecycleScope.launch {
            try {
                val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                    .getFamilies("Token $token")

                if (response.isSuccessful && response.body() != null) {
                    val familias = response.body()!!
                    
                    if (familias.isNotEmpty()) {
                        // Crear una lista de nombres para el adapter
                        val nombresFamilias = familias.map { it.nombre }
                        val adapter = ArrayAdapter(
                            this@CrearPlantaActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            nombresFamilias
                        )
                        etPlantFamily.setAdapter(adapter)

                        etPlantFamily.setOnItemClickListener { parent, _, position, _ ->
                            val nombreSeleccionado = parent.getItemAtPosition(position) as String
                            val familia = familias.find { it.nombre == nombreSeleccionado }
                            familiaIdSeleccionada = familia?.id ?: -1L
                            Log.d("CrearPlanta", "Familia seleccionada: $nombreSeleccionado (ID: $familiaIdSeleccionada)")
                        }

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
                Toast.makeText(this@CrearPlantaActivity, "Error cargando familias", Toast.LENGTH_SHORT).show()
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

    private fun showSelectedImage(imagePath: String) {
        ivPlantPreview.visibility = android.view.View.VISIBLE
        
        // Cargar desde archivo local
        val file = File(imagePath)
        if (file.exists()) {
            ivPlantPreview.setImageURI(Uri.fromFile(file))
            tvNoFileSelected.text = "Archivo seleccionado"
            tvNoFileSelected.setTextColor(resources.getColor(R.color.eco_primary, null))
        } else {
            Log.e("CrearPlanta", "Archivo no existe: $imagePath")
            tvNoFileSelected.text = "Error al cargar imagen"
        }
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
        // Obtener otros datos del formulario y mapear a claves del backend
        val estadoCapturado = spinnerStatus.text.toString()
        val aspectoCapturado = spinnerAppearance.text.toString()

        val estado = when (estadoCapturado) {
            "Saludable" -> "saludable"
            "Necesita agua" -> "necesita_agua"
            "En Peligro", "Peligro" -> "peligro"
            "Normal" -> "normal"
            else -> "normal"
        }

        val aspecto = when (aspectoCapturado) {
            "Normal" -> "normal"
            "Floreciendo" -> "floreciendo"
            "Con Frutos" -> "con_frutos"
            "Hojas Amarillas" -> "hojas_amarillas"
            "Crecimiento Lento" -> "crecimiento_lento"
            "Exuberante" -> "exuberante"
            else -> "normal"
        }
        val especie = etPlantSpecies.text.toString().trim()

        // Obtener la foto (ruta del archivo copiado)
        val foto = selectedImagePath ?: ""

        // Ubicación por defecto
        val ubicacion = ""

        // LOGS PARA DEPURAR
        Log.d("CrearPlanta", "=== DATOS CAPTURADOS ===")
        Log.d("CrearPlanta", "Nombre: $nombrePlanta")
        Log.d("CrearPlanta", "Especie: '$especie'")
        Log.d("CrearPlanta", "Foto Path: $foto")
        Log.d("CrearPlanta", "Descripción: $descripcion")
        Log.d("CrearPlanta", "Familia ID: $familiaIdSeleccionada")
        Log.d("CrearPlanta", "Estado: $estado")
        Log.d("CrearPlanta", "Aspecto: $aspecto")

        // Deshabilitar botón mientras se procesa
        btnSavePlant.isEnabled = false
        btnSavePlant.text = "Creando..."

        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null) ?: ""

        lifecycleScope.launch {
            try {
                val success = crearPlantaEnBackend(
                    token = token,
                    nombrePlanta = nombrePlanta,
                    especie = especie,
                    descripcion = descripcion,
                    familiaId = familiaIdSeleccionada,
                    estado = estado,
                    aspecto = aspecto,
                    fotoPath = foto
                )

                btnSavePlant.isEnabled = true
                btnSavePlant.text = "Crear Planta"

                if (success) {
                    Toast.makeText(this@CrearPlantaActivity, "¡Planta creada exitosamente!", Toast.LENGTH_SHORT).show()
                    val resultIntent = Intent().apply {
                        putExtra("PLANTA_CREADA", true)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this@CrearPlantaActivity, "Error al crear la planta", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("CrearPlanta", "Error creando planta: ${e.message}", e)
                btnSavePlant.isEnabled = true
                btnSavePlant.text = "Crear Planta"
                Toast.makeText(this@CrearPlantaActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Crea una planta en el backend usando la API de Django
     * Sube la imagen si existe y retorna true si fue exitoso
     */
    private suspend fun crearPlantaEnBackend(
        token: String,
        nombrePlanta: String,
        especie: String,
        descripcion: String,
        familiaId: Long,
        estado: String,
        aspecto: String,
        fotoPath: String?
    ): Boolean {
        return try {
            val partMap = mutableMapOf<String, RequestBody>()
            
            fun addPart(key: String, value: String) {
                partMap[key] = value.toRequestBody("text/plain".toMediaTypeOrNull())
            }

            addPart("nombrePersonalizado", nombrePlanta)
            addPart("especie", especie)
            addPart("descripcion", descripcion)
            addPart("familia", familiaId.toString())
            addPart("estado", estado)
            addPart("aspecto", aspecto)

            val imagePart = if (!fotoPath.isNullOrEmpty()) {
                val file = File(fotoPath)
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("foto", file.name, requestFile)
                } else null
            } else null

            val response = com.example.proyectofinal6to_ecobox.data.network.RetrofitClient.instance
                .createPlant("Token $token", partMap, imagePart)

            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string()
                Log.e("CrearPlanta", "Error en creación: $errorMsg")
            }

            response.isSuccessful
        } catch (e: Exception) {
            Log.e("CrearPlanta", "Error en crearPlantaEnBackend: ${e.message}", e)
            false
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