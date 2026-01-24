package com.example.proyectofinal6to_ecobox.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.FamilyAdapter
import com.example.proyectofinal6to_ecobox.data.dao.FamiliaDao
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class FamiliesFragment : Fragment(R.layout.fragment_families) {

    private lateinit var adapter: FamilyAdapter
    private var currentUserId: Long = -1

    // Referencias a las estadísticas
    private lateinit var tvTotalFamilias: TextView
    private lateinit var tvTotalMiembros: TextView
    private lateinit var fabAddQuick: ExtendedFloatingActionButton
    private lateinit var cardStats: MaterialCardView
    private lateinit var rvFamilies: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener ID de usuario
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", AppCompatActivity.MODE_PRIVATE)
        currentUserId = prefs.getLong("user_id", -1)

        // Inicializar referencias
        tvTotalFamilias = view.findViewById(R.id.tvTotalFamilias)
        tvTotalMiembros = view.findViewById(R.id.tvTotalMiembros)
        fabAddQuick = view.findViewById(R.id.fabAddQuick)
        cardStats = view.findViewById(R.id.cardStats)
        rvFamilies = view.findViewById(R.id.rvFamilies)

        // Configurar FAB
        fabAddQuick.setOnClickListener {
            dialogoCrear()
        }

        // Configurar RecyclerView con animaciones
        rvFamilies.layoutManager = LinearLayoutManager(requireContext())
        rvFamilies.setHasFixedSize(true)

        adapter = FamilyAdapter(emptyList()) { familia ->
            navegarADetalleFamilia(familia)
        }

        rvFamilies.adapter = adapter

        // Botones principales
        view.findViewById<MaterialButton>(R.id.btnJoinFamily).setOnClickListener {
            dialogoUnirse()
        }

        view.findViewById<MaterialButton>(R.id.btnCreateFamily).setOnClickListener {
            dialogoCrear()
        }

        // Cargar datos con animación
        cargarDatosConAnimacion()
    }

    private fun cargarDatosConAnimacion() {
        // Mostrar loading state
        mostrarLoading(true)

        Thread {
            try {
                // Obtener datos en paralelo (si tu DAO lo soporta)
                val familias = FamiliaDao.obtenerFamiliasPorUsuario(currentUserId)
                val totalMiembros = FamiliaDao.obtenerTotalMiembros(currentUserId)

                requireActivity().runOnUiThread {
                    // Actualizar UI
                    actualizarEstadisticas(familias.size, totalMiembros)
                    adapter.updateList(familias)

                    // Mostrar/ocultar elementos según hay datos
                    manejarEstadoVacio(familias.isEmpty())

                    // Ocultar loading
                    mostrarLoading(false)

                    // Animar entrada
                    animarEntradaDatos()
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    mostrarLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar familias: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun actualizarEstadisticas(totalFamilias: Int, totalMiembros: Int) {
        // Animar contadores
        tvTotalFamilias.text = totalFamilias.toString()
        tvTotalMiembros.text = totalMiembros.toString()

        // Opcional: animación de contador
        if (totalFamilias > 0) {
            cardStats.isVisible = true
        }
    }

    private fun manejarEstadoVacio(estaVacio: Boolean) {
        val emptyStateView = view?.findViewById<View>(R.id.layoutEmptyState)

        if (estaVacio) {
            emptyStateView?.isVisible = true
            emptyStateView?.animate()?.alpha(1f)?.duration = 300
            rvFamilies.isVisible = false
            fabAddQuick.isVisible = true

            // Configurar botón del empty state
            emptyStateView?.findViewById<MaterialButton>(R.id.btnCreateFirst)?.setOnClickListener {
                dialogoCrear()
            }
        } else {
            emptyStateView?.animate()?.alpha(0f)?.withEndAction {
                emptyStateView?.isVisible = false
            }?.duration = 300
            rvFamilies.isVisible = true
            fabAddQuick.isVisible = false
        }
    }
    private fun mostrarLoading(mostrar: Boolean) {
        val loadingView = view?.findViewById<View>(R.id.loadingView)
        loadingView?.isVisible = mostrar

        // Animar entrada/salida
        loadingView?.animate()?.alpha(if (mostrar) 1f else 0f)?.duration = 300

        rvFamilies.isVisible = !mostrar
        cardStats.isVisible = !mostrar
    }

    private fun animarEntradaDatos() {
        // Animación simple de fade in
        rvFamilies.alpha = 0f
        rvFamilies.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun navegarADetalleFamilia(familia: com.example.proyectofinal6to_ecobox.data.model.FamiliaUi) {
        val fragment = FamilyDetailFragment.newInstance(familia.id, familia.nombre)

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment) // Asegúrate que este ID existe
            .addToBackStack("family_detail")
            .commit()
    }

    private fun dialogoCrear() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_family, null)
        val input = dialogView.findViewById<EditText>(R.id.etFamilyName)

        AlertDialog.Builder(requireContext())
            .setTitle("Crear Nueva Familia")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    crearFamiliaEnBD(nombre)
                } else {
                    Toast.makeText(requireContext(), "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .show()
    }

    private fun crearFamiliaEnBD(nombre: String) {
        // Mostrar loading
        mostrarLoading(true)

        Thread {
            val exito = FamiliaDao.crearFamilia(nombre, currentUserId)

            requireActivity().runOnUiThread {
                mostrarLoading(false)

                if (exito) {
                    Toast.makeText(
                        requireContext(),
                        "✅ Familia '$nombre' creada",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Recargar datos
                    cargarDatosConAnimacion()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "❌ Error al crear familia",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun dialogoUnirse() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_family, null)
        val input = dialogView.findViewById<EditText>(R.id.etFamilyCode)

        AlertDialog.Builder(requireContext())
            .setTitle("Unirse a Familia")
            .setView(dialogView)
            .setPositiveButton("Unirse") { _, _ ->
                val codigo = input.text.toString().uppercase().trim()
                if (codigo.isNotEmpty()) {
                    unirseFamiliaEnBD(codigo)
                } else {
                    Toast.makeText(requireContext(), "Ingresa un código", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .show()
    }

    private fun unirseFamiliaEnBD(codigo: String) {
        // Mostrar loading
        mostrarLoading(true)

        Thread {
            val resultado = FamiliaDao.unirseFamilia(codigo, currentUserId)

            requireActivity().runOnUiThread {
                mostrarLoading(false)

                when (resultado) {
                    "OK" -> {
                        Toast.makeText(
                            requireContext(),
                            "✅ Te has unido a la familia",
                            Toast.LENGTH_SHORT
                        ).show()
                        cargarDatosConAnimacion()
                    }
                    "Ya perteneces a esta familia" -> {
                        Toast.makeText(
                            requireContext(),
                            "⚠️ Ya perteneces a esta familia",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    "Código inválido" -> {
                        Toast.makeText(
                            requireContext(),
                            "❌ Código inválido",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            requireContext(),
                            "❌ Error: $resultado",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }.start()
    }



    override fun onResume() {
        super.onResume()
        // Recargar datos cuando vuelves a este fragment
        cargarDatosConAnimacion()
    }
}