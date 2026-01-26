package com.example.proyectofinal6to_ecobox.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.MemberAdapter
import com.example.proyectofinal6to_ecobox.data.model.MiembroUi
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity

class FamilyDetailFragment : Fragment(R.layout.fragment_family_detail) {

    private lateinit var rvMembers: RecyclerView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvFamilyCode: TextView
    private lateinit var tvMembersCount: TextView
    private lateinit var tvPlantsCount: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var fabInvite: ExtendedFloatingActionButton
    private lateinit var btnShare: MaterialCardView

    private var familiaId: Long = -1
    private var nombreFamilia: String = ""

    companion object {
        private const val ARG_FAMILIA_ID = "familia_id"
        private const val ARG_FAMILIA_NOMBRE = "familia_nombre"

        fun newInstance(familiaId: Long, nombreFamilia: String): FamilyDetailFragment {
            return FamilyDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_FAMILIA_ID, familiaId)
                    putString(ARG_FAMILIA_NOMBRE, nombreFamilia)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            familiaId = it.getLong(ARG_FAMILIA_ID, -1)
            nombreFamilia = it.getString(ARG_FAMILIA_NOMBRE) ?: "Familia"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias de UI
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)
        tvFamilyCode = view.findViewById(R.id.tvFamilyCode)
        tvMembersCount = view.findViewById(R.id.tvMembersCount)
        tvPlantsCount = view.findViewById(R.id.tvPlantsCount)
        val btnBack = view.findViewById<MaterialCardView>(R.id.btnBack)
        btnShare = view.findViewById(R.id.btnShare)
        rvMembers = view.findViewById(R.id.rvMembers)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        fabInvite = view.findViewById(R.id.fabInvite)

        // Configurar UI inicial
        tvTitle.text = "👨‍👩‍👧‍👦 Miembros"
        tvSubtitle.text = "Hogar: $nombreFamilia"

        // Botón Atrás
        btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Botón Compartir
        btnShare.setOnClickListener {
            compartirFamilia()
        }

        // Botón Invitar
        fabInvite.setOnClickListener {
            invitarMiembro()
        }

        // Configurar RecyclerView
        rvMembers.layoutManager = LinearLayoutManager(requireContext())
        rvMembers.setHasFixedSize(true)

        // Cargar datos
        if (familiaId != -1L) {
            cargarDatosFamilia()
        }
    }

    private fun cargarDatosFamilia() {
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", AppCompatActivity.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getFamilyDetail("Token $token", familiaId)

                if (response.isSuccessful && response.body() != null) {
                    val familia = response.body()!!
                    
                    // Actualizar información de la familia
                    tvFamilyCode.text = "Código: ${familia.codigo_invitacion ?: "N/A"}"
                    tvMembersCount.text = "${familia.cantidad_miembros} miembros"
                    tvPlantsCount.text = "${familia.cantidad_plantas} plantas"

                    // Mapear miembros de la API a MiembroUi
                    val miembros = familia.miembros?.map { m ->
                        MiembroUi(
                            m.usuario_info.id,
                            m.usuario_info.first_name ?: m.usuario_info.username,
                            m.usuario_info.email,
                            if (m.es_administrador) "Administrador" else "Miembro",
                            m.es_administrador,
                            "Recientemente",
                            true,
                            generarColorAvatar(m.usuario_info.first_name ?: m.usuario_info.username)
                        )
                    } ?: emptyList()

                    // Actualizar lista de miembros
                    if (miembros.isNotEmpty()) {
                        rvMembers.adapter = MemberAdapter(miembros)
                        layoutEmptyState.visibility = View.GONE
                        rvMembers.visibility = View.VISIBLE
                    } else {
                        layoutEmptyState.visibility = View.VISIBLE
                        rvMembers.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(requireContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generarColorAvatar(nombre: String): String {
        val colors = listOf("#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#F44336", "#00BCD4", "#795548")
        val index = Math.abs(nombre.hashCode()) % colors.size
        return colors[index]
    }

    private fun compartirFamilia() {
        val shareText = "¡Únete a mi familia en EcoBox!\n" +
                "Familia: $nombreFamilia\n" +
                "Código: ${tvFamilyCode.text.toString().replace("Código: ", "")}\n\n" +
                "Descarga la app: [link de tu app]"
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir familia"))
    }

    private fun invitarMiembro() {
        // Mostrar diálogo para invitar por email o código
        val input = EditText(requireContext())
        input.hint = "Email del invitado"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        AlertDialog.Builder(requireContext())
            .setTitle("Invitar Miembro")
            .setMessage("Ingresa el email de la persona que quieres invitar")
            .setView(input)
            .setPositiveButton("Enviar Invitación") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    enviarInvitacion(email)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarInvitacion(email: String) {
        // Aquí implementarías el envío real de invitación
        Toast.makeText(requireContext(), "Invitación enviada a $email", Toast.LENGTH_SHORT).show()
    }
}