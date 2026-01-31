package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.proyectofinal6to_ecobox.R
import com.example.proyectofinal6to_ecobox.data.network.PlantResponse
import com.example.proyectofinal6to_ecobox.data.network.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class ChatbotBottomSheet : BottomSheetDialogFragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: ChatbotPagerAdapter
    
    private var plantsList = mutableListOf<PlantResponse>()
    private var userId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_chatbot_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1)

        initViews(view)
        setupListeners(view)
        loadPlantsAndSetupTabs()
    }

    private fun initViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayoutChatbot)
        viewPager = view.findViewById(R.id.viewPagerChatbot)
    }

    private fun setupListeners(view: View) {
        view.findViewById<View>(R.id.btnCloseChat)?.setOnClickListener {
            dismiss()
        }
        
        view.findViewById<View>(R.id.btnClearChat)?.setOnClickListener {
            pagerAdapter.getChatFragment()?.clearChat()
        }
    }

    private fun loadPlantsAndSetupTabs() {
        val prefs = requireContext().getSharedPreferences("ecobox_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token == null) {
            setupViewPagerWithEmptyPlants()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMyPlants("Token $token")
                if (response.isSuccessful && response.body() != null) {
                    plantsList.clear()
                    plantsList.addAll(response.body()!!)
                    
                    Log.d("ChatbotBottomSheet", "✅ ${plantsList.size} plantas cargadas")
                    setupViewPagerWithPlants()
                } else {
                    Log.e("ChatbotBottomSheet", "Error API: ${response.code()}")
                    setupViewPagerWithEmptyPlants()
                }
            } catch (e: Exception) {
                Log.e("ChatbotBottomSheet", "Error cargando plantas: ${e.message}")
                setupViewPagerWithEmptyPlants()
            }
        }
    }

    private fun setupViewPagerWithPlants() {
        pagerAdapter = ChatbotPagerAdapter(requireActivity(), plantsList) { plant: PlantResponse ->
            // Lógica cuando se presiona CONSULTAR en el tab de Datos
            viewPager.currentItem = 0
            viewPager.postDelayed({
                pagerAdapter.getChatFragment()?.consultPlant(plant)
            }, 100)
        }
        viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "💬 Chat"
                1 -> "📊 Datos"
                else -> ""
            }
        }.attach()
    }

    private fun setupViewPagerWithEmptyPlants() {
        pagerAdapter = ChatbotPagerAdapter(requireActivity(), emptyList()) { _: PlantResponse -> }
        viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "💬 Chat"
                1 -> "📊 Datos"
                else -> ""
            }
        }.attach()
    }



    companion object {
        const val TAG = "ChatbotBottomSheet"
        fun newInstance(): ChatbotBottomSheet {
            return ChatbotBottomSheet()
        }
    }
}
