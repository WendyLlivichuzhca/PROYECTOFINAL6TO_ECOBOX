package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.proyectofinal6to_ecobox.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. VALIDAR SESIÓN
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setupNavigation()
    }

    private fun setupNavigation() {
        // 1. Obtener NavController de forma segura
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // 2. Vincular BottomNav con NavController
        // Esto permite que la navegación funcione automáticamente si los IDs del menú coinciden con los del nav_graph
        NavigationUI.setupWithNavController(bottomNav, navController)

        // 3. Control de selección de items
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    // Ahora navegamos al fragmento que diseñamos, NO mostramos el diálogo aquí
                    navController.navigate(R.id.nav_profile)
                    true
                }
                else -> {
                    // Para los otros items (Home, Plantas, etc.) usamos la navegación estándar
                    NavigationUI.onNavDestinationSelected(item, navController)
                    true
                }
            }
        }

        bottomNav.setOnItemReselectedListener {
            // Se deja vacío para evitar que la pantalla parpadee si el usuario pulsa el icono donde ya está
        }
    }

    // Nota: He quitado mostrarDialogoCerrarSesion de aquí porque ahora ese diálogo
    // debe activarse desde el botón "Cerrar Sesión" DENTRO del ProfileFragment.
}