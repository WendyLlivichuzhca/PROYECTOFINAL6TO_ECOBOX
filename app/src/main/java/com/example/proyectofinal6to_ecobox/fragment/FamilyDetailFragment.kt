package com.example.proyectofinal6to_ecobox.fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.MemberAdapter
import com.example.proyectofinal6to_ecobox.data.dao.FamiliaDao

class FamilyDetailFragment : Fragment(R.layout.fragment_families) {

    private var familiaId: Long = -1
    private var nombreFamilia: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Recibir datos del bundle
        arguments?.let {
            familiaId = it.getLong("familia_id")
            nombreFamilia = it.getString("familia_nombre") ?: "Miembros"
        }

        // 2. Referencias de UI
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack) // Asegúrate de añadirlo al XML
        val containerButtons = view.findViewById<View>(R.id.containerButtons) // El LinearLayout de Unirse/Crear

        // 3. Personalización de la Cabecera
        tvTitle.text = "Miembros"
        tvSubtitle.text = "Hogar: $nombreFamilia"

        // Configuración de visibilidad
        containerButtons?.visibility = View.GONE
        btnBack?.visibility = View.VISIBLE

        // Acción del botón atrás
        btnBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 4. Configurar RecyclerView
        val rv = view.findViewById<RecyclerView>(R.id.rvFamilies)
        rv.layoutManager = LinearLayoutManager(context)

        cargarMiembros(rv)
    }

    private fun cargarMiembros(rv: RecyclerView) {
        Thread {
            val lista = FamiliaDao.obtenerMiembrosPorFamilia(familiaId)
            activity?.runOnUiThread {
                rv.adapter = MemberAdapter(lista)
            }
        }.start()
    }
}