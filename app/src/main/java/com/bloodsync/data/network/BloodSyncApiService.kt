package com.bloodsync.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ==============================================================================
// Data Transfer Objects (DTOs) for OTP Authentication API
// ==============================================================================

/**
 * Payload sent to request a 6-digit cryptographic OTP sent via Gmail SMTP.
 */
data class OtpRequestPayload(
    @SerializedName("email")
    val email: String
)

/**
 * Response received after requesting an OTP.
 */
data class OtpRequestResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: OtpRequestData? = null,

    @SerializedName("retryAfterSeconds")
    val retryAfterSeconds: Int? = null
)

data class OtpRequestData(
    @SerializedName("maskedEmail")
    val maskedEmail: String? = null,

    @SerializedName("expiresInMinutes")
    val expiresInMinutes: Int? = null
)

/**
 * Payload sent to verify the 6-digit OTP code against HMAC-SHA256 hash.
 */
data class OtpVerifyPayload(
    @SerializedName("email")
    val email: String,

    @SerializedName("otp")
    val otp: String
)

/**
 * Response received after verifying the OTP code.
 */
data class OtpVerifyResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: OtpVerifyData? = null
)

data class OtpVerifyData(
    @SerializedName("token")
    val token: String? = null,

    @SerializedName("firebaseCustomToken")
    val firebaseCustomToken: String? = null,

    @SerializedName("user")
    val user: OtpUserData? = null
)

data class OtpUserData(
    @SerializedName("email")
    val email: String? = null,

    @SerializedName("maskedEmail")
    val maskedEmail: String? = null
)

// ==============================================================================
// Retrofit API Service Interface
// ==============================================================================

interface BloodSyncApiService {

    /**
     * Request a 6-digit numeric OTP sent to the specified email address.
     * Enforces rate limiting (60s cooldown, max 3 per 10m).
     */
    @POST("auth/request-otp")
    suspend fun requestOtp(
        @Body payload: OtpRequestPayload
    ): Response<OtpRequestResponse>

    /**
     * Verify the 6-digit OTP code against the server HMAC hash.
     * Returns JWT session token and Firebase Custom Auth token on success.
     */
    @POST("auth/verify-otp")
    suspend fun verifyOtp(
        @Body payload: OtpVerifyPayload
    ): Response<OtpVerifyResponse>
}

// ==============================================================================
// Singleton Network Client
// ==============================================================================

object BloodSyncNetworkClient {

    /**
     * Standard Android emulator alias for host machine localhost:5000.
     * For physical device testing, you can update this to your LAN IP (e.g. http://192.168.1.xxx:5000/).
     */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/"

    @Volatile
    private var currentBaseUrl = DEFAULT_BASE_URL

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Volatile
    private var cachedService: BloodSyncApiService? = null

    /**
     * Provides or creates a singleton instance of [BloodSyncApiService].
     */
    fun getService(baseUrl: String = currentBaseUrl): BloodSyncApiService {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (cachedService == null || currentBaseUrl != normalized) {
            synchronized(this) {
                currentBaseUrl = normalized
                val retrofit = Retrofit.Builder()
                    .baseUrl(normalized)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                cachedService = retrofit.create(BloodSyncApiService::class.java)
            }
        }
        return cachedService!!
    }

    /**
     * Configures a custom base URL (e.g. for testing with physical hardware on local Wi-Fi).
     */
    fun setBaseUrl(newUrl: String) {
        val normalized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        if (currentBaseUrl != normalized) {
            synchronized(this) {
                currentBaseUrl = normalized
                cachedService = null
            }
        }
    }

    /**
     * Active API service handle.
     */
    val apiService: BloodSyncApiService
        get() = getService(currentBaseUrl)
}
