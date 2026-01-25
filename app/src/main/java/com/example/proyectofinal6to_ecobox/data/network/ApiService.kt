package com.example.proyectofinal6to_ecobox.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

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

    // --- IA & CONTROL GLOBAL ---
    @GET("ai/status/")
    suspend fun getAiStatus(@Header("Authorization") token: String): Response<AiStatusResponse>

    @POST("ai/control/")
    suspend fun controlAi(
        @Header("Authorization") token: String,
        @Body request: Map<String, String> // {"action": "start" | "stop" | "train_all"}
    ): Response<MessageResponse>

    @GET("ai/watering/predict/{plant_id}/")
    suspend fun getWateringPrediction(
        @Header("Authorization") token: String,
        @Path("plant_id") plantId: Long
    ): Response<WateringPredictionResponse>

    @GET("ai/watering/history/{plant_id}/")
    suspend fun getWateringHistory(
        @Header("Authorization") token: String,
        @Path("plant_id") plantId: Long
    ): Response<WateringHistoryResponse>

    @POST("ai/watering/activate/{plant_id}/")
    suspend fun activateWatering(
        @Header("Authorization") token: String,
        @Path("plant_id") plantId: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<IrrigateResponse>

    // --- RECOMENDACIONES FEEDBACK ---
    @POST("ai/recommendations/{id}/feedback/")
    suspend fun provideAiFeedback(
        @Header("Authorization") token: String,
        @Path("id") predictionId: Long,
        @Body request: Map<String, String> // {"feedback": "correct" | "incorrect"}
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
    suspend fun getRecommendations(@Header("Authorization") token: String): Response<RecommendationListResponse>

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

    @POST("familias/unirse/")
    suspend fun joinFamily(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<MessageResponse>

    @DELETE("familias/{family_id}/miembros/{member_id}/")
    suspend fun removeMember(
        @Header("Authorization") token: String,
        @Path("family_id") familyId: Long,
        @Path("member_id") memberId: Long
    ): Response<MessageResponse>

    @POST("familias/{family_id}/miembros/{member_id}/toggle_admin/")
    suspend fun toggleMemberAdmin(
        @Header("Authorization") token: String,
        @Path("family_id") familyId: Long,
        @Path("member_id") memberId: Long
    ): Response<MessageResponse>

    // --- PLANTAS ---
    @GET("plantas/")
    suspend fun getPlants(@Header("Authorization") token: String): Response<List<PlantResponse>>

    @GET("plantas/mis_plantas/")
    suspend fun getMyPlants(@Header("Authorization") token: String): Response<List<PlantResponse>>

    @GET("plantas/{id}/")
    suspend fun getPlant(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<PlantResponse>

    @Multipart
    @POST("plantas/")
    suspend fun createPlant(
        @Header("Authorization") token: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part imagen: MultipartBody.Part? = null
    ): Response<PlantResponse>

    @Multipart
    @PUT("plantas/{id}/")
    suspend fun updatePlant(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part imagen: MultipartBody.Part? = null
    ): Response<PlantResponse>

    @DELETE("plantas/{id}/")
    suspend fun deletePlant(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<MessageResponse>

    @GET("plantas/{id}/estadisticas/")
    suspend fun getPlantStats(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<PlantStatsResponse>

    @GET("plantas/{id}/estado/")
    suspend fun getPlantState(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<PlantStateResponse>

    @GET("sensores/")
    suspend fun getSensors(
        @Header("Authorization") token: String,
        @Query("planta") plantId: Long? = null
    ): Response<List<SensorResponse>>

    @GET("sensores/{id}/historial_mediciones/")
    suspend fun getSensorMeasurements(
        @Header("Authorization") token: String,
        @Path("id") sensorId: Long,
        @Query("limit") limit: Int = 1,
        @Query("ordering") ordering: String = "-fecha"
    ): Response<List<MeasurementResponse>>

    @GET("configuraciones/")
    suspend fun getPlantConfig(
        @Header("Authorization") token: String,
        @Query("idPlanta") idPlanta: Long
    ): Response<List<PlantConfigResponse>>

    @PATCH("configuraciones/{id}/")
    suspend fun updatePlantConfig(
        @Header("Authorization") token: String,
        @Path("id") configId: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<PlantConfigResponse>

    // --- SEGUIMIENTO Y EVENTOS ---
    @GET("plantas/{id}/historial/")
    suspend fun getPlantHistory(
        @Header("Authorization") token: String,
        @Path("id") plantId: Long
    ): Response<PlantHistoryResponse>

    @GET("seguimientos-estado/")
    suspend fun getTrackingRecords(
        @Header("Authorization") token: String,
        @Query("planta") plantId: Long
    ): Response<List<TrackingRecordResponse>>

    @Multipart
    @POST("seguimientos-estado/")
    suspend fun createTrackingRecord(
        @Header("Authorization") token: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part imagen: MultipartBody.Part? = null
    ): Response<TrackingRecordResponse>

    // --- SENSORES Y HARDWARE ---

    @POST("sensores/")
    suspend fun createSensor(
        @Header("Authorization") token: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<SensorResponse>

    @GET("tipos-sensor/")
    suspend fun getSensorTypes(
        @Header("Authorization") token: String
    ): Response<List<SensorTypeResponse>>

    // --- ALERTAS ---
    @GET("alerts/")
    suspend fun getAlerts(
        @Header("Authorization") token: String,
        @Query("no_leidas") noLeidas: Boolean? = null,
        @Query("planta_id") plantId: Long? = null
    ): Response<AlertListResponse>

    @GET("alerts/stats/")
    suspend fun getAlertStats(
        @Header("Authorization") token: String
    ): Response<AlertStatsResponseCloud>

    @POST("alerts/mark-read/")
    suspend fun markAlertAsRead(
        @Header("Authorization") token: String,
        @Body request: Map<String, Long> // {"alert_id": id}
    ): Response<MessageResponse>

    @GET("chatbot/plantas/")
    suspend fun getChatbotPlants(
        @Header("Authorization") token: String
    ): Response<List<PlantResponse>>

    @POST("chatbot/")
    suspend fun postChatbotMessage(
        @Header("Authorization") token: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<ChatbotResponse>

    // --- NOTIFICACIONES DE USUARIO ---
    @GET("notificaciones/")
    suspend fun getUserNotifications(
        @Header("Authorization") token: String
    ): Response<List<UserNotificationResponse>>

    @POST("notificaciones/{id}/marcar_leida/")
    suspend fun markUserNotificationAsRead(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<MessageResponse>
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
    val success: Boolean = true,
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
    @SerializedName("tipo")
    val type: String,
    @SerializedName("planta_nombre")
    val plant_name: String,
    @SerializedName("mensaje")
    val message: String,
    @SerializedName("hace")
    val time_ago: String,
    val action: String
)

data class RecommendationListResponse(
    val total: Int,
    val urgentes: Int,
    @SerializedName("recomendaciones")
    val recommendations: List<RecommendationResponse>
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

data class PlantResponse(
    @SerializedName("idPlanta")
    val id: Long,
    @SerializedName("nombrePersonalizado")
    val nombre: String,
    val especie: String?,
    val descripcion: String?,
    val aspecto: String?,
    @SerializedName("fecha_creacion")
    val fecha_plantacion: String?,
    @SerializedName("foto")
    val imagen_url: String?,
    val familia: Long,
    val familia_nombre: String?,
    @SerializedName("estado")
    val estado_salud: String?,
    val necesita_agua: Boolean?,
    val ultima_medicion: String?,
    val humedad_actual: Float?,
    val temperatura_actual: Float?
)

data class PlantStatsResponse(
    val total_sensores: Int,
    val total_riegos: Int,
    val ultimo_riego: Map<String, Any>?,
    val promedio_temperatura: Float?,
    val promedio_humedad: Float?
)

data class PlantStateResponse(
    @SerializedName("planta_id")
    val id: Long,
    val nombre: String,
    @SerializedName("humedad_actual")
    val humedad: Float?,
    @SerializedName("temperatura_actual")
    val temperatura: Float?,
    @SerializedName("luz_actual")
    val luz: Float?,
    @SerializedName("ultima_medicion")
    val ultimaMedicion: String?,
    @SerializedName("necesita_riego")
    val necesitaRiego: Boolean,
    @SerializedName("estado")
    val estadoSalud: String,
    @SerializedName("proximo_riego_recomendado")
    val proximoRiego: String?,
    @SerializedName("humedad_predicha")
    val humedadPredicha: Float?,
    @SerializedName("probabilidad_riego")
    val probabilidadRiego: Float?,
    @SerializedName("ultimo_riego")
    val ultimoRiego: String?,
    @SerializedName("riegos_hoy")
    val riegosHoy: Int,
    @SerializedName("promedio_humedad_24h")
    val promedioHumedad24h: Float?,
    @SerializedName("alertas_activas")
    val alertas: List<Map<String, Any>>? = null
)

data class PlantConfigResponse(
    val id: Long,
    val planta: Long,
    @SerializedName("humedad_objetivo")
    val humedadObjetivo: Float?,
    @SerializedName("temperatura_minima")
    val tempMin: Float?,
    @SerializedName("temperatura_maxima")
    val tempMax: Float?,
    @SerializedName("riego_automatico")
    val riegoAutomatico: Boolean,
    @SerializedName("mensaje_personalizado")
    val mensajePersonalizado: String?
)

data class SensorResponse(
    val id: Long,
    val nombre: String,
    val ubicacion: String?,
    @SerializedName("tipo_sensor")
    val tipoSensor: Int,
    @SerializedName("estado_sensor")
    val estadoSensor: Int,
    val activo: Boolean,
    val planta: Long?
)

data class SensorTypeResponse(
    val id: Int,
    val nombre: String,
    @SerializedName("unidad_medida")
    val unidadMedida: String,
    val descripcion: String?
)

data class SensorStateResponse(
    val id: Int,
    val nombre: String,
    val descripcion: String?
)

data class MeasurementResponse(
    val id: Long,
    val valor: Float,
    val fecha: String,
    val sensor: Long
)

data class PlantEventResponse(
    val id: Long,
    val fecha: String,
    val tipo: String,
    val descripcion: String,
    val estado: String?,
    val observaciones: String?,
    val imagen: String?,
    val usuario: String?
)

data class PlantHistoryResponse(
    val resumen: Map<String, Any>,
    val eventos: List<PlantEventResponse>,
    val ultimasMediciones: List<Map<String, Any>>,
    val estadisticas: Map<String, Any>
)

data class TrackingRecordResponse(
    val id: Long,
    val planta: Long,
    val estado: String,
    val observaciones: String?,
    @SerializedName("fecha_registro")
    val fechaRegistro: String,
    @SerializedName("imagen_url")
    val imagenUrl: String?
)

// --- ALERTAS CLOUD ---
data class AlertCloud(
    val id: Long,
    val titulo: String,
    val mensaje: String,
    val tipo: String,
    val leida: Boolean,
    val resuelta: Boolean,
    @SerializedName("creada_en")
    val creadaEn: String,
    @SerializedName("plant_id")
    val plantId: Long?,
    @SerializedName("plant_nombre")
    val plantNombre: String
)

data class AlertListResponse(
    val status: String,
    val alertas: List<AlertCloud>,
    val total: Int,
    @SerializedName("no_leidas")
    val noLeidas: Int
)

data class AlertStatsResponseCloud(
    val status: String,
    val estadisticas: Map<String, Any>
)

// --- CHATBOT ---
data class ChatbotResponse(
    val success: Boolean,
    val respuesta: ChatbotMessageData
)

data class ChatbotMessageData(
    val mensaje: String
)

// --- NOTIFICACIONES DE USUARIO ---
data class UserNotificationResponse(
    val id: Long,
    val mensaje: String,
    val leida: Boolean,
    @SerializedName("fecha_creacion")
    val fechaCreacion: String,
    val tipo: String,
    val usuario: Long
)
data class WateringHistoryResponse(
    val success: Boolean,
    val waterings: List<WateringHistoryItem>
)

data class WateringHistoryItem(
    val id: Long,
    val date: String,
    val status: String,
    val duration: Int,
    @SerializedName("initial_humidity")
    val initialHumidity: Float?,
    @SerializedName("final_humidity")
    val finalHumidity: Float?,
    val mode: String,
    val confidence: Float?
)

data class WateringPredictionResponse(
    val success: Boolean,
    val prediction: WateringPredictionData?,
    val message: String?
)

data class WateringPredictionData(
    val action: String,
    val confidence: Float,
    val reason: String,
    @SerializedName("current_humidity")
    val currentHumidity: Float,
    @SerializedName("duration_seconds")
    val durationSeconds: Int,
    val timestamp: String
)

data class AiStatusResponse(
    val status: String,
    @SerializedName("ai_version")
    val aiVersion: String?,
    @SerializedName("modelos_entrenados")
    val modelosEntrenados: String, // Corregido: Es un String, no una lista
    @SerializedName("modelos_activos")
    val modelosActivos: Int,
    @SerializedName("total_plantas")
    val totalPlantas: Int,
    @SerializedName("monitoreo_activo")
    val monitoreoActivo: Boolean? = null,
    @SerializedName("scheduler_activo")
    val schedulerActivo: Boolean? = null,
    @SerializedName("plantas_con_modelo")
    val plantasConModelo: Int? = null,
    val sistema: Map<String, Any>? = null
)
