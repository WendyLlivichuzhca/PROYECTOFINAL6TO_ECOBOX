package com.example.proyectofinal6to_ecobox.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- AUTENTICACIÓN ---
    @POST("auth/login/")
    suspend fun login(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("auth/registro/")
    suspend fun register(@Body request: Map<String, String>): Response<AuthResponse>

    @POST("auth/solicitar-reset-password/")
    suspend fun requestResetPassword(@Body request: Map<String, String>): Response<MessageResponse>

    @POST("auth/reset-password/{token}/")
    suspend fun resetPassword(
        @Path("token") token: String,
        @Body request: Map<String, String>
    ): Response<MessageResponse>

    // --- PERFIL ---
    @GET("auth/profile/")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfileResponse>

    @PUT("auth/profile/")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<UserProfileResponse>

    // --- DASHBOARD ---
    @GET("dashboard/")
    suspend fun getDashboardData(@Header("Authorization") token: String): Response<DashboardResponse>

    // --- IA & RECOMENDACIONES ---
    @GET("recommendations/")
    suspend fun getRecommendations(@Header("Authorization") token: String): Response<List<RecommendationResponse>>

    @POST("ai/watering/activate/{plant_id}/")
    suspend fun irrigatePlant(
        @Header("Authorization") token: String,
        @Path("plant_id") plantId: Long
    ): Response<IrrigateResponse>

    // --- FAMILIAS ---
    @GET("familias/")
    suspend fun getFamilies(@Header("Authorization") token: String): Response<List<FamilyResponse>>

    @POST("familias/")
    suspend fun createFamily(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<FamilyResponse>
}

// --- DATA CLASSES PARA RESPUESTAS ---

data class FamilyMemberResponse(
    val id: Long,
    val es_administrador: Boolean,
    val usuario_info: UserData
)

data class AuthResponse(
    val message: String,
    val token: String?,
    val user: UserData?
)

data class UserData(
    val id: Long,
    val email: String,
    val username: String,
    val first_name: String?,
    val last_name: String?
)

data class MessageResponse(
    val message: String,
    val error: String? = null
)

data class UserProfileResponse(
    val id: Long,
    val nombre: String?,
    val apellido: String?,
    val email: String,
    val username: String?,
    val telefono: String?,
    val estadisticas: Map<String, Any>?
)

data class DashboardResponse(
    val total_plantas: Int,
    val plantas_necesitan_agua: Int,
    val humedad_promedio: String,
    val ultima_actualizacion: String,
    val metricas_avanzadas: Map<String, Any>?,
    val estadisticas_semana: Map<String, Any>?
)

data class RecommendationResponse(
    val id: Long,
    val type: String,
    val plant_name: String,
    val message: String,
    val time_ago: String,
    val action: String
)

data class FamilyResponse(
    val id: Long,
    val nombre: String,
    val codigo_invitacion: String?,
    val cantidad_miembros: Int,
    val cantidad_plantas: Int,
    val es_admin: Boolean,
    val miembros: List<FamilyMemberResponse>?
)

data class IrrigateResponse(
    val success: Boolean,
    val message: String,
    val watering_id: Long? = null,
    val duration_seconds: Int? = null,
    val initial_humidity: Float? = null,
    val hardware_note: String? = null
)
