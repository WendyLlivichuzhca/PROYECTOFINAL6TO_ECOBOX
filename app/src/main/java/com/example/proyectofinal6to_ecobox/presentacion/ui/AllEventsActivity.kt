package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.EventsAdapter
import com.example.proyectofinal6to_ecobox.data.model.EventoDAO
import com.example.proyectofinal6to_ecobox.data.network.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Response

class AllEventsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EventsAdapter
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_events)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

        userId = intent.getLongExtra("USER_ID", -1)

        initViews()
        loadEventsCloud()
    }

    private fun initViews() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = EventsAdapter(emptyList())
        recyclerView.adapter = adapter
    }

    private fun loadEventsCloud() {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        lifecycleScope.launch {
            try {
                // Usamos las alertas como flujo de eventos principal por ahora
                val response = RetrofitClient.instance.getAlerts("Token $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val alerts = response.body()!!.alertas
                    
                    val events = alerts.map { cloud ->
                        EventoDAO(
                            tipo = cloud.tipo,
                            planta = cloud.plantNombre,
                            fecha = cloud.creadaEn.replace("T", " ").split(".")[0],
                            descripcion = cloud.mensaje,
                            iconoTipo = when(cloud.tipo) {
                                "CRITICA" -> 2
                                "INFO" -> 3
                                else -> 1
                            }
                        )
                    }

                    if (events.isEmpty()) {
                        Toast.makeText(this@AllEventsActivity, "No hay eventos registrados", Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.updateData(events)
                    }
                } else {
                    Log.e("AllEvents", "Error API: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AllEvents", "Error red: ${e.message}")
                Toast.makeText(this@AllEventsActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
