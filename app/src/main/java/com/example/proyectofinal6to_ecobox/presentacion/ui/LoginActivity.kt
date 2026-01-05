package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.dao.UsuarioDao

class LoginActivity : AppCompatActivity() {

    // Referencias UI
    private lateinit var etLoginEmail: EditText
    private lateinit var etLoginPass: EditText
    private lateinit var btnLogin: View
    private lateinit var btnBack: View
    private lateinit var btnTogglePassword: View

    // --- NUEVO: Referencias para Recordarme ---
    private lateinit var btnRememberMeContainer: View
    private lateinit var ivRememberCheck: ImageView
    private var isRememberMeChecked = false
    // -----------------------------------------

    // Referencias Animación
    private lateinit var lottieLeaf: LottieAnimationView
    private lateinit var tvWelcome: TextView
    private lateinit var containerForm: ViewGroup
    private lateinit var layoutFooter: View
    private lateinit var ivWave: View

    // Variables de estado
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- NUEVO: Verificar sesión antes de cargar la vista ---
        if (checkExistingSession()) {
            return
        }
        // --------------------------------------------------------

        setContentView(R.layout.activity_login)

        initViews()
        setupAnimations()
        setupListeners()
    }

    private fun initViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail)
        etLoginPass = findViewById(R.id.etLoginPass)
        btnLogin = findViewById(R.id.btnLogin)
        btnBack = findViewById(R.id.btnBack)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)

        // --- NUEVO: Inicializar vistas de Recordarme ---
        btnRememberMeContainer = findViewById(R.id.btnRememberMeContainer)
        ivRememberCheck = findViewById(R.id.ivRememberCheck)
        // -----------------------------------------------

        lottieLeaf = findViewById(R.id.lottieLeaf)
        // Ya le pusimos el ID correcto en el XML anterior
        tvWelcome = findViewById(R.id.tvWelcome)

        containerForm = findViewById(R.id.containerForm)
        layoutFooter = findViewById(R.id.layoutFooter)
        ivWave = findViewById(R.id.ivWave)
    }

    private fun setupListeners() {
        // Navegación
        findViewById<TextView>(R.id.btnGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<TextView>(R.id.btnGoToForgot).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Botón Atrás
        btnBack.setOnClickListener {
            finish()
        }

        // Mostrar/Ocultar Contraseña
        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // --- NUEVO: Listener para Recordarme ---
        btnRememberMeContainer.setOnClickListener {
            isRememberMeChecked = !isRememberMeChecked
            updateRememberMeUI()
        }
        // ---------------------------------------

        // Acción Principal
        btnLogin.setOnClickListener { attemptLogin() }

        // Validaciones en tiempo real
        setupFieldListener(etLoginEmail) { text ->
            Patterns.EMAIL_ADDRESS.matcher(text).matches()
        }
        setupFieldListener(etLoginPass) { text ->
            text.length >= 6
        }
    }

    // --- NUEVO: Lógica visual del Checkbox ---
    private fun updateRememberMeUI() {
        if (isRememberMeChecked) {
            ivRememberCheck.setImageResource(R.drawable.ic_check_circle)
            // Usa tu color verde (eco_primary o #2D5A40)
            ivRememberCheck.setColorFilter(ContextCompat.getColor(this, R.color.eco_primary))
        } else {
            ivRememberCheck.setImageResource(R.drawable.ic_circle_outline)
            // Usa color gris (gray_text o #A0A0A0)
            ivRememberCheck.setColorFilter(ContextCompat.getColor(this, R.color.gray_text))
        }
    }

    // --- NUEVO: Verificar sesión al inicio ---
    private fun checkExistingSession(): Boolean {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return true
        }
        return false
    }
    // -----------------------------------------

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etLoginPass.transformationMethod = HideReturnsTransformationMethod.getInstance()
            (btnTogglePassword as ImageView).setColorFilter(getColor(R.color.eco_primary))
        } else {
            etLoginPass.transformationMethod = PasswordTransformationMethod.getInstance()
            (btnTogglePassword as ImageView).setColorFilter(getColor(R.color.gray_text))
        }
        etLoginPass.setSelection(etLoginPass.text.length)
    }

    private fun attemptLogin() {
        val email = etLoginEmail.text.toString().trim()
        val pass = etLoginPass.text.toString().trim()

        if (!validateInputs(email, pass)) return

        Toast.makeText(this, "Verificando...", Toast.LENGTH_SHORT).show()
        btnLogin.isEnabled = false

        Thread {
            val codigoResultado = UsuarioDao.validarUsuario(email, pass)

            var userId: Long = -1
            if (codigoResultado == UsuarioDao.LOGIN_EXITOSO) {
                userId = UsuarioDao.obtenerIdPorEmail(email)
            }

            runOnUiThread {
                handleLoginResult(codigoResultado, userId, email)
                btnLogin.isEnabled = true
            }
        }.start()
    }

    private fun handleLoginResult(codigo: Int, userId: Long, email: String) {
        when (codigo) {
            UsuarioDao.LOGIN_EXITOSO -> {
                etLoginEmail.error = null
                etLoginPass.error = null
                Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                saveSessionAndNavigate(userId, email)
            }
            UsuarioDao.EMAIL_NO_ENCONTRADO -> {
                etLoginEmail.error = "Correo no registrado"
                etLoginEmail.requestFocus()
            }
            UsuarioDao.PASSWORD_INCORRECTO -> {
                etLoginPass.setText("")
                etLoginPass.error = "Contraseña incorrecta"
                etLoginPass.requestFocus()
            }
            else -> Toast.makeText(this, "Error de conexión", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveSessionAndNavigate(userId: Long, email: String) {
        val prefs = getSharedPreferences("ecobox_prefs", MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putLong("user_id", userId)
        editor.putString("user_email", email)

        // --- NUEVO: Guardar bandera solo si el usuario quiso ---
        if (isRememberMeChecked) {
            editor.putBoolean("is_logged_in", true)
        } else {
            // Aseguramos que sea false si no marcó la casilla
            editor.putBoolean("is_logged_in", false)
        }

        editor.apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // --- Validaciones (Igual que antes) ---

    private fun validateInputs(email: String, pass: String): Boolean {
        var isValid = true
        if (email.isEmpty()) {
            etLoginEmail.error = "Requerido"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.error = "Formato inválido"
            isValid = false
        }

        if (pass.isEmpty()) {
            etLoginPass.error = "Requerido"
            isValid = false
        }
        return isValid
    }

    private fun setupFieldListener(editText: EditText, isValidCondition: (String) -> Boolean) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().trim()
                if (input.isNotEmpty() && !isValidCondition(input)) {
                    // Opcional: editText.error = "Formato inválido"
                } else {
                    editText.error = null
                }
            }
        })
    }

    private fun setupAnimations() {
        // 1. Configuración Inicial: Ponemos todo abajo a la MISMA distancia
        // Así parecerá que la ola y el formulario son una sola pieza.
        val desplazamientoInicial = 600f

        // Posiciones iniciales
        ivWave.translationY = desplazamientoInicial
        containerForm.translationY = desplazamientoInicial
        layoutFooter.translationY = desplazamientoInicial

        // Elementos que aparecen (fade in)
        lottieLeaf.alpha = 0f
        tvWelcome.alpha = 0f
        containerForm.alpha = 1f // El formulario debe ser visible desde el inicio, solo que desplazado

        // Animamos la hoja un poco diferente para que de efecto "pop"
        lottieLeaf.translationY = 50f

        // -----------------------------------------------------------
        // 2. Ejecutar Animaciones Sincronizadas (Ola + Formulario)
        // -----------------------------------------------------------

        val interpoladorSuave = DecelerateInterpolator()
        val duracionAnimacion = 1000L

        // A. La Ola sube
        ivWave.animate()
            .translationY(0f)
            .setDuration(duracionAnimacion)
            .setInterpolator(interpoladorSuave)
            .start()

        // B. El Formulario sube EXACTAMENTE igual (Sin delay)
        containerForm.animate()
            .translationY(0f)
            .setDuration(duracionAnimacion)
            .setInterpolator(interpoladorSuave)
            .setUpdateListener {
                // Truco extra: Si por alguna razón se separan un milímetro,
                // esto asegura que la ola siga pegada visualmente (opcional)
            }
            .start()

        // C. El Footer sube igual
        layoutFooter.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duracionAnimacion)
            .setInterpolator(interpoladorSuave)
            .start()

        // La hoja aparece rebotando justo cuando el bloque llega arriba
        lottieLeaf.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(400) // Espera un poco a que suba el fondo
            .setInterpolator(BounceInterpolator())
            .start()

        // El texto de bienvenida aparece suavemente
        tvWelcome.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(600)
            .start()
    }
}