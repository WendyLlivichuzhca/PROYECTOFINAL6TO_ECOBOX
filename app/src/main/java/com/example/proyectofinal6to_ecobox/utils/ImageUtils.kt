package com.example.proyectofinal6to_ecobox.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.proyectofinal6to_ecobox.utils.AppConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {

    private const val TAG = "IMAGE_UTILS"

    /**
     * Detecta si un URI es del Photo Picker de Android 13+
     * Patrón: content://media/picker/...
     */
    fun isPhotoPickerUri(uri: Uri): Boolean {
        val uriString = uri.toString()
        return uriString.contains("content://media/picker/") ||
                uriString.contains("com.android.providers.media.photopicker")
    }

    /**
     * Detecta si es un content:// URI
     */
    fun isContentUri(uri: Uri): Boolean {
        return uri.scheme == "content"
    }

    /**
     * Copia un URI (especialmente del Photo Picker) a almacenamiento interno
     * para evitar problemas de permisos temporales.
     *
     * @param context Contexto de la aplicación
     * @param uri URI de la imagen a copiar
     * @param fileName Nombre del archivo (sin extensión, se agregará .jpg)
     * @return Ruta absoluta del archivo copiado, o null si falla
     */
    fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
        return try {
            Log.d(TAG, "Copiando URI a almacenamiento interno: $uri")

            // Crear directorio de plantas si no existe
            val plantasDir = File(context.filesDir, "plantas")
            if (!plantasDir.exists()) {
                plantasDir.mkdirs()
                Log.d(TAG, "Directorio creado: ${plantasDir.absolutePath}")
            }

            // Crear archivo de destino
            val timestamp = System.currentTimeMillis()
            val destinationFile = File(plantasDir, "${fileName}_${timestamp}.jpg")

            // Copiar contenido del URI al archivo
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val absolutePath = destinationFile.absolutePath
            Log.d(TAG, "Imagen copiada exitosamente a: $absolutePath")
            Log.d(TAG, "Tamaño del archivo: ${destinationFile.length()} bytes")

            absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Error de IO al copiar imagen: ${e.message}", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad al copiar imagen: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al copiar imagen: ${e.message}", e)
            null
        }
    }

    // Detectar si es ruta de archivo (más preciso)
    fun isFilePath(str: String): Boolean {
        if (str.isEmpty()) return false

        // Patrones comunes de rutas de archivo
        return str.startsWith("/") ||
                str.startsWith("file://") ||
                str.contains("/plantas/") ||
                (str.contains("/") &&
                        (str.endsWith(".jpg", ignoreCase = true) ||
                                str.endsWith(".jpeg", ignoreCase = true) ||
                                str.endsWith(".png", ignoreCase = true) ||
                                str.endsWith(".gif", ignoreCase = true)))
    }

    // Detectar si es URL
    fun isUrl(str: String): Boolean {
        if (str.isEmpty()) return false
        return str.startsWith("http://") ||
                str.startsWith("https://") ||
                str.startsWith("ftp://")
    }

    // Detectar Base64 (más robusto)
    fun isBase64(str: String): Boolean {
        if (str.isEmpty()) return false

        // Primero excluir casos obvios NO Base64
        if (isUrl(str) || isFilePath(str)) {
            Log.d("IMAGE_UTILS", "NO es Base64 (es URL o ruta)")
            return false
        }

        // Base64 típicamente no contiene espacios ni caracteres especiales
        if (str.contains(" ") || str.contains("\n") || str.contains("\t")) {
            return false
        }

        // Intentar decodificar
        return try {
            val decoded = Base64.decode(str, Base64.DEFAULT)
            // Si decodifica sin error y tiene algún tamaño
            decoded.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // Cargar imagen inteligente
    fun loadPlantImage(imageData: String?, imageView: ImageView, placeholderResId: Int) {
        if (imageData.isNullOrEmpty()) {
            Log.d(TAG, "Datos de imagen vacíos, usando placeholder")
            imageView.setImageResource(placeholderResId)
            return
        }

        Log.d(TAG, "Procesando imagen: ${imageData.take(50)}...")

        when {
            isBase64(imageData) -> {
                Log.d(TAG, "Detectado como Base64, decodificando...")
                try {
                    val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
                    Glide.with(imageView.context)
                        .load(decodedBytes)
                        .placeholder(placeholderResId)
                        .error(placeholderResId)
                        .centerCrop()
                        .into(imageView)
                    Log.d(TAG, "Base64 cargado exitosamente")
                } catch (e: Exception) {
                    Log.e(TAG, "Error decodificando Base64: ${e.message}")
                    imageView.setImageResource(placeholderResId)
                }
            }

            isUrl(imageData) -> {
                Log.d(TAG, "Detectado como URL completa")
                Glide.with(imageView.context)
                    .load(imageData)
                    .placeholder(placeholderResId)
                    .error(placeholderResId)
                    .centerCrop()
                    .into(imageView)
            }
            
            // --- NUEVO: Detectar si es una ruta del servidor (ej: "plantas/archivo.jpg") ---
            imageData.contains("plantas/") && !imageData.startsWith("/") -> {
                val fullUrl = AppConfig.getFullMediaUrl(imageData)
                Log.d(TAG, "Detectada ruta de servidor, cargando URL: $fullUrl")
                Glide.with(imageView.context)
                    .load(fullUrl)
                    .placeholder(placeholderResId)
                    .error(placeholderResId)
                    .centerCrop()
                    .into(imageView)
            }

            isFilePath(imageData) -> {
                Log.d(TAG, "Detectado como ruta de archivo local: $imageData")
                try {
                    val file = File(imageData)
                    if (file.exists()) {
                        Log.d(TAG, "Archivo local existe, cargando con Glide")
                        Glide.with(imageView.context)
                            .load(file)
                            .placeholder(placeholderResId)
                            .error(placeholderResId)
                            .centerCrop()
                            .into(imageView)
                    } else {
                        Log.w(TAG, "Archivo local no existe: $imageData")
                        imageView.setImageResource(placeholderResId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error cargando archivo: ${e.message}")
                    imageView.setImageResource(placeholderResId)
                }
            }

            else -> {
                Log.d(TAG, "Tipo desconocido, intentando cargar como ruta de servidor por defecto")
                val fullUrl = AppConfig.getFullMediaUrl(imageData)
                Glide.with(imageView.context)
                    .load(fullUrl)
                    .placeholder(placeholderResId)
                    .error(placeholderResId)
                    .centerCrop()
                    .into(imageView)
            }
        }
    }

    // Convertir Bitmap a Base64
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo a Base64: ${e.message}")
            ""
        }
    }

    /**
     * Obtener URL absoluta para imágenes del backend
     * Delegado a AppConfig.getFullMediaUrl() para centralizar lógica
     */
    fun getAbsoluteUrl(path: String?): String {
        return AppConfig.getFullMediaUrl(path)
    }
}