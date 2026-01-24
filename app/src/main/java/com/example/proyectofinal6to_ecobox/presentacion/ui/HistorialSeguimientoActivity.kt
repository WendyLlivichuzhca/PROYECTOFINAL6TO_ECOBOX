package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.data.adapter.SeguimientoAdapter
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class HistorialSeguimientoActivity : AppCompatActivity() {

    private lateinit var rvHistorial: RecyclerView
    private lateinit var tvPlantaNombreHeader: TextView
    private lateinit var tvContadorRegistrosHeader: TextView
    private lateinit var tvRegistrosCount: TextView
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private lateinit var btnBack: ImageButton

    private var plantaId: Long = -1
    private var plantaNombre: String = ""
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_seguimiento)

        // Obtener datos del Intent
        plantaId = intent.getLongExtra("PLANTA_ID", -1)
        plantaNombre = intent.getStringExtra("PLANTA_NOMBRE") ?: "Planta"
        userId = intent.getLongExtra("USER_ID", -1)

        if (plantaId == -1L || userId == -1L) {
            Toast.makeText(this, "Error: Datos de planta no disponibles", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        cargarHistorial()
    }

    private fun initViews() {
        rvHistorial = findViewById(R.id.rvHistorial)
        fabAdd = findViewById(R.id.fabAdd)
        btnBack = findViewById(R.id.btnBack)

        // IMPORTANTE: Primero necesitas agregar estos IDs en tu header_historial.xml
        // Si ya los agregaste según te indiqué antes, podrás acceder así:

        // 1. Nombre de la planta en el header superior
        tvPlantaNombreHeader = findViewById(R.id.tvPlantaNombre)

        // 2. Contador en el header superior (ej: "Seguimiento visual (5 seguimientos)")
        tvContadorRegistrosHeader = findViewById(R.id.tvContadorRegistros)

        // 3. Contador en la sección "Historial de seguimientos" (ej: "5 registros")
        tvRegistrosCount = findViewById(R.id.tvRegistrosCount)

        // Si los IDs no están, comenta estas líneas y usa las alternativas abajo

        btnBack.setOnClickListener { onBackPressed() }

        fabAdd.setOnClickListener {
            // Obtener la especie de la planta para pasarla a la siguiente actividad
            val planta = PlantaDao.obtenerPlantaPorId(plantaId, userId)
            val especie = planta?.especie ?: "Sin especie"

            // Abrir actividad para agregar nuevo seguimiento
            val intent = Intent(this, AgregarSeguimientoActivity::class.java).apply {
                putExtra("PLANTA_ID", plantaId)
                putExtra("PLANTA_NOMBRE", plantaNombre)
                putExtra("PLANTA_ESPECIE", especie)
                putExtra("USER_ID", userId)
            }
            startActivityForResult(intent, REQUEST_ADD_SEGUIMIENTO)
        }
    }

    private fun setupRecyclerView() {
        rvHistorial.layoutManager = LinearLayoutManager(this)
        rvHistorial.setHasFixedSize(true)
    }

    private fun cargarHistorial() {
        // Verificar acceso primero
        if (!PlantaDao.verificarAccesoPlanta(userId, plantaId)) {
            Toast.makeText(this, "No tienes acceso a esta planta", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Obtener historial de seguimiento
        val historial = PlantaDao.obtenerSeguimientoPlanta(plantaId)

        Log.d("Historial", "Registros encontrados: ${historial.size}")

        // Configurar adapter
        val adapter = SeguimientoAdapter(historial) { seguimiento ->
            // Click en un registro
            mostrarDetalleSeguimiento(seguimiento)
        }
        rvHistorial.adapter = adapter

        // Actualizar contador en el header
        actualizarHeader(historial.size)
    }

    private fun actualizarHeader(totalRegistros: Int) {
        // Opción 1: Si tienes los IDs correctos en tu XML
        try {
            tvPlantaNombreHeader.text = plantaNombre
            tvContadorRegistrosHeader.text = "Seguimiento visual ($totalRegistros seguimientos)"
            tvRegistrosCount.text = "$totalRegistros registros"
        } catch (e: Exception) {
            // Opción 2: Si no tienes los IDs, puedes usar ViewBinding o buscar alternativas
            Log.e("Historial", "Error al actualizar header: ${e.message}")

            // Alternativa: Usar findViewById con los IDs que deberías tener
            actualizarHeaderAlternativo(totalRegistros)
        }
    }

    private fun actualizarHeaderAlternativo(totalRegistros: Int) {
        // Buscar los views de manera alternativa
        // NOTA: Necesitas agregar estos IDs en tu XML primero

        // Buscar TextView del nombre de la planta
        val tvNombre = findViewById<TextView?>(R.id.tvPlantaNombre)
        tvNombre?.text = plantaNombre

        // Buscar TextView del contador en el header
        val tvContador = findViewById<TextView?>(R.id.tvContadorRegistros)
        tvContador?.text = "Seguimiento visual ($totalRegistros seguimientos)"

        // Buscar TextView del contador en la sección de historial
        val tvCount = findViewById<TextView?>(R.id.tvRegistrosCount)
        tvCount?.text = "$totalRegistros registros"
    }

    private fun mostrarDetalleSeguimiento(seguimiento: Map<String, Any>) {
        val estado = seguimiento["estado"] as? String ?: ""
        val observaciones = seguimiento["observaciones"] as? String ?: ""
        val fecha = seguimiento["fecha_formateada"] as? String ?: ""
        val tieneImagen = (seguimiento["imagen"] as? String ?: "").isNotEmpty()

        // Mostrar diálogo con detalles
        val dialog = AlertDialog.Builder(this)
            .setTitle("📋 Detalles del Seguimiento")
            .setMessage(
                "🌿 **Estado:** $estado\n\n" +
                        "📅 **Fecha:** $fecha\n\n" +
                        "📝 **Observaciones:**\n$observaciones\n\n" +
                        if (tieneImagen) "🖼️ **Con imagen adjunta**" else "📸 **Sin imagen**"
            )
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Ver imagen") { dialog, _ ->
                if (tieneImagen) {
                    val imagenPath = seguimiento["imagen"] as? String
                    if (imagenPath != null && imagenPath.isNotEmpty()) {
                        // Aquí podrías abrir una actividad para ver la imagen en grande
                        Toast.makeText(this, "Abrir imagen: $imagenPath", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_SEGUIMIENTO && resultCode == RESULT_OK) {
            // Recargar historial después de agregar nuevo registro
            cargarHistorial()
            Toast.makeText(this, "✅ Seguimiento agregado exitosamente", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_ADD_SEGUIMIENTO = 1001
    }
}