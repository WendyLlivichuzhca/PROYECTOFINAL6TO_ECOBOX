package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.NotificacionesAdapter
import com.example.proyectofinal6to_ecobox.data.network.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.Response

class AlertsActivity : AppCompatActivity() {

    private lateinit var rvNotif: RecyclerView
    private lateinit var adapter: NotificacionesAdapter
    private lateinit var txtCountUnread: TextView
    private lateinit var txtCountTotal: TextView
    private lateinit var btnMarcarTodas: MaterialButton
    
    private var allNotifications: List<UserNotificationResponse> = emptyList()
    private var currentFilter: String = "ALL" // ALL, UNREAD, READ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        initViews()
        loadNotifications()
    }

    private fun initViews() {
        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // Recycler
        rvNotif = findViewById(R.id.recyclerNotificaciones)
        rvNotif.layoutManager = LinearLayoutManager(this)
        
        adapter = NotificacionesAdapter(emptyList()) { notif ->
            markAsRead(notif.id)
        }
        rvNotif.adapter = adapter

        // Header
        txtCountUnread = findViewById(R.id.txtCountUnread)
        txtCountTotal = findViewById(R.id.txtCountTotal)
        btnMarcarTodas = findViewById(R.id.btnMarcarTodas)
        
        btnMarcarTodas.setOnClickListener {
            markAllAsRead()
        }

        // Filters
        findViewById<MaterialButton>(R.id.btnFilterAll).setOnClickListener { applyFilter("ALL") }
        findViewById<MaterialButton>(R.id.btnFilterUnread).setOnClickListener { applyFilter("UNREAD") }
        findViewById<MaterialButton>(R.id.btnFilterRead).setOnClickListener { applyFilter("READ") }
    }

    private fun loadNotifications() {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserNotifications("Token $token")
                if (response.isSuccessful && response.body() != null) {
                    allNotifications = response.body()!!
                    
                    updateStats()
                    applyFilter(currentFilter)
                } else {
                    Log.e("AlertsActivity", "Error loading notifications: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AlertsActivity", "Network error", e)
                Toast.makeText(this@AlertsActivity, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStats() {
        val total = allNotifications.size
        val unread = allNotifications.count { !it.leida }
        
        txtCountTotal.text = "Total: $total"
        txtCountUnread.text = "$unread sin leer"
        
        btnMarcarTodas.visibility = if (unread > 0) View.VISIBLE else View.GONE
    }

    private fun applyFilter(filter: String) {
        currentFilter = filter
        val filteredList = when (filter) {
            "UNREAD" -> allNotifications.filter { !it.leida }
            "READ" -> allNotifications.filter { it.leida }
            else -> allNotifications
        }
        adapter.actualizarLista(filteredList)
    }

    private fun markAsRead(notificationId: Long) {
        val token = getSharedPreferences("ecobox_prefs", MODE_PRIVATE).getString("auth_token", null) ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.markUserNotificationAsRead("Token $token", notificationId)
                if (response.isSuccessful) {
                    // Actualizar localmente para feedback inmediato
                    allNotifications = allNotifications.map {
                        if (it.id == notificationId) it.copy(leida = true) else it
                    }
                    updateStats()
                    applyFilter(currentFilter)
                }
            } catch (e: Exception) {
                Log.e("AlertsActivity", "Error marking as read", e)
            }
        }
    }

    private fun markAllAsRead() {
        val unreadOnes = allNotifications.filter { !it.leida }
        if (unreadOnes.isEmpty()) return

        Toast.makeText(this, "Marcando todas como leídas...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            // Podríamos enviarlas en paralelo o usar un endpoint masivo si existiera
            // Por ahora, para asegurar paridad web, simulamos el marcado local masivo
            // y llamamos a la API para cada una (o al menos refrescamos al final)
            
            unreadOnes.forEach { 
                try {
                    val token = getSharedPreferences("ecobox_prefs", MODE_PRIVATE).getString("auth_token", null)
                    RetrofitClient.instance.markUserNotificationAsRead("Token $token", it.id)
                } catch (e: Exception) { }
            }
            
            loadNotifications() // Recargar al terminar
        }
    }
}
