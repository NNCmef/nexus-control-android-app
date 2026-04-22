package com.example.nexuscontrol.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- Models ---
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val status: String, val user: UserDto?)
data class UserDto(val id: Int, val username: String)

data class Device(
    val id: String, 
    val name: String, 
    val active: Boolean, 
    @SerializedName("last_seen") val lastSeen: String
)
data class DevicesResponse(val devices: List<Device>)

data class MetricsResponse(val labels: List<String>, val values: List<com.google.gson.JsonElement>)

data class TerminalSendReq(
    @SerializedName("device_name") val deviceName: String, 
    val command: String
)
data class TerminalResultRes(val output: String)

// --- ApiService ---
interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body req: LoginRequest): Response<LoginResponse>

    @GET("/api/devices")
    suspend fun getDevices(): DevicesResponse

    @GET("/api/metrics/{metricName}")
    suspend fun getMetrics(
        @Path("metricName") metricName: String, 
        @Query("device") deviceId: String
    ): MetricsResponse

    @POST("/api/terminal/send")
    suspend fun sendCommand(@Body req: TerminalSendReq): Response<Unit>

    @GET("/api/terminal/result")
    suspend fun getTerminalResult(@Query("device") deviceId: String): TerminalResultRes
}

// --- Retrofit Client ---
object RetrofitClient {
    // ВАШ УКАЗАННЫЙ IP-АДРЕС API
    private const val BASE_URL = "http://81.26.178.250:8000"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
