package com.example.proyectofinal6to_ecobox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.FamilyResponse
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.example.proyectofinal6to_ecobox.presentacion.adapter.FamiliesApiAdapter
import com.example.proyectofinal6to_ecobox.presentacion.ui.FamilyManagementActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.launch

class FamiliesFragment : Fragment(R.layout.fragment_families) {

    private lateinit var adapter: FamiliesApiAdapter
    private var authToken: String? = null
    private var selectedFamily: FamilyResponse? = null

    // Referencias a las vistas
    private lateinit var tvTotalFamilias: TextView
    private lateinit var tvTotalMiembros: TextView
    private lateinit var fabAddQuick: ExtendedFloatingActionButton
    private lateinit var cardStats: MaterialCardView
    private lateinit var rvFamilies: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener token de autenticación
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", AppCompatActivity.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

        if (authToken == null) {
            Toast.makeText(requireContext(), "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        // Inicializar vistas
        tvTotalFamilias = view.findViewById(R.id.tvTotalFamilias)
        tvTotalMiembros = view.findViewById(R.id.tvTotalMiembros)
        fabAddQuick = view.findViewById(R.id.fabAddQuick)
        cardStats = view.findViewById(R.id.cardStats)
        rvFamilies = view.findViewById(R.id.rvFamilies)

        // Configurar RecyclerView
        rvFamilies.layoutManager = LinearLayoutManager(requireContext())
        rvFamilies.setHasFixedSize(true)

        adapter = FamiliesApiAdapter(emptyList()) { familia ->
            navegarADetalleFamilia(familia)
        }

        rvFamilies.adapter = adapter

        // Configurar botones
        view.findViewById<MaterialButton>(R.id.btnJoinFamily).setOnClickListener {
            showJoinFamilyDialog()
        }

        view.findViewById<MaterialButton>(R.id.btnCreateFamily).setOnClickListener {
            showCreateFamilyDialog()
        }

        view.findViewById<MaterialButton>(R.id.btnCreateFirst)?.setOnClickListener {
            showCreateFamilyDialog()
        }

        fabAddQuick.setOnClickListener {
            showCreateFamilyDialog()
        }

        // Cargar familias desde la API
        cargarFamiliasDesdeAPI()
    }

    private fun showJoinFamilyDialog() {
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_join_family, null)
        val etCode = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFamilyCode)

        builder.setView(dialogView)
            .setTitle("Unirse a una Familia")
            .setPositiveButton("Unirse") { dialog, _ ->
                val code = etCode.text.toString().trim()
                if (code.isNotEmpty()) {
                    joinFamily(code)
                } else {
                    Toast.makeText(requireContext(), "Ingresa un código", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showCreateFamilyDialog() {
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_create_family, null)
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFamilyName)

        builder.setView(dialogView)
            .setTitle("Crear Nueva Familia")
            .setPositiveButton("Crear") { dialog, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    createFamily(name)
                } else {
                    Toast.makeText(requireContext(), "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun joinFamily(code: String) {
        mostrarLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.joinFamily("Token $authToken", mapOf("codigo" to code))
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Te has unido a la familia", Toast.LENGTH_SHORT).show()
                    cargarFamiliasDesdeAPI()
                } else {
                    Toast.makeText(requireContext(), "Código inválido o familia no encontrada", Toast.LENGTH_SHORT).show()
                    mostrarLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_SHORT).show()
                mostrarLoading(false)
            }
        }
    }

    private fun createFamily(name: String) {
        mostrarLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.createFamily("Token $authToken", mapOf("nombre" to name))
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Familia '${name}' creada", Toast.LENGTH_SHORT).show()
                    cargarFamiliasDesdeAPI()
                } else {
                    Toast.makeText(requireContext(), "Error al crear familia", Toast.LENGTH_SHORT).show()
                    mostrarLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_SHORT).show()
                mostrarLoading(false)
            }
        }
    }

    private fun cargarFamiliasDesdeAPI() {
        mostrarLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getFamilies("Token $authToken")
                
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val familias = response.body()!!
                    
                    // Actualizar estadísticas
                    val totalMiembros = familias.sumOf { it.cantidad_miembros }
                    actualizarEstadisticas(familias.size, totalMiembros)
                    
                    // Actualizar lista
                    adapter.updateList(familias)
                    
                    // Manejar estado vacío
                    manejarEstadoVacio(false)
                    
                    Log.d("FamiliesFragment", "✅ ${familias.size} familias cargadas")
                } else {
                    Log.e("FamiliesFragment", "Error: ${response.code()}")
                    manejarEstadoVacio(true)
                }
                
                mostrarLoading(false)
                
            } catch (e: Exception) {
                Log.e("FamiliesFragment", "Error cargando familias", e)
                mostrarLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Error al cargar familias: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                manejarEstadoVacio(true)
            }
        }
    }

    private fun actualizarEstadisticas(totalFamilias: Int, totalMiembros: Int) {
        tvTotalFamilias.text = totalFamilias.toString()
        tvTotalMiembros.text = totalMiembros.toString()

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
        } else {
            emptyStateView?.animate()?.alpha(0f)?.withEndAction {
                emptyStateView?.isVisible = false
            }?.duration = 300
            rvFamilies.isVisible = true
        }
    }

    private fun mostrarLoading(mostrar: Boolean) {
        val loadingView = view?.findViewById<View>(R.id.loadingView)
        loadingView?.isVisible = mostrar
        loadingView?.animate()?.alpha(if (mostrar) 1f else 0f)?.duration = 300

        rvFamilies.isVisible = !mostrar
        cardStats.isVisible = !mostrar
    }

    private fun navegarADetalleFamilia(familia: FamilyResponse) {
        // Abrir FamilyManagementActivity con los datos de la familia seleccionada
        val intent = Intent(requireContext(), FamilyManagementActivity::class.java)
        intent.putExtra("family_id", familia.id)
        intent.putExtra("family_name", familia.nombre)
        intent.putExtra("family_code", familia.codigo_invitacion)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Recargar familias cuando vuelves al fragmento
        if (authToken != null) {
            cargarFamiliasDesdeAPI()
        }
    }
}
