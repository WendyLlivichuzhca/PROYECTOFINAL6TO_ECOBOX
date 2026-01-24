package com.example.proyectofinal6to_ecobox.utils

object AppConfig {
    // IMPORTANTE: Cambiar según el entorno
    // - Para emulador: "10.0.2.2"
    // - Para dispositivo físico en misma red: "192.168.0.106" (IP real de tu PC)
    // - Para producción: Dominio real
    
    const val SERVER_IP = "10.0.2.2"  // Android Emulator (localhost del host)
    //const val SERVER_IP = "192.168.0.106"  // Descomentar para dispositivo físico
    
    // Configuración de Base de Datos
    const val DB_PORT = "3306" // Puerto MySQL
    const val DB_NAME = "base_ecobox"
    const val DB_USER = "root"
    const val DB_PASSWORD = "1234"
    
    // Configuración de API y Media (Django)
    const val HTTP_PORT = "8000"
    const val API_BASE_URL = "http://$SERVER_IP:$HTTP_PORT/api/"
    const val MEDIA_BASE_URL = "http://$SERVER_IP:$HTTP_PORT/media/"
    
    /**
     * Convierte una ruta relativa de imagen a URL completa
     * @param relativePath Ruta relativa (ej: "plantas/foto.jpg") o URL completa
     * @return URL completa lista para usar con Glide
     */
    fun getFullMediaUrl(relativePath: String?): String {
        if (relativePath.isNullOrEmpty()) return ""
        
        // Si ya es URL completa, retornar tal cual
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath
        }
        
        // Si es ruta relativa, construir URL completa
        val cleanPath = relativePath
            .removePrefix("/media/")
            .removePrefix("media/")
            .removePrefix("/")
        
        return "$MEDIA_BASE_URL$cleanPath"
    }
}
