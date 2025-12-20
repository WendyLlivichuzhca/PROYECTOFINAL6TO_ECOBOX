package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.EventsAdapter
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao

class AllEventsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventsAdapter
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_events)

        // Barra de estado transparente (Opcional, igual que en History)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        // Obtener ID del usuario
        userId = intent.getLongExtra("USER_ID", -1)

        initViews()
        loadAllEvents()
    }

    private fun initViews() {
        // Botón Atrás
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish() // Cierra la actividad y vuelve atrás
        }

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializar adaptador vacío
        adapter = EventsAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun loadAllEvents() {
        if (userId == -1L) return

        Thread {
            try {
                // AQUÍ: Necesitas un método en tu DAO que traiga TODOS (sin límite)
                // Si no tienes uno específico, usa el mismo de recientes pero ponle un límite alto (ej. 100)
                val events = PlantaDao.obtenerEventosRecientesFamiliar(userId, 50, -1)

                runOnUiThread {
                    if (events.isEmpty()) {
                        Toast.makeText(this, "No hay eventos registrados", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        adapter.updateData(events)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}