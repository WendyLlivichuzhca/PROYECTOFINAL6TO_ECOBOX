package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AgregarSeguimientoActivity : AppCompatActivity() {

    private lateinit var etEstado: TextInputEditText
    private lateinit var etObservaciones: TextInputEditText
    private lateinit var btnUploadPhoto: MaterialCardView
    private lateinit var btnBack: ImageButton
    private lateinit var btnGuardar: MaterialButton
    private lateinit var btnCancelar: MaterialButton
    private lateinit var tvInfoNombre: TextView
    private lateinit var tvInfoEspecie: TextView

    private var plantaId: Long = -1
    private var plantaNombre: String = ""
    private var plantaEspecie: String = ""
    private var currentPhotoPath: String? = null
    private var selectedImageUri: Uri? = null
    private var userId: Long = -1
    private var photoUri: Uri? = null

    // Registro para permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            mostrarDialogoSeleccionImagen()
        } else {
            Toast.makeText(
                this,
                "Se necesitan permisos para acceder a la cámara y almacenamiento",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Registro para resultado de cámara
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            // Foto tomada exitosamente
            selectedImageUri = photoUri
            currentPhotoPath = getRealPathFromURI(photoUri!!)
            mostrarImagenSeleccionada(photoUri!!)
            Toast.makeText(this, "Foto tomada exitosamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al tomar la foto", Toast.LENGTH_SHORT).show()
        }
    }

    // Registro para galería
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            currentPhotoPath = getRealPathFromURI(uri)
            mostrarImagenSeleccionada(uri)
            Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.agregar_seguimiento_planta)

        // Obtener datos del Intent
        plantaId = intent.getLongExtra("PLANTA_ID", -1)
        plantaNombre = intent.getStringExtra("PLANTA_NOMBRE") ?: "Planta"
        plantaEspecie = intent.getStringExtra("PLANTA_ESPECIE") ?: "Sin especie"
        userId = intent.getLongExtra("USER_ID", -1)

        if (plantaId == -1L || userId == -1L) {
            Toast.makeText(this, "Error: Datos de planta no disponibles", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        cargarInformacionPlanta()
    }

    private fun initViews() {
        etEstado = findViewById(R.id.etEstado)
        etObservaciones = findViewById(R.id.etObservaciones)
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto)
        btnBack = findViewById(R.id.btnBack)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnCancelar = findViewById(R.id.btnCancelar)

        // Referencias para información de la planta
        tvInfoNombre = findViewById(R.id.tvInfoNombre)
        tvInfoEspecie = findViewById(R.id.tvInfoEspecie)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        btnCancelar.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        btnGuardar.setOnClickListener {
            guardarSeguimiento()
        }

        btnUploadPhoto.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun cargarInformacionPlanta() {
        tvInfoNombre.text = plantaNombre
        tvInfoEspecie.text = plantaEspecie
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Permiso de cámara
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // Permisos de almacenamiento según versión de Android
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 y anteriores
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Solicitar permisos
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Ya tenemos todos los permisos
            mostrarDialogoSeleccionImagen()
        }
    }

    private fun mostrarDialogoSeleccionImagen() {
        val options = arrayOf("Tomar foto", "Seleccionar de galería", "Cancelar")

        val builder = AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> tomarFoto()
                    1 -> seleccionarFoto()
                    2 -> dialog.dismiss()
                }
            }
        builder.show()
    }

    private fun tomarFoto() {
        try {
            val photoFile = createImageFile()
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    photoFile
                )
                takePictureLauncher.launch(photoUri)
            }
        } catch (ex: Exception) {
            Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
            ex.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun createImageFile(): File? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(null)

            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )

            currentPhotoPath = imageFile.absolutePath
            return imageFile
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun seleccionarFoto() {
        pickImageLauncher.launch("image/*")
    }

    private fun guardarSeguimiento() {
        val estado = etEstado.text.toString().trim()
        val observaciones = etObservaciones.text.toString().trim()

        // Validaciones
        if (estado.isEmpty()) {
            etEstado.error = "Por favor, describe el estado de la planta"
            etEstado.requestFocus()
            return
        }

        if (observaciones.isEmpty()) {
            etObservaciones.error = "Por favor, agrega observaciones"
            etObservaciones.requestFocus()
            return
        }

        // Verificar acceso a la planta
        if (!PlantaDao.verificarAccesoPlanta(userId, plantaId)) {
            Toast.makeText(this, "No tienes acceso a esta planta", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Determinar la ruta de la imagen
        val imagenPath = when {
            currentPhotoPath != null -> currentPhotoPath
            selectedImageUri != null -> getRealPathFromURI(selectedImageUri!!)
            else -> null
        }

        // Mostrar diálogo de progreso
        val progressDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_progress)
            .setCancelable(false)
            .create()
        progressDialog.show()

        // Usar thread para no bloquear la UI
        Thread {
            try {
                // Guardar en base de datos
                val exitoso = PlantaDao.agregarSeguimientoPlanta(
                    plantaId = plantaId,
                    estado = estado,
                    observaciones = observaciones,
                    imagenPath = imagenPath
                )

                runOnUiThread {
                    progressDialog.dismiss()

                    if (exitoso) {
                        Toast.makeText(this, "✅ Seguimiento guardado exitosamente", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this, "❌ Error al guardar el seguimiento", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun mostrarImagenSeleccionada(uri: Uri) {
        // Obtener el LinearLayout interno del card
        val container = btnUploadPhoto.getChildAt(0) as? LinearLayout
        container?.let {
            // Limpiar el contenedor
            it.removeAllViews()

            // Crear un FrameLayout para contener la imagen y el botón de eliminar
            val frameLayout = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            // ImageView para mostrar la imagen
            val imageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            // Cargar imagen con Glide
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_camera)
                .error(R.drawable.ic_camera)
                .into(imageView)

            frameLayout.addView(imageView)

            // Botón para eliminar la imagen
            val deleteButton = ImageButton(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dpToPx(40),
                    dpToPx(40)
                ).apply {
                    gravity = Gravity.END or Gravity.TOP
                    topMargin = dpToPx(8)
                    marginEnd = dpToPx(8)
                }
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                background = ContextCompat.getDrawable(this@AgregarSeguimientoActivity, R.drawable.shape_circle_white)
                setOnClickListener {
                    // Restaurar vista original
                    selectedImageUri = null
                    currentPhotoPath = null
                    photoUri = null
                    restaurarVistaFotoOriginal()
                }
            }

            frameLayout.addView(deleteButton)
            it.addView(frameLayout)
        }
    }

    private fun restaurarVistaFotoOriginal() {
        val container = btnUploadPhoto.getChildAt(0) as? LinearLayout
        container?.let {
            it.removeAllViews()

            // Restaurar la vista original
            val originalLayout = layoutInflater.inflate(R.layout.layout_default_photo, null)
            it.addView(originalLayout)
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        var path: String? = null

        // Para Android 10+, usar contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName = cursor.getString(0)
                    // Guardar en directorio de la app
                    val file = File(getExternalFilesDir(null), displayName)
                    // Copiar el archivo (opcional)
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    path = file.absolutePath
                }
            }
        } else {
            // Para versiones anteriores
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    path = cursor.getString(columnIndex)
                }
            }
        }
        return path
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}