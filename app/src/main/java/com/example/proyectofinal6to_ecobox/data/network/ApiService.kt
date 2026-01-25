package com.example.proyectofinal6to_ecobox.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("ai/watering/activate/{plant_id}/")
    suspend fun irrigatePlant(
        @Header("Authorization") token: String,
        @Path("plant_id") plantId: Long
    ): Response<IrrigateResponse>
}

data class IrrigateRequest(
    val planta_id: Long
)

data class IrrigateResponse(
    val success: Boolean,
    val message: String,
    val watering_id: Long? = null,
    val duration_seconds: Int? = null,
    val initial_humidity: Float? = null,
    val hardware_note: String? = null
)

data class IrrigateData(
    val riego_id: Long,
    val fecha: String,
    val cantidad_agua: Double
)
