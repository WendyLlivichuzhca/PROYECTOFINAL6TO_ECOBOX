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
import com.example.proyectofinal6to_ecobox.data.dao.FamiliaDao
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

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
        Thread {
            // Obtener datos de la familia
            val familia = FamiliaDao.obtenerFamiliaPorId(familiaId)
            val miembros = FamiliaDao.obtenerMiembrosPorFamilia(familiaId)
            val plantasCount = FamiliaDao.obtenerCantidadPlantasPorFamilia(familiaId)

            requireActivity().runOnUiThread {
                // Actualizar información de la familia
                familia?.let {
                    tvFamilyCode.text = "Código: ${it.codigo}"
                }

                tvMembersCount.text = "${miembros.size} miembros"
                tvPlantsCount.text = "$plantasCount plantas"

                // Actualizar lista de miembros
                if (miembros.isNotEmpty()) {
                    rvMembers.adapter = MemberAdapter(miembros)
                    layoutEmptyState.visibility = View.GONE
                    rvMembers.visibility = View.VISIBLE
                } else {
                    layoutEmptyState.visibility = View.VISIBLE
                    rvMembers.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun compartirFamilia() {
        val familia = FamiliaDao.obtenerFamiliaPorId(familiaId)
        familia?.let {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT,
                    "¡Únete a mi familia en EcoBox!\n" +
                            "Familia: ${it.nombre}\n" +
                            "Código: ${it.codigo}\n\n" +
                            "Descarga la app: [link de tu app]")
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir familia"))
        }
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