package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.FamilyMemberResponse
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.example.proyectofinal6to_ecobox.presentacion.adapter.FamilyMembersAdapter
import kotlinx.coroutines.launch

class FamilyManagementActivity : AppCompatActivity() {

    private lateinit var tvFamilyName: TextView
    private lateinit var tvInviteCode: TextView
    private lateinit var btnCopyCode: ImageButton
    private lateinit var rvMembers: RecyclerView
    private lateinit var adapter: FamilyMembersAdapter
    private lateinit var toolbar: Toolbar

    private var authToken: String? = null
    private var familyId: Long = -1
    private var iAmAdmin: Boolean = false
    private var currentUserId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_management)

        val prefs = getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)
        currentUserId = prefs.getLong("user_id", -1)

        if (authToken == null) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadFamilyData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        tvFamilyName = findViewById(R.id.tvFamilyName)
        tvInviteCode = findViewById(R.id.tvInviteCode)
        btnCopyCode = findViewById(R.id.btnCopyCode)
        rvMembers = findViewById(R.id.rvMembers)

        setupAdapter(emptyList())
        rvMembers.layoutManager = LinearLayoutManager(this)

        btnCopyCode.setOnClickListener {
            val code = tvInviteCode.text.toString()
            if (code != "XXXX-XXXX" && code != "Sin código") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Código Invitación EcoBox", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAdapter(members: List<FamilyMemberResponse>) {
        adapter = FamilyMembersAdapter(
            members = members,
            iAmAdmin = iAmAdmin,
            currentUserId = currentUserId,
            onChangeRole = { member -> toggleAdmin(member) },
            onRemoveMember = { member -> confirmRemove(member) }
        )
        rvMembers.adapter = adapter
    }

    private fun loadFamilyData() {
        val intentFamilyId = intent.getLongExtra("family_id", -1)
        val intentFamilyName = intent.getStringExtra("family_name")
        val intentFamilyCode = intent.getStringExtra("family_code")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getFamilies("Token $authToken")
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val targetFamily = if (intentFamilyId != -1L) {
                        response.body()!!.find { it.id == intentFamilyId }
                    } else {
                        response.body()!![0]
                    }

                    targetFamily?.let { family ->
                        familyId = family.id
                        iAmAdmin = family.es_admin
                        tvFamilyName.text = family.nombre
                        tvInviteCode.text = family.codigo_invitacion ?: "Sin código"
                        
                        // Actualizar menú después de saber si es admin
                        invalidateOptionsMenu()
                        
                        family.miembros?.let {
                            setupAdapter(it)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FamilyManagement", "Error red familias", e)
            }
        }
    }

    private fun toggleAdmin(member: FamilyMemberResponse) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.toggleMemberAdmin("Token $authToken", familyId, member.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@FamilyManagementActivity, "Rol actualizado", Toast.LENGTH_SHORT).show()
                    loadFamilyData() // Recargar datos
                }
            } catch (e: Exception) {
                Toast.makeText(this@FamilyManagementActivity, "Error al cambiar rol", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmRemove(member: FamilyMemberResponse) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Remover Miembro")
            .setMessage("¿Estás seguro de que deseas eliminar a ${member.usuario_info.first_name} de la familia?")
            .setPositiveButton("Eliminar") { _, _ -> removeMember(member) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun removeMember(member: FamilyMemberResponse) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.removeMember("Token $authToken", familyId, member.id)
                if (response.isSuccessful) {
                    Toast.makeText(this@FamilyManagementActivity, "Miembro eliminado", Toast.LENGTH_SHORT).show()
                    loadFamilyData() // Recargar datos
                }
            } catch (e: Exception) {
                Toast.makeText(this@FamilyManagementActivity, "Error al eliminar miembro", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (iAmAdmin) {
            menuInflater.inflate(R.menu.menu_family_management, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_family -> {
                confirmDeleteFamily()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDeleteFamily() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Eliminar Familia")
            .setMessage("¿Estás seguro de que deseas eliminar esta familia?\n\n" +
                    "Esta acción es IRREVERSIBLE y eliminará:\n" +
                    "• Todos los miembros\n" +
                    "• Todas las plantas asociadas\n" +
                    "• Todo el historial")
            .setPositiveButton("Eliminar") { _, _ ->
                // Segunda confirmación
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("⚠️ Confirmación Final")
                    .setMessage("¿Realmente deseas eliminar la familia?\nEsta acción NO se puede deshacer.")
                    .setPositiveButton("Sí, eliminar") { _, _ -> deleteFamily() }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteFamily() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deleteFamily("Token $authToken", familyId)
                if (response.isSuccessful) {
                    Toast.makeText(this@FamilyManagementActivity, "Familia eliminada exitosamente", Toast.LENGTH_SHORT).show()
                    finish() // Cerrar activity y volver
                } else {
                    Toast.makeText(this@FamilyManagementActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FamilyManagement", "Error al eliminar familia", e)
                Toast.makeText(this@FamilyManagementActivity, "Error al eliminar familia", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
