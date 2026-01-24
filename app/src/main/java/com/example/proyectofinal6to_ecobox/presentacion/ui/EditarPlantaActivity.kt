package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.model.Planta
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.proyectofinal6to_ecobox.utils.ImageUtils
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class EditarPlantaActivity : AppCompatActivity() {

    // Declarar las vistas manualmente
    private lateinit var toolbar: Toolbar
    private lateinit var ivPlanta: ImageView
    private lateinit var btnCambiarFoto: MaterialButton
    private lateinit var btnEliminarFoto: MaterialButton
    private lateinit var etNombre: TextInputEditText
    private lateinit var etEspecie: TextInputEditText
    private lateinit var autoEstado: AutoCompleteTextView
    private lateinit var autoAspecto: AutoCompleteTextView
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSaveChanges: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var plantaOriginal: Planta
    private var nuevaFotoUri: Uri? = null
    private var nuevaFotoPath: String? = null
    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        const val EXTRA_PLANTA = "planta"
        const val EXTRA_USER_ID = "user_id"
        const val REQUEST_CODE_IMAGE_PICKER = 100
        const val RESULT_PLANTA_EDITADA = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_planta)

        // Inicializar vistas manualmente
        inicializarVistas()

        // Obtener planta del intent
        plantaOriginal = intent.getSerializableExtra(EXTRA_PLANTA) as? Planta ?: run {
            Toast.makeText(this, "Error: Planta no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
        if (userId == -1L) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        cargarDatosPlanta()
        setupListeners(userId)
    }

    private fun inicializarVistas() {
        toolbar = findViewById(R.id.toolbar)
        ivPlanta = findViewById(R.id.ivPlanta)
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto)
        btnEliminarFoto = findViewById(R.id.btnEliminarFoto)
        etNombre = findViewById(R.id.etNombre)
        etEspecie = findViewById(R.id.etEspecie)
        autoEstado = findViewById(R.id.autoEstado)
        autoAspecto = findViewById(R.id.autoAspecto)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnCancel = findViewById(R.id.btnCancel)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupUI() {
        // Configurar toolbar
        toolbar.title = "Editar Planta"
        toolbar.setNavigationOnClickListener {
            mostrarDialogoCancelar()
        }

        // Configurar adapters para los dropdowns
        val estados = listOf(
            "Saludable",
            "Normal",
            "Necesita agua",
            "Advertencia",
            "Crítico",
            "Excelente",
            "En observación"
        )

        val aspectos = listOf(
            "Normal",
            "Floreciendo",
            "Con frutos",
            "Marchito",
            "Enfermo",
            "Con hojas nuevas",
            "Podado",
            "En crecimiento"
        )

        // Usar layout de Android por defecto si no tienes dropdown_item.xml
        val estadoAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        val aspectoAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, aspectos)

        autoEstado.setAdapter(estadoAdapter)
        autoAspecto.setAdapter(aspectoAdapter)
    }

    private fun cargarDatosPlanta() {
        // Cargar datos en los campos
        etNombre.setText(plantaOriginal.nombre)
        etEspecie.setText(plantaOriginal.especie)
        etDescripcion.setText(plantaOriginal.descripcion)

        // Establecer estado y aspecto
        val estado = if (plantaOriginal.estado.isNotEmpty()) plantaOriginal.estado else "Normal"
        val aspecto = if (plantaOriginal.aspecto.isNotEmpty()) plantaOriginal.aspecto else "Normal"

        autoEstado.setText(estado, false)
        autoAspecto.setText(aspecto, false)

        // Cargar foto si existe
        if (plantaOriginal.foto.isNotEmpty()) {
            cargarFoto(plantaOriginal.foto)
        } else {
            ivPlanta.setImageResource(R.drawable.ic_plant)
            btnEliminarFoto.visibility = View.GONE
        }
    }

    private fun cargarFoto(fotoPath: String) {
        try {
            ImageUtils.loadPlantImage(
                imageData = fotoPath,
                imageView = ivPlanta,
                placeholderResId = R.drawable.ic_plant
            )
            btnEliminarFoto.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
            ivPlanta.setImageResource(R.drawable.ic_plant)
            btnEliminarFoto.visibility = View.GONE
        }
    }

    private fun setupListeners(userId: Long) {
        // Botón para cambiar foto
        btnCambiarFoto.setOnClickListener {
            abrirSelectorImagen()
        }

        // Botón para eliminar foto
        btnEliminarFoto.setOnClickListener {
            eliminarFoto()
        }

        // Botón guardar cambios
        btnSaveChanges.setOnClickListener {
            guardarCambios(userId)
        }

        // Botón cancelar
        btnCancel.setOnClickListener {
            mostrarDialogoCancelar()
        }
    }

    private fun abrirSelectorImagen() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
    }

    private fun eliminarFoto() {
        nuevaFotoUri = null
        nuevaFotoPath = null
        ivPlanta.setImageResource(R.drawable.ic_plant)
        btnEliminarFoto.visibility = View.GONE

        Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_IMAGE_PICKER && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                nuevaFotoUri = uri
                ivPlanta.setImageURI(uri)
                btnEliminarFoto.visibility = View.VISIBLE

                guardarImagenLocalmente(uri)
            }
        }
    }

    private fun guardarImagenLocalmente(uri: Uri) {
        executor.execute {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val archivo = File(filesDir, "planta_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(archivo)

                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                nuevaFotoPath = archivo.absolutePath

                runOnUiThread {
                    Toast.makeText(this, "Imagen cargada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarCambios(userId: Long) {
        // Validar campos
        val nombre = etNombre.text.toString().trim()
        val especie = etEspecie.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val estado = autoEstado.text.toString().trim()
        val aspecto = autoAspecto.text.toString().trim()

        if (nombre.isEmpty()) {
            (etNombre.parent.parent as? TextInputLayout)?.error = "El nombre es requerido"
            return
        }

        if (estado.isEmpty()) {
            (autoEstado.parent.parent as? TextInputLayout)?.error = "El estado es requerido"
            return
        }

        if (aspecto.isEmpty()) {
            (autoAspecto.parent.parent as? TextInputLayout)?.error = "El aspecto es requerido"
            return
        }

        // Mostrar loading
        progressBar.visibility = View.VISIBLE
        btnSaveChanges.isEnabled = false
        btnCancel.isEnabled = false

        // Usar constructor vacío y settear todos los campos
        val plantaActualizada = Planta().apply {
            this.id = plantaOriginal.id
            this.nombre = nombre
            this.especie = especie
            this.fechaCreacion = plantaOriginal.fechaCreacion ?: ""
            this.descripcion = descripcion
            this.familiaId = plantaOriginal.familiaId
            this.ubicacion = plantaOriginal.ubicacion ?: ""
            this.aspecto = aspecto
            this.estado = estado
            this.foto = nuevaFotoPath ?: plantaOriginal.foto ?: ""
        }

        // Actualizar en base de datos
        actualizarPlantaEnBD(plantaActualizada, userId)
    }

    private fun actualizarPlantaEnBD(planta: Planta, userId: Long) {
        executor.execute {
            try {
                val actualizado = PlantaDao.actualizarPlanta(planta, userId)

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSaveChanges.isEnabled = true
                    btnCancel.isEnabled = true

                    if (actualizado) {
                        val resultIntent = Intent()
                        resultIntent.putExtra("UPDATED_NAME", planta.nombre)
                        resultIntent.putExtra("UPDATED_LOCATION", planta.ubicacion)
                        resultIntent.putExtra("UPDATED_PHOTO", planta.foto)
                        resultIntent.putExtra("UPDATED_DESCRIPTION", planta.descripcion)
                        resultIntent.putExtra("UPDATED_SPECIES", planta.especie)
                        setResult(RESULT_OK, resultIntent)

                        Toast.makeText(this, "✓ Planta actualizada exitosamente", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "✗ Error al actualizar la planta", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSaveChanges.isEnabled = true
                    btnCancel.isEnabled = true
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private var isBackPressedDialogShowing = false

    private fun mostrarDialogoCancelar() {
        if (isBackPressedDialogShowing) return

        val hayCambios = verificarCambios()

        if (!hayCambios) {
            super.onBackPressed()
            return
        }

        isBackPressedDialogShowing = true
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Descartar cambios?")
            .setMessage("Tienes cambios sin guardar. ¿Seguro que quieres salir?")
            .setPositiveButton("Salir") { dialog, which ->
                isBackPressedDialogShowing = false
                super.onBackPressed()
            }
            .setNegativeButton("Seguir editando") { dialog, which ->
                isBackPressedDialogShowing = false
            }
            .setOnCancelListener {
                isBackPressedDialogShowing = false
            }
            .show()
    }

    override fun onBackPressed() {
        mostrarDialogoCancelar()
    }

    private fun verificarCambios(): Boolean {
        val nombre = etNombre.text.toString().trim()
        val especie = etEspecie.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val estado = autoEstado.text.toString().trim()
        val aspecto = autoAspecto.text.toString().trim()

        return (nombre != plantaOriginal.nombre ||
                especie != plantaOriginal.especie ||
                descripcion != plantaOriginal.descripcion ||
                estado != (if (plantaOriginal.estado.isNotEmpty()) plantaOriginal.estado else "Normal") ||
                aspecto != (if (plantaOriginal.aspecto.isNotEmpty()) plantaOriginal.aspecto else "Normal") ||
                nuevaFotoUri != null)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}