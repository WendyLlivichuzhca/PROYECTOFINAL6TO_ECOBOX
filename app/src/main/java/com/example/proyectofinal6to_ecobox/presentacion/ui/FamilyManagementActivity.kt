package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_family_management)

        val prefs = getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        authToken = prefs.getString("auth_token", null)

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

        adapter = FamilyMembersAdapter(emptyList())
        rvMembers.layoutManager = LinearLayoutManager(this)
        rvMembers.adapter = adapter

        btnCopyCode.setOnClickListener {
            val code = tvInviteCode.text.toString()
            if (code != "XXXX-XXXX") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Código Invitación EcoBox", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFamilyData() {
        // Intentar obtener datos del intent primero
        val intentFamilyId = intent.getLongExtra("family_id", -1)
        val intentFamilyName = intent.getStringExtra("family_name")
        val intentFamilyCode = intent.getStringExtra("family_code")

        if (intentFamilyId != -1L && intentFamilyName != null) {
            // Usar datos del intent (familia seleccionada)
            familyId = intentFamilyId
            tvFamilyName.text = intentFamilyName
            tvInviteCode.text = intentFamilyCode ?: "Sin código"
            
            // Cargar miembros de esta familia específica
            loadMembersForFamily(intentFamilyId)
        } else {
            // Fallback: cargar la primera familia disponible
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.getFamilies("Token $authToken")
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val family = response.body()!![0]
                        familyId = family.id
                        tvFamilyName.text = family.nombre
                        tvInviteCode.text = family.codigo_invitacion ?: "Sin código"
                        
                        family.miembros?.let {
                            adapter.updateMembers(it)
                        }
                    } else {
                        tvFamilyName.text = "Sin familia activa"
                        Log.e("FamilyManagement", "Error familias: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("FamilyManagement", "Error red familias", e)
                }
            }
        }
    }

    private fun loadMembersForFamily(familyId: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getFamilies("Token $authToken")
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val familia = response.body()!!.find { it.id == familyId }
                    familia?.miembros?.let {
                        adapter.updateMembers(it)
                        Log.d("FamilyManagement", "✅ ${it.size} miembros cargados para familia $familyId")
                    }
                }
            } catch (e: Exception) {
                Log.e("FamilyManagement", "Error cargando miembros", e)
            }
        }
    }
}
