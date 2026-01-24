package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.AlertPlantaAdapter
import com.example.proyectofinal6to_ecobox.data.dao.AlertaPlantaDao
import com.example.proyectofinal6to_ecobox.data.model.AlertaPlanta
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertaPlantasFragment : Fragment() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var adapter: AlertPlantaAdapter
    private lateinit var tvCritical: TextView
    private lateinit var tvWarning: TextView
    private lateinit var tvResolved: TextView
    private lateinit var llEmpty: View
    
    private var token: String = ""
    private val alertsList = mutableListOf<AlertaPlanta>()
    
    // Timer para refresco automático cada 30 segundos
    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadAlerts()
            refreshHandler.postDelayed(this, 30000) // 30 segundos
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alerta_plantas, container, false)
    }

    override fun onStart() {
        super.onStart()
        // Iniciar refresco automático cuando el usuario ve el fragmento
        refreshHandler.post(refreshRunnable)
    }

    override fun onStop() {
        super.onStop()
        // Detener para no gastar batería innecesariamente
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        token = prefs.getString("auth_token", "") ?: ""

        initViews(view)
        setupRecyclerView()
        loadAlerts()
    }

    private fun initViews(view: View) {
        rvAlerts = view.findViewById(R.id.rvPlantAlerts)
        tvCritical = view.findViewById(R.id.tvCountCritical)
        tvWarning = view.findViewById(R.id.tvCountWarning)
        tvResolved = view.findViewById(R.id.tvCountResolved)
        llEmpty = view.findViewById(R.id.llEmptyAlerts)

        view.findViewById<MaterialButton>(R.id.btnRefreshAlerts).setOnClickListener { 
            Toast.makeText(context, "Actualizando...", Toast.LENGTH_SHORT).show()
            loadAlerts() 
        }
        
        view.findViewById<MaterialButton>(R.id.btnTestNotification).setOnClickListener { 
            simulateTestNotification()
        }

        view.findViewById<MaterialButton>(R.id.btnMarkAllRead).setOnClickListener {
            Toast.makeText(context, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun simulateTestNotification() {
        if (token.isEmpty()) return

        // 1. Mostrar notificación inmediata para probar UI del celular
        com.example.proyectofinal6to_ecobox.utils.NotificationHelper.showNotification(
            requireContext(),
            999,
            "🧪 Prueba EcoBox",
            "¡Funciona! Así verás las alertas de tus plantas en tu celular."
        )

        // 2. Crear alerta real en el servidor para probar el "Vigilante" (Worker)
        // Guardamos el ID actual para que el Worker de fondo tenga algo que detectar
        CoroutineScope(Dispatchers.IO).launch {
            val success = AlertaPlantaDao.crearAlertaPrueba(token)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Alerta de prueba creada en el servidor 📡", Toast.LENGTH_SHORT).show()
                    loadAlerts()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AlertPlantaAdapter(alertsList, 
            onResolveClick = { alert -> resolveAlert(alert) },
            onItemClick = { alert -> markAsRead(alert) }
        )
        rvAlerts.layoutManager = LinearLayoutManager(context)
        rvAlerts.adapter = adapter
    }

    private fun loadAlerts() {
        if (token.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            val alerts = AlertaPlantaDao.obtenerAlertasDesdeApi(token)
            withContext(Dispatchers.Main) {
                val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
                val lastNotifiedId = prefs.getLong("last_alert_notified_id", -1L)

                // Si la App está abierta y detectamos una nueva alerta (ID mayor al último notificado)
                if (alerts.isNotEmpty()) {
                    val newestAlerts = alerts.filter { it.id > lastNotifiedId && !it.leida && !it.resuelta }
                    
                    if (newestAlerts.isNotEmpty()) {
                        Log.d("AlertaFragment", "🔔 Detectadas ${newestAlerts.size} nuevas alertas para notificar")
                        
                        // Notificar la más reciente
                        val mostRecent = newestAlerts.maxByOrNull { it.id }!!
                        val tipo = mostRecent.tipoAlerta.uppercase()
                        
                        // Notificamos si es CRITICA o ADVERTENCIA (como en la web)
                        if (tipo == "CRITICA" || tipo == "ADVERTENCIA") {
                            com.example.proyectofinal6to_ecobox.utils.NotificationHelper.showNotification(
                                requireContext(), 
                                mostRecent.id.toInt(), 
                                "⚠️ EcoBox: ${mostRecent.titulo}", 
                                mostRecent.mensaje
                            )
                        }

                        // Actualizar el puntero global para que el Worker no la repita
                        prefs.edit().putLong("last_alert_notified_id", mostRecent.id).apply()
                    }
                }

                alertsList.clear()
                alertsList.addAll(alerts)
                adapter.updateList(alertsList)
                updateStats()
                
                llEmpty.visibility = if (alertsList.isEmpty()) View.VISIBLE else View.GONE
                rvAlerts.visibility = if (alertsList.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun updateStats() {
        val critical = alertsList.count { it.tipoAlerta.uppercase() == "CRITICA" && !it.resuelta }
        val warning = alertsList.count { it.tipoAlerta.uppercase() == "ADVERTENCIA" && !it.resuelta }
        val resolved = alertsList.count { it.resuelta }

        tvCritical.text = critical.toString()
        tvWarning.text = warning.toString()
        tvResolved.text = resolved.toString()
    }

    private fun markAsRead(alert: AlertaPlanta) {
        if (alert.leida) return
        
        CoroutineScope(Dispatchers.IO).launch {
            val success = AlertaPlantaDao.marcarLeida(token, alert.id)
            if (success) {
                withContext(Dispatchers.Main) {
                    loadAlerts() // Recargar para actualizar UI
                }
            }
        }
    }

    private fun resolveAlert(alert: AlertaPlanta) {
        CoroutineScope(Dispatchers.IO).launch {
            val success = AlertaPlantaDao.resolverAlerta(token, alert.id)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "¡Problema resuelto! ✅", Toast.LENGTH_SHORT).show()
                    loadAlerts()
                } else {
                    Toast.makeText(context, "Error al resolver", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
