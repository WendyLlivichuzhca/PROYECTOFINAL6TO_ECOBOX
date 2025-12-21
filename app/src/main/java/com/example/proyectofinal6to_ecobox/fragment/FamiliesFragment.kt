package com.example.proyectofinal6to_ecobox.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.FamilyAdapter
import com.example.proyectofinal6to_ecobox.data.dao.FamiliaDao
import com.google.android.material.button.MaterialButton

class FamiliesFragment : Fragment(R.layout.fragment_families) {

    private lateinit var adapter: FamilyAdapter
    private var currentUserId: Long = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recuperar ID de usuario
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", AppCompatActivity.MODE_PRIVATE)
        currentUserId = prefs.getLong("user_id", -1)

        // Configurar RecyclerView
        val rvFamilies: RecyclerView = view.findViewById(R.id.rvFamilies)
        rvFamilies.layoutManager = LinearLayoutManager(requireContext())

        // --- CORRECCIÓN AQUÍ: UNA SOLA INSTANCIA DEL ADAPTADOR ---
        adapter = FamilyAdapter(emptyList()) { familia ->
            // AL DAR CLICK EN UNA FAMILIA:
            val bundle = Bundle()
            bundle.putLong("familia_id", familia.id)
            bundle.putString("familia_nombre", familia.nombre)

            // Navegar al detalle
            androidx.navigation.Navigation.findNavController(view)
                .navigate(R.id.nav_family_detail, bundle)
        }

        // Asignar el adaptador al RecyclerView
        rvFamilies.adapter = adapter
        // ---------------------------------------------------------

        // Botones
        view.findViewById<MaterialButton>(R.id.btnJoinFamily).setOnClickListener { dialogoUnirse() }
        view.findViewById<MaterialButton>(R.id.btnCreateFamily).setOnClickListener { dialogoCrear() }

        // Cargar datos
        cargarFamilias()
    }

    private fun cargarFamilias() {
        Thread {
            val lista = FamiliaDao.obtenerFamiliasPorUsuario(currentUserId)
            activity?.runOnUiThread {
                adapter.updateList(lista)
            }
        }.start()
    }

    private fun dialogoCrear() {
        val input = EditText(context)
        input.hint = "Nombre de la familia"
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60, 20, 60, 0)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Crear Nueva Familia")
            .setView(container)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = input.text.toString()
                if (nombre.isNotEmpty()) crearFamiliaEnBD(nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearFamiliaEnBD(nombre: String) {
        Thread {
            val exito = FamiliaDao.crearFamilia(nombre, currentUserId)
            activity?.runOnUiThread {
                if (exito) {
                    Toast.makeText(context, "Familia creada", Toast.LENGTH_SHORT).show()
                    cargarFamilias()
                } else {
                    Toast.makeText(context, "Error al crear", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun dialogoUnirse() {
        val input = EditText(context)
        input.hint = "Código (Ej: X9Y8Z7)"
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60, 20, 60, 0)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Unirse a Familia")
            .setView(container)
            .setPositiveButton("Unirse") { _, _ ->
                val codigo = input.text.toString().uppercase().trim()
                if (codigo.isNotEmpty()) unirseFamiliaEnBD(codigo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun unirseFamiliaEnBD(codigo: String) {
        Thread {
            val resp = FamiliaDao.unirseFamilia(codigo, currentUserId)
            activity?.runOnUiThread {
                if (resp == "OK") {
                    Toast.makeText(context, "Te has unido", Toast.LENGTH_SHORT).show()
                    cargarFamilias()
                } else {
                    Toast.makeText(context, resp, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}