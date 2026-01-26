package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.ChatAdapter
import com.example.proyectofinal6to_ecobox.data.model.ChatMessage
import com.example.proyectofinal6to_ecobox.data.network.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Response

class ChatbotBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var llSuggestions: LinearLayout
    private lateinit var spinnerPlants: Spinner
    private lateinit var tvStatPlants: TextView
    private lateinit var tvStatMessages: TextView
    private lateinit var tvStatStatus: TextView
    
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var plantsList = mutableListOf<PlantResponse>()
    private var selectedPlantId: Long? = null
    private var userId: Long = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_chatbot_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        initViews(view)
        setupRecyclerView()
        setupListeners(view)
        
        loadInitialData()
    }

    private fun initViews(view: View) {
        rvChat = view.findViewById(R.id.rvChatMessages)
        etMessage = view.findViewById(R.id.etChatMessage)
        btnSend = view.findViewById(R.id.btnSendChat)
        llSuggestions = view.findViewById(R.id.llChatSuggestions)
        spinnerPlants = view.findViewById(R.id.spinnerChatPlants)
        tvStatPlants = view.findViewById(R.id.tvStatPlants)
        tvStatMessages = view.findViewById(R.id.tvStatMessages)
        tvStatStatus = view.findViewById(R.id.tvStatStatus)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        rvChat.adapter = adapter
    }

    private fun setupListeners(view: View) {
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) sendMessageToBot(text)
        }

        view.findViewById<ImageButton>(R.id.btnCloseChat).setOnClickListener { dismiss() }
        
        view.findViewById<ImageButton>(R.id.btnClearChat).setOnClickListener {
            messages.clear()
            adapter.notifyDataSetChanged()
            addMessage(formatBotMessage(WELCOME_MESSAGE), false)
            updateFooterStats()
        }

        spinnerPlants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPlantId = if (position == 0) null else plantsList[position - 1].id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadInitialData() {
        loadSuggestions()
        loadPlantsCloud()
        
        if (messages.isEmpty()) {
            addMessage(formatBotMessage(WELCOME_MESSAGE), false)
        }
        updateFooterStats()
    }

    private fun loadSuggestions() {
        val suggestions = listOf(
            "¿Cómo están mis plantas?",
            "¿Qué plantas necesitan agua?",
            "Consejos de cuidado",
            "¿Hay alertas activas?",
            "Temperatura ideal"
        )
        
        llSuggestions.removeAllViews()
        suggestions.forEach { sug ->
            val tv = TextView(context).apply {
                text = sug
                setBackgroundResource(R.drawable.bg_suggestion_pill)
                setPadding(30, 15, 30, 15)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 20, 0)
                layoutParams = params
                setTextColor(resources.getColor(R.color.gray_text))
                setOnClickListener {
                    etMessage.setText(sug)
                    sendMessageToBot(sug)
                }
            }
            llSuggestions.addView(tv)
        }
    }

    private fun loadPlantsCloud() {
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) return

        lifecycleScope.launch {
            try {
                // Cambiamos a getMyPlants ya que chatbot/plantas requiere sesión web y falla en móvil (Redirect 404)
                val response = RetrofitClient.instance.getMyPlants("Token $token")
                if (response.isSuccessful && response.body() != null) {
                    val plants = response.body()!!
                    
                    plantsList.clear()
                    plantsList.addAll(plants)
                    
                    val plantNames = mutableListOf("🌿 Todas las plantas")
                    plantNames.addAll(plants.map { p -> "${p.nombre} ${getStatusEmoji(p.estado_salud)}" })
                    
                    val adapterSp = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, plantNames)
                    spinnerPlants.adapter = adapterSp
                    
                    tvStatPlants.text = "🌱 ${plants.size} plantas"
                    Log.d("Chatbot", "✅ ${plants.size} plantas cargadas exitosamente (vía mis_plantas)")
                } else {
                    Log.e("Chatbot", "Error API: ${response.code()} ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Chatbot", "Error cargando plantas: ${e.message}")
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        rvChat.scrollToPosition(messages.size - 1)
        updateFooterStats()
    }

    private fun updateFooterStats() {
        tvStatMessages.text = "💬 ${messages.size} mensajes"
    }

    private fun sendMessageToBot(text: String) {
        addMessage(text, true)
        etMessage.text.clear()
        btnSend.isEnabled = false
        tvStatStatus.text = "⚡ Procesando..."
        tvStatStatus.setTextColor(resources.getColor(R.color.status_warning))
        
        fetchBotResponseCloud(text)
    }

    private fun fetchBotResponseCloud(userInput: String) {
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) {
            addMessage("❌ Error de sesión. Por favor, re-inicia sesión.", false)
            btnSend.isEnabled = true
            tvStatStatus.text = "⚠️ Error"
            return
        }

        lifecycleScope.launch {
            try {
                val requestBody: Map<String, Any> = mutableMapOf<String, Any>(
                    "mensaje" to userInput
                ).apply {
                    selectedPlantId?.let { put("planta_id", it) }
                }
                
                val response = RetrofitClient.instance.postChatbotMessage("Token $token", requestBody)

                if (response.isSuccessful && response.body() != null) {
                    val botMsg = formatBotMessage(response.body()!!.respuesta.mensaje)
                    addMessage(botMsg, false)
                    tvStatStatus.text = "✅ Listo"
                    tvStatStatus.setTextColor(resources.getColor(R.color.status_healthy))
                } else {
                    val errorStatus = response.code()
                    Log.e("ChatbotAPI", "Error del servidor: $errorStatus")
                    if (errorStatus == 401 || errorStatus == 403) {
                        addMessage("🤖 Tu sesión ha expirado o no tienes acceso. Por favor, re-inicia sesión.", false)
                    } else {
                        addMessage("🤖 Ups, EcoBot está tomando una siesta ($errorStatus). Inténtalo de nuevo en un momento.", false)
                    }
                    tvStatStatus.text = "⚠️ Error"
                }
            } catch (e: Exception) {
                Log.e("ChatbotAPI", "Error de red: ${e.message}")
                addMessage("❌ No tengo conexión con el jardín. Verifica tu internet.", false)
                tvStatStatus.text = "⚠️ Error"
                tvStatStatus.setTextColor(resources.getColor(R.color.status_critical))
            } finally {
                btnSend.isEnabled = true
            }
        }
    }

    private fun formatBotMessage(text: String): String {
        return text.replace("\\n", "\n").replace("**", "")
    }

    private fun getStatusEmoji(estado: String?): String {
        return when {
            estado?.contains("saludable", true) == true -> "✅"
            estado?.contains("agua", true) == true -> "💧"
            estado?.contains("peligro", true) == true -> "⚠️"
            else -> "🌱"
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    companion object {
        const val TAG = "ChatbotBottomSheet"
        fun newInstance() = ChatbotBottomSheet()
        private const val WELCOME_MESSAGE = "🌿 **¡Hola! Soy EcoBot, tu asistente de plantas inteligente.**\n\n" +
                "Puedo ayudarte con:\n" +
                "• 📊 Estado de tus plantas\n" +
                "• 💧 Recomendaciones de riego\n" +
                "• ⚠️ Alertas y problemas\n" +
                "• 📈 Datos de sensores\n" +
                "• 🌱 Consejos de cuidado\n\n" +
                "**Selecciona una planta o pregúntame directamente.**"
    }
}
