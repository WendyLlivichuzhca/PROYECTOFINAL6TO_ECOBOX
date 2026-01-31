package com.example.proyectofinal6to_ecobox.presentacion.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.proyectofinal6to_ecobox.data.network.PlantResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


/**
 * Adapter para el ViewPager2 del chatbot
 * Maneja los dos tabs: Chat y Datos
 */
class ChatbotPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val plants: List<PlantResponse>,
    private val onConsultarClick: (PlantResponse) -> Unit
) : FragmentStateAdapter(fragmentActivity) {
    
    private var chatFragment: ChatTabFragment? = null
    private var dataFragment: DataTabFragment? = null
    
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        val plantsJson = Gson().toJson(plants)
        return when (position) {
            0 -> {
                chatFragment = ChatTabFragment.newInstance(plantsJson)
                chatFragment!!
            }
            1 -> {
                dataFragment = DataTabFragment.newInstance(plantsJson)
                dataFragment?.setOnConsultarClickListener(onConsultarClick)
                dataFragment!!
            }
            else -> ChatTabFragment.newInstance(plantsJson)
        }
    }
    
    fun getChatFragment(): ChatTabFragment? = chatFragment
    fun getDataFragment(): DataTabFragment? = dataFragment
    
    fun updatePlants(newPlants: List<PlantResponse>) {
        chatFragment?.updatePlants(newPlants)
        dataFragment?.updatePlants(newPlants)
    }
}
