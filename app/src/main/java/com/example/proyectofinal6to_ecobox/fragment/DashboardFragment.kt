package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.PlantaDao
import com.example.proyectofinal6to_ecobox.presentacion.ui.AlertsActivity
import com.example.proyectofinal6to_ecobox.presentacion.ui.ChatbotBottomSheet

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener User ID
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getLong("user_id", -1)
        val userEmail = prefs.getString("user_email", "") ?: ""

        // Configurar Saludo
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val nombre = if (userEmail.isNotEmpty()) userEmail.substringBefore("@")
            .replaceFirstChar { it.uppercase() } else "Usuario"
        tvWelcome.text = "Hola, $nombre"

        // Cargar Estadísticas
        cargarEstadisticas(view, userId)

        // Listeners de Alertas
        val btnAlerts = view.findViewById<View>(R.id.btnAlerts)
        val cardAlertCritical = view.findViewById<View>(R.id.cardAlertCritical)

        val goToAlerts = View.OnClickListener {
            startActivity(Intent(requireContext(), AlertsActivity::class.java))
        }
        btnAlerts.setOnClickListener(goToAlerts)
        cardAlertCritical.setOnClickListener(goToAlerts)

        // FAB Chatbot
        val fabChatbot = view.findViewById<View>(R.id.fabChatbot)
        fabChatbot.setOnClickListener {
            val chatbotSheet = ChatbotBottomSheet.newInstance()
            chatbotSheet.show(childFragmentManager, ChatbotBottomSheet.TAG)
        }
    }

    private fun cargarEstadisticas(view: View, userId: Long) {
        // Referencias a las vistas dentro del include
        val tvTotal = view.findViewById<View>(R.id.cardStatTotal).findViewById<TextView>(R.id.tvCount)
        val tvHealthy = view.findViewById<View>(R.id.cardStatHealthy).findViewById<TextView>(R.id.tvCount)
        val tvCritical = view.findViewById<View>(R.id.cardStatCritical).findViewById<TextView>(R.id.tvCount)

        // Textos fijos de las tarjetas
        view.findViewById<View>(R.id.cardStatTotal).findViewById<TextView>(R.id.tvLabel).text = "Total"
        view.findViewById<View>(R.id.cardStatHealthy).findViewById<TextView>(R.id.tvLabel).text = "Sanas"
        view.findViewById<View>(R.id.cardStatCritical).findViewById<TextView>(R.id.tvLabel).text = "Críticas"

        val cardAlertCritical = view.findViewById<View>(R.id.cardAlertCritical)
        val tvAlertMessage = view.findViewById<TextView>(R.id.tvAlertMessage)

        Thread {
            val stats = PlantaDao.obtenerEstadisticas(userId) // [Total, Sanas, Críticas]
            activity?.runOnUiThread {
                tvTotal.text = stats[0].toString()
                tvHealthy.text = stats[1].toString()
                tvCritical.text = stats[2].toString()

                val numCriticas = stats[2]
                if (numCriticas > 0) {
                    cardAlertCritical.visibility = View.VISIBLE
                    tvAlertMessage.text = if (numCriticas == 1) "1 planta necesita atención" else "$numCriticas plantas necesitan atención"
                } else {
                    cardAlertCritical.visibility = View.GONE
                }
            }
        }.start()
    }
}