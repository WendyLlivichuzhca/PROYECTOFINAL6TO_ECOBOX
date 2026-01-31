package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.adapter.ChatAdapter
import com.example.proyectofinal6to_ecobox.data.model.ChatMessage
import com.example.proyectofinal6to_ecobox.data.network.PlantResponse
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.*

class ChatTabFragment : Fragment() {
    
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var llChatSuggestions: LinearLayout
    private lateinit var spinnerChatPlants: Spinner
    private lateinit var etChatMessage: EditText
    private lateinit var btnSendChat: ImageButton
    private lateinit var tvStatPlants: TextView
    private lateinit var tvStatMessages: TextView
    private lateinit var tvStatStatus: TextView
    
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var plants: List<PlantResponse> = emptyList()
    private var selectedPlantId: Long? = null
    private var userId: Long = -1
    
    // Callback para cuando se presiona CONSULTAR desde el tab de Datos
    var onSwitchToChat: ((PlantResponse, String) -> Unit)? = null
    
    companion object {
        private const val ARG_PLANTS_JSON = "plants_json"
        
        fun newInstance(plantsJson: String): ChatTabFragment {
            val fragment = ChatTabFragment()
            val args = Bundle()
            args.putString(ARG_PLANTS_JSON, plantsJson)
            fragment.arguments = args
            return fragment
        }
        
        fun newInstance(): ChatTabFragment = ChatTabFragment()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        arguments?.getString(ARG_PLANTS_JSON)?.let { json ->
            val type = object : TypeToken<List<PlantResponse>>() {}.type
            plants = Gson().fromJson(json, type)
        }
        
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chatbot_chat, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupListeners()
        loadSuggestions()
        setupPlantSpinner()
        
        if (messages.isEmpty()) {
            addWelcomeMessage()
        }
        
        updateFooterStats()
    }
    
    private fun initViews(view: View) {
        rvChatMessages = view.findViewById(R.id.rvChatMessages)
        llChatSuggestions = view.findViewById(R.id.llChatSuggestions)
        spinnerChatPlants = view.findViewById(R.id.spinnerChatPlants)
        etChatMessage = view.findViewById(R.id.etChatMessage)
        btnSendChat = view.findViewById(R.id.btnSendChat)
        tvStatPlants = view.findViewById(R.id.tvStatPlants)
        tvStatMessages = view.findViewById(R.id.tvStatMessages)
        tvStatStatus = view.findViewById(R.id.tvStatStatus)
    }
    
    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        rvChatMessages.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        rvChatMessages.adapter = adapter
    }
    
    private fun setupListeners() {
        btnSendChat.setOnClickListener {
            val text = etChatMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessageToBot(text)
            }
        }
        
        spinnerChatPlants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPlantId = if (position == 0) null else plants[position - 1].id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun loadSuggestions() {
        val suggestions = listOf(
            "¿Cómo están mis plantas?",
            "¿Qué plantas necesitan agua?",
            "Ver todas mis plantas",
            "Consejos para suculentas"
        )
        
        llChatSuggestions.removeAllViews()
        
        suggestions.forEach { suggestion ->
            val button = Button(context).apply {
                text = suggestion
                textSize = 12f
                setTextColor(Color.parseColor("#2E7D32"))
                setBackgroundResource(R.drawable.bg_suggestion_chip)
                setPadding(24, 12, 24, 12)
                isAllCaps = false
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = 12
                layoutParams = params
                
                setOnClickListener {
                    etChatMessage.setText(suggestion)
                    sendMessageToBot(suggestion)
                }
            }
            llChatSuggestions.addView(button)
        }
    }
    
    private fun setupPlantSpinner() {
        val plantNames = mutableListOf("Todas las plantas")
        plantNames.addAll(plants.map { it.nombre })
        
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            plantNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChatPlants.adapter = spinnerAdapter
    }
    
    private fun addWelcomeMessage() {
        val welcome = """
            🌿 **¡Hola! Soy EcoBot, tu asistente de plantas inteligente de EcoBox.**
            
            Tienes **${plants.size} plantas** en tu sistema.
            
            **¿En qué puedo ayudarte?** Puedes:
            • 📊 Preguntar por el estado de una planta
            • 💧 Saber si necesitan riego
            • ⚠️ Ver alertas y problemas
            • 📈 Consultar datos de sensores
            • 🌱 Recibir consejos específicos
            
            **Ejemplos:**
            - ¿Cómo está el Príncipe Negro?
            - ¿Necesita agua la Gasteria?
            - Muéstrame todas mis plantas
        """.trimIndent()
        
        addMessage(welcome, false)
    }
    
    fun sendMessageToBot(text: String) {
        // Agregar mensaje del usuario
        addMessage(text, true)
        etChatMessage.setText("")
        
        // Mostrar typing indicator
        adapter.showTypingIndicator = true
        rvChatMessages.scrollToPosition(adapter.itemCount - 1)
        
        // Llamar al backend
        lifecycleScope.launch {
            try {
                val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("auth_token", "") ?: ""
                
                val response = RetrofitClient.instance.postChatbotMessage(
                    token = "Token $token",
                    request = mapOf(
                        "message" to text,
                        "plant_id" to (selectedPlantId ?: "")
                    )
                )
                
                adapter.showTypingIndicator = false
                
                if (response.isSuccessful && response.body()?.status == "success") {
                    val botResponse = response.body()?.data?.text ?: "No entendí tu pregunta."
                    addMessage(botResponse, false)
                } else {
                    addMessage("⚠️ Error al procesar tu mensaje. Intenta de nuevo.", false)
                }
                
            } catch (e: Exception) {
                adapter.showTypingIndicator = false
                addMessage("⚠️ Error de conexión: ${e.message}", false)
                Log.e("ChatTabFragment", "Error sending message", e)
            }
        }
        
        updateFooterStats()
    }
    
    private fun addMessage(text: String, isUser: Boolean) {
        val message = ChatMessage(
            text = text,
            isUser = isUser,
            timestamp = System.currentTimeMillis()
        )
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        rvChatMessages.scrollToPosition(messages.size - 1)
        
        updateFooterStats()
    }
    
    private fun updateFooterStats() {
        tvStatPlants.text = "🌱 ${plants.size} plantas"
        tvStatMessages.text = "💬 ${messages.size} mensajes"
        tvStatStatus.text = "✅ Listo"
    }
    
    fun updatePlants(newPlants: List<PlantResponse>) {
        plants = newPlants
        if (::spinnerChatPlants.isInitialized) {
            setupPlantSpinner()
            updateFooterStats()
        }
    }
    
    fun consultPlant(plant: PlantResponse) {
        // Seleccionar la planta en el spinner
        val plantIndex = plants.indexOf(plant) + 1 // +1 por "Todas las plantas"
        if (plantIndex > 0 && plantIndex < spinnerChatPlants.adapter.count) {
            spinnerChatPlants.setSelection(plantIndex)
        }
        
        // Enviar mensaje automático
        val mensaje = "¿Cómo está ${plant.nombre}?"
        etChatMessage.setText(mensaje)
        sendMessageToBot(mensaje)
    }

    fun clearChat() {
        messages.clear()
        adapter.notifyDataSetChanged()
        addWelcomeMessage()
        updateFooterStats()
    }
}
