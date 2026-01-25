package com.example.proyectofinal6to_ecobox.data.model

data class PlantTemplate(
    val id: Int,
    val nombre: String,
    val icono: String,
    val descripcion: String,
    val humedadOptima: String,
    val tempMin: Float,
    val tempMax: Float,
    val frecuenciaRiego: String
) {
    override fun toString(): String = "$icono $nombre"

    companion object {
        fun getTemplates(): List<PlantTemplate> = listOf(
            PlantTemplate(1, "Árboles", "🌳", "Plantas de gran tamaño con tronco leñoso", "40-70", 10f, 30f, "7-21 días"),
            PlantTemplate(2, "Arbustos", "🌿", "Plantas leñosas de menor tamaño que los árboles", "40-65", 15f, 28f, "5-10 días"),
            PlantTemplate(3, "Hierbas y Aromáticas", "🌱", "Plantas de tallo tierno, incluye medicinales", "50-70", 18f, 25f, "3-7 días"),
            PlantTemplate(4, "Plantas Trepadoras", "🪜", "Plantas que crecen apoyándose en estructuras", "50-80", 18f, 28f, "3-7 días"),
            PlantTemplate(5, "Suculentas y Cactus", "🌵", "Plantas que almacenan agua en hojas/tallos", "30-50", 18f, 30f, "10-21 días"),
            PlantTemplate(6, "Helechos", "🍃", "Plantas sin flores que se reproducen por esporas", "60-80", 18f, 24f, "2-3 días"),
            PlantTemplate(7, "Bonsáis", "🎋", "Árboles miniaturizados mediante técnicas", "50-70", 15f, 25f, "1-2 días"),
            PlantTemplate(8, "Palmeras", "🌴", "Plantas tropicales con tronco alto", "50-70", 20f, 30f, "7-14 días"),
            PlantTemplate(9, "Orquídeas", "🌺", "Familia de plantas con flores complejas", "50-70", 18f, 25f, "7-10 días"),
            PlantTemplate(10, "Plantas de Interior", "🏠", "Plantas adaptadas a condiciones de interior", "40-60", 18f, 24f, "5-10 días"),
            PlantTemplate(11, "Hortalizas", "🥕", "Plantas cultivadas para consumo alimenticio", "50-70", 15f, 25f, "3-5 días"),
            PlantTemplate(12, "Frutales", "🍎", "Plantas que producen frutos comestibles", "50-70", 15f, 30f, "5-14 días")
        )
    }
}
